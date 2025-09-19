package com.quackers29.businesscraft.api;

/**
 * Interface for registry operations
 */
public interface RegistryHelper {
    void registerBlock(String name, Object block);
    void registerBlockItem(String name, Object block);
    void registerEntityType(String name, Object entityType);
    void registerBlockEntityType(String name, Object blockEntityType);
    void registerMenuType(String name, Object menuType);
    Object getItem(Object location);
    Object getItemKey(Object item);
}
