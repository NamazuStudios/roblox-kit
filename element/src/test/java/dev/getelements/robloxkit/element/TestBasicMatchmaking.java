package dev.getelements.robloxkit.element;

import dev.getelements.elements.sdk.dao.ApplicationConfigurationDao;
import dev.getelements.elements.sdk.model.application.MatchmakingApplicationConfiguration;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.List;

import static dev.getelements.robloxkit.element.TestMatchmakingClientContext.TEST_ROBLOX_USERS;
import static dev.getelements.robloxkit.element.TestMatchmakingServer.APPLICATION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class TestBasicMatchmaking {

    public static final String CONFIGURATION = "basic";

    private final TestMatchmakingServer server = TestMatchmakingServer.getInstance();

    private Client client;

    private List<TestMatchmakingClientContext> clients;

    private MatchmakingApplicationConfiguration configuration;

    @BeforeClass
    public void setupClients() {
        client = ClientBuilder.newBuilder().build();
        clients = TEST_ROBLOX_USERS
                .stream()
                .map(userId -> TestMatchmakingClientContext.with(client, userId))
                .toList();
    }

    @BeforeClass
    public void setupConfiguration() {
        final var dao = server.getDao(ApplicationConfigurationDao.class);
        configuration = new MatchmakingApplicationConfiguration();
        configuration.setName(CONFIGURATION);
        configuration.setParent(server.getApplication());
        configuration.setMaxProfiles(TEST_ROBLOX_USERS.size());
        configuration.setDescription("Basic Matchmaking Configuration");
        configuration = dao.createApplicationConfiguration(APPLICATION, configuration);
    }

    @AfterClass
    public void teardownClients() {
        client.close();
    }

    @DataProvider
    public Object[][] allClients() {
        return clients
                .stream()
                .map(c -> new Object[]{c})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "allClients")
    public void testSignIn(final TestMatchmakingClientContext context) {

        final var client = context.client();

        // Attempt to log in the user
        final var result = client.login(context.userId());
        assertTrue(client.isLoggedOn());

        // Check that logging in again with the same user ID returns the same user
        final var secondResult = client.login(context.userId());
        assertTrue(client.isLoggedOn());
        assertEquals(result.getUser().getId(), secondResult.getUser().getId());

    }

    @Test(dataProvider = "allClients", dependsOnMethods = "testSignIn")
    public void testFindMatch(final TestMatchmakingClientContext context) {

        final var client = context.client();
        assertTrue(client.isLoggedOn());

        final var findMatchResponse = client.findMatch(configuration.getName());
        assertNotNull(findMatchResponse);

    }

}
