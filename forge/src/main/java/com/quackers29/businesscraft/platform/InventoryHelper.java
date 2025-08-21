package com.quackers29.businesscraft.platform;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * Platform abstraction interface for inventory operations.
 * This interface provides a common API for inventory handling across mod loaders.
 * 
 * Abstracts:
 * - Forge: ItemStackHandler + IItemHandler + LazyOptional capabilities
 * - Fabric: SimpleInventory + Storage<ItemVariant> + Component system
 * 
 * Key Features Preserved:
 * - Slot-based item access and modification
 * - Hopper automation support (insertion/extraction)
 * - Transaction-safe operations
 * - Change callbacks for UI synchronization
 * - NBT serialization/deserialization
 * - Stack size validation and item validity checking
 * - Capability/component attachment to block entities
 */
public interface InventoryHelper {
    
    /**
     * Creates a platform-specific inventory handler with the specified number of slots.
     * 
     * @param slots Number of inventory slots
     * @param changeCallback Called when inventory contents change (for setChanged() calls)
     * @return Platform-specific inventory wrapper
     */
    PlatformInventory createInventory(int slots, Runnable changeCallback);
    
    /**
     * Creates a platform-specific inventory handler with slot validation.
     * 
     * @param slots Number of inventory slots  
     * @param changeCallback Called when inventory contents change
     * @param itemValidator Called to check if an item is valid for a slot
     * @return Platform-specific inventory wrapper with validation
     */
    PlatformInventory createInventory(int slots, Runnable changeCallback, ItemValidator itemValidator);
    
    /**
     * Platform-agnostic inventory interface that wraps platform-specific implementations.
     * This provides consistent behavior across Forge ItemStackHandler and Fabric SimpleInventory.
     */
    interface PlatformInventory {
        
        /**
         * Get the number of slots in this inventory.
         * @return Slot count
         */
        int getSlots();
        
        /**
         * Get the ItemStack in a specific slot.
         * @param slot Slot index
         * @return ItemStack in the slot (copy for safety)
         */
        @NotNull ItemStack getStackInSlot(int slot);
        
        /**
         * Set the ItemStack in a specific slot.
         * @param slot Slot index
         * @param stack ItemStack to place in the slot
         */
        void setStackInSlot(int slot, @NotNull ItemStack stack);
        
        /**
         * Insert an ItemStack into a specific slot.
         * Follows Forge ItemStackHandler behavior exactly.
         * 
         * @param slot Target slot index
         * @param stack ItemStack to insert
         * @param simulate If true, insertion is only simulated
         * @return Remainder that could not be inserted
         */
        @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate);
        
        /**
         * Extract items from a specific slot.
         * Follows Forge ItemStackHandler behavior exactly.
         * 
         * @param slot Source slot index
         * @param amount Maximum amount to extract
         * @param simulate If true, extraction is only simulated
         * @return ItemStack that was extracted
         */
        @NotNull ItemStack extractItem(int slot, int amount, boolean simulate);
        
        /**
         * Get the stack size limit for a specific slot.
         * @param slot Slot index
         * @return Maximum stack size for this slot
         */
        int getSlotLimit(int slot);
        
        /**
         * Check if an ItemStack is valid for a specific slot.
         * @param slot Slot index
         * @param stack ItemStack to validate
         * @return True if the stack is valid for this slot
         */
        boolean isItemValid(int slot, @NotNull ItemStack stack);
        
        /**
         * Serialize the inventory to NBT.
         * @return CompoundTag containing all inventory data
         */
        CompoundTag serializeNBT();
        
        /**
         * Deserialize the inventory from NBT.
         * @param nbt CompoundTag containing inventory data
         */
        void deserializeNBT(CompoundTag nbt);
        
        /**
         * Get the platform-specific inventory object for capability/component attachment.
         * Returns:
         * - Forge: The underlying ItemStackHandler for LazyOptional<IItemHandler> capabilities
         * - Fabric: The component for Storage<ItemVariant> attachment
         * 
         * @return Platform-specific object for block entity attachment
         */
        Object getPlatformInventory();
        
        /**
         * Set a callback to be called when inventory contents change.
         * Used for block entity setChanged() calls and client synchronization.
         * 
         * @param callback Callback to run on content changes
         */
        void setChangeCallback(Runnable callback);
        
        /**
         * Set an item validator for controlling what items can be placed in slots.
         * 
         * @param validator Item validation function
         */
        void setItemValidator(ItemValidator validator);
        
        /**
         * Create a view of this inventory that blocks insertion (extraction-only).
         * Used for buffer inventories that should only be extracted by hoppers.
         * 
         * @return Extraction-only view of this inventory
         */
        PlatformInventory createExtractionOnlyView();
    }
    
    /**
     * Functional interface for validating items in inventory slots.
     */
    @FunctionalInterface
    interface ItemValidator {
        /**
         * Check if an ItemStack is valid for a specific slot.
         * @param slot Slot index
         * @param stack ItemStack to validate
         * @return True if the stack is valid for this slot
         */
        boolean isItemValid(int slot, @NotNull ItemStack stack);
    }
    
    /**
     * Creates a platform-specific inventory capability/component for block entity attachment.
     * This handles the platform-specific mechanism for exposing inventories to external systems.
     * 
     * Forge: Returns LazyOptional<IItemHandler> for capability system
     * Fabric: Returns component for Storage<ItemVariant> system
     * 
     * @param inventory The PlatformInventory to wrap
     * @return Platform-specific capability/component wrapper
     */
    Object createInventoryCapability(PlatformInventory inventory);
    
    /**
     * Invalidate a platform-specific inventory capability when a block entity is removed.
     * This prevents memory leaks and ensures proper cleanup.
     * 
     * @param capability The capability object returned by createInventoryCapability
     */
    void invalidateCapability(Object capability);
    
    /**
     * Creates a combined inventory that appears as a single inventory to external systems
     * but internally manages multiple separate inventories.
     * 
     * Used for menu systems that need to present player inventory + custom inventories
     * as a unified interface.
     * 
     * @param inventories Array of PlatformInventory instances to combine
     * @return Combined inventory that delegates to the constituent inventories
     */
    PlatformInventory createCombinedInventory(PlatformInventory... inventories);
}