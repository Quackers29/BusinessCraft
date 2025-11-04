package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import org.lwjgl.glfw.GLFW;

/**
 * Handles key inputs for toggling the Town Debug Overlay (F4)
 */
public class TownDebugKeyHandler {
    
    /**
     * Initialize event callbacks. Should be called during mod initialization.
     */
    public static void initialize() {
        PlatformAccess.getEvents().registerKeyInputCallback(TownDebugKeyHandler::onKeyInput);
    }
    
    /**
     * Handles key presses to detect F4 key
     */
    private static boolean onKeyInput(int keyCode, int action) {
        // Only handle key press events (not releases)
        if (action != GLFW.GLFW_PRESS) {
            return false;
        }
        
        // Handle F4 key
        if (keyCode == GLFW.GLFW_KEY_F4) {
            // Toggle the debug overlay visibility
            TownDebugOverlay.toggleVisibility();
            return false; // Don't cancel the event
        }
        
        return false;
    }
} 
