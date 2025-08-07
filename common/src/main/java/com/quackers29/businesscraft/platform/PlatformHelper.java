package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for loader-specific operations.
 * This interface provides a common API for operations that differ between mod loaders.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs.
 */
public interface PlatformHelper {
    
    /**
     * Gets the name of the current mod loader platform.
     * @return "forge" or "fabric"
     */
    String getPlatformName();
    
    /**
     * Checks if the specified mod is loaded.
     * @param modid The mod ID to check
     * @return true if the mod is loaded
     */
    boolean isModLoaded(String modid);
    
    /**
     * Checks if we're running in a development environment.
     * @return true if in development environment
     */
    boolean isDevelopmentEnvironment();
    
    /**
     * Gets the mod version.
     * @return The current mod version
     */
    String getModVersion();
    
    /**
     * Gets the display name for the mod.
     * @return The mod's display name
     */
    String getModDisplayName();
    
    /**
     * Send a message to a player with specified formatting.
     * Platform implementations handle player messaging with appropriate formatting.
     * 
     * @param player Platform-specific player object
     * @param message The message text to send
     * @param color Color code ("RED", "GREEN", "GOLD", etc.)
     */
    void sendPlayerMessage(Object player, String message, String color);
    
    /**
     * Force a block update to ensure all clients receive the changes.
     * Platform implementations trigger block state updates.
     * 
     * @param player Platform-specific player object (for world access)
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     */
    void forceBlockUpdate(Object player, int x, int y, int z);
    
    /**
     * Clear client-side caches that might contain stale data.
     * Platform implementations clear relevant UI and data caches.
     */
    void clearClientCaches();
    
    /**
     * Check if the current execution is on the client side.
     * Platform implementations determine the current side.
     * 
     * @return true if running on client side
     */
    boolean isClientSide();
    
    /**
     * Check if the current execution is on the server side.
     * Platform implementations determine the current side.
     * 
     * @return true if running on server side
     */
    boolean isServerSide();
    
    /**
     * Get the current logical side as a string.
     * Platform implementations return "CLIENT" or "SERVER".
     * 
     * @return Logical side identifier
     */
    String getLogicalSide();
    
    /**
     * Clear town platform cache data for a specific town.
     * Platform implementations clear client-side platform cache data.
     * 
     * @param townId Town UUID string
     */
    void clearTownPlatformCache(String townId);
    
    /**
     * Refresh any open platform management screens.
     * Platform implementations update platform management UI if open.
     */
    void refreshPlatformManagementScreen();
    
    /**
     * Update trade screen output item on the client side.
     * Platform implementations update trade UI with payment result.
     * 
     * @param itemStack Platform-specific ItemStack object
     */
    void updateTradeScreenOutput(Object itemStack);
    
    /**
     * Execute a task on the client main thread.
     * Platform implementations schedule client-side tasks.
     * 
     * @param task Runnable task to execute
     */
    void executeClientTask(Runnable task);
}