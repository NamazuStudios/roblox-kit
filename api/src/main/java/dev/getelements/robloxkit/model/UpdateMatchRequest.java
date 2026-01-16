package dev.getelements.robloxkit.model;

import java.util.Map;

/**
 * Request model for updating a match with a reserved server ID.
 */
public class UpdateMatchRequest {

    private String reservedServerId;

    private Map<String, Object> metadata;

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getReservedServerId() {
        return reservedServerId;
    }

    public void setReservedServerId(String reservedServerId) {
        this.reservedServerId = reservedServerId;
    }

}
