package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for menu operations.
 * This interface provides a common API for menu handling across mod loaders.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs.
 */
public interface MenuHelper {
    
    /**
     * Refresh an active menu with updated data.
     * Platform implementations handle menu refreshing for their specific menu types.
     * 
     * @param player Platform-specific player object
     * @param refreshType Type of refresh needed (e.g., "search_radius", "town_data")
     */
    void refreshActiveMenu(Object player, String refreshType);
}