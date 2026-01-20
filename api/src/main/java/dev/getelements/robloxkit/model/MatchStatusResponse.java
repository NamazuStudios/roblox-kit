package dev.getelements.robloxkit.model;

import dev.getelements.elements.sdk.model.match.MultiMatch;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description =
    "Returned when a match is successfully found based on the provided configuration. This includes the " +
    "match ID, profile ID, and multi-match details. Additionally, it designates whether the user is the host of the " +
    "match. The host may be responsible for certain match management tasks such as creating the reserved server."
)
public class MatchStatusResponse {

    private boolean host;

    private String reservedServerId;

    @NotNull
    private String profileId;

    @NotNull
    private MultiMatch multiMatch;

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public String getReservedServerId() {
        return reservedServerId;
    }

    public void setReservedServerId(String reservedServerId) {
        this.reservedServerId = reservedServerId;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public MultiMatch getMultiMatch() {
        return multiMatch;
    }

    public void setMultiMatch(MultiMatch multiMatch) {
        this.multiMatch = multiMatch;
    }

}
