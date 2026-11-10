package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.application.mapper.FuncionarioOutputMapper;
import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import jakarta.ws.rs.NotFoundException;
import java.util.UUID;

public class EditarFuncionarioUseCaseImpl implements EditarFuncionarioUseCase {

    private final FuncionarioRepository repository;

    public EditarFuncionarioUseCaseImpl(FuncionarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public FuncionarioOutput executar(UUID id, String nome, String sobrenome, String cpf, String telefone, CargoFuncionario cargo) {
        Funcionario funcionario = repository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Funcionário não encontrado"));

        Funcionario dadosAtualizados = Funcionario.builder()
                .nome(nome)
                .sobrenome(sobrenome)
                .cpf(cpf)
                .telefone(telefone)
                .cargo(cargo)
                .build();

        funcionario.atualizar(dadosAtualizados);
        repository.atualizar(funcionario);

        return FuncionarioOutputMapper.toOutput(funcionario);
    }
}






