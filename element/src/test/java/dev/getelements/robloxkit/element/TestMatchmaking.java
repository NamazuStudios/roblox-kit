package dev.getelements.robloxkit.element;

import org.testng.annotations.Test;

public class TestMatchmaking {

    private final TestMatchmakingServer server = TestMatchmakingServer.getInstance();

    @Test
    public void testSignIn() {
        server.getApplication();
    }

}
