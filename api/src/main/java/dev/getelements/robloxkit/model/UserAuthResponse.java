package dev.getelements.robloxkit.model;

import dev.getelements.elements.sdk.model.profile.Profile;
import dev.getelements.elements.sdk.model.session.SessionCreation;
import dev.getelements.elements.sdk.model.user.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response returned after successfully authenticating a Roblox user.")
public class UserAuthResponse {

    @Schema(description = "The User object created or retrieved for the authenticated Roblox user.")
    private User user;

    @Schema(description = "The Profile object associated with the authenticated user.")
    private Profile profile;

    @Schema(description = "The session information for the authenticated user.")
    private SessionCreation session;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public SessionCreation getSession() {
        return session;
    }

    public void setSession(SessionCreation session) {
        this.session = session;
    }

}
