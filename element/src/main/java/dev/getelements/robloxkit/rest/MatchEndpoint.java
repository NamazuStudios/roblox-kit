package dev.getelements.robloxkit.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;

import static dev.getelements.robloxkit.RobloxKitApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/roblox/match")
public class MatchEndpoint {

    @POST
    public MatchStatusResponse findMatch(final FindMatchRequest matchRequest) {

        final var matchmakingService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxMatchmakingService.class);

        return matchmakingService.findMatch(matchRequest);

    }

    @GET
    @Path("{matchId}")
    public MatchStatusResponse getMatchStatus(@PathParam("matchId") final String matchId) {

        final var matchmakingService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxMatchmakingService.class);

        return matchmakingService.getMatchStatus(matchId);

    }

    @PUT
    @Path("{matchId}")
    public MatchStatusResponse updateMatch(

            @PathParam("matchId")
            final String matchId,

            final UpdateMatchRequest updateMatchRequest) {

        final var matchmakingService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxMatchmakingService.class);

        return matchmakingService.updateMatch(matchId, updateMatchRequest);

    }

    @DELETE
    @Path("{matchId}")
    public void deleteMatch(
            @PathParam("matchId")
            final String matchId) {

        final var matchmakingService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxMatchmakingService.class);

        matchmakingService.deleteMatch(matchId);

    }

    @DELETE
    @Path("{matchId}/{profileId}")
    public MatchStatusResponse leaveMatch(

            @PathParam("matchId")
            final String matchId,

            @PathParam("profileId")
            final String profileId) {

        final var matchmakingService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxMatchmakingService.class);

        return matchmakingService.leaveMatch(matchId, profileId);

    }

}
