package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for inventory operations.
 * This interface provides a common API for inventory management across mod loaders.
 * Abstracts differences between Forge's ItemStackHandler and Fabric's inventory systems.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs.
 */
public interface InventoryHelper {
    
    /**
     * Check if an ItemStack is valid (not empty).
     * Platform implementations handle ItemStack validation.
     * 
     * @param itemStack Platform-specific ItemStack object
     * @return True if the ItemStack is valid (not empty)
     */
    boolean isItemStackValid(Object itemStack);
    
    // ========================================
    // ENHANCED METHODS FOR TOWN MANAGEMENT
    // Note: Will be implemented in Phase 10.2 after resolving interface conflicts
    // Current platform implementations have sophisticated inventory abstractions
    // that need to be integrated with these methods.
    // ========================================
}