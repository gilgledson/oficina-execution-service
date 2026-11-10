package br.com.fiap.oficina.execution.execucao.application.usecase;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import br.com.fiap.oficina.execution.execucao.application.dto.ServicoAprovado;
import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import br.com.fiap.oficina.execution.execucao.domain.ServicoExecucao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Comandos sobre a fila e a execução das ordens. Os métodos chamados a partir
 * de eventos de outros serviços são idempotentes, porque o broker pode
 * entregar a mesma mensagem mais de uma vez.
 */
@ApplicationScoped
@RequiredArgsConstructor
public class GerenciarExecucaoUseCase {

    private final OrdemExecucaoRepository repository;

    /** Evento {@code os.aberta}: coloca a OS na fila de diagnóstico. */
    public void registrarNaFila(UUID ordemServicoId) {
        if (repository.buscarPorOrdemServicoId(ordemServicoId).isPresent()) {
            return;
        }
        repository.salvar(OrdemExecucao.novaNaFila(ordemServicoId, Instant.now()));
    }

    public OrdemExecucao iniciarDiagnostico(UUID ordemServicoId, UUID usuarioId) {
        return alterar(ordemServicoId, e -> e.iniciarDiagnostico(usuarioId, Instant.now()));
    }

    public OrdemExecucao concluirDiagnostico(UUID ordemServicoId, UUID usuarioId) {
        return alterar(ordemServicoId, e -> e.concluirDiagnostico(usuarioId, Instant.now()));
    }

    /** Evento {@code os.aprovada}: coloca a OS na fila de execução. */
    public void enfileirarParaExecucao(UUID ordemServicoId, List<ServicoAprovado> servicosAprovados) {
        OrdemExecucao execucao = buscar(ordemServicoId);
        List<ServicoExecucao> servicos = servicosAprovados.stream()
                .map(s -> new ServicoExecucao(s.servicoOsId(), s.servicoId(), s.nome()))
                .toList();
        if (execucao.enfileirarParaExecucao(servicos, Instant.now())) {
            repository.salvar(execucao);
        }
    }

    public OrdemExecucao iniciarExecucao(UUID ordemServicoId, UUID usuarioId) {
        return alterar(ordemServicoId, e -> e.iniciarExecucao(usuarioId, Instant.now()));
    }

    public OrdemExecucao iniciarServico(UUID ordemServicoId, UUID servicoOsId, UUID executorId) {
        return alterar(ordemServicoId, e -> e.iniciarServico(servicoOsId, executorId, Instant.now()));
    }

    public OrdemExecucao finalizarServico(UUID ordemServicoId, UUID servicoOsId) {
        return alterar(ordemServicoId, e -> e.finalizarServico(servicoOsId, Instant.now()));
    }

    public OrdemExecucao concluirExecucao(UUID ordemServicoId, UUID usuarioId) {
        return alterar(ordemServicoId, e -> e.concluirExecucao(usuarioId, Instant.now()));
    }

    public OrdemExecucao reportarFalha(UUID ordemServicoId, String motivo, UUID usuarioId) {
        return alterar(ordemServicoId, e -> e.reportarFalha(motivo, usuarioId, Instant.now()));
    }

    /**
     * Compensação da Saga: outro serviço encerrou a OS (cancelamento, orçamento
     * rejeitado ou não gerado). Tira a ordem da fila, se ela ainda estiver lá.
     */
    public void cancelar(UUID ordemServicoId, String motivo) {
        repository.buscarPorOrdemServicoId(ordemServicoId).ifPresent(execucao -> {
            if (execucao.cancelar(motivo, Instant.now())) {
                repository.salvar(execucao);
            }
        });
    }

    private OrdemExecucao alterar(UUID ordemServicoId, Consumer<OrdemExecucao> comando) {
        OrdemExecucao execucao = buscar(ordemServicoId);
        comando.accept(execucao);
        repository.salvar(execucao);
        return execucao;
    }

    private OrdemExecucao buscar(UUID ordemServicoId) {
        return repository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new NotFoundException("Execução não encontrada para a OS " + ordemServicoId));
    }
}
