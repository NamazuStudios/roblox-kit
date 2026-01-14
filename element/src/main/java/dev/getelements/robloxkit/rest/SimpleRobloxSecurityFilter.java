package dev.getelements.robloxkit.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;

import java.io.IOException;

import static jakarta.ws.rs.core.Response.*;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;

@PreMatching
public class SimpleRobloxSecurityFilter implements ContainerRequestFilter {

    public static final String ROBLOX_SECURITY_HEADER = "RobloxKit-Secret";

    public static final String ROBLOX_SECRET = "dev.getelements.robloxkit.secret";

    private String robloxSecret;

    @PostConstruct
    public void init() {

        final var attributes = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getElementRecord()
                .attributes()
                .getAttribute(ROBLOX_SECRET);

        if (robloxSecret == null) {
            throw new IllegalStateException("RobloxKit-Secret attribute is not set");
        }

        this.robloxSecret = attributes.toString();

    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        final var header = requestContext
                .getHeaders()
                .get(ROBLOX_SECURITY_HEADER)
                .stream()
                .findFirst()
                .orElse(null);

        if (header == null) {
            requestContext.abortWith(
                    status(UNAUTHORIZED)
                            .entity("Missing '%s' header". formatted(ROBLOX_SECURITY_HEADER))
                            .build()
            );
        } else if (!robloxSecret.equals(header)) {
            requestContext.abortWith(
                    status(FORBIDDEN)
                            .entity("Invalid '%s' header". formatted(ROBLOX_SECURITY_HEADER))
                            .build()
            );
        }

    }

}
