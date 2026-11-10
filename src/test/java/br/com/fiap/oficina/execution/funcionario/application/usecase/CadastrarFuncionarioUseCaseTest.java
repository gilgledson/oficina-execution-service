package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Cadastrar Funcionário - Testes de Uso de Caso")
class CadastrarFuncionarioUseCaseTest {

    private FuncionarioRepository repository;
    private CadastrarFuncionarioUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FuncionarioRepository.class);
        useCase = new CadastrarFuncionarioUseCaseImpl(repository);
    }

    @Test
    @DisplayName("Deve cadastrar funcionário com sucesso")
    void deveCadastrarComSucesso() {
        UUID usuarioId = UUID.randomUUID();
        String cpf = "123.456.789-00";
        
        when(repository.buscarPorCpf(cpf)).thenReturn(Optional.empty());
        when(repository.buscarPorUsuarioId(usuarioId)).thenReturn(Optional.empty());

        FuncionarioOutput output = useCase.executar(usuarioId, "João", "Silva", cpf, "(11) 99999-9999", CargoFuncionario.MECANICO);

        assertNotNull(output);
        assertEquals("João", output.nome());
        verify(repository, times(1)).salvar(any(Funcionario.class));
    }

    @Test
    @DisplayName("Deve falhar ao cadastrar com CPF já existente")
    void deveFalharComCpfExistente() {
        String cpf = "123.456.789-00";
        when(repository.buscarPorCpf(cpf)).thenReturn(Optional.of(Mockito.mock(Funcionario.class)));

        assertThrows(IllegalArgumentException.class, () -> 
            useCase.executar(UUID.randomUUID(), "João", "Silva", cpf, "(11) 99999-9999", CargoFuncionario.MECANICO)
        );
    }
}









