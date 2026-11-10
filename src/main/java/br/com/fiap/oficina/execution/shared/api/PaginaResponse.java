package br.com.fiap.oficina.execution.shared.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import br.com.fiap.oficina.execution.shared.domain.Pagina;

import java.util.List;

public record PaginaResponse<T>(
        @JsonProperty("itens") List<T> itens,
        @JsonProperty("pagina_atual") int paginaAtual,
        @JsonProperty("tamanho_pagina") int tamanhoPagina,
        @JsonProperty("total_paginas") int totalPaginas,
        @JsonProperty("total_elementos") long totalElementos) {
    public static <T> PaginaResponse<T> fromDomain(Pagina<T> pagina) {
        return new PaginaResponse<>(pagina.itens(), pagina.paginaAtual(), pagina.tamanhoPagina(), pagina.totalPaginas(),
                pagina.totalElementos());
    }
}






