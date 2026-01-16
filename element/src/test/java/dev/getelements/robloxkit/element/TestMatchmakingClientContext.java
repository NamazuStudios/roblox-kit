package dev.getelements.robloxkit.element;

import jakarta.ws.rs.client.Client;

import java.util.List;

public record TestMatchmakingClientContext(TestMatchmakingClient client, String userId) {

    /**
     * A pre-registered set of Roblox user IDs for testing.
     */
    public static final List<String> TEST_ROBLOX_USERS = List.of(
            "10355133923",
            "10355127792",
            "10355168272",
            "10355177280"
    );

    /**
     * Creates a new TestMatchmakingClientContext with the given client and userId.
     * @param client the client
     * @param userId the user ID
     * @return the TestMatchmakingClientContext
     */
    public static TestMatchmakingClientContext with(final Client client, final String userId) {
        final var testMatchmakingClient = new TestMatchmakingClient(client);
        return new TestMatchmakingClientContext(testMatchmakingClient, userId);
    }

}
