package io.github.pastorgl.rest;

import io.github.pastorgl.rest.filters.CORSFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import static javax.ws.rs.HttpMethod.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CORSFilterTest {

    private ContainerRequestContext requestContext;

    @BeforeEach
    void setup() {
        requestContext = Mockito.mock(ContainerRequestContext.class);
    }

    @Test
    void requestFilterTest() {
        Mockito.when(requestContext.getMethod()).thenReturn(OPTIONS);
        Mockito.doAnswer(invocation -> {
            Response argument = invocation.getArgument(0);

            assertEquals(204, argument.getStatus());

            return null;
        }).when(requestContext).abortWith(Mockito.any(Response.class));

        CORSFilter corsFilter = new CORSFilter();
        corsFilter.filter(requestContext);
    }

    @Test
    void responseFilterTest() {
        Mockito.when(requestContext.getMethod()).thenReturn(OPTIONS);

        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        Mockito.when(responseContext.getHeaders()).thenReturn(headers);

        CORSFilter corsFilter = new CORSFilter();
        corsFilter.filter(requestContext, responseContext);

        assertTrue(headers.containsKey("Access-Control-Allow-Origin"));
        assertTrue(headers.containsKey("Access-Control-Allow-Credentials"));
        assertTrue(headers.containsKey("Access-Control-Allow-Headers"));
        assertTrue(headers.containsKey("Access-Control-Allow-Methods"));
        assertTrue(headers.containsKey("Access-Control-Max-Age"));
        assertTrue(headers.containsKey("Access-Control-Expose-Headers"));
    }

    @ParameterizedTest
    @ValueSource(strings = {GET, POST, DELETE, PUT, HEAD})
    void corsHeaderTest(String method) {
        Mockito.when(requestContext.getMethod()).thenReturn(method);

        ContainerResponseContext responseContext = Mockito.mock(ContainerResponseContext.class);
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        Mockito.when(responseContext.getHeaders()).thenReturn(headers);

        CORSFilter corsFilter = new CORSFilter();
        corsFilter.filter(requestContext, responseContext);

        assertTrue(headers.containsKey("Access-Control-Allow-Origin"));
        assertTrue(headers.containsKey("Access-Control-Expose-Headers"));
    }
}
