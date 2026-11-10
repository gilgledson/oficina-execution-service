package br.com.fiap.oficina.execution.execucao.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

/**
 * Agregado do Execution Service: o andamento de uma OS dentro da oficina —
 * posição na fila, diagnóstico, execução de cada serviço e histórico de fases.
 * O status "oficial" da OS continua no OS Service, que o atualiza a partir
 * dos eventos gerados aqui.
 */
@Getter
public class OrdemExecucao {

    private final UUID ordemServicoId;
    private FaseExecucao fase;
    /** Momento em que a OS entrou na fila atual (diagnóstico ou execução). */
    private Instant entradaNaFila;
    private Instant inicioDiagnostico;
    private Instant fimDiagnostico;
    private Instant inicioExecucao;
    private Instant fimExecucao;
    private String motivoEncerramento;
    private final List<ServicoExecucao> servicos;
    private final List<RegistroFase> historico;
    private final List<EventoPendente> eventosPendentes;
    /** Controle de concorrência otimista; 0 = ainda não gravada. */
    private final long versao;

    @Builder
    private OrdemExecucao(UUID ordemServicoId, FaseExecucao fase, Instant entradaNaFila, Instant inicioDiagnostico,
            Instant fimDiagnostico, Instant inicioExecucao, Instant fimExecucao, String motivoEncerramento,
            List<ServicoExecucao> servicos, List<RegistroFase> historico, List<EventoPendente> eventosPendentes,
            long versao) {
        this.ordemServicoId = ordemServicoId;
        this.fase = fase;
        this.entradaNaFila = entradaNaFila;
        this.inicioDiagnostico = inicioDiagnostico;
        this.fimDiagnostico = fimDiagnostico;
        this.inicioExecucao = inicioExecucao;
        this.fimExecucao = fimExecucao;
        this.motivoEncerramento = motivoEncerramento;
        this.servicos = servicos == null ? new ArrayList<>() : new ArrayList<>(servicos);
        this.historico = historico == null ? new ArrayList<>() : new ArrayList<>(historico);
        this.eventosPendentes = eventosPendentes == null ? new ArrayList<>() : new ArrayList<>(eventosPendentes);
        this.versao = versao;
    }

    /** OS recém-aberta: entra no fim da fila de diagnóstico. */
    public static OrdemExecucao novaNaFila(UUID ordemServicoId, Instant agora) {
        if (ordemServicoId == null) {
            throw new IllegalArgumentException("Ordem de serviço é obrigatória.");
        }
        OrdemExecucao execucao = OrdemExecucao.builder()
                .ordemServicoId(ordemServicoId)
                .entradaNaFila(agora)
                .build();
        execucao.mudarFase(FaseExecucao.FILA_DIAGNOSTICO, agora, null);
        return execucao;
    }

    public void iniciarDiagnostico(UUID usuarioId, Instant agora) {
        exigirFase(FaseExecucao.FILA_DIAGNOSTICO, "Apenas ordens na fila de diagnóstico podem iniciar o diagnóstico.");
        this.inicioDiagnostico = agora;
        mudarFase(FaseExecucao.EM_DIAGNOSTICO, agora, usuarioId);
        registrarEvento(EventoPendente.DIAGNOSTICO_INICIADO, agora, dados());
    }

    public void concluirDiagnostico(UUID usuarioId, Instant agora) {
        exigirFase(FaseExecucao.EM_DIAGNOSTICO, "Apenas ordens em diagnóstico podem concluir o diagnóstico.");
        this.fimDiagnostico = agora;
        mudarFase(FaseExecucao.AGUARDANDO_APROVACAO, agora, usuarioId);
        registrarEvento(EventoPendente.DIAGNOSTICO_CONCLUIDO, agora, dados());
    }

    /**
     * Serviços aprovados pelo cliente. Na primeira aprovação a OS entra na fila
     * de execução; se ela já está na fila ou em execução (serviço extra
     * aprovado depois), os serviços novos são acrescentados.
     *
     * @return {@code false} se nada mudou (evento repetido ou OS já encerrada)
     */
    public boolean enfileirarParaExecucao(List<ServicoExecucao> servicosAprovados, Instant agora) {
        if (fase.isEncerrada()) {
            return false;
        }
        if (fase == FaseExecucao.FILA_EXECUCAO || fase == FaseExecucao.EM_EXECUCAO) {
            return acrescentarServicos(servicosAprovados);
        }
        exigirFase(FaseExecucao.AGUARDANDO_APROVACAO,
                "Apenas ordens aguardando aprovação podem entrar na fila de execução.");
        if (servicosAprovados == null || servicosAprovados.isEmpty()) {
            throw new IllegalArgumentException("A ordem precisa de ao menos um serviço aprovado para ser executada.");
        }
        this.servicos.clear();
        this.servicos.addAll(servicosAprovados);
        this.entradaNaFila = agora;
        mudarFase(FaseExecucao.FILA_EXECUCAO, agora, null);
        return true;
    }

