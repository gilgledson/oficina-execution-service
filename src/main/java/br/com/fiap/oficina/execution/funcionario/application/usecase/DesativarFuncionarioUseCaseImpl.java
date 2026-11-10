package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import jakarta.ws.rs.NotFoundException;
import java.util.UUID;

public class DesativarFuncionarioUseCaseImpl implements DesativarFuncionarioUseCase {

    private final FuncionarioRepository repository;

    public DesativarFuncionarioUseCaseImpl(FuncionarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public void executar(UUID id) {
        Funcionario funcionario = repository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Funcionário não encontrado"));

        funcionario.desativar();
        repository.atualizar(funcionario);
    }
}






