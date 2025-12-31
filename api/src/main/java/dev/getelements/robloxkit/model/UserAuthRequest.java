package dev.getelements.robloxkit.model;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description =
        "Authorizes the given Roblox user ID for use with the Roblox Kit services. This registers the user in " +
        "Elements with a user account tied to their Roblox user ID."
)
public class UserAuthRequest {

    @NotNull
    @Schema(description = "The Roblox user ID of the user to authenticate.")
    private String robloxUserId;

    @NotNull
    @Schema($schema = "The application name or ID to use when creating the user's profile in Elements.")
    private String application;

    public String getRobloxUserId() {
        return robloxUserId;
    }

    public void setRobloxUserId(String robloxUserId) {
        this.robloxUserId = robloxUserId;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

}
