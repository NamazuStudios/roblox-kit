package dev.getelements.robloxkit.element;

import dev.getelements.robloxkit.model.*;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static dev.getelements.robloxkit.element.TestMatchmakingServer.*;
import static dev.getelements.robloxkit.element.rest.SimpleRobloxSecurityFilter.ROBLOX_SECURITY_HEADER;
import static jakarta.ws.rs.core.HttpHeaders.AUTHORIZATION;

/**
 * Test client for matchmaking.
 */
public class TestMatchmakingClient {

    private static final Logger logger = LoggerFactory.getLogger(TestMatchmakingClient.class);

    private final String url;

    private final Client client;

    private final String application;

    private final String robloxSecret;

    private UserAuthResponse userAuthResponse;

    private MatchStatusResponse matchStatusResponse;

    public TestMatchmakingClient(final Client client) {
        this(client, URL, APPLICATION, TEST_ROBLOX_SECRET);
    }

    public TestMatchmakingClient(
            final Client client,
            final String url,
            final String application,
            final String robloxSecret) {
        this.url = url;
        this.client = client;
        this.application = application;
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
     * True if the user is in a match.
     *
     * @return the in-match status
     */
    public boolean isInMatch() {
        return matchStatusResponse != null;
    }

    /**
     * Gets the last recorded {@link MatchStatusResponse}.
     *
     * @return the match status
     */
    public MatchStatusResponse getMatchStatusResponse() {
        return matchStatusResponse;
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

        final var httpResponse = client
                .target("%s/auth".formatted(url))
                .request()
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .post(entity);

        if (httpResponse.getStatus() == 200) {
            return userAuthResponse = httpResponse.readEntity(UserAuthResponse.class);
        } else {

            final var responseText = httpResponse.readEntity(String.class);

            logger.error("Error response logging in {} - {}",
                    httpResponse.getStatus(),
                    responseText
            );

            return null;
        }

    }

    /**
     * Finds a match for the user.
     *
     * @return the match status
     */
    public MatchStatusResponse findMatch(final String configuration) {

        if (!isLoggedOn()) {
            throw new IllegalStateException("User must be logged on to find a match.");
        }

        if (isInMatch()) {
            throw new IllegalStateException("User is already in a match.");
        }

        final var request = new FindMatchRequest();
        request.setConfiguration(configuration);

        final var entity = Entity.json(request);
        final var authorization = "Bearer %s".formatted(userAuthResponse.getSession().getSessionSecret());

        final var response =  client
                .target("%s/match".formatted(url))
                .request()
                .header(AUTHORIZATION, authorization)
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .post(entity);

        if (response.getStatus() == 200) {
            return matchStatusResponse = response.readEntity(MatchStatusResponse.class);
        } else {

            logger.error("Error response finding match {} - {}",
                    response.getStatus(),
                    response.readEntity(String.class)
            );

            return null;

        }

    }

    /**
     * Polls the current match.
     *
     * @return the match status
     */
    public MatchStatusResponse pollMatch() {

        if (!isInMatch()) {
            throw new IllegalStateException("User must be logged on to find a match.");
        }

        return pollMatch(matchStatusResponse.getMultiMatch().getId());

    }

    /*
     * Polls the match status.
     *
     * @param matchId the match ID
     * @return the match status
     */
    public MatchStatusResponse pollMatch(final String matchId) {

        if (!isLoggedOn()) {
            throw new IllegalStateException("User must be logged on to poll a match.");
        } else if (isInMatch() && !matchStatusResponse.getMultiMatch().getId().equals(matchId)) {
            throw new IllegalStateException("Client is already in match %s. Cannot poll a match %s.".formatted(
                    matchStatusResponse.getMultiMatch().getId(),
                    matchId)
            );
        }

        final var authorization = "Bearer %s".formatted(userAuthResponse.getSession().getSessionSecret());

        final var response = client
                .target("%s/match/%s".formatted(url, matchId))
                .request()
                .header(AUTHORIZATION, authorization)
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .get();

        if (response.getStatus() == 200) {
            return matchStatusResponse = response.readEntity(MatchStatusResponse.class);
        } else {
            logger.error("Error response polling match {} - {}",
                    response.getStatus(),
                    response.readEntity(String.class)
            );
            return null;
        }

    }

    /**
     * Updates the current match.
     *
     * @param updateMatchRequest the update request
     * @return the match status
     */
    public MatchStatusResponse updateCurrentMatch(final UpdateMatchRequest updateMatchRequest) {

        if (!isInMatch()) {
            throw new IllegalStateException("User must be logged on to find a match.");
        }

        return updateMatch(matchStatusResponse.getMultiMatch().getId(), updateMatchRequest);

    }

    /**
     * Updates the match.
     *
     * @param matchId the match ID
     * @param update the update request
     * @return the match status
     */
    public MatchStatusResponse updateMatch(final String matchId, final UpdateMatchRequest update) {

        if (!isLoggedOn()) {
            throw new IllegalStateException("User must be logged on to poll a match.");
        } else if (isInMatch() && !matchStatusResponse.getMultiMatch().getId().equals(matchId)) {
            throw new IllegalStateException("Client is already in match %s. Cannot poll a match %s.".formatted(
                    matchStatusResponse.getMultiMatch().getId(),
                    matchId)
            );
        }

        final var entity = Entity.json(update);
        final var authorization = "Bearer %s".formatted(userAuthResponse.getSession().getSessionSecret());

        final var response =  client
                .target("%s/match/%s".formatted(url, matchId))
                .request()
                .header(AUTHORIZATION, authorization)
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .put(entity);

        if (response.getStatus() == 200) {
            return matchStatusResponse = response.readEntity(MatchStatusResponse.class);
        } else {

            logger.error("Error response updating match {} - {}",
                    response.getStatus(),
                    response.readEntity(String.class)
            );

            return null;

        }

    }

    /**
     * Leaves the current match.
     *
     * @return the match status
     */
    public MatchStatusResponse leaveMatch() {

        if (!isInMatch()) {
            throw new IllegalStateException("User must be logged on to find a match.");
        }

        return leaveMatch(matchStatusResponse.getMultiMatch().getId());

    }

    /**
     * Leaves the match.
     *
     * @param matchId the match id
     * @return the match status
     */
    public MatchStatusResponse leaveMatch(final String matchId) {

        if (!isLoggedOn()) {
            throw new IllegalStateException("User must be logged on to find a match.");
        }

        final var profileId = userAuthResponse.getProfile().getId();
        final var authorization = "Bearer %s".formatted(userAuthResponse.getSession().getSessionSecret());

        final var response =  client
                .target("%s/match/%s/%s".formatted(url, matchId, profileId))
                .request()
                .header(AUTHORIZATION, authorization)
                .header(ROBLOX_SECURITY_HEADER, robloxSecret)
                .delete();

        if (response.getStatus() == 200) {
            matchStatusResponse = null;
            return response.readEntity(MatchStatusResponse.class);
        } else {

            logger.error("Error response leaving match {} - {}",
                    response.getStatus(),
                    response.readEntity(String.class)
            );

            return null;

        }

    }

}
