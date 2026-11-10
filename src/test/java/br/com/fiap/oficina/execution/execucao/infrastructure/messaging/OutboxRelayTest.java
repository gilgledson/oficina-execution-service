package br.com.fiap.oficina.execution.execucao.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.execution.execucao.infrastructure.persistence.OrdemExecucaoDocument;
import br.com.fiap.oficina.execution.execucao.infrastructure.persistence.OrdemExecucaoDocument.EventoDoc;
import br.com.fiap.oficina.execution.execucao.infrastructure.persistence.OrdemExecucaoRepositoryImpl;
import br.com.fiap.oficina.execution.shared.web.CorrelationIdFilter;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OrdemExecucaoRepositoryImpl repository;

    @Mock
    private Emitter<JsonObject> emitter;

    private OutboxRelay relay;
    private final List<Message<JsonObject>> enviadas = new ArrayList<>();

    @BeforeEach
    void setUp() {
        relay = new OutboxRelay(repository, emitter);
    }

    private static EventoDoc evento(String routingKey, String correlationId) {
        EventoDoc evento = new EventoDoc();
        evento.eventoId = UUID.randomUUID();
        evento.routingKey = routingKey;
        evento.payload = new JsonObject().put("ordemServicoId", "abc").encode();
        evento.correlationId = correlationId;
        evento.criadoEm = Instant.now();
        return evento;
    }

    private static OrdemExecucaoDocument execucao(EventoDoc... eventos) {
        OrdemExecucaoDocument doc = new OrdemExecucaoDocument();
        doc.ordemServicoId = UUID.randomUUID();
        doc.outbox = List.of(eventos);
        return doc;
    }

    /** Simula o broker: confirma (ack) ou recusa (nack) cada mensagem enviada. */
    @SuppressWarnings("unchecked")
    private void brokerResponde(boolean confirma) {
        doAnswer(invocation -> {
            Message<JsonObject> mensagem = invocation.getArgument(0);
            enviadas.add(mensagem);
            if (confirma) {
                mensagem.ack();
            } else {
                mensagem.nack(new IllegalStateException("broker indisponível"));
            }
            return null;
        }).when(emitter).send(any(Message.class));
    }

    @Test
    @DisplayName("Deve enviar o evento com routing key, id e correlationId e tirá-lo da outbox")
    void devePublicarEventoPendente() {
        EventoDoc evento = evento("execucao.concluida", "corr-123");
        OrdemExecucaoDocument doc = execucao(evento);
        when(repository.buscarComEventosPendentes(anyInt())).thenReturn(List.of(doc));
        brokerResponde(true);

        relay.publicarPendentes();

        Message<JsonObject> mensagem = enviadas.get(0);
        assertEquals("abc", mensagem.getPayload().getString("ordemServicoId"));
        OutgoingRabbitMQMetadata metadata = mensagem.getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow();
        assertEquals("execucao.concluida", metadata.getRoutingKey());
        assertEquals(evento.eventoId.toString(), metadata.getMessageId());
        assertEquals("corr-123", metadata.getCorrelationId());
        assertEquals("corr-123", metadata.getHeaders().get(CorrelationIdFilter.HEADER));
        verify(repository).removerEvento(doc.ordemServicoId, evento.eventoId);
    }

    @Test
    @DisplayName("Eventos da mesma OS saem na ordem; evento sem correlationId vai sem o cabeçalho")
    void devePublicarNaOrdem() {
        EventoDoc primeiro = evento("execucao.iniciada", null);
        EventoDoc segundo = evento("execucao.concluida", null);
        OrdemExecucaoDocument doc = execucao(primeiro, segundo);
        when(repository.buscarComEventosPendentes(anyInt())).thenReturn(List.of(doc));
        brokerResponde(true);

        relay.publicarPendentes();

        assertEquals(2, enviadas.size());
        OutgoingRabbitMQMetadata metadata = enviadas.get(0).getMetadata(OutgoingRabbitMQMetadata.class).orElseThrow();
        assertEquals("execucao.iniciada", metadata.getRoutingKey());
        assertNull(metadata.getCorrelationId());
        verify(repository).removerEvento(doc.ordemServicoId, primeiro.eventoId);
        verify(repository).removerEvento(doc.ordemServicoId, segundo.eventoId);
    }

    @Test
    @DisplayName("Broker recusou: os eventos dessa OS ficam na outbox, mas as outras OS seguem")
    void deveManterPendenteQuandoBrokerRecusa() {
        OrdemExecucaoDocument comFalha = execucao(evento("execucao.iniciada", null),
                evento("execucao.concluida", null));
        OrdemExecucaoDocument outra = execucao(evento("execucao.falhou", null));
        when(repository.buscarComEventosPendentes(anyInt())).thenReturn(List.of(comFalha, outra));
        brokerResponde(false);

        relay.publicarPendentes();

        // 1 tentativa da primeira OS (o segundo evento dela espera) + 1 da outra OS
        assertEquals(2, enviadas.size());
        verify(repository, never()).removerEvento(any(), any());
    }

    @Test
    @DisplayName("Falha ao enviar (emitter) também mantém o evento na outbox")
    @SuppressWarnings("unchecked")
    void deveManterPendenteQuandoEnvioFalha() {
        when(repository.buscarComEventosPendentes(anyInt()))
                .thenReturn(List.of(execucao(evento("execucao.iniciada", null))));
        doThrow(new IllegalStateException("sem conexão")).when(emitter).send(any(Message.class));

        relay.publicarPendentes();

        verify(repository, never()).removerEvento(any(), any());
    }
}
