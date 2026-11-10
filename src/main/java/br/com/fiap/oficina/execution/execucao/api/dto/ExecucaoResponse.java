package br.com.fiap.oficina.execution.execucao.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;

public record ExecucaoResponse(UUID ordemServicoId, String fase, Instant entradaNaFila, Instant inicioDiagnostico,
        Instant fimDiagnostico, Instant inicioExecucao, Instant fimExecucao, String motivoEncerramento,
        List<ServicoResponse> servicos, List<FaseResponse> historico) {

    public record ServicoResponse(UUID servicoOsId, UUID servicoId, String nome, String status, Instant inicio,
            Instant fim, UUID executorId) {
    }

    public record FaseResponse(String fase, Instant em, UUID usuarioId) {
    }

    public static ExecucaoResponse fromDomain(OrdemExecucao e) {
        return new ExecucaoResponse(e.getOrdemServicoId(), e.getFase().name(), e.getEntradaNaFila(),
                e.getInicioDiagnostico(), e.getFimDiagnostico(), e.getInicioExecucao(), e.getFimExecucao(),
                e.getMotivoEncerramento(),
                e.getServicos().stream()
                        .map(s -> new ServicoResponse(s.getServicoOsId(), s.getServicoId(), s.getNome(),
                                s.getStatus().name(), s.getInicio(), s.getFim(), s.getExecutorId()))
                        .toList(),
                e.getHistorico().stream()
                        .map(h -> new FaseResponse(h.fase().name(), h.em(), h.usuarioId()))
                        .toList());
    }
}
