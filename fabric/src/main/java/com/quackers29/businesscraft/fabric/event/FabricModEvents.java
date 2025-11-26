package com.quackers29.businesscraft.fabric.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.core.BlockPos;

/**
 * Fabric event registration using delegate pattern
 * Provides platform-agnostic event handling interface
 */
public class FabricModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModEvents.class);

    /**
     * Set the active town block for path creation mode
     */
    public static void setActiveTownBlock(BlockPos pos) {
        com.quackers29.businesscraft.event.TownEventHandler.setActiveTownBlock(pos);
    }

    /**
     * Clear the active town block
     */
    public static void clearActiveTownBlock() {
        com.quackers29.businesscraft.event.TownEventHandler.clearActiveTownBlock();
    }

    /**
     * Register all Fabric events
     */
    public static void register() {
        LOGGER.info("Registering Fabric events...");

        try {
            // Initialize the event callback handler which registers the actual Fabric
            // events
            FabricEventCallbackHandler.initialize();

            LOGGER.info("Fabric events registered successfully");
        } catch (Exception e) {
            LOGGER.error("Error registering Fabric events", e);
        }
    }
}
