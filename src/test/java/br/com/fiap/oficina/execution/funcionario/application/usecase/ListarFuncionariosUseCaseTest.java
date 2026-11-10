package br.com.fiap.oficina.execution.funcionario.application.usecase;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import br.com.fiap.oficina.execution.shared.domain.Pagina;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Listar Funcionários - Testes de Uso de Caso")
class ListarFuncionariosUseCaseTest {

    private FuncionarioRepository repository;
    private ListarFuncionariosUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(FuncionarioRepository.class);
        useCase = new ListarFuncionariosUseCaseImpl(repository);
    }

    @Test
    @DisplayName("Deve listar funcionários com paginação")
    void deveListarComPaginacao() {
        Funcionario f1 = new Funcionario(UUID.randomUUID(), "João", "Silva", "111.111.111-11", "(11) 1111-1111", CargoFuncionario.MECANICO);
        Pagina<Funcionario> pagina = new Pagina<>(List.of(f1), 0, 10, 1, 1);
        
        when(repository.listarTodos(0, 10, false)).thenReturn(pagina);

        Pagina<FuncionarioOutput> output = useCase.executar(0, 10, false);

        assertNotNull(output);
        assertEquals(1, output.itens().size());
        assertEquals("João", output.itens().get(0).nome());
        verify(repository, times(1)).listarTodos(0, 10, false);
    }
}









