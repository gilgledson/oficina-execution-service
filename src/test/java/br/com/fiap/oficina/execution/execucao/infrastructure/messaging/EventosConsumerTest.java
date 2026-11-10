package br.com.fiap.oficina.execution.execucao.infrastructure.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logmanager.MDC;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.execution.execucao.application.listener.EventosExternosListener;
import br.com.fiap.oficina.execution.shared.web.CorrelationIdFilter;
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class EventosConsumerTest {

    @Mock
    private EventosExternosListener listener;

    @InjectMocks
    private EventosConsumer consumer;

    private final JsonObject payload = new JsonObject().put("ordemServicoId", UUID.randomUUID().toString());
    private final AtomicBoolean ack = new AtomicBoolean();
    private final AtomicBoolean nack = new AtomicBoolean();

    private Message<JsonObject> mensagem(String routingKey, String correlationId) {
        IncomingRabbitMQMetadata metadata = mock(IncomingRabbitMQMetadata.class);
        when(metadata.getRoutingKey()).thenReturn(routingKey);
        when(metadata.getHeader(CorrelationIdFilter.HEADER, String.class))
                .thenReturn(Optional.ofNullable(correlationId));
        return Message.of(payload, Metadata.of(metadata),
                () -> {
                    ack.set(true);
                    return CompletableFuture.completedFuture(null);
                },
                falha -> {
                    nack.set(true);
                    return CompletableFuture.completedFuture(null);
                });
    }

    @Test
    @DisplayName("Cada routing key é entregue ao método certo do listener")
    void deveDespacharPorRoutingKey() {
        consumer.consumir(mensagem(EventosConsumer.ORDEM_ABERTA, null));
        consumer.consumir(mensagem(EventosConsumer.ORDEM_APROVADA, null));
        consumer.consumir(mensagem(EventosConsumer.ORDEM_CANCELADA, null));
        consumer.consumir(mensagem(EventosConsumer.ORCAMENTO_REJEITADO, null));
        consumer.consumir(mensagem(EventosConsumer.ORCAMENTO_FALHOU, null));

        verify(listener).onOrdemAberta(payload);
        verify(listener).onOrdemAprovada(payload);
        verify(listener).onOrdemCancelada(payload);
        verify(listener).onOrcamentoRejeitado(payload);
        verify(listener).onOrcamentoFalhou(payload);
        assertTrue(ack.get());
        assertFalse(nack.get());
    }

    @Test
    @DisplayName("Evento desconhecido deve ser confirmado e ignorado")
    void deveIgnorarEventoDesconhecido() {
        consumer.consumir(mensagem("billing.orcamento-gerado", null));

        verifyNoInteractions(listener);
        assertTrue(ack.get());
    }

    @Test
    @DisplayName("Falha no processamento deve rejeitar a mensagem (DLQ)")
    void deveRejeitarMensagemQuandoProcessamentoFalha() {
        doThrow(new IllegalStateException("falha")).when(listener).onOrdemAprovada(any());

        consumer.consumir(mensagem(EventosConsumer.ORDEM_APROVADA, null));

        assertTrue(nack.get());
        assertFalse(ack.get());
    }

    @Test
    @DisplayName("Deve propagar o correlationId do cabeçalho para o log e limpar ao final")
    void devePropagarCorrelationId() {
        AtomicReference<String> duranteProcessamento = new AtomicReference<>();
        doAnswer(invocation -> {
            duranteProcessamento.set(MDC.get(CorrelationIdFilter.MDC_KEY));
            return null;
        }).when(listener).onOrdemAberta(any());

        consumer.consumir(mensagem(EventosConsumer.ORDEM_ABERTA, "corr-123"));

        assertEquals("corr-123", duranteProcessamento.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    @DisplayName("Mensagem sem metadados do RabbitMQ deve ser ignorada")
    void deveIgnorarMensagemSemMetadados() {
        consumer.consumir(Message.of(payload));

        verifyNoInteractions(listener);
    }
}
