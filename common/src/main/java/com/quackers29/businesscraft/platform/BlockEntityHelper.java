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
    
    /**
     * Check if more platforms can be added to the town interface.
     * Platform implementations check the platform capacity limits.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return True if more platforms can be added
     */
    boolean canAddMorePlatforms(Object blockEntity);
    
    /**
     * Add a new platform to the town interface.
     * Platform implementations handle platform creation and setup.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return True if platform was successfully added
     */
    boolean addPlatform(Object blockEntity);
    
    /**
     * Mark a block entity as changed for persistence and client sync.
     * Platform implementations trigger the block entity's change notification.
     * 
     * @param blockEntity Platform-specific block entity
     */
    void markBlockEntityChanged(Object blockEntity);
    
    /**
     * Delete a platform from the town interface.
     * Platform implementations handle platform removal.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformIndex Index of the platform to delete
     * @return True if platform was successfully deleted
     */
    boolean deletePlatform(Object blockEntity, int platformIndex);
    
    /**
     * Remove a platform from the town interface by UUID.
     * Platform implementations handle platform removal by UUID.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @return True if platform was successfully removed
     */
    boolean removePlatform(Object blockEntity, String platformId);
    
    /**
     * Get the number of platforms in the town interface.
     * Platform implementations return the current platform count.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return Number of platforms
     */
    int getPlatformCount(Object blockEntity);
    
    /**
     * Enable or disable a platform in the town interface.
     * Platform implementations update the platform's enabled state.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformIndex Index of the platform
     * @param enabled New enabled state
     */
    void setPlatformEnabled(Object blockEntity, int platformIndex, boolean enabled);
    
    /**
     * Check if a platform is enabled.
     * Platform implementations return the platform's enabled state.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformIndex Index of the platform
     * @return True if the platform is enabled
     */
    boolean isPlatformEnabled(Object blockEntity, int platformIndex);
    
    /**
     * Get a block entity on the client side at the specified coordinates.
     * Platform implementations access client world block entities.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return Platform-specific block entity object, or null if not found
     */
    @Nullable
    Object getClientBlockEntity(int x, int y, int z);
}