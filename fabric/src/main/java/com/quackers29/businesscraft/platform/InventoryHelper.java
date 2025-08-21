package com.quackers29.businesscraft.platform;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.NotNull;

/**
 * Platform-agnostic inventory helper interface using Yarn mappings.
 * Provides cross-platform inventory operations for the Enhanced MultiLoader approach.
 */
public interface InventoryHelper {
    
    /**
     * Create a platform-specific inventory implementation
     * @param size The number of slots in the inventory
     * @return A new platform inventory
     */
    PlatformInventory createInventory(int size);
    
    /**
     * Create an inventory with validation support
     * @param size The number of slots in the inventory
     * @param validator Item validator for slot restrictions
     * @return A new platform inventory with validation
     */
    PlatformInventory createInventory(int size, ItemValidator validator);
    
    /**
     * Create an extraction-only view of an existing inventory
     * @param inventory The source inventory
     * @return An extraction-only view
     */
    PlatformInventory createExtractionOnlyView(PlatformInventory inventory);
    
    /**
     * Check if two item stacks can be combined
     * @param stack1 First item stack
     * @param stack2 Second item stack
     * @return true if stacks can be combined
     */
    boolean canCombineStacks(@NotNull ItemStack stack1, @NotNull ItemStack stack2);
    
    /**
     * Get the maximum stack size for an item
     * @param stack The item stack
     * @return Maximum stack size
     */
    int getMaxStackSize(@NotNull ItemStack stack);
    
    /**
     * Copy an item stack
     * @param stack The stack to copy
     * @return A copy of the stack
     */
    @NotNull ItemStack copyStack(@NotNull ItemStack stack);
    
    /**
     * Create an empty item stack
     * @return Empty item stack
     */
    @NotNull ItemStack emptyStack();
    
    /**
     * Platform-agnostic inventory interface.
     * Provides common inventory operations across different mod platforms.
     */
    interface PlatformInventory {
        
        /**
         * Get the number of slots in this inventory
         * @return Slot count
         */
        int getSlots();
        
        /**
         * Get the item stack in a specific slot
         * @param slot The slot index
         * @return The item stack in the slot
         */
        @NotNull ItemStack getStackInSlot(int slot);
        
        /**
         * Set the item stack in a specific slot
         * @param slot The slot index
         * @param stack The item stack to set
         */
        void setStackInSlot(int slot, @NotNull ItemStack stack);
        
        /**
         * Get the maximum stack size for a specific slot
         * @param slot The slot index
         * @return Maximum stack size for this slot
         */
        int getSlotLimit(int slot);
        
        /**
         * Insert an item stack into a specific slot
         * @param slot The slot index
         * @param stack The item stack to insert
         * @param simulate Whether to simulate the insertion
         * @return The remaining item stack that couldn't be inserted
         */
        @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate);
        
        /**
         * Extract items from a specific slot
         * @param slot The slot index
         * @param amount The amount to extract
         * @param simulate Whether to simulate the extraction
         * @return The extracted item stack
         */
        @NotNull ItemStack extractItem(int slot, int amount, boolean simulate);
        
        /**
         * Check if a specific slot is empty
         * @param slot The slot index
         * @return true if the slot is empty
         */
        boolean isSlotEmpty(int slot);
        
        /**
         * Check if an item is valid for a specific slot
         * @param slot The slot index
         * @param stack The item stack to check
         * @return true if the item is valid for this slot
         */
        boolean isItemValid(int slot, @NotNull ItemStack stack);
        
        /**
         * Serialize this inventory to NBT
         * @return NBT compound containing inventory data
         */
        NbtCompound serializeNBT();
        
        /**
         * Deserialize inventory data from NBT
         * @param nbt NBT compound containing inventory data
         */
        void deserializeNBT(NbtCompound nbt);
        
        /**
         * Mark the inventory as changed
         */
        void markDirty();
        
        /**
         * Check if the inventory has changed since the last mark
         * @return true if the inventory is dirty
         */
        boolean isDirty();
        
        /**
         * Clear the dirty flag
         */
        void clearDirty();
        
        /**
         * Check if the inventory is empty
         * @return true if all slots are empty
         */
        boolean isEmpty();
        
        /**
         * Clear all items from the inventory
         */
        void clear();
        
        /**
         * Get the total number of items in the inventory
         * @return Total item count
         */
        int getTotalItems();
        
        /**
         * Check if the inventory contains a specific item
         * @param stack The item stack to search for
         * @return true if the inventory contains this item
         */
        boolean contains(@NotNull ItemStack stack);
        
        /**
         * Find the first slot containing a specific item
         * @param stack The item stack to search for
         * @return The slot index, or -1 if not found
         */
        int findSlot(@NotNull ItemStack stack);
        
        /**
         * Get the first empty slot
         * @return The slot index, or -1 if no empty slots
         */
        int getFirstEmptySlot();
    }
    
    /**
     * Interface for validating items in inventory slots
     */
    interface ItemValidator {
        /**
         * Check if an item is valid for a specific slot
         * @param slot The slot index
         * @param stack The item stack to validate
         * @return true if the item is valid for this slot
         */
        boolean isItemValid(int slot, @NotNull ItemStack stack);
    }
}