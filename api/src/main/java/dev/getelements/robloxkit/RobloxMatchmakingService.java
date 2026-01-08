package dev.getelements.robloxkit;

import dev.getelements.elements.sdk.model.match.MultiMatch;
import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;

import javax.swing.text.html.Option;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Roblox matchmaking operations.
 */
public interface RobloxMatchmakingService {

    /**
     * Metadata key for the host profile ID.
     */
    String HOST_PROFILE_ID = "hostProfileId";

    /**
     * Metadata key for the reserved server ID.
     */
    String RESERVED_SERVER_ID = "reservedServerId";

    /**
     * Finds a match based on the provided match request.
     *
     * @param matchRequest the match request containing matchmaking criteria
     * @return an optional containing the match request if a match is found, or empty if no match is found
     */
    MatchStatusResponse findMatch(FindMatchRequest matchRequest);

    /**
     * Gets the status of a match by its ID.
     * @param matchId the ID of the match
     * @return the match status response containing match details
     */
    MatchStatusResponse getMatchStatus(String matchId);

    /**
     * Updates a match with the provided update request.
     * @param matchId the ID of the match to update
     * @param updateMatchRequest the update request containing new match details
     * @return the updated match status response
     */
    MatchStatusResponse updateMatch(String matchId, UpdateMatchRequest updateMatchRequest);

    /**
     * Deletes a match by its ID.
     * @param matchId the ID of the match to delete
     */
    void deleteMatch(String matchId);

    /**
     * Leaves a match for a given profile ID.
     *
     * @param matchId the ID of the match to leave
     * @param profileId the profile ID of the user leaving the match
     * @return the match status response after leaving the match
     */
    MatchStatusResponse leaveMatch(String matchId, String profileId);

    /**
     * Finds the host profile ID from the given multi-match.
     *
     * @param multiMatch the multi-match object
     * @return an optional containing the host profile ID if present, otherwise empty
     */
    static Optional<String> findHostProfileId(final MultiMatch multiMatch) {
        return findMetadataProperty(multiMatch, RobloxMatchmakingService.HOST_PROFILE_ID);
    }

    /**
     * Finds the host profile ID from the given multi-match.
     *
     * @param multiMatch the multi-match object
     * @return an optional containing the host profile ID if present, otherwise empty
     */
    static Optional<String> findReservedServerId(final MultiMatch multiMatch) {
        return findMetadataProperty(multiMatch, RobloxMatchmakingService.RESERVED_SERVER_ID);
    }

    /**
     * Finds a specific metadata property from the given multi-match.
     * @param multiMatch the multi-match object
     * @param propertyName the name of the metadata property to find
     * @return an optional containing the property value if present, otherwise empty
     */
    static Optional<String> findMetadataProperty(final MultiMatch multiMatch, final String propertyName) {
        return Optional
                .of(multiMatch.getMetadata())
                .map(metadata -> metadata.get(propertyName))
                .map(Object::toString);
    }

    /**
     * Sets the host profile ID in the given multi-match's metadata.
     * @param multiMatch the multi-match object
     * @param profileId the host profile ID to set
     * @return the updated multi-match object (always the same object as the input)
     */
    static MultiMatch setHostProfileId(final MultiMatch multiMatch, final String profileId) {
        return setMetadataProperty(multiMatch, RobloxMatchmakingService.HOST_PROFILE_ID, profileId);
    }

    /**
     * Sets the reserved server ID in the given multi-match's metadata.
     * @param multiMatch the multi-match object
     * @param reservedServerId the reserved server ID to set
     * @return the updated multi-match object (always the same object as the input)
     */
    static MultiMatch setReservedServerId(final MultiMatch multiMatch, final String reservedServerId) {
        return setMetadataProperty(multiMatch, RobloxMatchmakingService.RESERVED_SERVER_ID, reservedServerId);
    }

    /**
     * Sets a specific metadata property in the given multi-match.
     * @param multiMatch the multi-match object
     * @param propertyName the name of the metadata property to set
     * @param propertyValue the value of the metadata property to set
     * @return the updated multi-match object (always the same object as the input)
     */
    static MultiMatch setMetadataProperty(final MultiMatch multiMatch, final String propertyName, final String propertyValue) {

        final var metadata = Optional
                .of(multiMatch.getMetadata())
                .orElseGet(LinkedHashMap::new);

        metadata.put(propertyName, propertyValue);
        multiMatch.setMetadata(metadata);

        return multiMatch;

    }

}
