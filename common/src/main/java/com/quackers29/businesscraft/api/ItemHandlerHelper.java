package com.quackers29.businesscraft.api;

import net.minecraft.world.item.ItemStack;

/**
 * Platform-agnostic interface for item handler operations.
 * Implementations will provide access to platform-specific item handler classes.
 */
public interface ItemHandlerHelper {
    /**
     * Create a new ItemStackHandler with the given size
     */
    Object createItemStackHandler(int size);

    /**
     * Get the number of slots in the item handler
     */
    int getSlots(Object itemHandler);

    /**
     * Set the ItemStack in the specified slot
     */
    void setStackInSlot(Object itemHandler, int slot, ItemStack stack);

    /**
     * Get the ItemStack in the specified slot
     */
    ItemStack getStackInSlot(Object itemHandler, int slot);

    /**
     * Create a slot for the item handler at the given position
     */
    Object createSlot(Object itemHandler, int index, int x, int y);

    /**
     * Create a withdrawal-only slot for the item handler (for buffer storage)
     */
    Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y);
}
