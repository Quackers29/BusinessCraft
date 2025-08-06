package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic event helper interface using Yarn mappings.
 * Provides cross-platform event registration for the Enhanced MultiLoader approach.
 */
public interface EventHelper {
    
    /**
     * Register a server starting event handler
     * @param handler The event handler
     */
    void registerServerStartingEvent(ServerStartingHandler handler);
    
    /**
     * Register a server stopping event handler
     * @param handler The event handler
     */
    void registerServerStoppingEvent(ServerStoppingHandler handler);
    
    /**
     * Register a player login event handler
     * @param handler The event handler
     */
    void registerPlayerLoginEvent(PlayerLoginHandler handler);
    
    /**
     * Interface for handling server starting events
     */
    @FunctionalInterface
    interface ServerStartingHandler {
        void onServerStarting();
    }
    
    /**
     * Interface for handling server stopping events
     */
    @FunctionalInterface
    interface ServerStoppingHandler {
        void onServerStopping();
    }
    
    /**
     * Interface for handling player login events
     */
    @FunctionalInterface
    interface PlayerLoginHandler {
        void onPlayerLogin();
    }
}