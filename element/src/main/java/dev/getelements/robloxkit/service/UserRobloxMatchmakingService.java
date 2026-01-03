package dev.getelements.robloxkit.service;

import dev.getelements.elements.sdk.dao.ApplicationConfigurationDao;
import dev.getelements.elements.sdk.dao.ApplicationDao;
import dev.getelements.elements.sdk.dao.MultiMatchDao;
import dev.getelements.elements.sdk.dao.ProfileDao;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.application.MatchmakingApplicationConfiguration;
import dev.getelements.elements.sdk.model.exception.ForbiddenException;
import dev.getelements.elements.sdk.model.exception.InvalidDataException;
import dev.getelements.elements.sdk.model.profile.Profile;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class UserRobloxMatchmakingService implements RobloxMatchmakingService {

    private Session session;

    private Provider<Transaction> transactionProvider;

    @Override
    public MatchStatusResponse findMatch(final FindMatchRequest matchRequest) {
        return getTransactionProvider().get().performAndClose(txn -> {
            
            final var multimatchDao = txn.getDao(MultiMatchDao.class);
            final var applicationDao = txn.getDao(ApplicationDao.class);
            final var applicationConfigurationDao = txn.getDao(ApplicationConfigurationDao.class);

            final var profile = getProfile(txn, matchRequest.getProfileId());

            final var configuration = applicationConfigurationDao.getApplicationConfiguration(
                    MatchmakingApplicationConfiguration.class,
                    profile.getApplication().getId(),
                    matchRequest.getConfiguration()
            );

            return null;

        });
    }

    @Override
    public MatchStatusResponse getMatchStatus(final String matchId) {
        return null;
    }

    @Override
    public MatchStatusResponse updateMatch(final String matchId, final UpdateMatchRequest updateMatchRequest) {
        return null;
    }

    @Override
    public void deleteMatch(final String matchId) {

    }

    @Override
    public MatchStatusResponse leaveMatch(final String matchId, final String profileId) {
        return null;
    }

    private Profile getProfile(final Transaction transaction,
                               final String profileId) {

        final var profileDao = transaction.getDao(ProfileDao.class);

        if (profileId != null) {
            return profileDao
                    .findActiveProfileForUser(profileId, session.getUser().getId())
                    .orElseThrow(() -> new InvalidDataException("Profile with ID %s not found.".formatted(profileId)));
        } else if (session.getProfile() != null) {
            return profileDao
                    .findActiveProfileForUser(session.getProfile().getId(), session.getUser().getId())
                    .orElseThrow(() -> new ForbiddenException("Profile with ID %s not found.".formatted(session.getProfile().getId())));
        } else {
            throw new InvalidDataException("Unable to determine profile for the session.");
        }

    }


    public Session getSession() {
        return session;
    }

    @Inject
    public void setSession(Session session) {
        this.session = session;
    }

    public Provider<Transaction> getTransactionProvider() {
        return transactionProvider;
    }

    @Inject
    public void setTransactionProvider(Provider<Transaction> transactionProvider) {
        this.transactionProvider = transactionProvider;
    }

}
