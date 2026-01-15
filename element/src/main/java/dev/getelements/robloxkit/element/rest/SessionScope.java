package dev.getelements.robloxkit.element.rest;

import dev.getelements.elements.sdk.ElementScope;
import dev.getelements.elements.sdk.ElementSupplier;
import dev.getelements.elements.sdk.MutableAttributes;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.elements.sdk.util.SimpleAttributes;
import jakarta.ws.rs.container.ContainerRequestContext;

import java.util.LinkedHashMap;

public class SessionScope implements ElementScope.Handle {

    private final ElementScope.Handle delegate;

    private SessionScope(final ElementScope.Handle delegate) {
        this.delegate = delegate;
    }

    @Override
    public void close() {
        delegate.close();
    }

    public static class Builder {

        private String name = SessionScope.class.getName();

        private MutableAttributes attributes = new SimpleAttributes(new LinkedHashMap<>());

        /**
         * Sets the name of the scope.
         * @param name the name
         * @return this instance
         */
        public Builder withName(final String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the session for this scope.
         *
         * @param session the session
         * @return this instance
         */
        public Builder withSession(final Session session) {
            attributes.setAttribute(Session.SESSION_ATTRIBUTE, session);
            return this;
        }

        /**
         * Builds the session scope.
         *
         * @param containerRequestContext the container request
         * @return this instance
         */
        public Builder withContainerRequestContext(final ContainerRequestContext containerRequestContext) {
            final var session = (Session) containerRequestContext.getProperty(Session.SESSION_ATTRIBUTE);
            return withSession(session).withName(containerRequestContext.getUriInfo().getPath());
        }

        /**
         * Builds the session scope.
         *
         * @return the session scope
         */
        public SessionScope build() {

            final var delegate = ElementSupplier
                    .getElementLocal(getClass())
                    .get()
                    .withScope()
                    .with(attributes)
                    .named(name)
                    .enter();

            return new SessionScope(delegate);

        }

    }

}
