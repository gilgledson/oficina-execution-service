package br.com.fiap.oficina.execution.execucao.application.dto;

import java.util.UUID;

/** Serviço aceito pelo cliente, como chega no evento de aprovação da OS. */
public record ServicoAprovado(UUID servicoOsId, UUID servicoId, String nome) {
}
