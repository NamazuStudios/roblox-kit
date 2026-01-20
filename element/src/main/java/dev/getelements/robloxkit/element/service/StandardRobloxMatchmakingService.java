package dev.getelements.robloxkit.element.service;

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
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.service.profile.ProfileService;
import dev.getelements.elements.sdk.service.user.UserService;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.LinkedHashMap;
import java.util.Optional;

import static dev.getelements.elements.sdk.model.user.User.Level.UNPRIVILEGED;

public class StandardRobloxMatchmakingService implements RobloxMatchmakingService {

    private UserService userService;

    private ProfileService profileService;

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
            return toMatchStatusResponse(multiMatch, profile);

        });
    }

    @Override
    public MatchStatusResponse getMatchStatus(final String matchId) {
        return getTransactionProvider().get().performAndClose(txn -> {

            final var profile = getProfileService().getCurrentProfile();
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            final var inMatch = multimatchDao.getProfiles(matchId)
                    .stream()
                    .anyMatch(p -> p.getId().equals(profile.getId()));

            if (!inMatch) {
                throw new MultiMatchNotFoundException();
            }

            final var multiMatch = multimatchDao.getMultiMatch(matchId);
            return toMatchStatusResponse(multiMatch, profile);

        });
    }

    @Override
    public MatchStatusResponse updateMatch(final String matchId, final UpdateMatchRequest updateMatchRequest) {
        return getTransactionProvider().get().performAndClose(txn -> {

            final var profile = getProfileService().getCurrentProfile();
            final var multimatchDao = txn.getDao(MultiMatchDao.class);


            final var inMatch = multimatchDao.getProfiles(matchId)
                    .stream()
                    .anyMatch(p -> p.getId().equals(profile.getId()));

            if (!inMatch) {
                throw new MultiMatchNotFoundException();
            }

            var multiMatch = multimatchDao.getMultiMatch(matchId);

            if (!RobloxMatchmakingService.isHost(multimatchDao.getMultiMatch(matchId), profile)) {
                throw new MultiMatchNotFoundException();
            }

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
                    .map(reservedServerId -> !reservedServerId.equals(updateMatchRequest.getReservedServerId()))
                    .orElse(true);

            if (updateReservedServerId) {
                RobloxMatchmakingService.setReservedServerId(multiMatch, updateMatchRequest.getReservedServerId());
                multiMatch = multimatchDao.updateMultiMatch(multiMatch.getId(), multiMatch);
            }

            return toMatchStatusResponse(multiMatch, profile);

        });
    }

    @Override
    public void deleteMatch(final String matchId) {
        getTransactionProvider().get().performAndCloseV(txn -> {

            final var profile = getProfileService().getCurrentProfile();
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            if (!RobloxMatchmakingService.isHost(multimatchDao.getMultiMatch(matchId), profile)) {
                throw new MultiMatchNotFoundException();
            }

            multimatchDao.endMatch(matchId);

        });
    }

    @Override
    public MatchStatusResponse leaveMatch(final String matchId, final String profileId) {
        return getTransactionProvider().get().performAndClose(txn -> {

            final var profile = getProfileService().getCurrentProfile();
            final var multimatchDao = txn.getDao(MultiMatchDao.class);

            final var inMatch = multimatchDao.getProfiles(matchId)
                    .stream()
                    .anyMatch(p -> p.getId().equals(profile.getId()));

            if (!inMatch) {
                throw new MultiMatchNotFoundException();
            }

            var multiMatch = multimatchDao.getMultiMatch(matchId);
            final var isHost = RobloxMatchmakingService.isHost(multiMatch, profile);

            multiMatch = multimatchDao.removeProfile(matchId, profile);

            if (multiMatch.getCount() == 0) {
                multiMatch = multimatchDao.endMatch(matchId);
            } else if (isHost) {
                final var host = multimatchDao.getProfiles(matchId).getFirst();
                RobloxMatchmakingService.setHostProfileId(multiMatch, host.getId());
                multiMatch = multimatchDao.updateMultiMatch(multiMatch.getId(), multiMatch);
            }

            return toMatchStatusResponse(multiMatch, profile);

        });
    }

    private MatchStatusResponse toMatchStatusResponse(final MultiMatch multiMatch,
                                                      final Profile profile) {

        final var response = new MatchStatusResponse();

        final var isHost = RobloxMatchmakingService
                .isHost(multiMatch, profile);

        final var reservedServerId = RobloxMatchmakingService
                .findReservedServerId(multiMatch)
                .orElse(null);

        response.setHost(isHost);
        response.setReservedServerId(reservedServerId);
        response.setMultiMatch(multiMatch);
        response.setProfileId(profile.getId());

        return response;

    }

    private Profile getProfile(final Transaction transaction,
                               final String profileId) {

        final var user = getUserService().getCurrentUser();
        final var profileDao = transaction.getDao(ProfileDao.class);
        final var currentProfile = getProfileService().findCurrentProfile();

        if (profileId != null) {
            return profileDao
                    .findActiveProfileForUser(profileId, user.getId())
                    .orElseThrow(() -> new InvalidDataException("Profile with ID %s not found.".formatted(profileId)));
        } else if (currentProfile.isPresent()) {
            return profileDao
                    .findActiveProfileForUser(currentProfile.get().getId(), user.getId())
                    .orElseThrow(() -> new ForbiddenException("Profile with ID %s not found.".formatted(currentProfile.get().getId())));
        } else {
            throw new InvalidDataException("Unable to determine profile for the session.");
        }

    }

    public UserService getUserService() {
        return userService;
    }

    @Inject
    public void setUserService(UserService userService) {
        this.userService = userService;
    }

    public ProfileService getProfileService() {
        return profileService;
    }

    @Inject
    public void setProfileService(ProfileService profileService) {
        this.profileService = profileService;
    }

    public Provider<Transaction> getTransactionProvider() {
        return transactionProvider;
    }

    @Inject
    public void setTransactionProvider(Provider<Transaction> transactionProvider) {
        this.transactionProvider = transactionProvider;
    }

}
