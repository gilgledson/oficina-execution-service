package br.com.fiap.oficina.execution.execucao.application.listener;

import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina.execution.execucao.application.dto.ServicoAprovado;
import br.com.fiap.oficina.execution.execucao.application.usecase.GerenciarExecucaoUseCase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Reage aos eventos do OS Service e do Billing que interessam à execução.
 * Quem entrega as mensagens é o consumidor do broker
 * ({@code infrastructure.messaging.EventosConsumer}).
 */
@ApplicationScoped
@RequiredArgsConstructor
public class EventosExternosListener {

    private final GerenciarExecucaoUseCase gerenciarExecucao;

    /** OS aberta: entra na fila de diagnóstico. */
    public void onOrdemAberta(JsonObject json) {
        gerenciarExecucao.registrarNaFila(ordemServicoId(json));
    }

    /** OS aprovada pelo cliente: entra na fila de execução com os serviços aceitos. */
    public void onOrdemAprovada(JsonObject json) {
        JsonArray servicos = json.getJsonArray("servicos", new JsonArray());
        List<ServicoAprovado> aprovados = servicos.stream()
                .map(JsonObject.class::cast)
                .map(s -> new ServicoAprovado(UUID.fromString(s.getString("id")),
                        UUID.fromString(s.getString("servicoId")), s.getString("nome")))
                .toList();
        gerenciarExecucao.enfileirarParaExecucao(ordemServicoId(json), aprovados);
    }

    /** Compensação: a OS foi cancelada. */
    public void onOrdemCancelada(JsonObject json) {
        gerenciarExecucao.cancelar(ordemServicoId(json), json.getString("motivo", "OS cancelada"));
    }

    /** Compensação: o cliente rejeitou o orçamento. */
    public void onOrcamentoRejeitado(JsonObject json) {
        gerenciarExecucao.cancelar(ordemServicoId(json), "Orçamento rejeitado pelo cliente");
    }

    /** Compensação: o Billing não conseguiu gerar o orçamento. */
    public void onOrcamentoFalhou(JsonObject json) {
        gerenciarExecucao.cancelar(ordemServicoId(json), "Falha ao gerar o orçamento");
    }

    private static UUID ordemServicoId(JsonObject json) {
        return UUID.fromString(json.getString("ordemServicoId"));
    }
}
