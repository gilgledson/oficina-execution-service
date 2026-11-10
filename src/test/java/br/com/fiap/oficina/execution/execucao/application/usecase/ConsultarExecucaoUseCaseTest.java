package br.com.fiap.oficina.execution.execucao.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ConsultarExecucaoUseCaseTest {

    @Mock
    private OrdemExecucaoRepository repository;

    @InjectMocks
    private ConsultarExecucaoUseCase useCase;

    @Test
    @DisplayName("Deve buscar a execução de uma OS")
    void deveBuscar() {
        UUID osId = UUID.randomUUID();
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, Instant.now());
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(execucao));

        assertSame(execucao, useCase.buscar(osId));
    }

    @Test
    @DisplayName("Deve lançar NotFoundException para OS desconhecida")
    void deveLancarNotFound() {
        UUID osId = UUID.randomUUID();
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.buscar(osId));
    }

    @Test
    @DisplayName("Cada tipo de fila consulta a fase correspondente")
    void deveListarFilas() {
        List<OrdemExecucao> diagnostico = List.of(OrdemExecucao.novaNaFila(UUID.randomUUID(), Instant.now()));
        when(repository.listarPorFase(FaseExecucao.FILA_DIAGNOSTICO)).thenReturn(diagnostico);
        when(repository.listarPorFase(FaseExecucao.FILA_EXECUCAO)).thenReturn(List.of());

        assertEquals(diagnostico, useCase.fila(TipoFila.DIAGNOSTICO));
        assertEquals(List.of(), useCase.fila(TipoFila.EXECUCAO));
    }
}
