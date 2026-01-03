package dev.getelements.robloxkit.service;

import dev.getelements.elements.sdk.annotation.ElementDefaultAttribute;
import dev.getelements.elements.sdk.dao.*;
import dev.getelements.elements.sdk.model.exception.InternalException;
import dev.getelements.elements.sdk.model.profile.Profile;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.model.user.UserUid;
import dev.getelements.robloxkit.RobloxAuthService;
import dev.getelements.robloxkit.model.RobloxProfile;
import dev.getelements.robloxkit.model.UserAuthRequest;
import dev.getelements.robloxkit.model.UserAuthResponse;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.ws.rs.client.Client;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;

import static dev.getelements.elements.sdk.model.user.User.Level.USER;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.SECONDS;

public class StandardRobloxAuthService implements RobloxAuthService {

    @ElementDefaultAttribute("https://users.roblox.com/v1")
    public static final String ROBLOX_USER_API_URL = "dev.getelements.robloxkit.roblox.users.api.url";

    @ElementDefaultAttribute("300")
    public static final String ROBLOX_SESSION_TIMEOUT_SECONDS = "dev.getelements.robloxkit.session.expiry.seconds";

    private Client client;

    private String robloxApiUrl;

    private long sessionExpirySeconds;

    private Provider<Transaction> transactionProvider;

    @Override
    public Optional<RobloxProfile> findRobloxProfile(final String robloxUserId) {

        final var url = "%s/users/%s".formatted(getRobloxApiUrl(), robloxUserId);

        final var response = client.target(url)
                .request(APPLICATION_JSON)
                .get();

        return switch (response.getStatusInfo().getStatusCode()) {
            case 200 -> Optional.of(response.readEntity(RobloxProfile.class));
            case 404 -> Optional.empty();
            default -> throw new InternalException("Failed to fetch Roblox profile: %s".formatted(response.getStatusInfo().toString()));
        };

    }

    @Override
    public UserAuthResponse authenticateRobloxUser(final UserAuthRequest authRequest) {

        final var robloxProfile = getRobloxProfile(authRequest.getRobloxUserId());

        return getTransactionProvider().get().performAndClose(txn -> {

            final var userDao = txn.getDao(UserDao.class);
            final var userUidDao = txn.getDao(UserUidDao.class);
            final var profileDao = txn.getDao(ProfileDao.class);
            final var sessionDao = txn.getDao(SessionDao.class);
            final var applicationDao = txn.getDao(ApplicationDao.class);

            final var application = applicationDao.getActiveApplication(authRequest.getApplication());

            final var user = userUidDao
                    .findUserUid(authRequest.getRobloxUserId(), ROBLOX_AUTH_SCHEME)
                    .map(uid -> userDao.getUser(uid.getUserId()))
                    .orElseGet(() -> {

                        final var uid = new UserUid();
                        uid.setUserId(authRequest.getRobloxUserId());
                        uid.setScheme(ROBLOX_AUTH_SCHEME);

                        final var u = new User();
                        u.setLevel(USER);
                        u.setLinkedAccounts(Set.of(ROBLOX_AUTH_SCHEME));
                        return userDao.createUser(u);

                    });

            final var query = ".ref.applicaion:%s AND .ref.user:%s".formatted(
                    application.getId(),
                    user.getId()
            );

            final var profile = profileDao
                    .getActiveProfiles(0, 1, query)
                    .stream()
                    .findFirst()
                    .orElseGet(() -> {

                        final var p = new Profile();
                        p.setUser(user);
                        p.setApplication(application);
                        p.setDisplayName(robloxProfile.getDisplayName());
                        p.setLastLogin(currentTimeMillis());

                        final var metadata = new HashMap<>();
                        metadata.put(ROBLOX_PROFILE_METADATA_KEY, robloxProfile);

                        return p;
                    });

            final var session = new Session();
            final var expiry = currentTimeMillis() + SECONDS.toMillis(getSessionExpirySeconds() + currentTimeMillis());
            session.setUser(user);
            session.setExpiry(expiry);
            session.setProfile(profile);
            session.setApplication(application);

            final var creation = sessionDao.create(session);

            final var response = new UserAuthResponse();
            response.setUser(user);
            response.setProfile(profile);
            response.setSession(creation);

            return response;
        });
    }

    public Client getClient() {
        return client;
    }

    @Inject
    public void setClient(Client client) {
        this.client = client;
    }

    public String getRobloxApiUrl() {
        return robloxApiUrl;
    }

    @Inject
    public void setRobloxApiUrl(@Named(ROBLOX_USER_API_URL) String robloxApiUrl) {
        this.robloxApiUrl = robloxApiUrl;
    }

    public long getSessionExpirySeconds() {
        return sessionExpirySeconds;
    }

    @Inject
    public void setSessionExpirySeconds(@Named(ROBLOX_SESSION_TIMEOUT_SECONDS) long sessionExpirySeconds) {
        this.sessionExpirySeconds = sessionExpirySeconds;
    }

    public Provider<Transaction> getTransactionProvider() {
        return transactionProvider;
    }

    @Inject
    public void setTransactionProvider(final Provider<Transaction> transactionProvider) {
        this.transactionProvider = transactionProvider;
    }

}
