package com.quackers29.businesscraft.fabric.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric event registration using delegate pattern.
 */
public class FabricModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModEvents.class);

    public static void register() {
        LOGGER.info("Registering Fabric events...");

        try {
            FabricEventCallbackHandler.initialize();
            LOGGER.info("Fabric events registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering Fabric events", e);
        }
    }
}
