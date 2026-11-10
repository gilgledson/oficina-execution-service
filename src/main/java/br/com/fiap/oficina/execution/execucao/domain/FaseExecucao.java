package br.com.fiap.oficina.execution.execucao.domain;

/** Fases pelas quais uma OS passa dentro da oficina, da fila à conclusão. */
public enum FaseExecucao {
    FILA_DIAGNOSTICO,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    FILA_EXECUCAO,
    EM_EXECUCAO,
    CONCLUIDA,
    FALHOU,
    CANCELADA;

    public boolean isEncerrada() {
        return this == CONCLUIDA || this == FALHOU || this == CANCELADA;
    }
}
