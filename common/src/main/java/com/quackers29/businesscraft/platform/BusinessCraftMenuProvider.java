package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic menu provider interface.
 * This abstraction allows common module code to request menu operations
 * without depending on platform-specific menu implementations.
 *
 * Key design principles:
 * - Zero platform dependencies in interface definition
 * - Platform services handle all Minecraft API interactions
 * - Common module uses this for menu coordination
 * - Platform implementations provide actual menu functionality
 *
 * Renamed from MenuProvider to BusinessCraftMenuProvider to avoid collision
 * with Minecraft's net.minecraft.world.MenuProvider interface.
 */
public interface BusinessCraftMenuProvider {

    /**
     * Open a town interface menu for the specified player.
     *
     * @param player Platform-specific player object
     * @param position Block position as int array [x, y, z]
     * @return true if menu was opened successfully, false otherwise
     */
    boolean openTownInterfaceMenu(Object player, int[] position);

    /**
     * Open a trade menu for the specified player.
     *
     * @param player Platform-specific player object
     * @return true if menu was opened successfully, false otherwise
     */
    boolean openTradeMenu(Object player);

    /**
     * Open a storage menu for the specified player.
     *
     * @param player Platform-specific player object
     * @return true if menu was opened successfully, false otherwise
     */
    boolean openStorageMenu(Object player);

    /**
     * Open a payment board menu for the specified player.
     *
     * @param player Platform-specific player object
     * @param data Additional menu data (platform-specific)
     * @return true if menu was opened successfully, false otherwise
     */
    boolean openPaymentBoardMenu(Object player, Object data);

    /**
     * Check if a menu can be opened for the player.
     * Used to validate menu opening requests before attempting to open.
     *
     * @param player Platform-specific player object
     * @param menuType Type of menu being requested
     * @return true if menu can be opened, false otherwise
     */
    boolean canOpenMenu(Object player, String menuType);

    /**
     * Get the display name for a menu type.
     * Used for UI consistency across platforms.
     *
     * @param menuType Type of menu
     * @return Localized display name for the menu
     */
    String getMenuDisplayName(String menuType);
}
