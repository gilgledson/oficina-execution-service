package br.com.fiap.oficina.execution.execucao.domain;

import java.util.UUID;

public record RelatorioTempoMedioServico(UUID servicoId, String nomeServico, Integer quantidadeExecucoesHistoricas,
        Long tempoMedioMinutos) {
}
