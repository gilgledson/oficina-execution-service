package br.com.fiap.oficina.execution.shared.domain;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Pagina<T>(
        List<T> itens,
        int paginaAtual,
        int tamanhoPagina,
        int totalPaginas,
        long totalElementos) {

    public <R> Pagina<R> map(Function<T, R> mapper) {
        List<R> novasItens = itens.stream().map(mapper).collect(Collectors.toList());
        return new Pagina<>(novasItens, paginaAtual, tamanhoPagina, totalPaginas, totalElementos);
    }
}






