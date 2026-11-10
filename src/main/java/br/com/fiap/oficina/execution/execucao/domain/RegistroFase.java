package br.com.fiap.oficina.execution.execucao.domain;

import java.time.Instant;
import java.util.UUID;

/** Entrada do histórico: quando a OS entrou em uma fase e quem provocou. */
public record RegistroFase(FaseExecucao fase, Instant em, UUID usuarioId) {
}
