package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for basic menu operations.
 * Only contains methods that can be called from common module.
 * Platform-specific methods are defined in platform modules.
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