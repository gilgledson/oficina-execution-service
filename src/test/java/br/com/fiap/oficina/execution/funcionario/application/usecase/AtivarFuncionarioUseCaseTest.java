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

@DisplayName("Ativar Funcionário - Testes de Uso de Caso")
class AtivarFuncionarioUseCaseTest {

    private FuncionarioRepository repository;
    private AtivarFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FuncionarioRepository.class);
        useCase = new AtivarFuncionarioUseCaseImpl(repository);
    }

    @Test
    @DisplayName("Deve ativar funcionário inativo")
    void deveAtivarComSucesso() {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00",
                "(11) 99999-9999", CargoFuncionario.MECANICO);
        funcionario.desativar();

        when(repository.buscarPorId(id)).thenReturn(Optional.of(funcionario));

        useCase.executar(id);

        assertTrue(funcionario.isAtivo());
        verify(repository, times(1)).atualizar(funcionario);
    }

    @Test
    @DisplayName("Deve falhar ao ativar funcionário não encontrado")
    void deveFalharAoAtivarComFuncionarioNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.executar(id));
    }

    @Test
    @DisplayName("Deve falhar ao ativar funcionário que já está ativo")
    void deveFalharAoAtivarFuncionarioAtivo() {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00",
                "(11) 99999-9999", CargoFuncionario.MECANICO);

        when(repository.buscarPorId(id)).thenReturn(Optional.of(funcionario));

        assertThrows(IllegalArgumentException.class, () -> useCase.executar(id));
    }

}









