package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import jakarta.ws.rs.NotFoundException;
import java.util.UUID;

public class DeletarFuncionarioUseCaseImpl implements DeletarFuncionarioUseCase {

    private final FuncionarioRepository repository;

    public DeletarFuncionarioUseCaseImpl(FuncionarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public void executar(UUID id) {
        var funcionario = repository.buscarPorId(id)
                .orElseThrow(() -> new NotFoundException("Funcionário não encontrado"));

        funcionario.desativar();
        repository.atualizar(funcionario);
    }
}






