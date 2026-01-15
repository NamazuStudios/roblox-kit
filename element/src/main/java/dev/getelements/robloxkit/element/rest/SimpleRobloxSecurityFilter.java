package dev.getelements.robloxkit.element.rest;

import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.sdk.util.LazyValue;
import dev.getelements.elements.sdk.util.ThreadSafeLazyValue;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.HttpHeaders;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static jakarta.ws.rs.core.HttpHeaders.WWW_AUTHENTICATE;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.UNAUTHORIZED;
import static jakarta.ws.rs.core.Response.status;

@PreMatching
public class SimpleRobloxSecurityFilter implements ContainerRequestFilter {

    public static final String ROBLOX_SECURITY_HEADER = "RobloxKit-Secret";

    public static final String ROBLOX_SECRET = "dev.getelements.robloxkit.secret";

    private final LazyValue<String> robloxSecret = new ThreadSafeLazyValue<>(() -> {

        final var attributes = ElementSupplier
                .getElementLocal(getClass())
                .get()
                .getElementRecord()
                .attributes()
                .getAttribute(ROBLOX_SECRET);

        final var robloxSecret = attributes.toString();

        if (robloxSecret == null) {
            throw new IllegalStateException("RobloxKit-Secret attribute is not set");
        }

        return robloxSecret;

    });

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {

        final var header = Optional.ofNullable(requestContext
                        .getHeaders()
                        .getFirst(ROBLOX_SECURITY_HEADER)
        );

        if (header.isEmpty()) {
            requestContext.abortWith(
                    status(UNAUTHORIZED)
                            .header(WWW_AUTHENTICATE, "Bearer")
                            .entity("Missing '%s' header". formatted(ROBLOX_SECURITY_HEADER))
                            .build()
            );
        } else if (!robloxSecret.get().equals(header.get())) {
            requestContext.abortWith(
                    status(FORBIDDEN)
                            .entity("Invalid '%s' header". formatted(ROBLOX_SECURITY_HEADER))
                            .build()
            );
        }

    }

}
