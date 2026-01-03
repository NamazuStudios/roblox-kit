package dev.getelements.robloxkit.model;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

@Schema(description = "Represents a Roblox user profile with various attributes.")
public class RobloxProfile {

    private long id;

    private Date created;

    private String description;

    private boolean isBanned;

    private boolean hasVerifiedBadge;

    private String name;

    private String displayName;

    private String externalAppDisplayName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public boolean isHasVerifiedBadge() {
        return hasVerifiedBadge;
    }

    public void setHasVerifiedBadge(boolean hasVerifiedBadge) {
        this.hasVerifiedBadge = hasVerifiedBadge;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getExternalAppDisplayName() {
        return externalAppDisplayName;
    }

    public void setExternalAppDisplayName(String externalAppDisplayName) {
        this.externalAppDisplayName = externalAppDisplayName;
    }

}
