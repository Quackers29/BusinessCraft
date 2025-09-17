package com.quackers29.businesscraft.api;

import java.util.function.Consumer;

/**
 * Platform-agnostic interface for event handling operations.
 * Implementations will handle platform-specific event systems.
 */
public interface EventHelper {
    /**
     * Register a mod event bus listener
     */
    void registerModEvent(Object listener);

    /**
     * Register a forge event bus listener
     */
    void registerForgeEvent(Object listener);

    /**
     * Add a server stopping event listener
     */
    void addServerStoppingListener(Consumer<Void> listener);

    /**
     * Add a server started event listener
     */
    void addServerStartedListener(Consumer<Void> listener);

    /**
     * Add a level unload event listener
     */
    void addLevelUnloadListener(Consumer<Void> listener);

    /**
     * Set the active town block for path creation mode
     */
    void setActiveTownBlock(net.minecraft.core.BlockPos pos);

    /**
     * Clear the active town block
     */
    void clearActiveTownBlock();
}
