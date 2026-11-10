package br.com.fiap.oficina.execution.funcionario.api.dto;

import br.com.fiap.oficina.execution.funcionario.domain.valueObject.CargoFuncionario;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.UUID;

public record FuncionarioRequest(
    @Schema(description = "ID do usuário associado") @JsonProperty("usuario_id") UUID usuarioId,
    
    @NotBlank(message = "O nome é obrigatório") 
    @Schema(description = "Nome do funcionário") 
    String nome,
    
    @NotBlank(message = "O sobrenome é obrigatório") 
    @Schema(description = "Sobrenome do funcionário") 
    String sobrenome,
    
    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}\\-\\d{2}", message = "CPF inválido. Use o formato 000.000.000-00")
    @Schema(description = "CPF do funcionário") 
    String cpf,
    
    @NotBlank(message = "O telefone é obrigatório")
    @Pattern(regexp = "\\(\\d{2}\\) \\d{4,5}-\\d{4}", message = "Telefone inválido. Use o formato (00) 00000-0000")
    @Schema(description = "Telefone do funcionário") 
    String telefone,
    
    @NotNull(message = "O cargo é obrigatório")
    @Schema(description = "Cargo do funcionário") 
    CargoFuncionario cargo
) {}






