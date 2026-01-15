import dev.getelements.elements.sdk.dao.UserDao;
import dev.getelements.elements.sdk.local.ElementsLocalBuilder;
import dev.getelements.elements.sdk.model.user.User;
import dev.getelements.elements.sdk.util.PropertiesAttributes;
import dev.getelements.elements.sdk.util.UniqueCodeGenerator;
import dev.getelements.robloxkit.element.rest.SimpleRobloxSecurityFilter;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import static dev.getelements.robloxkit.element.rest.SimpleRobloxSecurityFilter.ROBLOX_SECRET;

public class Main {

    public static final String PLACEHOLDER_API_KEY = "ChangeMe!";

    public static void main(String[] args) throws IOException {

        final var elementProperties = new Properties();
        elementProperties.put(ROBLOX_SECRET, PLACEHOLDER_API_KEY);

        // Create the local instance of the Elements server
        final var local = ElementsLocalBuilder.getDefault()
                .withElementNamed(
                        "robloxkit",
                        "dev.getelements.robloxkit.element",
                        PropertiesAttributes.wrap(elementProperties))
                .build();

        // Put it in a try-with-resources block to ensure clean shutdown.
        try (local) {

            // Starts the server
            local.start();

            // Run the server. Blocks as long as the server runs.
            local.run();

        }

    }
}
