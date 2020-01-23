package io.github.pastorgl.rest;

import io.github.pastorgl.rest.entity.AuthorizedUser;
import io.github.pastorgl.rest.entity.Role;
import io.github.pastorgl.rest.filters.AuthHeader;
import io.github.pastorgl.rest.filters.AuthProvider;
import io.github.pastorgl.rest.init.GlobalConfig;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class AuthProviderTest {

    @Mock
    UriInfo uriInfo;

    @Mock
    ResourceInfo resourceInfo;

    @Mock
    ContainerRequestContext requestContext;

    @InjectMocks
    TestAuthProvider authenticationProvider;

    @ParameterizedTest
    @EnumSource(value = Role.class)
    void successfulAuthorizationTest(Role role) {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(method.isAnnotationPresent(PermitAll.class)).thenReturn(true);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn("Bearer " + role.name());
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        authenticationProvider.filter(requestContext);

        verify(requestContext, atLeastOnce()).setSecurityContext(any());
        verify(requestContext, never()).abortWith(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "Digest", "Basic", "", " ", "foo"})
    void wrongHeaderTest(String header) {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn(header);
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        doAnswer((r) -> {
            Response argument = r.getArgument(0);

            assertEquals(Status.UNAUTHORIZED.getStatusCode(), argument.getStatus());

            return null;
        }).when(requestContext).abortWith(any());

        authenticationProvider.filter(requestContext);

        verify(requestContext, never()).setSecurityContext(any());
    }

    @Test
    void digestAuthTest() {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn("Digest ");
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        doAnswer((r) -> {
            Response argument = r.getArgument(0);

            assertEquals(Status.FORBIDDEN.getStatusCode(), argument.getStatus());

            return null;
        }).when(requestContext).abortWith(any());

        authenticationProvider.filter(requestContext);

        verify(requestContext, never()).setSecurityContext(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer ", "Bearer wrong"})
    void wrongTokenTest(String headerParam) {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn(headerParam);
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        doAnswer((r) -> {
            Response argument = r.getArgument(0);

            assertEquals(Status.FORBIDDEN.getStatusCode(), argument.getStatus());

            return null;
        }).when(requestContext).abortWith(any());

        authenticationProvider.filter(requestContext);

        verify(requestContext, never()).setSecurityContext(any());
    }

    @Test
    void noRolesTest() {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(method.isAnnotationPresent(any())).thenReturn(false);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn("BEARER USER");
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        authenticationProvider.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void permitAllTest() {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(method.isAnnotationPresent(PermitAll.class)).thenReturn(true);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn("BEARER USER");
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        authenticationProvider.filter(requestContext);

        verify(requestContext, never()).abortWith(any());
    }

    @Test
    void denyAllTest() {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(method.isAnnotationPresent(DenyAll.class)).thenReturn(true);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn("BEARER USER");
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        authenticationProvider.filter(requestContext);

        verify(requestContext, atLeastOnce()).abortWith(any());
    }

    @Test
    void rolesAllowedTest() {
        Method method = mock(Method.class);
        when(resourceInfo.getResourceMethod()).thenReturn(method);
        when(resourceInfo.getResourceClass()).thenReturn((Class) Class.class);
        when(method.isAnnotationPresent(RolesAllowed.class)).thenReturn(true);

        MultivaluedMap<String, String> mockedHeaders = mock(MultivaluedStringMap.class);
        when(mockedHeaders.getFirst(any())).thenReturn("BEARER USER");
        when(requestContext.getHeaders()).thenReturn(mockedHeaders);

        authenticationProvider.filter(requestContext);

        verify(requestContext, atLeastOnce()).abortWith(any());
    }

    @BeforeEach
    void init() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromUri("http://example.com"));
        when(uriInfo.getPath()).thenReturn("path");
        when(uriInfo.getAbsolutePath()).thenReturn(new URL("http://not-null").toURI());
    }

    private static class TestAuthProvider extends AuthProvider {
        @Inject
        public TestAuthProvider(Properties properties, @Context ResourceInfo resourceInfo, @Context UriInfo uriInfo) {
            super(new Properties() {{setProperty(GlobalConfig.PROPERTY_AUTH_CHECK_ENDPOINT, "not-null");}}, resourceInfo, uriInfo);
        }

        @Override
        public AuthorizedUser callAuthCheckEndpoint(String authCheckEndpoint, AuthHeader authHeader) {
            return new AuthorizedUser("test-uuid", "test@email.com", Role.valueOf(authHeader.param), "name");
        }
    }
}
