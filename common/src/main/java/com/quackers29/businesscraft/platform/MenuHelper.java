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
    
    /**
     * Open a screen for the player using platform-specific networking.
     * This method handles the platform differences between Forge's NetworkHooks
     * and Fabric's ServerPlayerInterface.openHandledScreen().
     * 
     * @param player The ServerPlayer to open the screen for
     * @param menuProvider The MenuProvider that creates the container menu
     * @param pos The position associated with the screen (for extra data)
     */
    void openScreen(Object player, Object menuProvider, Object pos);
}