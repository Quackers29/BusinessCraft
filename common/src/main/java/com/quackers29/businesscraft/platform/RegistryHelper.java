package com.quackers29.businesscraft.platform;

import java.util.function.Supplier;

/**
 * Platform-agnostic interface for registry operations.
 * This interface provides a common API for registration across mod loaders.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs (Forge/Fabric).
 * 
 * Note: This interface uses Object return types to avoid platform-specific imports
 * in the common module while maintaining compatibility with typed platform implementations.
 */
public interface RegistryHelper {
    
    /**
     * Register a block with the platform's registry system.
     */
    <T> Supplier<T> registerBlock(String name, Supplier<T> blockSupplier);
    
    /**
     * Register an item with the platform's registry system.
     */
    <T> Supplier<T> registerItem(String name, Supplier<T> itemSupplier);
    
    /**
     * Register a block entity type with the platform's registry system.
     */
    <T> Supplier<T> registerBlockEntity(String name, Supplier<T> blockEntitySupplier);
    
    /**
     * Register an entity type with the platform's registry system.
     */
    <T> Supplier<T> registerEntity(String name, Supplier<T> entitySupplier);
    
    /**
     * Register a menu type with the platform's registry system.
     */
    <T> Supplier<T> registerMenu(String name, Supplier<T> menuSupplier);
    
    /**
     * Finalize all registrations for the platform.
     */
    void finalizeRegistrations();
    
    // ========================================
    // ENHANCED METHODS FOR TOWN MANAGEMENT
    // ========================================
    
    /**
     * Get an item by its resource location string.
     * This is used for town resource storage and serialization.
     * 
     * @param resourceLocation Resource location string (e.g., "minecraft:bread")
     * @return Item object, or null if not found
     */
    Object getItem(String resourceLocation);
    
    /**
     * Get the resource location string from an item.
     * This is the inverse of getItem() for serialization.
     * 
     * @param item Item object (platform-specific)
     * @return Resource location string (e.g., "minecraft:bread")
     */
    String getItemId(Object item);
    
    /**
     * Serialize an item to a platform-agnostic format for NBT storage.
     * Used for town resource persistence and network packets.
     * 
     * @param item Item object to serialize
     * @return Serialized data (typically String resource location)
     */
    Object serializeItem(Object item);
    
    /**
     * Deserialize an item from platform-agnostic format.
     * This is the inverse of serializeItem() for loading.
     * 
     * @param data Serialized item data (from NBT or network)
     * @return Item object, or null if deserialization fails
     */
    Object deserializeItem(Object data);
    
    /**
     * Check if an item exists in the registry.
     * Used for validation during town resource operations.
     * 
     * @param resourceLocation Resource location string to check
     * @return true if item exists in registry
     */
    boolean itemExists(String resourceLocation);
    
    /**
     * Get all registered item resource locations.
     * Useful for debugging and admin tools.
     * 
     * @return Array of all item resource location strings
     */
    String[] getAllItemIds();
}