package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.MenuHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of MenuHelper using Yarn mappings.
 * Simplified to match common interface pattern for unified architecture.
 */
public class FabricMenuHelper implements MenuHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricMenuHelper.class);
    
    @Override
    public void refreshActiveMenu(Object player, String refreshType) {
        LOGGER.debug("FABRIC MENU HELPER: refreshActiveMenu not yet implemented");
        // TODO: Implement Fabric-specific menu refreshing
    }
    
    @Override
    public void openScreen(Object player, Object menuProvider, Object pos) {
        // Fabric uses different mappings, so use reflection to avoid mapping conflicts
        try {
            // Get player class (should be ServerPlayer)
            Class<?> playerClass = player.getClass();
            
            // Call openMenu method on the player
            java.lang.reflect.Method openMenuMethod = playerClass.getMethod("openMenu", Object.class);
            openMenuMethod.invoke(player, menuProvider);
            
            LOGGER.debug("Successfully opened Fabric menu screen");
            
        } catch (Exception e) {
            LOGGER.error("Failed to open Fabric menu screen: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to open Fabric menu screen", e);
        }
    }
}