    private boolean acrescentarServicos(List<ServicoExecucao> servicosAprovados) {
        if (servicosAprovados == null) {
            return false;
        }
        List<ServicoExecucao> novos = servicosAprovados.stream()
                .filter(novo -> servicos.stream()
                        .noneMatch(existente -> existente.getServicoOsId().equals(novo.getServicoOsId())))
                .toList();
        this.servicos.addAll(novos);
        return !novos.isEmpty();
    }

    public void iniciarExecucao(UUID usuarioId, Instant agora) {
        exigirFase(FaseExecucao.FILA_EXECUCAO, "Apenas ordens na fila de execução podem iniciar a execução.");
        this.inicioExecucao = agora;
        mudarFase(FaseExecucao.EM_EXECUCAO, agora, usuarioId);
        registrarEvento(EventoPendente.EXECUCAO_INICIADA, agora, dados());
    }

    public void iniciarServico(UUID servicoOsId, UUID executorId, Instant agora) {
        exigirFase(FaseExecucao.EM_EXECUCAO, "Apenas ordens em execução podem ter serviços iniciados.");
        buscarServico(servicoOsId).iniciar(executorId, agora);

        Map<String, Object> dados = dados();
        dados.put("servicoOsId", servicoOsId.toString());
        dados.put("usuarioExecutorId", executorId != null ? executorId.toString() : null);
        dados.put("em", agora.toString());
        registrarEvento(EventoPendente.SERVICO_INICIADO, agora, dados);
    }

    public void finalizarServico(UUID servicoOsId, Instant agora) {
        exigirFase(FaseExecucao.EM_EXECUCAO, "Apenas ordens em execução podem ter serviços finalizados.");
        buscarServico(servicoOsId).finalizar(agora);

        Map<String, Object> dados = dados();
        dados.put("servicoOsId", servicoOsId.toString());
        dados.put("em", agora.toString());
        registrarEvento(EventoPendente.SERVICO_FINALIZADO, agora, dados);
    }

    public void concluirExecucao(UUID usuarioId, Instant agora) {
        exigirFase(FaseExecucao.EM_EXECUCAO, "Apenas ordens em execução podem ser concluídas.");
        if (servicos.stream().anyMatch(s -> !s.isFinalizado())) {
            throw new IllegalStateException(
                    "A ordem não pode ser concluída enquanto houver serviços pendentes ou em execução.");
        }
        this.fimExecucao = agora;
        mudarFase(FaseExecucao.CONCLUIDA, agora, usuarioId);
        registrarEvento(EventoPendente.EXECUCAO_CONCLUIDA, agora, dados());
    }

    /**
     * A oficina não consegue seguir com a OS (peça indisponível, defeito sem
     * conserto...). Dispara a compensação da Saga nos outros serviços.
     */
    public void reportarFalha(String motivo, UUID usuarioId, Instant agora) {
        if (fase.isEncerrada()) {
            throw new IllegalStateException("A ordem já está encerrada.");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("O motivo da falha é obrigatório.");
        }
        this.motivoEncerramento = motivo;
        mudarFase(FaseExecucao.FALHOU, agora, usuarioId);

        Map<String, Object> dados = dados();
        dados.put("motivo", motivo);
        registrarEvento(EventoPendente.EXECUCAO_FALHOU, agora, dados);
    }

    /**
     * Compensação vinda de outro serviço (OS cancelada, orçamento rejeitado ou
     * não gerado): tira a OS da fila. Não gera evento, porque quem cancelou já
     * avisou os demais.
     *
     * @return {@code false} se a ordem já estava encerrada (evento repetido)
     */
    public boolean cancelar(String motivo, Instant agora) {
        if (fase.isEncerrada()) {
            return false;
        }
        this.motivoEncerramento = motivo;
        mudarFase(FaseExecucao.CANCELADA, agora, null);
        return true;
    }

    /** Guarda o correlationId nos eventos que ainda não têm um. */
    public void atribuirCorrelationId(String correlationId) {
        if (correlationId == null) {
            return;
        }
        eventosPendentes.replaceAll(e -> e.correlationId() == null ? e.comCorrelationId(correlationId) : e);
    }

    private ServicoExecucao buscarServico(UUID servicoOsId) {
        return servicos.stream()
                .filter(s -> s.getServicoOsId().equals(servicoOsId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Serviço não está entre os aprovados desta ordem."));
    }

    private void exigirFase(FaseExecucao esperada, String mensagem) {
        if (fase != esperada) {
            throw new IllegalStateException(mensagem + " Fase atual: " + fase + ".");
        }
    }

    private void mudarFase(FaseExecucao novaFase, Instant agora, UUID usuarioId) {
        this.fase = novaFase;
        this.historico.add(new RegistroFase(novaFase, agora, usuarioId));
    }

    private Map<String, Object> dados() {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("ordemServicoId", ordemServicoId.toString());
        return dados;
    }

    private void registrarEvento(String routingKey, Instant agora, Map<String, Object> dados) {
        eventosPendentes.add(new EventoPendente(UUID.randomUUID(), routingKey, dados, null, agora));
    }
}
