package dev.getelements.robloxkit.element.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/")
public class StatusEndpoint {

    @GET
    public String getStatus() {
        return "OK";
    }

}
