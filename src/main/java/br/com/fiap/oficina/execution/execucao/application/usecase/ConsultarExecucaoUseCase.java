package br.com.fiap.oficina.execution.execucao.application.usecase;

import java.util.List;
import java.util.UUID;

import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class ConsultarExecucaoUseCase {

    private final OrdemExecucaoRepository repository;

    public OrdemExecucao buscar(UUID ordemServicoId) {
        return repository.buscarPorOrdemServicoId(ordemServicoId)
                .orElseThrow(() -> new NotFoundException("Execução não encontrada para a OS " + ordemServicoId));
    }

    /** Ordens esperando atendimento, na ordem em que devem ser atendidas. */
    public List<OrdemExecucao> fila(TipoFila tipo) {
        return repository.listarPorFase(tipo.fase());
    }
}
