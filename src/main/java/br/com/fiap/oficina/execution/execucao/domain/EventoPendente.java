package br.com.fiap.oficina.execution.execucao.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Evento de integração gerado pelo agregado e ainda não entregue ao broker.
 * Fica guardado dentro do próprio documento da execução (outbox), então é
 * gravado de forma atômica com a mudança de estado que o gerou.
 */
public record EventoPendente(UUID id, String routingKey, Map<String, Object> dados, String correlationId,
        Instant criadoEm) {

    public static final String DIAGNOSTICO_INICIADO = "execucao.diagnostico-iniciado";
    public static final String DIAGNOSTICO_CONCLUIDO = "execucao.diagnostico-concluido";
    public static final String EXECUCAO_INICIADA = "execucao.iniciada";
    public static final String SERVICO_INICIADO = "execucao.servico-iniciado";
    public static final String SERVICO_FINALIZADO = "execucao.servico-finalizado";
    public static final String EXECUCAO_CONCLUIDA = "execucao.concluida";
    public static final String EXECUCAO_FALHOU = "execucao.falhou";

    public EventoPendente comCorrelationId(String novoCorrelationId) {
        return new EventoPendente(id, routingKey, dados, novoCorrelationId, criadoEm);
    }
}
