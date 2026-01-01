package dev.getelements.robloxkit.model;

/**
 * Request model for updating a match with a reserved server ID.
 */
public class UpdateMatchRequest {

    private String reservedServerId;

    public String getReservedServerId() {
        return reservedServerId;
    }

    public void setReservedServerId(String reservedServerId) {
        this.reservedServerId = reservedServerId;
    }

}
