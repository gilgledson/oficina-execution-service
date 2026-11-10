package br.com.fiap.oficina.execution.execucao.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.fiap.oficina.execution.execucao.application.dto.ServicoAprovado;
import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import br.com.fiap.oficina.execution.execucao.domain.StatusServico;
import jakarta.ws.rs.NotFoundException;

@ExtendWith(MockitoExtension.class)
class GerenciarExecucaoUseCaseTest {

    @Mock
    private OrdemExecucaoRepository repository;

    @InjectMocks
    private GerenciarExecucaoUseCase useCase;

    private final UUID osId = UUID.randomUUID();
    private final UUID mecanico = UUID.randomUUID();
    private final UUID servicoOsId = UUID.randomUUID();
    private final ServicoAprovado aprovado = new ServicoAprovado(servicoOsId, UUID.randomUUID(), "Troca de óleo");

    private OrdemExecucao existente() {
        OrdemExecucao execucao = OrdemExecucao.novaNaFila(osId, Instant.now());
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.of(execucao));
        return execucao;
    }

    private OrdemExecucao emExecucao() {
        OrdemExecucao execucao = existente();
        useCase.iniciarDiagnostico(osId, mecanico);
        useCase.concluirDiagnostico(osId, mecanico);
        useCase.enfileirarParaExecucao(osId, List.of(aprovado));
        useCase.iniciarExecucao(osId, mecanico);
        return execucao;
    }

    @Test
    @DisplayName("OS aberta deve entrar na fila de diagnóstico")
    void deveRegistrarNaFila() {
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        useCase.registrarNaFila(osId);

        ArgumentCaptor<OrdemExecucao> captor = ArgumentCaptor.forClass(OrdemExecucao.class);
        verify(repository).salvar(captor.capture());
        assertEquals(osId, captor.getValue().getOrdemServicoId());
        assertEquals(FaseExecucao.FILA_DIAGNOSTICO, captor.getValue().getFase());
    }

    @Test
    @DisplayName("Evento de abertura repetido não deve duplicar a OS na fila")
    void naoDeveDuplicarNaFila() {
        existente();

        useCase.registrarNaFila(osId);

        verify(repository, never()).salvar(any());
    }

    @Test
    @DisplayName("Fluxo completo: diagnóstico, aprovação, execução e conclusão")
    void deveExecutarFluxoCompleto() {
        OrdemExecucao execucao = emExecucao();

        useCase.iniciarServico(osId, servicoOsId, mecanico);
        assertEquals(StatusServico.EM_EXECUCAO, execucao.getServicos().get(0).getStatus());
        assertEquals(mecanico, execucao.getServicos().get(0).getExecutorId());
        useCase.finalizarServico(osId, servicoOsId);
        OrdemExecucao retorno = useCase.concluirExecucao(osId, mecanico);

        assertSame(execucao, retorno);
        assertEquals(FaseExecucao.CONCLUIDA, execucao.getFase());
        // 4 passos até EM_EXECUCAO + iniciar serviço + finalizar serviço + concluir
        verify(repository, org.mockito.Mockito.times(7)).salvar(execucao);
    }

    @Test
    @DisplayName("Aprovação repetida não deve gravar de novo")
    void naoDeveGravarAprovacaoRepetida() {
        OrdemExecucao execucao = emExecucao();
        org.mockito.Mockito.clearInvocations(repository);

        useCase.enfileirarParaExecucao(osId, List.of(aprovado));

        verify(repository, never()).salvar(any());
        assertEquals(FaseExecucao.EM_EXECUCAO, execucao.getFase());
    }

    @Test
    @DisplayName("Falha reportada deve encerrar a OS e gravar")
    void deveReportarFalha() {
        OrdemExecucao execucao = existente();

        useCase.reportarFalha(osId, "Peça indisponível", mecanico);

        assertEquals(FaseExecucao.FALHOU, execucao.getFase());
        verify(repository).salvar(execucao);
    }

    @Test
    @DisplayName("Transição inválida não deve gravar")
    void naoDeveGravarTransicaoInvalida() {
        existente();

        assertThrows(IllegalStateException.class, () -> useCase.concluirExecucao(osId, mecanico));

        verify(repository, never()).salvar(any());
    }

    @Test
    @DisplayName("Comando sobre OS desconhecida deve lançar NotFoundException")
    void deveLancarNotFound() {
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> useCase.iniciarDiagnostico(osId, mecanico));
        assertThrows(NotFoundException.class, () -> useCase.enfileirarParaExecucao(osId, List.of(aprovado)));
    }

    @Test
    @DisplayName("Compensação: cancela a OS que ainda está ativa")
    void deveCancelar() {
        OrdemExecucao execucao = existente();

        useCase.cancelar(osId, "OS cancelada");

        assertEquals(FaseExecucao.CANCELADA, execucao.getFase());
        verify(repository).salvar(execucao);
    }

    @Test
    @DisplayName("Compensação repetida ou de OS desconhecida não faz nada")
    void deveIgnorarCancelamentoRepetidoOuDesconhecido() {
        OrdemExecucao execucao = existente();
        execucao.cancelar("OS cancelada", Instant.now());

        useCase.cancelar(osId, "OS cancelada");
        when(repository.buscarPorOrdemServicoId(osId)).thenReturn(Optional.empty());
        useCase.cancelar(osId, "OS cancelada");

        verify(repository, never()).salvar(any());
    }
}
