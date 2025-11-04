package com.quackers29.businesscraft.api;

import net.minecraft.world.item.ItemStack;

/**
 * Platform-agnostic interface for slot-based storage operations.
 * Used by item handler wrappers to interact with storage implementations.
 */
public interface SlotBasedStorageAccess {
    /**
     * Get the number of slots in this storage
     */
    int getSlotCount();
    
    /**
     * Get the ItemStack in a specific slot
     * @param index Slot index (0-based)
     * @return ItemStack in the slot, or ItemStack.EMPTY if invalid index
     */
    ItemStack getSlot(int index);
    
    /**
     * Set the ItemStack in a specific slot
     * @param index Slot index (0-based)
     * @param stack ItemStack to place in the slot
     */
    void setSlot(int index, ItemStack stack);
    
    /**
     * Notification that contents changed (for UI updates)
     * @param slot The slot that changed
     */
    void onContentsChanged(int slot);
}

