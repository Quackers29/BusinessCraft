package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.PlatformAccess;
import org.lwjgl.glfw.GLFW;

public class TownDebugKeyHandler {

    public static void initialize() {
        PlatformAccess.getEvents().registerKeyInputCallback(TownDebugKeyHandler::onKeyInput);
    }

    private static boolean onKeyInput(int keyCode, int action) {
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_F4) {
            TownDebugOverlay.toggleVisibility();
            return false;
        }

        return false;
    }
}
