package br.com.fiap.oficina.execution.core.exception;

import java.time.LocalDateTime;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Operação inválida para a fase atual da OS (ou conflito de gravação
 * concorrente): responde 409 em vez de 500.
 */
@Provider
public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {

    @Override
    public Response toResponse(IllegalStateException exception) {
        ErroResponse erroResponse = new ErroResponse(
                Response.Status.CONFLICT.getStatusCode(),
                "CONFLICT",
                exception.getMessage(),
                LocalDateTime.now());
        return Response.status(Response.Status.CONFLICT).entity(erroResponse).build();
    }
}
