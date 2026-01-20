package dev.getelements.robloxkit.element;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.getelements.elements.sdk.dao.ApplicationConfigurationDao;
import dev.getelements.elements.sdk.model.application.MatchmakingApplicationConfiguration;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static dev.getelements.robloxkit.element.TestMatchmakingClientContext.TEST_ROBLOX_USERS;
import static dev.getelements.robloxkit.element.TestMatchmakingServer.APPLICATION;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertTrue;

public class TestBasicMatchmaking {

    private static final Logger logger = LoggerFactory.getLogger(TestBasicMatchmaking.class);

    public static long SERVER_CREATE_DELAY = 10000;

    public static long SERVER_CREATE_POLL_INTERVAL = 1000;

    public static final String CONFIGURATION = "basic";

    private final Set<String> matchIds = ConcurrentHashMap.newKeySet();

    private final String mockReservedServerId = UUID.randomUUID().toString();

    private final TestMatchmakingServer server = TestMatchmakingServer.getInstance();

    private Client client;

    private List<TestMatchmakingClientContext> clients;

    private MatchmakingApplicationConfiguration configuration;

    @BeforeClass
    public void setupClients() {

        final ObjectMapper mapper = new ObjectMapper();

        client = ClientBuilder.newBuilder()
                .register(new JacksonFeature())
                .build();

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

    @DataProvider(parallel = true)
    public Object[][] allPlayers() {
        return clients
                .stream()
                .map(c -> new Object[]{c})
                .toArray(Object[][]::new);
    }

    @DataProvider(parallel = true)
    public Object[][] allHosts() {
        return clients
                .stream()
                .limit(1)
                .map(c -> new Object[]{c})
                .toArray(Object[][]::new);
    }

    @DataProvider(parallel = true)
    public Object[][] allClients() {
        return clients
                .stream()
                .skip(1)
                .map(c -> new Object[]{c})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "allPlayers", threadPoolSize = 4)
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

    @Test(dataProvider = "allHosts", dependsOnMethods = "testSignIn")
    public void testFindMatchHost(final TestMatchmakingClientContext context) throws Exception{

        final var client = context.client();
        assertTrue(client.isLoggedOn());

        final var findMatchResponse = client.findMatch(configuration.getName());
        assertNotNull(findMatchResponse);
        assertTrue(findMatchResponse.isHost());
        assertNull(findMatchResponse.getReservedServerId());
        assertTrue(matchIds.add(findMatchResponse.getMultiMatch().getId()));

    }

    @Test(dataProvider = "allClients", threadPoolSize = 3, dependsOnMethods = {"testSignIn", "testFindMatchHost"})
    public void testFindMatchClient(final TestMatchmakingClientContext context) throws Exception{

        final var client = context.client();
        assertTrue(client.isLoggedOn());

        var status = client.findMatch(configuration.getName());
        assertNotNull(status);
        assertFalse(status.isHost());

        // Ensures that one, and only one, match gets generated for the sake of this test. The host has to have
        // created ths match in the first find operation so this should never change the collection of hosts ids.
        assertFalse(matchIds.add(status.getMultiMatch().getId()));

    }

    @Test(dataProvider = "allPlayers",
          threadPoolSize = 4,
          dependsOnMethods = {"testSignIn", "testFindMatchHost", "testFindMatchClient"}
    )
    public void testStartGame(final TestMatchmakingClientContext context) throws Exception {

        final var client = context.client();
        assertTrue(client.isLoggedOn());
        assertTrue(client.isInMatch());

        final var status = client.getMatchStatusResponse();
        assertNotNull(status);

        if (status.isHost()) {
            hostDelayAndStartGame(client);
        } else {
            clientPollAndWaitForGameStart(client);
        }

    }

    private void hostDelayAndStartGame(final TestMatchmakingClient client) throws Exception {

        logger.info("Delaying to simulate server start delay.");
        Thread.sleep(SERVER_CREATE_DELAY);

        logger.info("Staring simulated server.");

        final var request = new UpdateMatchRequest();
        request.setMetadata(new HashMap<>());
        request.setReservedServerId(mockReservedServerId);

        final var response = client.updateCurrentMatch(request);
        assertNotNull(response);
        assertEquals(response.getReservedServerId(), mockReservedServerId);

    }

    private void clientPollAndWaitForGameStart(final TestMatchmakingClient client) throws Exception {

        final var timeout = System.currentTimeMillis() + (SERVER_CREATE_DELAY * 2);

        MatchStatusResponse status;

        do {
            logger.info("Waiting for reserved server id ...");
            Thread.sleep(SERVER_CREATE_POLL_INTERVAL);
            status = client.pollMatch();
            assertNotNull(status);
            assertFalse(status.isHost());
            assertTrue(timeout > System.currentTimeMillis());
        } while (status.getReservedServerId() == null);

        assertEquals(status.getReservedServerId(), mockReservedServerId);

    }

    @Test(dataProvider = "allPlayers",
          threadPoolSize = 4,
          dependsOnMethods = {"testStartGame"})
    public void testLeaveMatch(final TestMatchmakingClientContext context) throws Exception {

        final var client = context.client();
        assertTrue(client.isLoggedOn());
        client.leaveMatch();

    }

}
