package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import java.util.UUID;

public interface CadastrarFuncionarioUseCase {
    FuncionarioOutput executar(UUID usuarioId, String nome, String sobrenome, String cpf, String telefone, CargoFuncionario cargo);
}






