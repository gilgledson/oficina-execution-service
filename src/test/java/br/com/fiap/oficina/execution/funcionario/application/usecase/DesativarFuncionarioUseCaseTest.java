package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Desativar Funcionário - Testes de Uso de Caso")
class DesativarFuncionarioUseCaseTest {

    private FuncionarioRepository repository;
    private DesativarFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FuncionarioRepository.class);
        useCase = new DesativarFuncionarioUseCaseImpl(repository);
    }

    @Test
    @DisplayName("Deve desativar funcionário com sucesso")
    void deveDesativarComSucesso() {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00",
                "(11) 99999-9999", CargoFuncionario.MECANICO);

        when(repository.buscarPorId(id)).thenReturn(Optional.of(funcionario));

        useCase.executar(id);

        assertFalse(funcionario.isAtivo());
        verify(repository, times(1)).atualizar(funcionario);
    }

    @Test
    @DisplayName("Deve falhar ao desativar funcionário não encontrado")
    void deveFalharAoDesativarFuncionarioNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.executar(id));
    }

    @Test
    @DisplayName("Deve falhar ao desativar funcionário que já está desativado")
    void deveFalharAoDesativarFuncionarioDesativado() {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00",
                "(11) 99999-9999", CargoFuncionario.MECANICO);
        funcionario.desativar();

        when(repository.buscarPorId(id)).thenReturn(Optional.of(funcionario));

        assertThrows(IllegalArgumentException.class, () -> useCase.executar(id));
    }
}









