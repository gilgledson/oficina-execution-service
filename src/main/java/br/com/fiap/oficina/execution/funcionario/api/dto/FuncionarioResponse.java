package br.com.fiap.oficina.execution.funcionario.api.dto;

import br.com.fiap.oficina.execution.funcionario.application.dto.FuncionarioOutput;
import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.UUID;

public record FuncionarioResponse(
    UUID id,
    @JsonProperty("usuario_id") UUID usuarioId,
    String nome,
    String sobrenome,
    String cpf,
    String telefone,
    CargoFuncionario cargo,
    boolean ativo,
    @JsonProperty("deletado_em") LocalDateTime deletadoEm
) {
    public static FuncionarioResponse fromOutput(FuncionarioOutput output) {
        return new FuncionarioResponse(
            output.id(),
            output.usuarioId(),
            output.nome(),
            output.sobrenome(),
            output.cpf(),
            output.telefone(),
            output.cargo(),
            output.ativo(),
            output.deletadoEm()
        );
    }
}






