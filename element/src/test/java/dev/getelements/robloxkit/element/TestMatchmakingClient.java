package dev.getelements.robloxkit.element;

import dev.getelements.robloxkit.model.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;

import static dev.getelements.robloxkit.element.TestMatchmakingServer.*;
import static dev.getelements.robloxkit.element.rest.SimpleRobloxSecurityFilter.ROBLOX_SECURITY_HEADER;

public class TestMatchmakingClient {

    private final String url;

    private final Client client;

    private final String application;

    private final String configuration;

    private final String robloxSecret;

    private UserAuthResponse userAuthResponse;

    public TestMatchmakingClient(final Client client) {
        this(client, URL, APPLICATION, CONFIGURATION, TEST_ROBLOX_SECRET);
    }

    public TestMatchmakingClient(
            final Client client,
            final String url,
            final String application,
            final String configuration,
            final String robloxSecret) {
        this.url = url;
        this.client = client;
        this.application = application;
        this.configuration = configuration;
        this.robloxSecret = robloxSecret;
    }

    /**
     * True if the user is logged on.
     *
     * @return the logged on status
     */
    public boolean isLoggedOn() {
        return userAuthResponse != null;
    }

    /**
     * Logs in the user.
     *
     * @return the match status response
     */
    public UserAuthResponse login(final String robloxUserId) {
        final var authRequest = new UserAuthRequest();
        authRequest.setApplication(application);
        authRequest.setRobloxUserId(robloxUserId);
        return login(authRequest);
    }

    /**
     * Logs in the user.
     *
     * @return the match status response
     */
    public UserAuthResponse login(final UserAuthRequest userAuthRequest) {

        final var entity = Entity.json(userAuthRequest);

        return userAuthResponse = client
                .target("%s/auth".formatted(url))
                .request()
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .post(entity)
                .readEntity(UserAuthResponse.class);

    }

    /**
     * Finds a match for the user.
     *
     * @param request the request
     * @return the match status
     */
    public MatchStatusResponse findMatch(FindMatchRequest request) {

        if (request == null) {
            request = new FindMatchRequest();
        }

        if (request.getSessionKey() == null) {
            request.setSessionKey(userAuthResponse.getSession().getSessionSecret());
        }

        if (request.getConfiguration() == null) {
            request.setConfiguration(configuration);
        }

        final var entity = Entity.json(request);

        return client
                .target("%s/match".formatted(url))
                .request()
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .post(entity)
                .readEntity(MatchStatusResponse.class);

    }

    /**
     * Polls the match status.
     *
     * @param matchId the match ID
     * @return the match status
     */
    public MatchStatusResponse pollMatch(final String matchId) {
        return client
                .target("%s/match/%s".formatted(url, matchId))
                .request()
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .get()
                .readEntity(MatchStatusResponse.class);

    }

    /**
     * Updates the match.
     *
     * @param matchId the match ID
     * @param update the update request
     * @return the match status
     */
    public MatchStatusResponse updateMatch(final String matchId, final UpdateMatchRequest update) {

        final var entity = Entity.json(update);

        return client
                .target("%s/match/%s".formatted(url, matchId))
                .request()
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .put(entity)
                .readEntity(MatchStatusResponse.class);

    }

}
