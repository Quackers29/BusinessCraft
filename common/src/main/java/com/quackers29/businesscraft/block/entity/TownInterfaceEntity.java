package com.quackers29.businesscraft.block.entity;

/**
 * Unified architecture interface for TownInterfaceEntity.
 * Provides direct access methods to replace BlockEntityHelper platform service calls.
 * Implementation in platform modules (TownInterfaceEntity implements this interface).
 */
public interface TownInterfaceEntity {
    
    /**
     * Mark the block entity as changed for persistence
     */
    void setChanged();
    
    /**
     * Sync data to client (replaces BlockEntityHelper.syncTownData)
     */
    void syncToClient();
    
    /**
     * Get the town name (replaces BlockEntityHelper.getTownName)
     */
    String getTownName();
    
    /**
     * Set the town name (replaces BlockEntityHelper.setTownName)
     */
    void setTownName(String townName);
    
    /**
     * Check if tourist spawning is enabled (replaces BlockEntityHelper.isTouristSpawningEnabled)
     */
    boolean isTouristSpawningEnabled();
    
    /**
     * Set tourist spawning enabled state (replaces BlockEntityHelper.setTouristSpawningEnabled)
     */
    void setTouristSpawningEnabled(boolean enabled);
    
    /**
     * Get search radius (replaces BlockEntityHelper.getSearchRadius)
     */
    int getSearchRadius();
    
    /**
     * Set search radius (replaces BlockEntityHelper.setSearchRadius)
     */
    void setSearchRadius(int radius);
}