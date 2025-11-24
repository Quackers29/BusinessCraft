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

    // Track active town block for path creation mode (similar to ForgeModEvents)
    private static BlockPos activeTownBlockPos = null;
    private static long lastClickTime = 0;
    private static boolean awaitingSecondClick = false;

    /**
     * Set the active town block for path creation mode
     */
    public static void setActiveTownBlock(BlockPos pos) {
        LOGGER.debug("Setting active town block to: {}", pos);
        activeTownBlockPos = pos;
        awaitingSecondClick = false;
    }

    /**
     * Clear the active town block
     */
    public static void clearActiveTownBlock() {
        LOGGER.debug("Clearing active town block");
        activeTownBlockPos = null;
        awaitingSecondClick = false;
    }

    /**
     * Get the active town block position
     */
    public static BlockPos getActiveTownBlockPos() {
        return activeTownBlockPos;
    }

    /**
     * Get whether we're awaiting the second click
     */
    public static boolean isAwaitingSecondClick() {
        return awaitingSecondClick;
    }

    /**
     * Set whether we're awaiting the second click
     */
    public static void setAwaitingSecondClick(boolean awaiting) {
        awaitingSecondClick = awaiting;
    }

    /**
     * Get the last click time (for debouncing)
     */
    public static long getLastClickTime() {
        return lastClickTime;
    }

    /**
     * Set the last click time
     */
    public static void setLastClickTime(long time) {
        lastClickTime = time;
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
