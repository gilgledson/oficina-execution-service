package br.com.fiap.oficina.execution.execucao.infrastructure.messaging;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.jboss.logging.Logger;

import br.com.fiap.oficina.execution.execucao.infrastructure.persistence.OrdemExecucaoDocument;
import br.com.fiap.oficina.execution.execucao.infrastructure.persistence.OrdemExecucaoDocument.EventoDoc;
import br.com.fiap.oficina.execution.execucao.infrastructure.persistence.OrdemExecucaoRepositoryImpl;
import br.com.fiap.oficina.execution.shared.web.CorrelationIdFilter;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Entrega ao RabbitMQ os eventos pendentes guardados nos documentos de
 * execução. Um evento só sai da outbox depois que o broker confirma o
 * recebimento; se a entrega falhar ele é reenviado no próximo ciclo. A
 * garantia é "pelo menos uma vez", por isso os consumidores são idempotentes.
 */
@ApplicationScoped
public class OutboxRelay {

    private static final Logger LOG = Logger.getLogger(OutboxRelay.class);
    private static final int LOTE = 50;
    private static final long TIMEOUT_CONFIRMACAO_SEGUNDOS = 5;

    private final OrdemExecucaoRepositoryImpl repository;
    private final Emitter<JsonObject> emitter;

    public OutboxRelay(OrdemExecucaoRepositoryImpl repository,
            @Channel("execution-events") Emitter<JsonObject> emitter) {
        this.repository = repository;
        this.emitter = emitter;
    }

    @Scheduled(every = "${oficina.outbox.intervalo:2s}", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void publicarPendentes() {
        List<OrdemExecucaoDocument> execucoes = repository.buscarComEventosPendentes(LOTE);
        for (OrdemExecucaoDocument execucao : execucoes) {
            // Os eventos de uma mesma OS saem na ordem em que foram gerados; se
            // um falhar, os seguintes dessa OS esperam o próximo ciclo.
            for (EventoDoc evento : execucao.outbox) {
                if (!publicar(evento)) {
                    break;
                }
                repository.removerEvento(execucao.ordemServicoId, evento.eventoId);
            }
        }
    }

    private boolean publicar(EventoDoc evento) {
        CompletableFuture<Void> confirmacao = new CompletableFuture<>();

        OutgoingRabbitMQMetadata.Builder metadata = OutgoingRabbitMQMetadata.builder()
                .withRoutingKey(evento.routingKey)
                .withMessageId(evento.eventoId.toString());
        if (evento.correlationId != null) {
            metadata.withCorrelationId(evento.correlationId)
                    .withHeader(CorrelationIdFilter.HEADER, evento.correlationId);
        }

        Message<JsonObject> mensagem = Message.of(new JsonObject(evento.payload), Metadata.of(metadata.build()),
                () -> {
                    confirmacao.complete(null);
                    return CompletableFuture.completedFuture(null);
                },
                falha -> {
                    confirmacao.completeExceptionally(falha);
                    return CompletableFuture.completedFuture(null);
                });

        try {
            emitter.send(mensagem);
            confirmacao.get(TIMEOUT_CONFIRMACAO_SEGUNDOS, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            LOG.warnf("Evento %s (%s) não entregue ao broker; será reenviado: %s", evento.eventoId,
                    evento.routingKey, e.getMessage());
            return false;
        }
    }
}
