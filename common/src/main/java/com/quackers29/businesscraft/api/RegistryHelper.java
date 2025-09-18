package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for registration operations.
 * Implementations will handle platform-specific registration.
 */
public interface RegistryHelper {
    /**
     * Register a block with the given name
     */
    void registerBlock(String name, Object block);

    /**
     * Register a block item for the given block
     */
    void registerBlockItem(String name, Object block);

    /**
     * Register an entity type with the given name
     */
    void registerEntityType(String name, Object entityType);

    /**
     * Register a block entity type with the given name
     */
    void registerBlockEntityType(String name, Object blockEntityType);

    /**
     * Register a menu type with the given name
     */
    void registerMenuType(String name, Object menuType);

    /**
     * Get an item by its ResourceLocation
     */
    Object getItem(Object location);

    /**
     * Get the ResourceLocation for an item
     */
    Object getItemKey(Object item);
}
