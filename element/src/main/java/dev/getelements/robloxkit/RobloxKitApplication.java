package dev.getelements.robloxkit;

import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.annotation.ElementServiceExport;
import dev.getelements.elements.sdk.annotation.ElementServiceImplementation;
import dev.getelements.robloxkit.rest.AuthEndpoint;
import dev.getelements.robloxkit.rest.MatchEndpoint;
import dev.getelements.robloxkit.rest.RobloxExceptionMapper;
import dev.getelements.robloxkit.rest.SimpleRobloxSecurityFilter;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.server.ServerProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@ElementServiceImplementation
@ElementServiceExport(Application.class)
public class RobloxKitApplication extends Application {

    @ElementDefaultAttribute("true")
    public static final String AUTH_ENABLED = "dev.getelements.elements.auth.enabled";

    @ElementDefaultAttribute("example-element")
    public static final String APPLICATION_PREFIX = "dev.getelements.elements.app.serve.prefix";

    public static final String OPENAPI_TAG = "RobloxKit";

    /**
     * Here we register all the classes that we want to be included in the Element.
     */
    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                AuthEndpoint.class,
                MatchEndpoint.class,
                RobloxExceptionMapper.class,
                OpenApiResource.class,
                SimpleRobloxSecurityFilter.class,
                OpenAPISecurityConfig.class
        );
    }

    @Override
    public Map<String,Object> getProperties() {
        final Map<String,Object> props = new HashMap<>();
        props.put(ServerProperties.MOXY_JSON_FEATURE_DISABLE, true);
        return props;
    }

}
