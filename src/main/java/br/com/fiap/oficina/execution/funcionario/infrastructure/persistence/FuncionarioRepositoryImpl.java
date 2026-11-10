package br.com.fiap.oficina.execution.funcionario.infrastructure.persistence;

import static com.mongodb.client.model.Filters.eq;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.bson.Document;
import org.bson.conversions.Bson;

import br.com.fiap.oficina.execution.funcionario.application.repository.FuncionarioRepository;
import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.shared.domain.Pagina;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class FuncionarioRepositoryImpl
        implements FuncionarioRepository, PanacheMongoRepositoryBase<FuncionarioDocument, UUID> {

    @Override
    public Optional<Funcionario> buscarPorId(UUID id) {
        return findByIdOptional(id).map(FuncionarioDocument::toDomain);
    }

    @Override
    public Optional<Funcionario> buscarPorCpf(String cpf) {
        return primeiro(eq("cpf", cpf));
    }

    @Override
    public Optional<Funcionario> buscarPorUsuarioId(UUID usuarioId) {
        return primeiro(eq("usuarioId", usuarioId));
    }

    @Override
    public Funcionario salvar(Funcionario funcionario) {
        persist(FuncionarioDocument.fromDomain(funcionario));
        return funcionario;
    }

    @Override
    public Funcionario atualizar(Funcionario funcionario) {
        FuncionarioDocument doc = FuncionarioDocument.fromDomain(funcionario);
        if (mongoCollection().replaceOne(eq("_id", doc.id), doc).getMatchedCount() == 0) {
            throw new NotFoundException("Funcionario não encontrado");
        }
        return funcionario;
    }

    @Override
    public void deletar(UUID id) {
        deleteById(id);
    }

    @Override
    public Pagina<Funcionario> listarTodos(int pagina, int tamanhoLista, boolean incluirInativos) {
        Bson filtro = incluirInativos ? new Document() : eq("ativo", true);
        long total = mongoCollection().countDocuments(filtro);
        List<Funcionario> itens = mongoCollection().find(filtro)
                .skip(pagina * tamanhoLista)
                .limit(tamanhoLista)
                .into(new ArrayList<>())
                .stream()
                .map(FuncionarioDocument::toDomain)
                .toList();
        int totalPaginas = (int) Math.ceil((double) total / tamanhoLista);
        return new Pagina<>(itens, pagina, tamanhoLista, totalPaginas, total);
    }

    private Optional<Funcionario> primeiro(Bson filtro) {
        return Optional.ofNullable(mongoCollection().find(filtro).first()).map(FuncionarioDocument::toDomain);
    }
}
