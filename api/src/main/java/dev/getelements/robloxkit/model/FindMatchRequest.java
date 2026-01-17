package dev.getelements.robloxkit.model;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to find a match based on the provided configuration.")
public class FindMatchRequest {

    @Schema(description =
            "The profile ID of the user requesting the match. If blank, the session key will be used to " +
            "identify the user."
    )
    private String profileId;

    @NotNull
    @Schema(description = "The multi-match configuration or id used to perform the matchmaking.")
    private String configuration;

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

}
