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
    
    /**
     * Set the active platform for path creation mode.
     * Platform implementations update the path handler state.
     * 
     * @param x Block X coordinate  
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param platformId Platform UUID string
     */
    void setActivePlatformForPathCreation(int x, int y, int z, String platformId);
    
    /**
     * Clear the active platform for path creation mode.
     * Platform implementations clear the path handler state.
     */
    void clearActivePlatformForPathCreation();
    
    /**
     * Create a platform-specific BlockPos object from coordinates.
     * Platform implementations create BlockPos using their specific constructors.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate 
     * @param z Block Z coordinate
     * @return Platform-specific BlockPos object
     */
    Object createBlockPos(int x, int y, int z);
    
    /**
     * Get the current client-side screen if available.
     * Platform implementations return the current Minecraft screen.
     * 
     * @return Platform-specific Screen object or null if no screen is open
     */
    Object getCurrentScreen();
    
    /**
     * Update payment board screen with new reward data.
     * Platform implementations update the payment board UI if it's currently open.
     * 
     * @param screen Platform-specific PaymentBoardScreen object
     * @param rewards List of reward data to display
     */
    void updatePaymentBoardScreen(Object screen, java.util.List<Object> rewards);
    
    /**
     * Serialize a reward entry for network transmission.
     * Platform implementations handle proper RewardEntry serialization.
     * 
     * @param reward Platform-specific RewardEntry object
     * @return Serialized reward data as string
     */
    String serializeRewardEntry(Object reward);
    
    /**
     * Enable platform and boundary visualization for a town block.
     * Platform implementations handle client-side visualization rendering.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return true if visualization was successfully enabled
     */
    boolean enablePlatformVisualization(int x, int y, int z);
    
    /**
     * Update boundary visualization radius on the client side.
     * Platform implementations handle updating the boundary renderer with new radius data.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param boundaryRadius New boundary radius in blocks
     * @return true if boundary visualization was successfully updated
     */
    boolean updateBoundaryVisualization(int x, int y, int z, int boundaryRadius);
}