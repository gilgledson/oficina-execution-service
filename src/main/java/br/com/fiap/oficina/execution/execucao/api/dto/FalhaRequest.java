package br.com.fiap.oficina.execution.execucao.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FalhaRequest(@NotBlank(message = "O motivo da falha é obrigatório") String motivo) {
}
