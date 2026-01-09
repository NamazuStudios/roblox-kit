package dev.getelements.robloxkit.guice;

import com.google.inject.PrivateModule;
import dev.getelements.elements.sdk.model.session.Session;
import dev.getelements.robloxkit.RobloxAuthService;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.service.UserRobloxMatchmakingService;

public class RobloxKitModule extends PrivateModule {

    @Override
    protected void configure() {
        bind(Session.class).toProvider(SessionProvider.class);
        bind(RobloxAuthService.class).to(RobloxAuthService.class);
        bind(RobloxMatchmakingService.class).to(UserRobloxMatchmakingService.class);
        expose(RobloxAuthService.class);
        expose(RobloxMatchmakingService.class);
    }

}
