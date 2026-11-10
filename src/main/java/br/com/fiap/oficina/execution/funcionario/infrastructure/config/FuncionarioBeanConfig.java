package br.com.fiap.oficina.execution.funcionario.infrastructure.config;

import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.application.usecase.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class FuncionarioBeanConfig {

    @Produces
    @ApplicationScoped
    public CadastrarFuncionarioUseCase cadastrarFuncionarioUseCase(FuncionarioRepository repository) {
        return new CadastrarFuncionarioUseCaseImpl(repository);
    }

    @Produces
    @ApplicationScoped
    public ListarFuncionariosUseCase listarFuncionariosUseCase(FuncionarioRepository repository) {
        return new ListarFuncionariosUseCaseImpl(repository);
    }

    @Produces
    @ApplicationScoped
    public EditarFuncionarioUseCase editarFuncionarioUseCase(FuncionarioRepository repository) {
        return new EditarFuncionarioUseCaseImpl(repository);
    }

    @Produces
    @ApplicationScoped
    public DesativarFuncionarioUseCase desativarFuncionarioUseCase(FuncionarioRepository repository) {
        return new DesativarFuncionarioUseCaseImpl(repository);
    }

    @Produces
    @ApplicationScoped
    public DeletarFuncionarioUseCase deletarFuncionarioUseCase(FuncionarioRepository repository) {
        return new DeletarFuncionarioUseCaseImpl(repository);
    }

    @Produces
    @ApplicationScoped
    public AtivarFuncionarioUseCase ativarFuncionarioUseCase(FuncionarioRepository repository) {
        return new AtivarFuncionarioUseCaseImpl(repository);
    }
}






