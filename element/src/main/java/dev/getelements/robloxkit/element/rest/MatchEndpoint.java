package dev.getelements.robloxkit.element.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.model.FindMatchRequest;
import dev.getelements.robloxkit.model.MatchStatusResponse;
import dev.getelements.robloxkit.model.UpdateMatchRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;

import static dev.getelements.robloxkit.element.rest.RobloxKitApplication.OPENAPI_TAG;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("/match")
@Tag(name = OPENAPI_TAG)
@Produces(APPLICATION_JSON)
public class MatchEndpoint {

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public MatchStatusResponse findMatch(
            @Context
            final ContainerRequestContext requestContext,
            final FindMatchRequest matchRequest) {
        final var matchmakingService = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getServiceLocator()
                .getInstance(RobloxMatchmakingService.class);

        return matchmakingService.findMatch(matchRequest);
    }

    @GET
    @Path("{matchId}")
    @Produces(APPLICATION_JSON)
    public MatchStatusResponse getMatchStatus(
            @Context
            final ContainerRequestContext requestContext,
            @PathParam("matchId")
            final String matchId) {
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
            @Context
            final ContainerRequestContext requestContext,
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
            @Context
            final ContainerRequestContext requestContext,
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
            @Context
            final ContainerRequestContext requestContext,
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
