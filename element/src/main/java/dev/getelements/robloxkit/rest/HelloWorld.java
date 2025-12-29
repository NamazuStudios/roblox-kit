package dev.getelements.robloxkit.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import static dev.getelements.robloxkit.RobloxKitApplication.OPENAPI_TAG;

@Tag(name = OPENAPI_TAG)
@Path("/helloworld")
public class HelloWorld {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Hello world probe", description = "Returns a simple greeting")
    public String sayHello() {

        return "Hello world!";

    }

}
