@ElementDefinition(recursive = true)
@GuiceElementModule(RobloxKitModule.class)
@ElementDependency("dev.getelements.elements.sdk.dao")
@ElementService(RobloxAuthService.class)
@ElementService(RobloxMatchmakingService.class)
package dev.getelements.robloxkit.element;

import dev.getelements.elements.sdk.annotation.ElementDefinition;
import dev.getelements.elements.sdk.annotation.ElementDependency;
import dev.getelements.elements.sdk.annotation.ElementService;
import dev.getelements.elements.sdk.spi.guice.annotations.GuiceElementModule;
import dev.getelements.robloxkit.RobloxAuthService;
import dev.getelements.robloxkit.RobloxMatchmakingService;
import dev.getelements.robloxkit.element.guice.RobloxKitModule;
