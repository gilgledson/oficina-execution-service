package br.com.fiap.oficina.execution.execucao.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Um serviço aprovado da OS, do ponto de vista da execução: quem fez, quando
 * começou e quando terminou. Preço e produtos continuam no OS Service.
 */
@Getter
@AllArgsConstructor
public class ServicoExecucao {

    /** Identificador do item de serviço dentro da OS. */
    private final UUID servicoOsId;
    /** Identificador do serviço no catálogo (usado no relatório de tempo médio). */
    private final UUID servicoId;
    private final String nome;
    private StatusServico status;
    private Instant inicio;
    private Instant fim;
    private UUID executorId;

    public ServicoExecucao(UUID servicoOsId, UUID servicoId, String nome) {
        this(servicoOsId, servicoId, nome, StatusServico.PENDENTE, null, null, null);
    }

    void iniciar(UUID executorId, Instant agora) {
        if (status != StatusServico.PENDENTE) {
            throw new IllegalStateException("Apenas serviços pendentes podem ser iniciados.");
        }
        this.status = StatusServico.EM_EXECUCAO;
        this.inicio = agora;
        this.executorId = executorId;
    }

    void finalizar(Instant agora) {
        if (status != StatusServico.EM_EXECUCAO) {
            throw new IllegalStateException("O serviço deve estar em execução para ser finalizado.");
        }
        this.status = StatusServico.FINALIZADO;
        this.fim = agora;
    }

    public boolean isFinalizado() {
        return status == StatusServico.FINALIZADO;
    }

    /** Tempo gasto no serviço, ou zero se ele ainda não foi finalizado. */
    public Duration duracao() {
        if (!isFinalizado() || inicio == null || fim == null) {
            return Duration.ZERO;
        }
        return Duration.between(inicio, fim);
    }
}
