package io.github.pastorgl.rest.filters;

import io.github.pastorgl.rest.ErrorMessage;

import javax.validation.*;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {
    private static final Logger LOGGER = Logger.getLogger(ValidationExceptionMapper.class.getName());

    private static Response.Status getResponseStatus(final ConstraintViolationException violation) {
        final Iterator<ConstraintViolation<?>> iterator = violation.getConstraintViolations().iterator();

        if (iterator.hasNext()) {
            for (final Path.Node node : iterator.next().getPropertyPath()) {
                final ElementKind kind = node.getKind();

                if (ElementKind.RETURN_VALUE.equals(kind)) {
                    return Response.Status.INTERNAL_SERVER_ERROR;
                }
            }
        }

        return Response.Status.BAD_REQUEST;
    }

    @Override
    public Response toResponse(ValidationException exception) {
        if (exception instanceof ConstraintViolationException) {
            LOGGER.log(Level.FINER, "Constraint validation exception", exception);

            ConstraintViolationException cve = (ConstraintViolationException) exception;
            final Response.ResponseBuilder response = Response.status(getResponseStatus(cve));

            response.type(MediaType.APPLICATION_JSON_TYPE);
            response.entity(new GenericEntity<>(
                    wrapException(cve),
                    new GenericType<ErrorMessage>() {
                    }.getType()
            ));

            return response.build();
        } else {
            LOGGER.log(Level.WARNING, "Validation exception", exception);

            return Response.serverError().entity(exception.getMessage()).build();
        }
    }

    private ErrorMessage wrapException(ConstraintViolationException exception) {
        StringBuilder message = new StringBuilder();
        for (ConstraintViolation<?> cv : exception.getConstraintViolations()) {
            message.append(cv.getPropertyPath()).append(" ").append(cv.getMessage()).append("\n");
        }

        return new ErrorMessage(message.toString());
    }
}
