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
}