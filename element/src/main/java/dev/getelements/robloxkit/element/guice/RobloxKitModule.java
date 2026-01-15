package dev.getelements.robloxkit.element.guice;

import com.google.inject.PrivateModule;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.robloxkit.RobloxAuthService;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.element.service.StandardRobloxAuthService;
import dev.getelements.robloxkit.element.service.UserRobloxMatchmakingService;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

public class RobloxKitModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(Session.class).toProvider(SessionProvider.class);
        bind(RobloxAuthService.class).to(StandardRobloxAuthService.class);
        bind(RobloxMatchmakingService.class).to(UserRobloxMatchmakingService.class);
        bind(Client.class).toProvider(ClientBuilder::newClient).asEagerSingleton();
        expose(RobloxAuthService.class);
        expose(RobloxMatchmakingService.class);
    }

}
