package com.quackers29.businesscraft.platform;

/**
 * Platform abstraction service that provides access to platform-specific functionality.
 * This allows the common business logic to work across different mod platforms.
 * Unified architecture implementation for cross-platform compatibility.
 */
public interface PlatformService {
    
    /**
     * Get the item service for platform-specific item operations
     */
    ItemService getItemService();
    
    /**
     * Get the world service for platform-specific world operations  
     */
    WorldService getWorldService();
    
    /**
     * Get the position factory for creating platform-specific positions
     */
    PositionFactory getPositionFactory();
    
    /**
     * Get the data serialization service for saving/loading data
     */
    DataSerializationService getDataSerializationService();
}