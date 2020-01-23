package io.github.pastorgl.rest.filters;

import io.github.pastorgl.rest.entity.AuthorizedUser;
import io.github.pastorgl.rest.entity.Token;
import io.github.pastorgl.rest.ErrorMessage;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.*;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Provider
@Priority(Priorities.USER)
public class WrappingWriterInterceptor implements WriterInterceptor {

    private UriInfo uriInfo;

    @Inject
    public WrappingWriterInterceptor(@Context UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        if (!uriInfo.getPath().startsWith("openapi.")) {
            MultivaluedMap<String, Object> headers = context.getHeaders();

            if (!headers.containsKey(HttpHeaders.AUTHORIZATION) && !headers.containsKey(HttpHeaders.WWW_AUTHENTICATE)) {

                Object entity = context.getEntity();

                if (!((entity instanceof AuthorizedUser)
                        || (entity instanceof Token)
                        || (entity instanceof ErrorMessage)
                        || (entity instanceof StreamingOutput))) {
                    context.setEntity(entity);
                    context.setMediaType(APPLICATION_JSON_TYPE);
                    context.setType(entity.getClass());
                }
            }
        }

        context.proceed();
    }
}
