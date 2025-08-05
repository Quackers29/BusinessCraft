package com.quackers29.businesscraft.platform;

/**
 * Service for item-related operations that need platform abstraction
 */
public interface ItemService {
    /**
     * Get a platform-specific bread item instance
     */
    Object getBreadItem();
    
    /**
     * Get the display name of an item
     */
    String getItemDisplayName(Object item);
    
    /**
     * Check if two items are the same type
     */
    boolean areItemsEqual(Object item1, Object item2);
}