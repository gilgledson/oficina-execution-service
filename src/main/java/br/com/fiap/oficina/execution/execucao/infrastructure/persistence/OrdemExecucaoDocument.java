package br.com.fiap.oficina.execution.execucao.infrastructure.persistence;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

import org.bson.codecs.pojo.annotations.BsonId;

import br.com.fiap.oficina.execution.execucao.domain.EventoPendente;
import br.com.fiap.oficina.execution.execucao.domain.FaseExecucao;
import br.com.fiap.oficina.execution.execucao.domain.OrdemExecucao;
import br.com.fiap.oficina.execution.execucao.domain.RegistroFase;
import br.com.fiap.oficina.execution.execucao.domain.ServicoExecucao;
import br.com.fiap.oficina.execution.execucao.domain.StatusServico;
import io.quarkus.mongodb.panache.common.MongoEntity;
import io.vertx.core.json.JsonObject;

/**
 * Documento da coleção {@code execucoes}: uma OS com seus serviços, histórico
 * de fases e eventos ainda não entregues ao broker ({@code outbox}), tudo no
 * mesmo documento. É o que torna a gravação do estado e dos eventos atômica
 * sem depender de transações multi-documento.
 */
@MongoEntity(collection = "execucoes")
public class OrdemExecucaoDocument {

    @BsonId
    public UUID ordemServicoId;
    public String fase;
    public Instant entradaNaFila;
    public Instant inicioDiagnostico;
    public Instant fimDiagnostico;
    public Instant inicioExecucao;
    public Instant fimExecucao;
    public String motivoEncerramento;
    public List<ServicoDoc> servicos = new ArrayList<>();
    public List<RegistroFaseDoc> historico = new ArrayList<>();
    public List<EventoDoc> outbox = new ArrayList<>();
    public long versao;

    public static class ServicoDoc {
        public UUID servicoOsId;
        public UUID servicoId;
        public String nome;
        public String status;
        public Instant inicio;
        public Instant fim;
        public UUID executorId;
    }

    public static class RegistroFaseDoc {
        public String fase;
        public Instant em;
        public UUID usuarioId;
    }

    public static class EventoDoc {
        public UUID eventoId;
        public String routingKey;
        /** Payload do evento em JSON. */
        public String payload;
        public String correlationId;
        public Instant criadoEm;
    }

    public static OrdemExecucaoDocument fromDomain(OrdemExecucao execucao) {
        OrdemExecucaoDocument doc = new OrdemExecucaoDocument();
        doc.ordemServicoId = execucao.getOrdemServicoId();
        doc.fase = execucao.getFase().name();
        doc.entradaNaFila = execucao.getEntradaNaFila();
        doc.inicioDiagnostico = execucao.getInicioDiagnostico();
        doc.fimDiagnostico = execucao.getFimDiagnostico();
        doc.inicioExecucao = execucao.getInicioExecucao();
        doc.fimExecucao = execucao.getFimExecucao();
        doc.motivoEncerramento = execucao.getMotivoEncerramento();
        doc.versao = execucao.getVersao();
        for (ServicoExecucao servico : execucao.getServicos()) {
            ServicoDoc s = new ServicoDoc();
            s.servicoOsId = servico.getServicoOsId();
            s.servicoId = servico.getServicoId();
            s.nome = servico.getNome();
            s.status = servico.getStatus().name();
            s.inicio = servico.getInicio();
            s.fim = servico.getFim();
            s.executorId = servico.getExecutorId();
            doc.servicos.add(s);
        }
        for (RegistroFase registro : execucao.getHistorico()) {
            RegistroFaseDoc r = new RegistroFaseDoc();
            r.fase = registro.fase().name();
            r.em = registro.em();
            r.usuarioId = registro.usuarioId();
            doc.historico.add(r);
        }
        for (EventoPendente evento : execucao.getEventosPendentes()) {
            EventoDoc e = new EventoDoc();
            e.eventoId = evento.id();
            e.routingKey = evento.routingKey();
            e.payload = new JsonObject(new LinkedHashMap<>(evento.dados())).encode();
            e.correlationId = evento.correlationId();
            e.criadoEm = evento.criadoEm();
            doc.outbox.add(e);
        }
        return doc;
    }

    public OrdemExecucao toDomain() {
        return OrdemExecucao.builder()
                .ordemServicoId(ordemServicoId)
                .fase(FaseExecucao.valueOf(fase))
                .entradaNaFila(entradaNaFila)
                .inicioDiagnostico(inicioDiagnostico)
                .fimDiagnostico(fimDiagnostico)
                .inicioExecucao(inicioExecucao)
                .fimExecucao(fimExecucao)
                .motivoEncerramento(motivoEncerramento)
                .versao(versao)
                .servicos(lista(servicos).stream()
                        .map(s -> new ServicoExecucao(s.servicoOsId, s.servicoId, s.nome,
                                StatusServico.valueOf(s.status), s.inicio, s.fim, s.executorId))
                        .toList())
                .historico(lista(historico).stream()
                        .map(r -> new RegistroFase(FaseExecucao.valueOf(r.fase), r.em, r.usuarioId))
                        .toList())
                .eventosPendentes(lista(outbox).stream()
                        .map(e -> new EventoPendente(e.eventoId, e.routingKey, new JsonObject(e.payload).getMap(),
                                e.correlationId, e.criadoEm))
                        .toList())
                .build();
    }

    private static <T> List<T> lista(List<T> itens) {
        return itens == null ? List.of() : itens;
    }
}
