package dev.getelements.robloxkit.element;

import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UserAuthResponse;
import jakarta.ws.rs.client.Client;

public class TestMatchmakingClient {

    private final String url;

    private final Client client;

    private UserAuthResponse userAuthResponse;

    public TestMatchmakingClient(final String url, final Client client) {
        this.url = url;
        this.client = client;
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
     * Finds a match for the user.
     *
     * @param request the request
     * @return the match status
     */
    public MatchStatusResponse findMatch(final FindMatchRequest request) {
        return null;
    }

    /**
     * Polls the match status.
     *
     * @param matchId the match ID
     * @return the match status
     */
    public MatchStatusResponse pollMatch(final String matchId) {
        return null;
    }

}
