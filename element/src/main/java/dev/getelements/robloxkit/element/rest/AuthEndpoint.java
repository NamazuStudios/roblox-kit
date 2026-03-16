package dev.getelements.robloxkit.element.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.robloxkit.RobloxAuthService;
import dev.getelements.robloxkit.model.UserAuthRequest;
import dev.getelements.robloxkit.model.UserAuthResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import static dev.getelements.robloxkit.element.rest.RobloxKitApplication.OPENAPI_TAG;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/auth")
@Tag(name = OPENAPI_TAG)
@Produces(APPLICATION_JSON)
public class AuthEndpoint {

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public UserAuthResponse createsSession(final UserAuthRequest request) {

        final var authService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxAuthService.class);

        return authService.authenticateRobloxUser(request);

    }

}
