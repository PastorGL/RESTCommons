package io.github.pastorgl.rest;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import io.github.pastorgl.rest.entity.AuthorizedUser;
import io.github.pastorgl.rest.entity.Role;
import io.github.pastorgl.rest.entity.Token;
import io.github.pastorgl.rest.filters.AuthHeader;
import io.github.pastorgl.rest.init.GlobalConfig;
import io.logz.guice.jersey.JerseyModule;
import io.logz.guice.jersey.JerseyServer;
import io.logz.guice.jersey.configuration.JerseyConfiguration;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WrappingWriterInterceptorTest {

    private final String[] PROVIDER_CLASSNAMES = new String[]{
            RolesAllowedDynamicFeature.class.getCanonicalName()
    };
    private final String[] PROVIDER_PACKAGES = new String[]{
            "io.github.pastorgl.rest"
    };
    private JerseyServer server;
    private JerseyClient client;

    private void configureServer() {
        ResourceConfig resourceConfig = new ResourceConfig()
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .property(ServerProperties.PROVIDER_PACKAGES, PROVIDER_PACKAGES)
                .property(ServerProperties.PROVIDER_CLASSNAMES, PROVIDER_CLASSNAMES)
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);

        JerseyConfiguration configuration = JerseyConfiguration.builder()
                .withResourceConfig(resourceConfig)
                .addHost("localhost", 9999)
                .build();

        List<Module> modules = new ArrayList<>();
        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                Properties properties = new Properties();
                properties.setProperty(GlobalConfig.PROPERTY_AUTH_CHECK_ENDPOINT, "http://localhost:9999/test/check");
                bind(Properties.class).toInstance(properties);
            }
        });
        modules.add(new JerseyModule(configuration));

        server = Guice.createInjector(modules).getInstance(JerseyServer.class);
    }

    @BeforeEach
    public void beforeTest() throws Exception {
        configureServer();
        server.start();
        client = new JerseyClientBuilder().build();
    }

    @AfterEach
    public void afterTest() throws Exception {
        client.close();
        server.stop();
    }

    @Test
    public void getToken() {
        Response response = client.target("http://localhost:9999/test/token").request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + Role.USER.name() + "user-uuid1")
                .get();

        assertEquals(200, response.getStatus());

        assertDoesNotThrow(() -> response.readEntity(TestToken.class));
    }

    @Test
    public void validationEntity() {
        Response response = client.target("http://localhost:9999/test/validation/entity").request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + Role.USER.name() + "user-uuid1")
                .post(Entity.entity(new TestEntity(), MediaType.APPLICATION_JSON_TYPE));

        assertEquals(400, response.getStatus());

        assertDoesNotThrow(() -> response.readEntity(new GenericType<ErrorMessage>() {
        }));
    }

    @Test
    public void validationParam() {
        Response response = client.target("http://localhost:9999/test/validation/query-param")
                .queryParam("message").request()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + Role.USER.name() + "user-uuid1")
                .get();

        assertEquals(400, response.getStatus());

        assertDoesNotThrow(() -> response.readEntity(new GenericType<ErrorMessage>() {
        }));
    }

    @Test
    public void webApplicationException() {
        Response response = client.target("http://localhost:9999/not-found").request().get();

        assertEquals(404, response.getStatus());

        ErrorMessage errorMessage = response.readEntity(new GenericType<ErrorMessage>() {
        });

        assertNotNull(errorMessage.getError());
    }

    @Test
    public void internalError() {
        Response response = client.target("http://localhost:9999/test/internal-error").request().get();

        assertEquals(500, response.getStatus());

        ErrorMessage errorMessage = assertDoesNotThrow(() -> response.readEntity(new GenericType<ErrorMessage>() {
        }));

        assertNotNull(errorMessage.getError());
    }

    @Test
    public void faultTest() {
        for (int i = 0; i < 2; i++) {
            Response tokenResponse = client.target("http://localhost:9999/test/token").request()
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + Role.USER.name() + "user-uuid1")
                    .get();

            assertEquals(200, tokenResponse.getStatus());

            Response notFoundResponse = client.target("http://localhost:9999/not-found").request().get();

            assertEquals(404, notFoundResponse.getStatus());
        }
    }

    @Path("test")
    @Produces(MediaType.APPLICATION_JSON)
    public static class TestEndpoint {

        @GET
        @Path("token")
        public Response getToken() {
            return Response.ok(new TestToken()).build();
        }

        @GET
        @Path("validation/query-param")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response failValidation(@QueryParam("message") @NotNull @NotBlank String entity) {
            return Response.ok(entity).build();
        }

        @POST
        @Path("validation/entity")
        @Consumes(MediaType.APPLICATION_JSON)
        public Response failValidation(@Valid TestEntity entity) {
            return Response.ok(entity).build();
        }

        @GET
        @Path("internal-error")
        public Response internalError() {
            throw new NullPointerException();
        }

        @POST
        @Path("check")
        public Response check(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
            AuthHeader authHeader = AuthHeader.parse(authorization);
            String[] params = authHeader.param.split(" ");
            return Response.ok(new AuthorizedUser(params[1], "test@email.com", Role.valueOf(params[0]), "foo")).build();
        }
    }

    @JsonRootName("testEntity")
    public static class TestEntity {

        @NotNull
        String name;

        public String getName() {
            return name;
        }
    }

    public static class TestToken implements Token {
        String access;
        String refresh;
        long expirationDate;

        public TestToken() {
        }

        @Override
        public String getAccess() {
            return access;
        }

        @Override
        public String getRefresh() {
            return refresh;
        }

        @Override
        public long getExpirationDate() {
            return expirationDate;
        }
    }

}
