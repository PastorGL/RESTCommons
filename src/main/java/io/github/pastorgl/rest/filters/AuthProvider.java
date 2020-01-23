package io.github.pastorgl.rest.filters;

import io.github.pastorgl.rest.entity.AuthorizedUser;
import io.github.pastorgl.rest.init.GlobalConfig;
import org.glassfish.jersey.client.JerseyClientBuilder;

import javax.annotation.Priority;
import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Method;
import java.security.Principal;
import java.util.Properties;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AuthProvider implements ContainerRequestFilter {
    static public Response INTERNAL_ERROR = Response.status(Status.INTERNAL_SERVER_ERROR).build();
    static public Response FORBIDDEN = Response.status(Status.FORBIDDEN).build();
    static public Response NOT_FOUND = Response.status(Status.NOT_FOUND).build();

    private ResourceInfo resourceInfo;
    private UriInfo uriInfo;
    private String authCheckEndpoint;

    @Inject
    public AuthProvider(Properties properties, @Context ResourceInfo resourceInfo, @Context UriInfo uriInfo) {
        this.resourceInfo = resourceInfo;
        this.uriInfo = uriInfo;
        this.authCheckEndpoint = properties.getProperty(GlobalConfig.PROPERTY_AUTH_CHECK_ENDPOINT);
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (uriInfo.getPath().startsWith("openapi.") || uriInfo.getAbsolutePath().toString().startsWith(authCheckEndpoint)) {
            return;
        }

        Method method = resourceInfo.getResourceMethod();
        if (method.isAnnotationPresent(DenyAll.class)) {
            requestContext.abortWith(FORBIDDEN);
        } else if (method.isAnnotationPresent(PermitAll.class) || method.isAnnotationPresent(RolesAllowed.class)) {
            MultivaluedMap<String, String> headers = requestContext.getHeaders();
            String authorization = headers.getFirst(AUTHORIZATION);

            AuthHeader authHeader = AuthHeader.parse(authorization);

            if (AuthHeader.EMPTY_AUTH_HEADER.equals(authHeader)) {
                // Returning request for authorization
                requestContext.abortWith(Response.status(Status.UNAUTHORIZED).build());

                return;
            }

            if (authHeader.scheme == AuthScheme.BEARER) {
                try {
                    AuthorizedUser user = callAuthCheckEndpoint(authCheckEndpoint, authHeader);

                    if (user == null) {
                        requestContext.abortWith(FORBIDDEN);

                        return;
                    }

                    boolean allowed = false;

                    if (method.isAnnotationPresent(RolesAllowed.class)) {
                        String[] roles = method.getAnnotation(RolesAllowed.class).value();

                        String userRole = user.getRole().name();
                        for (String role : roles) {
                            if (userRole.equalsIgnoreCase(role)) {
                                allowed = true;
                                break;
                            }
                        }

                        if (!allowed) {
                            requestContext.abortWith(FORBIDDEN);

                            return;
                        }
                    }

                    requestContext.setSecurityContext(new SecurityContext() {
                        @Override
                        public Principal getUserPrincipal() {
                            return user;
                        }

                        @Override
                        public boolean isUserInRole(String role) {
                            return true;
                        }

                        @Override
                        public boolean isSecure() {
                            return "https".equals(uriInfo.getRequestUri().getScheme());
                        }

                        @Override
                        public String getAuthenticationScheme() {
                            return "Bearer";
                        }
                    });
                } catch (Exception ignored) {
                    requestContext.abortWith(FORBIDDEN);
                }
            } else {
                requestContext.abortWith(FORBIDDEN);
            }
        }
    }

    protected AuthorizedUser callAuthCheckEndpoint(String authCheckEndpoint, AuthHeader authHeader) {
        return new JerseyClientBuilder().build()
                .target(authCheckEndpoint + "/check")
                .request()
                .header(AUTHORIZATION, "bearer " + authHeader.param)
                .post(null)
                .readEntity(AuthorizedUser.class);
    }
}
