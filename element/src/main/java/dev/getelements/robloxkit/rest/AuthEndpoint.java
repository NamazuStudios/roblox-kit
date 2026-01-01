package dev.getelements.robloxkit.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.robloxkit.RobloxAuthService;
import dev.getelements.robloxkit.model.UserAuthRequest;
import dev.getelements.robloxkit.model.UserAuthResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import static dev.getelements.robloxkit.RobloxKitApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/roblox/auth")
public class AuthEndpoint {

    @POST
    public UserAuthResponse createsSession(final UserAuthRequest request) {

        final var authService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxAuthService.class);

        return authService.authenticateRobloxUser(request);

    }

}
