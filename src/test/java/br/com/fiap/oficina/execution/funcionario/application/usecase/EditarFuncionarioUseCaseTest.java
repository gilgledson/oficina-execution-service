package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
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

@DisplayName("Editar Funcionário - Testes de Uso de Caso")
class EditarFuncionarioUseCaseTest {

    private FuncionarioRepository repository;
    private EditarFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FuncionarioRepository.class);
        useCase = new EditarFuncionarioUseCaseImpl(repository);
    }

    @Test
    @DisplayName("Deve editar funcionário com sucesso")
    void deveEditarComSucesso() {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00",
                "(11) 99999-9999", CargoFuncionario.MECANICO);

        when(repository.buscarPorId(id)).thenReturn(Optional.of(funcionario));

        FuncionarioOutput output = useCase.executar(id, "João Alterado", "Silva", "123.456.789-00", "(11) 88888-8888",
                CargoFuncionario.ADMINISTRADOR);

        assertNotNull(output);
        assertEquals("João Alterado", output.nome());
        assertEquals("(11) 88888-8888", output.telefone());
        assertEquals(CargoFuncionario.ADMINISTRADOR, output.cargo());
        verify(repository, times(1)).atualizar(any(Funcionario.class));
    }

    @Test
    @DisplayName("Deve falhar ao editar funcionário inexistente")
    void deveFalharSeNaoEncontrado() {
        UUID id = UUID.randomUUID();
        when(repository.buscarPorId(id)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.executar(id, "Nome", "Sobrenome", "111.111.111-11",
                "(11) 1111-1111", CargoFuncionario.MECANICO));
    }

    @Test
    @DisplayName("Deve falhar ao editar funcionário que já está deletado")
    void deveFalharAoEditarFuncionarioJaDeletado() {
        UUID id = UUID.randomUUID();
        Funcionario funcionario = new Funcionario(UUID.randomUUID(), "João", "Silva", "123.456.789-00",
                "(11) 99999-9999", CargoFuncionario.MECANICO);
        funcionario.desativar();

        when(repository.buscarPorId(id)).thenReturn(Optional.of(funcionario));

        assertThrows(IllegalArgumentException.class, () -> useCase.executar(id,
                "Nome", "Sobrenome", "111.111.111-11", "(11) 1111-1111", CargoFuncionario.MECANICO));
    }
}









