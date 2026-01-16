package dev.getelements.robloxkit.element.service;

import com.restfb.types.Link;
import dev.getelements.elements.sdk.dao.ApplicationConfigurationDao;
import dev.getelements.elements.sdk.dao.MultiMatchDao;
import dev.getelements.elements.sdk.dao.ProfileDao;
import dev.getelements.elements.sdk.dao.Transaction;
import dev.getelements.elements.sdk.model.application.MatchmakingApplicationConfiguration;
import dev.getelements.elements.sdk.model.exception.ForbiddenException;
import dev.getelements.elements.sdk.model.exception.InvalidDataException;
import dev.getelements.elements.sdk.model.exception.MultiMatchNotFoundException;
import dev.getelements.elements.sdk.model.match.MultiMatch;
import dev.getelements.elements.sdk.model.match.MultiMatchStatus;
import dev.getelements.elements.sdk.model.profile.Profile;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class UserRobloxMatchmakingService implements RobloxMatchmakingService {

    private Session session;

    private Provider<Transaction> transactionProvider;

    @Override
    public MatchStatusResponse findMatch(final FindMatchRequest matchRequest) {
        return getTransactionProvider().get().performAndClose(txn -> {
            
            final var multimatchDao = txn.getDao(MultiMatchDao.class);
            final var applicationConfigurationDao = txn.getDao(ApplicationConfigurationDao.class);

            final var profile = getProfile(txn, matchRequest.getProfileId());

            final var configuration = applicationConfigurationDao.getApplicationConfiguration(
                    MatchmakingApplicationConfiguration.class,
                    profile.getApplication().getId(),
                    matchRequest.getConfiguration()
            );

            var multiMatch = multimatchDao
                    .findOldestAvailableMultiMatchCandidate(configuration, profile.getId(), "")
                    .orElseGet(() -> {
                        final var newMatch = new MultiMatch();
                        newMatch.setConfiguration(configuration);
                        newMatch.setStatus(MultiMatchStatus.OPEN);
                        RobloxMatchmakingService.setHostProfileId(newMatch, profile.getId());
                        return multimatchDao.createMultiMatch(newMatch);
                    });

            multiMatch = multimatchDao.addProfile(multiMatch.getId(), profile);

            final var isHost = RobloxMatchmakingService
                    .findHostProfileId(multiMatch)
                    .map(profileId -> profileId.equals(profile.getId()))
                    .orElse(false);

            final var response = new MatchStatusResponse();
            response.setMultiMatch(multiMatch);
            response.setHost(isHost);
            response.setMultiMatch(multiMatch);
            response.setProfileId(profile.getId());

            return response;

        });
    }

    @Override
    public MatchStatusResponse getMatchStatus(final String matchId) {
        return getTransactionProvider().get().performAndClose(txn -> {

            final var profile = getProfile(txn, matchId);
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            final var inMatch = multimatchDao.getProfiles(matchId)
                    .stream()
                    .anyMatch(p -> p.getId().equals(profile.getId()));

            if (!inMatch) {
                throw new MultiMatchNotFoundException();
            }

            final var multiMatch = multimatchDao.getMultiMatch(matchId);

            final var response = new MatchStatusResponse();
            final var isHost = RobloxMatchmakingService.isHost(multiMatch, profile);

            response.setMultiMatch(multiMatch);
            response.setHost(isHost);
            response.setMultiMatch(multiMatch);
            response.setProfileId(profile.getId());

            return response;
        });
    }

    @Override
    public MatchStatusResponse updateMatch(final String matchId, final UpdateMatchRequest updateMatchRequest) {
        return getTransactionProvider().get().performAndClose(txn -> {

            final var profile = getProfile(txn, matchId);
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            final var inMatch = multimatchDao.getProfiles(matchId)
                    .stream()
                    .anyMatch(p -> p.getId().equals(profile.getId()));

            if (!inMatch) {
                throw new MultiMatchNotFoundException();
            }

            var multiMatch = multimatchDao.getMultiMatch(matchId);

            if (multiMatch.getMetadata() == null) {
                multiMatch.setMetadata(new LinkedHashMap<>());
            } else {
                multiMatch.setMetadata(new LinkedHashMap<>(multiMatch.getMetadata()));
            }

            final var multiMatchMetadata = multiMatch.getMetadata();

            Optional
                    .ofNullable(updateMatchRequest.getMetadata())
                    .ifPresent(m -> m.entrySet()
                            .stream()
                            .filter(e -> !e.getKey().equals(HOST_PROFILE_ID))
                            .filter(e -> !e.getKey().equals(RESERVED_SERVER_ID))
                            .forEach(e -> multiMatchMetadata.put(e.getKey(), e.getValue()))
                    );

            final var updateReservedServerId = RobloxMatchmakingService
                    .findReservedServerId(multiMatch)
                    .map(reservedServerId -> reservedServerId.equals(updateMatchRequest.getReservedServerId()))
                    .orElse(true);

            if (!updateReservedServerId) {
                RobloxMatchmakingService.setReservedServerId(multiMatch, updateMatchRequest.getReservedServerId());
                multiMatch = multimatchDao.updateMultiMatch(multiMatch.getId(), multiMatch);
            }

            final var response = new MatchStatusResponse();
            final var isHost = RobloxMatchmakingService.isHost(multiMatch, profile);

            response.setMultiMatch(multiMatch);
            response.setHost(isHost);
            response.setMultiMatch(multiMatch);
            response.setProfileId(profile.getId());

            return response;

        });
    }

    @Override
    public void deleteMatch(final String matchId) {
        getTransactionProvider().get().performAndCloseV(txn -> {

            final var profile = getProfile(txn, matchId);
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            if (!RobloxMatchmakingService.isHost(multimatchDao.getMultiMatch(matchId), profile)) {
                throw new MultiMatchNotFoundException();
            }

            multimatchDao.deleteMultiMatch(matchId);

        });
    }

    @Override
    public MatchStatusResponse leaveMatch(final String matchId, final String profileId) {
        return getTransactionProvider().get().performAndClose(txn -> {

            final var profile = getProfile(txn, profileId);
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            final var inMatch = multimatchDao.getProfiles(matchId)
                    .stream()
                    .anyMatch(p -> p.getId().equals(profile.getId()));

            if (!inMatch) {
                throw new MultiMatchNotFoundException();
            }

            final var multiMatch = multimatchDao.getMultiMatch(matchId);
            multimatchDao.removeProfile(matchId, profile);

            final var response = new MatchStatusResponse();
            final var isHost = RobloxMatchmakingService.isHost(multiMatch, profile);

            response.setMultiMatch(multiMatch);
            response.setHost(isHost);
            response.setMultiMatch(multiMatch);
            response.setProfileId(profile.getId());

            return response;

        });
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
