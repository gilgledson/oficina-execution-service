package br.com.fiap.oficina.execution.execucao.infrastructure.persistence;

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import br.com.fiap.oficina.execution.funcionario.infrastructure.persistence.FuncionarioRepositoryImpl;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import lombok.RequiredArgsConstructor;

/** Cria os índices das coleções na subida da aplicação (a operação é idempotente). */
@ApplicationScoped
@RequiredArgsConstructor
public class MongoIndexes {

    private final OrdemExecucaoRepositoryImpl execucoes;
    private final FuncionarioRepositoryImpl funcionarios;

    void criar(@Observes StartupEvent evento) {
        // Consulta das filas: por fase, na ordem de chegada.
        execucoes.mongoCollection().createIndex(Indexes.ascending("fase", "entradaNaFila"));
        funcionarios.mongoCollection().createIndex(Indexes.ascending("cpf"), new IndexOptions().unique(true));
        funcionarios.mongoCollection().createIndex(Indexes.ascending("usuarioId"));
    }
}
