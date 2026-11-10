package br.com.fiap.oficina.execution.execucao.domain;

import java.util.UUID;

public record RelatorioEsforcoOs(UUID ordemDeServicoId, Integer totalServicosRealizados, Long esforcoTotalMinutos) {
}
