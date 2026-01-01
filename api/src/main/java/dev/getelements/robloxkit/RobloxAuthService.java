package dev.getelements.robloxkit;

import dev.getelements.robloxkit.model.UserAuthRequest;
import dev.getelements.robloxkit.model.UserAuthResponse;

/**
 * Service interface for authenticating Roblox users.
 */
public interface RobloxAuthService {

    /**
     * Authenticates a Roblox user based on the provided authentication request.
     * @param authRequest the authentication request containing Roblox user ID and application info
     * @return the response containing user, profile, and session information
     */
    UserAuthResponse authenticateRobloxUser(UserAuthRequest authRequest);

}
