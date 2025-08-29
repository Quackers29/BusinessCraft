package com.quackers29.businesscraft.platform;

import java.util.function.Supplier;

/**
 * Platform-agnostic interface for menu operations and MenuType management.
 * Provides abstraction for menu creation, registration, and opening across platforms.
 * Platform-specific implementations handle the differences between Forge and Fabric.
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
     * Open a town interface menu for the player.
     * Platform implementations handle the specific menu opening mechanics.
     *
     * @param player Platform-specific player object
     * @param blockPos Block position as int array [x, y, z]
     * @param displayName Menu display name
     * @return true if menu was opened successfully, false otherwise
     */
    boolean openTownInterfaceMenu(Object player, int[] blockPos, String displayName);

    /**
     * Create a simple MenuType for menus that don't need extra data.
     * Platform implementations handle the specific MenuType creation mechanics.
     *
     * @param <T> Menu type
     * @param menuFactory Factory for creating menu instances
     * @return Supplier of the created MenuType
     */
    <T> Supplier<Object> createSimpleMenuType(Object menuFactory);

    /**
     * Create a data-driven MenuType for menus that need extra data.
     * Platform implementations handle the specific MenuType creation mechanics.
     *
     * @param <T> Menu type
     * @param menuFactory Factory for creating menu instances with data
     * @return Supplier of the created MenuType
     */
    <T> Supplier<Object> createDataDrivenMenuType(Object menuFactory);

    /**
     * Register a MenuType with the platform's registry system.
     * Platform implementations handle the specific registration mechanics.
     *
     * @param name Registry name for the menu type
     * @param menuTypeSupplier Supplier of the MenuType to register
     * @return Registered MenuType supplier, or null if registration failed
     */
    Supplier<Object> registerMenuType(String name, Supplier<Object> menuTypeSupplier);

    /**
     * Check if a MenuType is registered with the platform's registry system.
     * Platform implementations handle the specific registry checking mechanics.
     *
     * @param name Registry name for the menu type
     * @return true if the MenuType is registered, false otherwise
     */
    boolean isMenuTypeRegistered(String name);

    /**
     * Get a registered MenuType by name from the platform's registry system.
     * Platform implementations handle the specific registry lookup mechanics.
     *
     * @param name Registry name for the menu type
     * @return The registered MenuType, or null if not found
     */
    Object getMenuType(String name);
}