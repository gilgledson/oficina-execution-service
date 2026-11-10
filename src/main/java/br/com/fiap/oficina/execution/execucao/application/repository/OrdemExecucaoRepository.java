package br.com.fiap.oficina.execution.execucao.application.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;

public interface OrdemExecucaoRepository {

    Optional<OrdemExecucao> buscarPorOrdemServicoId(UUID ordemServicoId);

    /**
     * Grava a execução junto com os eventos pendentes dela, de forma atômica.
     *
     * @throws br.com.fiap.oficina.execution.execucao.domain.ConflitoDeVersaoException
     *             se outra operação gravou a mesma execução nesse meio tempo
     */
    void salvar(OrdemExecucao execucao);

    /** Ordens em uma fase, da que está esperando há mais tempo para a mais recente. */
    List<OrdemExecucao> listarPorFase(FaseExecucao fase);

    /** Ordens com ao menos um serviço finalizado (base do relatório de tempo médio). */
    List<OrdemExecucao> listarComServicosFinalizados();
}
