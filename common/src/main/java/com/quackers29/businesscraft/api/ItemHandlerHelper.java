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

    /**
     * Create a LazyOptional wrapper for the item handler
     */
    Object createLazyOptional(Object itemHandler);

    /**
     * Check if the given capability is an item handler capability
     */
    boolean isItemHandlerCapability(Object capability);

    /**
     * Get an empty LazyOptional
     */
    Object getEmptyLazyOptional();

    /**
     * Cast the LazyOptional to the appropriate type for the capability system
     */
    Object castLazyOptional(Object lazyOptional, Object capability);

    /**
     * Create a custom ItemStackHandler with callback for contents changes
     */
    Object createCustomItemStackHandler(int size, Runnable onContentsChanged);

    /**
     * Invalidate a LazyOptional
     */
    void invalidateLazyOptional(Object lazyOptional);

    /**
     * Serialize the item handler to NBT
     */
    Object serializeNBT(Object itemHandler);

    /**
     * Deserialize the item handler from NBT
     */
    void deserializeNBT(Object itemHandler, Object nbt);

    /**
     * Extract an item from the specified slot
     */
    ItemStack extractItem(Object itemHandler, int slot, int amount, boolean simulate);

    /**
     * Insert an item into the specified slot
     */
    ItemStack insertItem(Object itemHandler, int slot, ItemStack stack, boolean simulate);
}
