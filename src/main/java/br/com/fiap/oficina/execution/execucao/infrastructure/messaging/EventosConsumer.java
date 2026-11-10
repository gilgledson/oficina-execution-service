package br.com.fiap.oficina.execution.execucao.infrastructure.messaging;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import org.jboss.logmanager.MDC;

import br.com.fiap.oficina.execution.execucao.application.listener.EventosExternosListener;
import br.com.fiap.oficina.execution.shared.web.CorrelationIdFilter;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Consome da fila do Execution Service os eventos do OS Service e do Billing
 * e entrega ao listener da aplicação conforme a routing key.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class EventosConsumer {

    public static final String ORDEM_ABERTA = "os.aberta";
    public static final String ORDEM_APROVADA = "os.aprovada";
    public static final String ORDEM_CANCELADA = "os.cancelada";
    public static final String ORCAMENTO_REJEITADO = "billing.orcamento-rejeitado";
    public static final String ORCAMENTO_FALHOU = "billing.orcamento-falhou";

    private static final Logger LOG = Logger.getLogger(EventosConsumer.class);

    private final EventosExternosListener listener;

    @Incoming("eventos-externos")
    @Blocking
    public CompletionStage<Void> consumir(Message<JsonObject> mensagem) {
        Optional<IncomingRabbitMQMetadata> metadata = mensagem.getMetadata(IncomingRabbitMQMetadata.class);
        String routingKey = metadata.map(IncomingRabbitMQMetadata::getRoutingKey).orElse("");
        String correlationId = metadata
                .flatMap(m -> m.getHeader(CorrelationIdFilter.HEADER, String.class))
                .orElse(null);

        if (correlationId != null) {
            MDC.put(CorrelationIdFilter.MDC_KEY, correlationId);
        }
        try {
            despachar(routingKey, mensagem.getPayload());
            return mensagem.ack();
        } catch (RuntimeException e) {
            // nack: a mensagem vai para a fila de mensagens mortas (DLQ) em vez
            // de ser perdida ou reprocessada em loop.
            LOG.errorf(e, "Falha ao processar evento %s", routingKey);
            return mensagem.nack(e);
        } finally {
            MDC.remove(CorrelationIdFilter.MDC_KEY);
        }
    }

    void despachar(String routingKey, JsonObject payload) {
        switch (routingKey) {
            case ORDEM_ABERTA -> listener.onOrdemAberta(payload);
            case ORDEM_APROVADA -> listener.onOrdemAprovada(payload);
            case ORDEM_CANCELADA -> listener.onOrdemCancelada(payload);
            case ORCAMENTO_REJEITADO -> listener.onOrcamentoRejeitado(payload);
            case ORCAMENTO_FALHOU -> listener.onOrcamentoFalhou(payload);
            default -> LOG.debugf("Evento %s ignorado pelo Execution Service", routingKey);
        }
    }
}
