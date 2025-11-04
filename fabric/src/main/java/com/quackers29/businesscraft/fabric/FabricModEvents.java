package com.quackers29.businesscraft.fabric;

import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric event registration using delegate pattern
 * Provides platform-agnostic event handling interface
 */
public class FabricModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModEvents.class);

    /**
     * Register all Fabric events using delegate pattern
     */
    public static void register() {
        LOGGER.info("Registering Fabric events...");

        try {
            // Initialize Fabric event callback handler
            FabricEventCallbackHandler.initialize();
            
            LOGGER.info("Fabric events registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering Fabric events", e);
            e.printStackTrace();
        }
    }
}
