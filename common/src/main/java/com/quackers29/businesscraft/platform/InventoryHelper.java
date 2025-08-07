package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for inventory operations.
 * This interface provides a common API for inventory management across mod loaders.
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
}