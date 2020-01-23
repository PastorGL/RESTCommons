package io.github.pastorgl.rest.filters;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import static javax.ws.rs.HttpMethod.*;
import static javax.ws.rs.core.HttpHeaders.*;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class CORSFilter implements ContainerResponseFilter, ContainerRequestFilter {
    private static final String HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String HEADER_ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
    private static final String HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String HEADER_ORIGIN = "Origin";

    private static final String HEADER_ALLOWED_HEADERS = String.join(", ", HEADER_ORIGIN, CONTENT_TYPE, ACCEPT, AUTHORIZATION, WWW_AUTHENTICATE);
    private static final String HEADER_ALLOWED_METHODS = String.join(", ", GET, POST, PUT, DELETE, OPTIONS, HEAD);
    private static final String HEADER_EXPOSED_HEADERS = String.join(", ", WWW_AUTHENTICATE, AUTHORIZATION);

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        String origin = requestContext.getHeaderString(HEADER_ORIGIN);
        headers.add(HEADER_ACCESS_CONTROL_ALLOW_ORIGIN, (origin == null) ? "*" : origin);
        headers.add(HEADER_ACCESS_CONTROL_EXPOSE_HEADERS, HEADER_EXPOSED_HEADERS);

        if (requestContext.getMethod().equals(OPTIONS)) {
            headers.add(HEADER_ACCESS_CONTROL_ALLOW_METHODS, HEADER_ALLOWED_METHODS);
            headers.add(HEADER_ACCESS_CONTROL_ALLOW_HEADERS, HEADER_ALLOWED_HEADERS);
            headers.add(HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
            headers.add(HEADER_ACCESS_CONTROL_MAX_AGE, "1209600");
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (requestContext.getMethod().equals(OPTIONS)) {
            requestContext.abortWith(Response.status(Status.NO_CONTENT).build());
        }
    }
}
