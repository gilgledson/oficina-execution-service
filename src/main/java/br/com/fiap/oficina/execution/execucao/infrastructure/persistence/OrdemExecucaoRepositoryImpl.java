package br.com.fiap.oficina.execution.execucao.infrastructure.persistence;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jboss.logmanager.MDC;

import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;

import br.com.fiap.oficina.execution.execucao.application.repository.OrdemExecucaoRepository;
import br.com.fiap.oficina.execution.execucao.domain.ConflitoDeVersaoException;
import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import br.com.fiap.oficina.execution.execucao.domain.StatusServico;
import br.com.fiap.oficina.execution.shared.web.CorrelationIdFilter;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrdemExecucaoRepositoryImpl
        implements OrdemExecucaoRepository, PanacheMongoRepositoryBase<OrdemExecucaoDocument, UUID> {

    @Override
    public Optional<OrdemExecucao> buscarPorOrdemServicoId(UUID ordemServicoId) {
        return findByIdOptional(ordemServicoId).map(OrdemExecucaoDocument::toDomain);
    }

    @Override
    public void salvar(OrdemExecucao execucao) {
        execucao.atribuirCorrelationId(MDC.get(CorrelationIdFilter.MDC_KEY));
        OrdemExecucaoDocument doc = OrdemExecucaoDocument.fromDomain(execucao);
        long versaoAnterior = execucao.getVersao();
        doc.versao = versaoAnterior + 1;

        if (versaoAnterior == 0) {
            persist(doc);
            return;
        }
        // Só substitui se ninguém gravou a mesma execução desde que ela foi lida.
        long alterados = mongoCollection()
                .replaceOne(and(eq("_id", doc.ordemServicoId), eq("versao", versaoAnterior)), doc)
                .getModifiedCount();
        if (alterados == 0) {
            throw new ConflitoDeVersaoException();
        }
    }

    @Override
    public List<OrdemExecucao> listarPorFase(FaseExecucao fase) {
        return listar(eq("fase", fase.name()), Sorts.ascending("entradaNaFila"), 0);
    }

    @Override
    public List<OrdemExecucao> listarComServicosFinalizados() {
        return listar(eq("servicos.status", StatusServico.FINALIZADO.name()), null, 0);
    }

    /** Execuções com eventos aguardando entrega ao broker (usado pelo OutboxRelay). */
    public List<OrdemExecucaoDocument> buscarComEventosPendentes(int limite) {
        return mongoCollection().find(exists("outbox.0")).limit(limite).into(new ArrayList<>());
    }

    /** Remove da outbox um evento já confirmado pelo broker. */
    public void removerEvento(UUID ordemServicoId, UUID eventoId) {
        mongoCollection().updateOne(eq("_id", ordemServicoId),
                Updates.pull("outbox", new Document("eventoId", eventoId)));
    }

    private List<OrdemExecucao> listar(Bson filtro, Bson ordem, int limite) {
        var consulta = mongoCollection().find(filtro);
        if (ordem != null) {
            consulta = consulta.sort(ordem);
        }
        if (limite > 0) {
            consulta = consulta.limit(limite);
        }
        return consulta.into(new ArrayList<>()).stream().map(OrdemExecucaoDocument::toDomain).toList();
    }
}
