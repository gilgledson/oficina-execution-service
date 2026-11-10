package br.com.fiap.oficina.execution.execucao.application.listener;

import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.execution.execucao.application.dto.ServicoAprovado;
import br.com.fiap.oficina.execution.execucao.application.usecase.GerenciarExecucaoUseCase;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

@ExtendWith(MockitoExtension.class)
class EventosExternosListenerTest {

    @Mock
    private GerenciarExecucaoUseCase gerenciarExecucao;

    @InjectMocks
    private EventosExternosListener listener;

    private final UUID osId = UUID.randomUUID();

    private JsonObject evento() {
        return new JsonObject().put("ordemServicoId", osId.toString());
    }

    @Test
    @DisplayName("OS aberta entra na fila de diagnóstico")
    void deveRegistrarOrdemAberta() {
        listener.onOrdemAberta(evento());

        verify(gerenciarExecucao).registrarNaFila(osId);
    }

    @Test
    @DisplayName("OS aprovada entra na fila de execução com os serviços do evento")
    void deveEnfileirarOrdemAprovada() {
        UUID servicoOsId = UUID.randomUUID();
        UUID servicoId = UUID.randomUUID();
        JsonObject json = evento().put("servicos", new JsonArray().add(new JsonObject()
                .put("id", servicoOsId.toString())
                .put("servicoId", servicoId.toString())
                .put("nome", "Troca de óleo")));

        listener.onOrdemAprovada(json);

        verify(gerenciarExecucao).enfileirarParaExecucao(osId,
                List.of(new ServicoAprovado(servicoOsId, servicoId, "Troca de óleo")));
    }

    @Test
    @DisplayName("OS aprovada sem lista de serviços repassa lista vazia (o domínio rejeita)")
    void deveRepassarListaVaziaQuandoEventoSemServicos() {
        listener.onOrdemAprovada(evento());

        verify(gerenciarExecucao).enfileirarParaExecucao(osId, List.of());
    }

    @Test
    @DisplayName("OS cancelada usa o motivo do evento, ou um padrão")
    void deveCancelarOrdem() {
        listener.onOrdemCancelada(evento().put("motivo", "Cliente desistiu"));
        listener.onOrdemCancelada(evento());

        verify(gerenciarExecucao).cancelar(osId, "Cliente desistiu");
        verify(gerenciarExecucao).cancelar(osId, "OS cancelada");
    }

    @Test
    @DisplayName("Orçamento rejeitado e orçamento não gerado tiram a OS da fila")
    void deveCompensarEventosDoBilling() {
        listener.onOrcamentoRejeitado(evento());
        listener.onOrcamentoFalhou(evento());

        verify(gerenciarExecucao).cancelar(osId, "Orçamento rejeitado pelo cliente");
        verify(gerenciarExecucao).cancelar(osId, "Falha ao gerar o orçamento");
    }
}
