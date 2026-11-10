package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.application.mapper.FuncionarioOutputMapper;
import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import java.util.UUID;

public class CadastrarFuncionarioUseCaseImpl implements CadastrarFuncionarioUseCase {

    private final FuncionarioRepository repository;

    public CadastrarFuncionarioUseCaseImpl(FuncionarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public FuncionarioOutput executar(UUID usuarioId, String nome, String sobrenome, String cpf, String telefone, CargoFuncionario cargo) {
        // Valida se já existe funcionário com o mesmo CPF
        repository.buscarPorCpf(cpf).ifPresent(c -> {
            throw new IllegalArgumentException("Já existe um funcionário cadastrado com este CPF");
        });

        // Valida se já existe funcionário com o mesmo usuarioId
        if (usuarioId != null) {
            repository.buscarPorUsuarioId(usuarioId).ifPresent(c -> {
                throw new IllegalArgumentException("Já existe um funcionário vinculado a este usuário");
            });
        }

        Funcionario funcionario = new Funcionario(usuarioId, nome, sobrenome, cpf, telefone, cargo);
        repository.salvar(funcionario);
        return FuncionarioOutputMapper.toOutput(funcionario);
    }
}






