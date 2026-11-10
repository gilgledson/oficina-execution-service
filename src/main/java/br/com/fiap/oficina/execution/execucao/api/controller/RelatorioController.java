package br.com.fiap.oficina.execution.execucao.api.controller;

import java.util.List;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import br.com.fiap.oficina.execution.execucao.application.usecase.RelatorioExecucaoUseCase;
import br.com.fiap.oficina.execution.execucao.domain.RelatorioTempoMedioServico;
import br.com.fiap.oficina.execution.shared.security.Perfis;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;

@Path("/api/relatorios")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Relatórios", description = "Métricas e relatórios operacionais")
@RequiredArgsConstructor
public class RelatorioController {

    private final RelatorioExecucaoUseCase relatorios;

    @GET
    @Path("/esforco-os/{osId}")
    @RolesAllowed({ Perfis.ADMIN, Perfis.MECANICO })
    @Operation(summary = "Obter esforço total de uma OS", description = "Total de serviços realizados e tempo total de esforço em minutos.")
    public Response obterEsforcoPorOs(@PathParam("osId") UUID osId) {
        return relatorios.esforcoPorOs(osId)
                .map(r -> Response.ok(r).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("/tempo-medio-servicos")
    @RolesAllowed({ Perfis.ADMIN })
    @Operation(summary = "Obter tempo médio de execução por serviço", description = "Média histórica de tempo de execução de cada serviço do catálogo.")
    public List<RelatorioTempoMedioServico> obterTemposMedios() {
        return relatorios.tempoMedioPorServico();
    }
}
