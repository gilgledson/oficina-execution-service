package br.com.fiap.oficina.execution.funcionario.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.UUID;

import org.bson.codecs.pojo.annotations.BsonId;

import br.com.fiap.oficina.execution.funcionario.domain.entity.Funcionario;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import io.quarkus.mongodb.panache.common.MongoEntity;

/**
 * Documento da coleção {@code funcionarios}. O {@code usuarioId} referencia o
 * usuário de login, que vive no OS Service; aqui é só o identificador.
 */
@MongoEntity(collection = "funcionarios")
public class FuncionarioDocument {

    @BsonId
    public UUID id;
    public UUID usuarioId;
    public String nome;
    public String sobrenome;
    public CargoFuncionario cargo;
    public String cpf;
    public String telefone;
    public boolean ativo;
    public LocalDateTime deletadoEm;

    public static FuncionarioDocument fromDomain(Funcionario funcionario) {
        FuncionarioDocument doc = new FuncionarioDocument();
        doc.id = funcionario.getId();
        doc.usuarioId = funcionario.getUsuarioId();
        doc.nome = funcionario.getNome();
        doc.sobrenome = funcionario.getSobrenome();
        doc.cargo = funcionario.getCargo();
        doc.cpf = funcionario.getCpf();
        doc.telefone = funcionario.getTelefone();
        doc.ativo = funcionario.isAtivo();
        doc.deletadoEm = funcionario.getDeletadoEm();
        return doc;
    }

    public Funcionario toDomain() {
        return Funcionario.builder()
                .id(id)
                .usuarioId(usuarioId)
                .nome(nome)
                .sobrenome(sobrenome)
                .cpf(cpf)
                .telefone(telefone)
                .cargo(cargo)
                .ativo(ativo)
                .deletadoEm(deletadoEm)
                .build();
    }
}
