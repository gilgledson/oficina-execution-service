package br.com.fiap.oficina.execution.execucao.application.usecase;

import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;

public enum TipoFila {
    DIAGNOSTICO(FaseExecucao.FILA_DIAGNOSTICO),
    EXECUCAO(FaseExecucao.FILA_EXECUCAO);

    private final FaseExecucao fase;

    TipoFila(FaseExecucao fase) {
        this.fase = fase;
    }

    public FaseExecucao fase() {
        return fase;
    }
}
