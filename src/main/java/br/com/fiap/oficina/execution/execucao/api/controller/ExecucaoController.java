package br.com.fiap.oficina.execution.execucao.api.controller;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.oficina.execution.execucao.api.dto.ExecucaoResponse;
import br.com.fiap.oficina.execution.execucao.api.dto.FalhaRequest;
import br.com.fiap.oficina.execution.execucao.application.usecase.ConsultarExecucaoUseCase;
import br.com.fiap.oficina.execution.execucao.application.usecase.GerenciarExecucaoUseCase;
import br.com.fiap.oficina.execution.execucao.application.usecase.TipoFila;
import br.com.fiap.oficina.execution.shared.security.Perfis;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;

@Path("/api/execucoes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Execução", description = "Fila de diagnóstico e execução das ordens de serviço")
@RequiredArgsConstructor
public class ExecucaoController {

    private final GerenciarExecucaoUseCase gerenciarExecucao;
    private final ConsultarExecucaoUseCase consultarExecucao;
    private final JsonWebToken jwt;

    @GET
    @Path("/fila")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO, Perfis.ATENDENTE })
    @Operation(summary = "Listar a fila de diagnóstico ou de execução, na ordem de atendimento")
    public List<ExecucaoResponse> fila(@QueryParam("tipo") @DefaultValue("DIAGNOSTICO") TipoFila tipo) {
        return consultarExecucao.fila(tipo).stream().map(ExecucaoResponse::fromDomain).toList();
    }

    @GET
    @Path("/{ordemServicoId}")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO, Perfis.ATENDENTE })
    @Operation(summary = "Consultar o andamento de uma OS na oficina")
    public ExecucaoResponse buscar(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return ExecucaoResponse.fromDomain(consultarExecucao.buscar(ordemServicoId));
    }

    @POST
    @Path("/{ordemServicoId}/iniciar-diagnostico")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Tirar a OS da fila e iniciar o diagnóstico")
    public ExecucaoResponse iniciarDiagnostico(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return ExecucaoResponse.fromDomain(gerenciarExecucao.iniciarDiagnostico(ordemServicoId, usuarioId()));
    }

    @POST
    @Path("/{ordemServicoId}/concluir-diagnostico")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Concluir o diagnóstico; a OS segue para orçamento e aprovação")
    public ExecucaoResponse concluirDiagnostico(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return ExecucaoResponse.fromDomain(gerenciarExecucao.concluirDiagnostico(ordemServicoId, usuarioId()));
    }

    @POST
    @Path("/{ordemServicoId}/iniciar-execucao")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Tirar a OS aprovada da fila e iniciar a execução")
    public ExecucaoResponse iniciarExecucao(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return ExecucaoResponse.fromDomain(gerenciarExecucao.iniciarExecucao(ordemServicoId, usuarioId()));
    }

    @POST
    @Path("/{ordemServicoId}/servicos/{servicoOsId}/iniciar")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Iniciar a execução de um serviço da OS")
    public ExecucaoResponse iniciarServico(@PathParam("ordemServicoId") UUID ordemServicoId,
            @PathParam("servicoOsId") UUID servicoOsId) {
        return ExecucaoResponse.fromDomain(gerenciarExecucao.iniciarServico(ordemServicoId, servicoOsId, usuarioId()));
    }

    @POST
    @Path("/{ordemServicoId}/servicos/{servicoOsId}/finalizar")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Finalizar a execução de um serviço da OS")
    public ExecucaoResponse finalizarServico(@PathParam("ordemServicoId") UUID ordemServicoId,
            @PathParam("servicoOsId") UUID servicoOsId) {
        return ExecucaoResponse.fromDomain(gerenciarExecucao.finalizarServico(ordemServicoId, servicoOsId));
    }

    @POST
    @Path("/{ordemServicoId}/concluir-execucao")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Concluir a execução e comunicar a finalização ao OS Service")
    public ExecucaoResponse concluirExecucao(@PathParam("ordemServicoId") UUID ordemServicoId) {
        return ExecucaoResponse.fromDomain(gerenciarExecucao.concluirExecucao(ordemServicoId, usuarioId()));
    }

    @POST
    @Path("/{ordemServicoId}/falha")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Reportar que a OS não pode ser concluída (dispara a compensação da Saga)")
    public ExecucaoResponse reportarFalha(@PathParam("ordemServicoId") UUID ordemServicoId,
            @Valid FalhaRequest request) {
        return ExecucaoResponse
                .fromDomain(gerenciarExecucao.reportarFalha(ordemServicoId, request.motivo(), usuarioId()));
    }

    private UUID usuarioId() {
        String usuarioId = jwt.getClaim("usuario_id");
        return usuarioId != null ? UUID.fromString(usuarioId) : null;
    }
}
