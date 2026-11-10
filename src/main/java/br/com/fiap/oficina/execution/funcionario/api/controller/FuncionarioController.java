package br.com.fiap.oficina.execution.funcionario.api.controller;

import br.com.fiap.oficina.execution.shared.security.Perfis;
import br.com.fiap.oficina.execution.funcionario.api.dto.FuncionarioRequest;
import br.com.fiap.oficina.execution.funcionario.api.dto.FuncionarioResponse;
import br.com.fiap.oficina.execution.funcionario.application.usecase.*;
import br.com.fiap.oficina.execution.shared.api.PaginaResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import java.util.UUID;

@Path("/api/funcionarios")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Funcionários", description = "Gestão de funcionários da oficina")
@RequiredArgsConstructor
public class FuncionarioController {

    private final CadastrarFuncionarioUseCase cadastrarUseCase;
    private final ListarFuncionariosUseCase listarUseCase;
    private final EditarFuncionarioUseCase editarUseCase;
    private final DesativarFuncionarioUseCase desativarUseCase;
    private final DeletarFuncionarioUseCase deletarUseCase;
    private final AtivarFuncionarioUseCase ativarUseCase;

    @POST
    @RolesAllowed(Perfis.ADMIN)
    @Operation(summary = "Cadastrar novo funcionário")
    public Response cadastrar(@Valid FuncionarioRequest request) {
        var output = cadastrarUseCase.executar(
                request.usuarioId(),
                request.nome(),
                request.sobrenome(),
                request.cpf(),
                request.telefone(),
                request.cargo());
        return Response.status(Response.Status.CREATED).entity(FuncionarioResponse.fromOutput(output)).build();
    }

    @GET
    @RolesAllowed({ Perfis.ADMIN })
    @Operation(summary = "Listar todos os funcionários")
    public Response listar(
            @QueryParam("pagina") @DefaultValue("0") @Min(0) int pagina,
            @QueryParam("tamanho") @DefaultValue("10") @Min(1) @Max(100) int tamanho,
            @QueryParam("incluir_inativos") @DefaultValue("false") boolean incluirInativas) {
        var paginaFuncionarios = listarUseCase.executar(pagina, tamanho, incluirInativas);
        var response = PaginaResponse.fromDomain(paginaFuncionarios.map(FuncionarioResponse::fromOutput));
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(Perfis.ADMIN)
    @Operation(summary = "Editar dados do funcionário")
    public Response editar(@PathParam("id") UUID id, @Valid FuncionarioRequest request) {
        var output = editarUseCase.executar(
                id,
                request.nome(),
                request.sobrenome(),
                request.cpf(),
                request.telefone(),
                request.cargo());
        return Response.ok(FuncionarioResponse.fromOutput(output)).build();
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Perfis.ADMIN)
    @Operation(summary = "Desativar funcionário (Soft Delete)")
    public Response desativar(@PathParam("id") UUID id) {
        desativarUseCase.executar(id);
        return Response.noContent().build();
    }

    @DELETE
    @Path("/{id}/permanente")
    @RolesAllowed(Perfis.ADMIN)
    @Operation(summary = "Deletar funcionário permanentemente")
    public Response deletar(@PathParam("id") UUID id) {
        deletarUseCase.executar(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/ativar")
    @RolesAllowed(Perfis.ADMIN)
    @Operation(summary = "Ativar funcionário")
    public Response ativar(@PathParam("id") UUID id) {
        ativarUseCase.executar(id);
        return Response.noContent().build();
    }
}
