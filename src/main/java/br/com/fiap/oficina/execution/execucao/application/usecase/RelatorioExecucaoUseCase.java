package br.com.fiap.oficina.execution.execucao.application.usecase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.RelatorioEsforcoOs;
import br.com.fiap.oficina.execution.execucao.domain.RelatorioTempoMedioServico;
import br.com.fiap.oficina.execution.execucao.domain.ServicoExecucao;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;

/**
 * Relatórios operacionais. No monólito eram views SQL sobre as tabelas da OS;
 * aqui são calculados a partir dos tempos registrados pelo próprio serviço.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class RelatorioExecucaoUseCase {

    private final OrdemExecucaoRepository repository;

    /** Quantos serviços da OS foram realizados e quanto tempo levaram, somados. */
    public Optional<RelatorioEsforcoOs> esforcoPorOs(UUID ordemServicoId) {
        return repository.buscarPorOrdemServicoId(ordemServicoId)
                .map(execucao -> execucao.getServicos().stream().filter(ServicoExecucao::isFinalizado).toList())
                .filter(finalizados -> !finalizados.isEmpty())
                .map(finalizados -> new RelatorioEsforcoOs(ordemServicoId, finalizados.size(),
                        emMinutos(finalizados.stream().mapToLong(s -> s.duracao().getSeconds()).sum())));
    }

    /** Tempo médio histórico de execução de cada serviço do catálogo. */
    public List<RelatorioTempoMedioServico> tempoMedioPorServico() {
        Map<UUID, List<ServicoExecucao>> porServico = new LinkedHashMap<>();
        repository.listarComServicosFinalizados().stream()
                .flatMap(execucao -> execucao.getServicos().stream())
                .filter(ServicoExecucao::isFinalizado)
                .forEach(s -> porServico.computeIfAbsent(s.getServicoId(), id -> new ArrayList<>()).add(s));

        return porServico.entrySet().stream()
                .map(entrada -> {
                    List<ServicoExecucao> execucoes = entrada.getValue();
                    double mediaSegundos = execucoes.stream().mapToLong(s -> s.duracao().getSeconds()).average()
                            .orElse(0);
                    return new RelatorioTempoMedioServico(entrada.getKey(), execucoes.get(0).getNome(),
                            execucoes.size(), Math.round(mediaSegundos / 60.0));
                })
                .sorted(Comparator.comparing(RelatorioTempoMedioServico::nomeServico))
                .toList();
    }

    private static long emMinutos(long segundos) {
        return Math.round(segundos / 60.0);
    }
}
