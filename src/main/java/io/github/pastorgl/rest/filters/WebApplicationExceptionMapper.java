package io.github.pastorgl.rest.filters;

import io.github.pastorgl.rest.ErrorMessage;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response exceptionResponse = exception.getResponse();
        ErrorMessage errorMessage = new ErrorMessage(exception.getLocalizedMessage());

        return Response.status(exceptionResponse.getStatus())
                .type(MediaType.APPLICATION_JSON_TYPE)
                .entity(errorMessage)
                .build();
    }
}
