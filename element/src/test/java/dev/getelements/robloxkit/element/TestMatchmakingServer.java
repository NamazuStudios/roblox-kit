package dev.getelements.robloxkit.element;

import dev.getelements.elements.sdk.dao.ApplicationDao;
import dev.getelements.elements.sdk.dao.ProfileDao;
import dev.getelements.elements.sdk.dao.SessionDao;
import dev.getelements.elements.sdk.dao.UserDao;
import dev.getelements.elements.sdk.local.ElementsLocal;
import dev.getelements.elements.sdk.local.ElementsLocalBuilder;
import dev.getelements.elements.sdk.model.application.Application;
import dev.getelements.elements.sdk.model.profile.Profile;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.elements.sdk.model.session.SessionCreation;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.mongo.test.DockerMongoTestInstance;
import dev.getelements.elements.sdk.mongo.test.MongoTestInstance;
import dev.getelements.elements.sdk.util.PropertiesAttributes;
import dev.getelements.elements.sdk.util.ShutdownHooks;
import jakarta.ws.rs.client.ClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

 import static dev.getelements.elements.sdk.mongo.MongoConfigurationService.MONGO_CLIENT_URI;
import static dev.getelements.robloxkit.element.rest.SimpleRobloxSecurityFilter.ROBLOX_SECRET;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class TestMatchmakingServer {

    private static final Logger logger = LoggerFactory.getLogger(TestMatchmakingServer.class);

    public static final String URL = "http://localhost:8080/app/rest/roblox";

    public static final String APPLICATION = "RobloxKit";

    public static final String TEST_ROBLOX_SECRET = "supersecret";

    private static final int STARTUP_TIMEOUT_SECONDS = 180;

    private static final int TEST_MONGO_PORT = 45005;

    private static final String PROJECT_VERSION = readProjectVersion();

    private static String readProjectVersion() {
        final var resource = "/META-INF/maven/dev.getelements.robloxkit/element/pom.properties";
        try (final var is = TestMatchmakingServer.class.getResourceAsStream(resource)) {
            if (is == null) throw new IllegalStateException("pom.properties not found on classpath: " + resource);
            final var props = new Properties();
            props.load(is);
            return props.getProperty("version");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read project version from " + resource, e);
        }
    }

    private static final ShutdownHooks shutdownHooks = new ShutdownHooks(TestMatchmakingServer.class);

    private static final TestMatchmakingServer instance = new TestMatchmakingServer();

    private static final AtomicLong counter = new AtomicLong();

    public static TestMatchmakingServer getInstance() {
        return instance;
    }

    private final ElementsLocal elementsLocal;

    private final MongoTestInstance mongoTestInstance;

    private final Application application;

    private TestMatchmakingServer() {

        mongoTestInstance = new DockerMongoTestInstance(TEST_MONGO_PORT);
        mongoTestInstance.start();

        final var systemProperties = System.getProperties();
        final var workingDirectory = Path.of(".");

        logger.info("Working Directory: {}", workingDirectory.toAbsolutePath().normalize());

        systemProperties.put(MONGO_CLIENT_URI, format("mongodb://127.0.0.1:%d", TEST_MONGO_PORT));

        final var elmArtifact = "dev.getelements.robloxkit:element:elm:%s".formatted(PROJECT_VERSION);

        elementsLocal = ElementsLocalBuilder.getDefault()
                .withProperties(systemProperties)
                .withSourceRoot()
                .withDeployment(builder -> builder
                        .elementPackage()
                        .elmArtifact(elmArtifact)
                        .pathAttribute("dev.getelements.robloxkit.element", ROBLOX_SECRET, TEST_ROBLOX_SECRET)
                        .endElementPackage()
                        .build()
                )
                .build();

        application = buildApplication();
        elementsLocal.start();
        shutdownHooks.add(elementsLocal::close);
        shutdownHooks.add(mongoTestInstance::stop);

        try (final var client = ClientBuilder.newClient()) {

            logger.info("Starting Server ...");

            for (int i = 0; i < STARTUP_TIMEOUT_SECONDS; i++) {

                try {

                    final var response = client
                            .target(URL)
                            .request()
                            .get()
                            .readEntity(String.class);

                    if ("OK".equals(response)) {
                        break;
                    }

                } catch (Exception e) {

                    logger.warn("Failed to connect to Test Server WebSocket {}, retrying in 1 second...", e.getMessage());

                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting for Test Server WebSocket connection", ie);
                    }

                }

                    logger.info("Test Server Started. Application: {}", application.getName());

                }

        }


    }

    private Application buildApplication() {
        final var application = new Application();
        application.setName(APPLICATION);
        application.setDescription("Roblox Kit test application");
        return getDao(ApplicationDao.class).createOrUpdateInactiveApplication(application);
    }

    public Application getApplication() {
        return getDao(ApplicationDao.class).getActiveApplication(application.getId());
    }

    public ElementsLocal getElementsLocal() {
        return elementsLocal;
    }

    public MongoTestInstance getMongoTestInstance() {
        return mongoTestInstance;
    }

    public <T> T getDao(final Class<T> dao) {
        return elementsLocal.getRootElementRegistry()
                .find("dev.getelements.elements.sdk.dao")
                .findFirst()
                .get()
                .getServiceLocator()
                .getInstance(dao);
    }

    public User createUser(final String name, final User.Level level) {
        final var user = new User();
        user.setLevel(level);
        user.setName(format("%s%d", name, counter.incrementAndGet()));
        user.setEmail(format("%s%d@example.com", name, counter.incrementAndGet()));
        return getDao(UserDao.class).createUser(user);
    }

    public Profile createProfile(final User user, final String name) {
        final var profile = new Profile();
        profile.setUser(user);
        profile.setApplication(application);
        profile.setDisplayName(format("%s%d", name, counter.incrementAndGet()));
        return getDao(ProfileDao.class).createOrReactivateProfile(profile);
    }

    public SessionCreation newSessionForUser(final User user, final Profile profile) {
        final var session = new Session();
        final var expiry = System.currentTimeMillis() + MILLISECONDS.convert(1, DAYS);
        session.setExpiry(expiry);
        session.setUser(user);
        session.setProfile(profile);
        session.setApplication(application);
        return getDao(SessionDao.class).create(session);
    }

}
