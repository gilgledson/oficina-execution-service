package br.com.fiap.oficina.execution.funcionario.application.dto;

import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import java.time.LocalDateTime;
import java.util.UUID;

public record FuncionarioOutput(
    UUID id,
    UUID usuarioId,
    String nome,
    String sobrenome,
    String cpf,
    String telefone,
    CargoFuncionario cargo,
    boolean ativo,
    LocalDateTime deletadoEm
) {}






