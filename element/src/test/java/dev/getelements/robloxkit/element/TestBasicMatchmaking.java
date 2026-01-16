package dev.getelements.robloxkit.element;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

public class TestBasicMatchmaking {

    private final TestMatchmakingServer server = TestMatchmakingServer.getInstance();

    private Client client;

    private List<TestMatchmakingClientContext> clients;

    @BeforeClass
    public void setupClients() {
        client = ClientBuilder.newBuilder().build();
        clients = TestMatchmakingClientContext.TEST_ROBLOX_USERS
                .stream()
                .map(userId -> TestMatchmakingClientContext.with(client, userId))
                .toList();
    }

    @AfterClass
    public void teardownClients() {
        client.close();
    }

    @DataProvider
    public Object[][] allClients() {
        return clients.stream().map(c -> new Object[]{c}).toArray(Object[][]::new);
    }

    @Test(dataProvider = "allClients")
    public void testSignIn(final TestMatchmakingClientContext context) throws Exception {

        final var client = context.client();

        // Attempt to log in the user
        final var result = client.login(context.userId());
        assertTrue(client.isLoggedOn());

        // Check that logging in again with the same user ID returns the same user
        final var secondResult = client.login(context.userId());
        assertTrue(client.isLoggedOn());
        assertEquals(result.getUser().getId(), secondResult.getUser().getId());

    }

}
