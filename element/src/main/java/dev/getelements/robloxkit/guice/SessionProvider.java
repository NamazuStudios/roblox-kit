package dev.getelements.robloxkit.guice;

import dev.getelements.elements.sdk.Element;
import dev.getelements.elements.sdk.model.session.Session;
import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class SessionProvider implements Provider<Session> {

    private Element element;

    @Override
    public Session get() {
        return getElement()
                .getCurrentScope()
                .getMutableAttributes()
                .getAttributeOptional(Session.SESSION_ATTRIBUTE)
                .map(Session.class::cast)
                .orElseThrow(IllegalStateException::new);
    }

    public Element getElement() {
        return element;
    }

    @Inject
    public void setElement(Element element) {
        this.element = element;
    }

}
