package com.quackers29.businesscraft.platform;

import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic interface for block entity operations.
 * This interface provides a common API for block entity access across mod loaders.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs (Forge/Fabric).
 */
public interface BlockEntityHelper {
    
    /**
     * Get a block entity at the specified position in the player's world.
     * Platform implementations convert coordinates and player context to block entity access.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate  
     * @param z Block Z coordinate
     * @return Platform-specific block entity object, or null if not found
     */
    @Nullable
    Object getBlockEntity(Object player, int x, int y, int z);
    
    /**
     * Get the town data provider from a town interface block entity.
     * Platform implementations access the block entity's town data provider.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return Platform-specific town data provider, or null if not found
     */
    @Nullable
    Object getTownDataProvider(Object blockEntity);
    
    /**
     * Check if tourist spawning is enabled for a town data provider.
     * Platform implementations access the provider's tourist spawning state.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return True if tourist spawning is enabled
     */
    boolean isTouristSpawningEnabled(Object townDataProvider);
    
    /**
     * Set the tourist spawning enabled state for a town data provider.
     * Platform implementations update the provider's tourist spawning state.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param enabled New enabled state
     */
    void setTouristSpawningEnabled(Object townDataProvider, boolean enabled);
    
    /**
     * Mark the town data provider as dirty for persistence.
     * Platform implementations trigger the provider's dirty marking.
     * 
     * @param townDataProvider Platform-specific town data provider
     */
    void markTownDataDirty(Object townDataProvider);
    
    /**
     * Sync town data between client and server.
     * Platform implementations trigger data synchronization.
     * 
     * @param blockEntity Platform-specific town interface block entity
     */
    void syncTownData(Object blockEntity);
    
    /**
     * Get the town name from a town data provider.
     * Platform implementations access the provider's town name.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return Town name, or null if not set
     */
    @Nullable
    String getTownName(Object townDataProvider);
    
    /**
     * Set the town name for a town data provider.
     * Platform implementations update the provider's town name.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param townName New town name
     */
    void setTownName(Object townDataProvider, String townName);
    
    /**
     * Get the town ID from a town data provider.
     * Platform implementations access the provider's town UUID.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return Town UUID string, or null if not set
     */
    @Nullable
    String getTownId(Object townDataProvider);
    
    /**
     * Check if the town data provider has been initialized.
     * Platform implementations check the provider's initialization state.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return True if the provider is initialized
     */
    boolean isTownDataInitialized(Object townDataProvider);
    
    /**
     * Get the search radius for platforms from a town data provider.
     * Platform implementations access the provider's search radius setting.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return Search radius in blocks
     */
    int getSearchRadius(Object townDataProvider);
    
    /**
     * Set the search radius for platforms for a town data provider.
     * Platform implementations update the provider's search radius setting.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param radius New search radius in blocks
     */
    void setSearchRadius(Object townDataProvider, int radius);
}