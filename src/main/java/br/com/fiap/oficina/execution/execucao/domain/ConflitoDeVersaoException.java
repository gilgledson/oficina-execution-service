package br.com.fiap.oficina.execution.execucao.domain;

/** Outra requisição alterou a mesma execução ao mesmo tempo; quem perdeu deve tentar de novo. */
public class ConflitoDeVersaoException extends IllegalStateException {

    public ConflitoDeVersaoException() {
        super("A execução desta ordem foi alterada por outra operação. Tente novamente.");
    }
}
