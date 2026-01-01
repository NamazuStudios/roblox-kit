package dev.getelements.robloxkit;

import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;

/**
 * Service interface for Roblox matchmaking operations.
 */
public interface RobloxMatchmakingService {

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
}
