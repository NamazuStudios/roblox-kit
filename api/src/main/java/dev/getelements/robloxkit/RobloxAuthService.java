package dev.getelements.robloxkit;

import dev.getelements.elements.sdk.annotation.ElementPublic;
import dev.getelements.elements.sdk.model.exception.NotFoundException;
import dev.getelements.robloxkit.model.RobloxProfile;
import dev.getelements.robloxkit.model.UserAuthRequest;
import dev.getelements.robloxkit.model.UserAuthResponse;

import java.util.Optional;

/**
 * Service interface for authenticating Roblox users.
 */
@ElementPublic
public interface RobloxAuthService {

    /**
     * The auth scheme used for Roblox user authentication.
     */
    String ROBLOX_AUTH_SCHEME = "dev.getelements.robloxkit.player.id";

    /**
     * The metadata key used to store Roblox profile information.
     */
    String ROBLOX_PROFILE_METADATA_KEY = "robloxProfile";

    /**
     * Fetches the Roblox profile for the given Roblox user ID.
     *
     * @param robloxUserId the Roblox user ID
     * @return the Roblox profile associated with the user ID
     */
    default RobloxProfile getRobloxProfile(String robloxUserId) {
        return findRobloxProfile(robloxUserId).orElseThrow(NotFoundException::new);
    }

    /**
     * Fetches the Roblox profile for the given Roblox user ID.
     *
     * @param robloxUserId the Roblox user ID
     * @return the Roblox profile associated with the user ID
     */
    Optional<RobloxProfile> findRobloxProfile(String robloxUserId);

    /**
     * Authenticates a Roblox user based on the provided authentication request.
     * @param authRequest the authentication request containing Roblox user ID and application info
     * @return the response containing user, profile, and session information
     */
    UserAuthResponse authenticateRobloxUser(UserAuthRequest authRequest);

}
