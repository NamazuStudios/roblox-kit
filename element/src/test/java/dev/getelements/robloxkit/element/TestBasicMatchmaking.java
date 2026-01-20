package dev.getelements.robloxkit.element;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.getelements.elements.sdk.dao.ApplicationConfigurationDao;
import dev.getelements.elements.sdk.model.application.MatchmakingApplicationConfiguration;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.ext.ContextResolver;
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
import java.util.concurrent.ThreadLocalRandom;

import static dev.getelements.robloxkit.element.TestMatchmakingClientContext.TEST_ROBLOX_USERS;
import static dev.getelements.robloxkit.element.TestMatchmakingServer.APPLICATION;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
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
    public void testFindMatch(final TestMatchmakingClientContext context) throws Exception{

        final var client = context.client();
        assertTrue(client.isLoggedOn());

        final var random = ThreadLocalRandom.current();
        Thread.sleep(random.nextInt(1000));

        final var findMatchResponse = client.findMatch(configuration.getName());
        assertNotNull(findMatchResponse);

        // Ensures that one, and only one, match gets generated for the sake of this test.
        matchIds.add(findMatchResponse.getMultiMatch().getId());
        assertEquals(matchIds.size(), 1);

        if (findMatchResponse.isHost()) {
            doHost(client, findMatchResponse);
        } else {
            doClient(client, findMatchResponse);
        }

    }

    private void doHost(final TestMatchmakingClient client, final MatchStatusResponse status) throws Exception {

        logger.info("Delaying to simulate server start.");
        Thread.sleep(SERVER_CREATE_DELAY);

        logger.info("Starting server.");
        final var request = new UpdateMatchRequest();
        request.setMetadata(new HashMap<>());
        request.setReservedServerId(mockReservedServerId);

        final var response = client.updateMatch(status.getMultiMatch().getId(), request);
        assertNotNull(response);
        assertEquals(status.getMultiMatch().getId(), response.getMultiMatch().getId());
        assertEquals(response.getReservedServerId(), mockReservedServerId);

    }

    private void doClient(final TestMatchmakingClient client, final MatchStatusResponse status) throws Exception {

        final var timeout = System.currentTimeMillis() + (SERVER_CREATE_DELAY * 2);

        var latest = status;

        do {
            logger.info("Waiting for reserved server id ...");
            Thread.sleep(SERVER_CREATE_POLL_INTERVAL);
            latest = client.pollMatch(status.getMultiMatch().getId());
            assertNotNull(latest);
            assertTrue(timeout > System.currentTimeMillis());
        } while (latest.getReservedServerId() == null);

        assertEquals(latest.getReservedServerId(), mockReservedServerId);

    }

    @Test(dataProvider = "allClients", dependsOnMethods = "testFindMatch")
    public void testLeaveMatch(final TestMatchmakingClientContext context) throws Exception{

        final var client = context.client();
        assertTrue(client.isLoggedOn());

        final var matchId = matchIds.iterator().next();
        client.leaveMatch(matchId);

    }

}
