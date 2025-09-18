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
     * Register a platform event bus listener
     */
    void registerPlatformEvent(Object listener);

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
    void setActiveTownBlock(Object pos);

    /**
     * Clear the active town block
     */
    void clearActiveTownBlock();
}
