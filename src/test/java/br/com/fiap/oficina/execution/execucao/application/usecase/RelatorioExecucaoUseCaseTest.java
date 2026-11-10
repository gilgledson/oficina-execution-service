package br.com.fiap.oficina.execution.execucao.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import br.com.fiap.oficina.execution.execucao.domain.RelatorioEsforcoOs;
import br.com.fiap.oficina.execution.execucao.domain.RelatorioTempoMedioServico;
import br.com.fiap.oficina.execution.execucao.domain.ServicoExecucao;
import br.com.fiap.oficina.execution.execucao.domain.StatusServico;

@ExtendWith(MockitoExtension.class)
class RelatorioExecucaoUseCaseTest {

    private static final Instant T0 = Instant.parse("2026-01-10T12:00:00Z");
    private static final UUID TROCA_OLEO = UUID.randomUUID();
    private static final UUID ALINHAMENTO = UUID.randomUUID();

    @Mock
    private OrdemExecucaoRepository repository;

    @InjectMocks
    private RelatorioExecucaoUseCase useCase;

    private static ServicoExecucao finalizado(UUID servicoId, String nome, long minutos) {
        return new ServicoExecucao(UUID.randomUUID(), servicoId, nome, StatusServico.FINALIZADO, T0,
                T0.plusSeconds(minutos * 60), UUID.randomUUID());
    }

    private static ServicoExecucao emAndamento(UUID servicoId, String nome) {
        return new ServicoExecucao(UUID.randomUUID(), servicoId, nome, StatusServico.EM_EXECUCAO, T0, null,
                UUID.randomUUID());
    }

    private static OrdemExecucao execucao(UUID osId, ServicoExecucao... servicos) {
        return OrdemExecucao.builder().ordemServicoId(osId).fase(FaseExecucao.EM_EXECUCAO)
                .servicos(List.of(servicos)).versao(1).build();
    }

    @Test
    @DisplayName("Esforço da OS soma só os serviços finalizados")
    void deveCalcularEsforcoDaOs() {
        UUID osId = UUID.randomUUID();
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(execucao(osId,
                finalizado(TROCA_OLEO, "Troca de óleo", 30),
                finalizado(ALINHAMENTO, "Alinhamento", 45),
                emAndamento(ALINHAMENTO, "Alinhamento"))));

        Optional<RelatorioEsforcoOs> relatorio = useCase.esforcoPorOs(osId);

        assertEquals(Optional.of(new RelatorioEsforcoOs(osId, 2, 75L)), relatorio);
    }

    @Test
    @DisplayName("OS sem serviço finalizado ou desconhecida não tem relatório de esforço")
    void naoDeveGerarEsforcoSemServicosFinalizados() {
        UUID semFinalizados = UUID.randomUUID();
        UUID desconhecida = UUID.randomUUID();
        when(repository.buscarPorOrdemServicoId(semFinalizados))
                .thenReturn(Optional.of(execucao(semFinalizados, emAndamento(TROCA_OLEO, "Troca de óleo"))));
        when(repository.buscarPorOrdemServicoId(desconhecida)).thenReturn(Optional.empty());

        assertTrue(useCase.esforcoPorOs(semFinalizados).isEmpty());
        assertTrue(useCase.esforcoPorOs(desconhecida).isEmpty());
    }

    @Test
    @DisplayName("Tempo médio agrupa por serviço do catálogo entre todas as OS")
    void deveCalcularTempoMedioPorServico() {
        when(repository.listarComServicosFinalizados()).thenReturn(List.of(
                execucao(UUID.randomUUID(), finalizado(TROCA_OLEO, "Troca de óleo", 30),
                        finalizado(ALINHAMENTO, "Alinhamento", 40)),
                execucao(UUID.randomUUID(), finalizado(TROCA_OLEO, "Troca de óleo", 45),
                        emAndamento(ALINHAMENTO, "Alinhamento"))));

        List<RelatorioTempoMedioServico> relatorio = useCase.tempoMedioPorServico();

        // Troca de óleo: (30 + 45) / 2 = 37,5 -> 38; ordenado por nome.
        assertEquals(List.of(
                new RelatorioTempoMedioServico(ALINHAMENTO, "Alinhamento", 1, 40L),
                new RelatorioTempoMedioServico(TROCA_OLEO, "Troca de óleo", 2, 38L)), relatorio);
    }

    @Test
    @DisplayName("Sem execuções finalizadas o relatório de tempo médio é vazio")
    void deveRetornarTempoMedioVazio() {
        when(repository.listarComServicosFinalizados()).thenReturn(List.of());

        assertTrue(useCase.tempoMedioPorServico().isEmpty());
    }
}
