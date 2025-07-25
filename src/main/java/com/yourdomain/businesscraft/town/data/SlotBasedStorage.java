package com.yourdomain.businesscraft.town.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Modular slot-based storage system for consistent UI chest behavior across all systems.
 * Replaces Map<Item, Integer> approach to preserve exact slot positions between UI sessions.
 * 
 * Features:
 * - Variable slot count (2 for trade, 18 for payment buffer, 27+ for future chests)
 * - Exact slot position preservation between UI opens/closes
 * - Hopper automation compatibility with partial extraction
 * - Smart item allocation for reward claiming
 * - NBT serialization for save/load persistence
 * - ItemStackHandler integration for existing UI components
 */
public class SlotBasedStorage {
    
    private final ItemStack[] slots;
    private final int slotCount;
    
    /**
     * Create a new slot-based storage with the specified number of slots
     * @param slotCount Number of slots (2 for trade, 18 for payment buffer, etc.)
     */
    public SlotBasedStorage(int slotCount) {
        this.slotCount = slotCount;
        this.slots = new ItemStack[slotCount];
        
        // Initialize all slots to empty
        for (int i = 0; i < slotCount; i++) {
            this.slots[i] = ItemStack.EMPTY;
        }
    }
    
    /**
     * Get the number of slots in this storage
     * @return The slot count
     */
    public int getSlotCount() {
        return slotCount;
    }
    
    /**
     * Get the ItemStack in a specific slot
     * @param index Slot index (0-based)
     * @return ItemStack in the slot, or ItemStack.EMPTY if invalid index
     */
    public ItemStack getSlot(int index) {
        if (index < 0 || index >= slotCount) {
            return ItemStack.EMPTY;
        }
        return slots[index].copy(); // Return copy to prevent external modification
    }
    
    /**
     * Set the ItemStack in a specific slot
     * @param index Slot index (0-based)
     * @param stack ItemStack to place in the slot (will be copied)
     */
    public void setSlot(int index, ItemStack stack) {
        if (index >= 0 && index < slotCount) {
            this.slots[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
        }
    }
    
    /**
     * Clear a specific slot (set to empty)
     * @param index Slot index to clear
     */
    public void clearSlot(int index) {
        setSlot(index, ItemStack.EMPTY);
    }
    
    /**
     * Find the first empty slot
     * @return Slot index, or -1 if no empty slots available
     */
    public int findEmptySlot() {
        for (int i = 0; i < slotCount; i++) {
            if (slots[i].isEmpty()) {
                return i;
            }
        }
        return -1; // No empty slots
    }
    
    /**
     * Find a slot that can stack with the given ItemStack
     * @param stack ItemStack to find stackable slot for
     * @return Slot index, or -1 if no stackable slot found
     */
    public int findStackableSlot(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        
        for (int i = 0; i < slotCount; i++) {
            ItemStack slotStack = slots[i];
            if (!slotStack.isEmpty() && 
                ItemStack.isSameItemSameTags(slotStack, stack) && 
                slotStack.getCount() < slotStack.getMaxStackSize()) {
                
                // Check if we can add at least 1 item to this stack
                int remainingSpace = slotStack.getMaxStackSize() - slotStack.getCount();
                if (remainingSpace > 0) {
                    return i;
                }
            }
        }
        return -1; // No stackable slot found
    }
    
    /**
     * Attempt to add an ItemStack to storage using smart allocation
     * Tries stacking first, then empty slots
     * @param stack ItemStack to add
     * @return True if successfully added (fully or partially), false if storage full
     */
    public boolean addItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        ItemStack remaining = stack.copy();
        
        // First, try to stack with existing items
        for (int i = 0; i < slotCount && !remaining.isEmpty(); i++) {
            ItemStack slotStack = slots[i];
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, remaining)) {
                int maxStackSize = slotStack.getMaxStackSize();
                int currentCount = slotStack.getCount();
                int remainingSpace = maxStackSize - currentCount;
                
                if (remainingSpace > 0) {
                    int toAdd = Math.min(remainingSpace, remaining.getCount());
                    slotStack.setCount(currentCount + toAdd);
                    remaining.shrink(toAdd);
                }
            }
        }
        
        // Then, try to place in empty slots
        for (int i = 0; i < slotCount && !remaining.isEmpty(); i++) {
            if (slots[i].isEmpty()) {
                int maxStackSize = remaining.getMaxStackSize();
                int toPlace = Math.min(maxStackSize, remaining.getCount());
                
                slots[i] = remaining.copy();
                slots[i].setCount(toPlace);
                remaining.shrink(toPlace);
            }
        }
        
        // Return true if we placed at least some items
        return remaining.getCount() < stack.getCount();
    }
    
    /**
     * Remove a specific amount of an item from storage
     * @param item Item type to remove
     * @param amount Amount to remove
     * @return ItemStack containing the removed items, or empty if not enough available
     */
    public ItemStack removeItem(Item item, int amount) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }
        
        int totalRemoved = 0;
        ItemStack result = ItemStack.EMPTY;
        
        // Remove from slots, starting from first occurrence
        for (int i = 0; i < slotCount && totalRemoved < amount; i++) {
            ItemStack slotStack = slots[i];
            if (!slotStack.isEmpty() && slotStack.getItem() == item) {
                int toRemove = Math.min(slotStack.getCount(), amount - totalRemoved);
                
                if (result.isEmpty()) {
                    result = new ItemStack(item, toRemove);
                } else {
                    result.setCount(result.getCount() + toRemove);
                }
                
                slotStack.shrink(toRemove);
                if (slotStack.isEmpty()) {
                    slots[i] = ItemStack.EMPTY;
                }
                
                totalRemoved += toRemove;
            }
        }
        
        return result;
    }
    
    /**
     * Get the total count of a specific item across all slots
     * @param item Item to count
     * @return Total count of the item
     */
    public int getTotalCount(Item item) {
        int total = 0;
        for (int i = 0; i < slotCount; i++) {
            ItemStack slotStack = slots[i];
            if (!slotStack.isEmpty() && slotStack.getItem() == item) {
                total += slotStack.getCount();
            }
        }
        return total;
    }
    
    /**
     * Check if storage is empty (all slots empty)
     * @return True if all slots are empty
     */
    public boolean isEmpty() {
        for (int i = 0; i < slotCount; i++) {
            if (!slots[i].isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Clear all slots (set all to empty)
     */
    public void clear() {
        for (int i = 0; i < slotCount; i++) {
            slots[i] = ItemStack.EMPTY;
        }
    }
    
    /**
     * Create an ItemStackHandler for UI integration
     * This allows existing UI components to work with slot-based storage
     * @return ItemStackHandler that wraps this storage
     */
    public ItemStackHandler createItemHandler() {
        return new SlotBasedItemStackHandler(this);
    }
    
    /**
     * Serialize storage to NBT for save/load
     * @return CompoundTag containing all slot data
     */
    public CompoundTag toNBT() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("SlotCount", slotCount);
        
        ListTag slotList = new ListTag();
        for (int i = 0; i < slotCount; i++) {
            CompoundTag slotTag = new CompoundTag();
            slotTag.putInt("Slot", i);
            if (!slots[i].isEmpty()) {
                slots[i].save(slotTag);
            }
            slotList.add(slotTag);
        }
        nbt.put("Slots", slotList);
        
        return nbt;
    }
    
    /**
     * Deserialize storage from NBT
     * @param nbt CompoundTag containing slot data
     */
    public void fromNBT(CompoundTag nbt) {
        // Clear current data
        clear();
        
        // Verify slot count matches (for safety)
        int savedSlotCount = nbt.getInt("SlotCount");
        if (savedSlotCount != slotCount) {
            throw new IllegalArgumentException(
                "Slot count mismatch: expected " + slotCount + ", got " + savedSlotCount);
        }
        
        // Load slot data
        ListTag slotList = nbt.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < slotList.size(); i++) {
            CompoundTag slotTag = slotList.getCompound(i);
            int slotIndex = slotTag.getInt("Slot");
            
            if (slotIndex >= 0 && slotIndex < slotCount) {
                // Load ItemStack if present
                if (slotTag.contains("id")) { // NBT has item data
                    slots[slotIndex] = ItemStack.of(slotTag);
                } else {
                    slots[slotIndex] = ItemStack.EMPTY;
                }
            }
        }
    }
    
    /**
     * Copy all slots from another SlotBasedStorage
     * @param source Source storage to copy from
     */
    public void copyFrom(SlotBasedStorage source) {
        int copyCount = Math.min(this.slotCount, source.slotCount);
        for (int i = 0; i < copyCount; i++) {
            this.slots[i] = source.slots[i].copy();
        }
        
        // Clear any remaining slots if this storage is larger
        for (int i = copyCount; i < this.slotCount; i++) {
            this.slots[i] = ItemStack.EMPTY;
        }
    }
    
    /**
     * Create a copy of this storage
     * @return New SlotBasedStorage with identical contents
     */
    public SlotBasedStorage copy() {
        SlotBasedStorage copy = new SlotBasedStorage(this.slotCount);
        copy.copyFrom(this);
        return copy;
    }
    
    /**
     * ItemStackHandler wrapper for UI integration
     * This allows existing UI code to work with SlotBasedStorage without changes
     */
    private static class SlotBasedItemStackHandler extends ItemStackHandler {
        private final SlotBasedStorage storage;
        
        public SlotBasedItemStackHandler(SlotBasedStorage storage) {
            super(storage.getSlotCount());
            this.storage = storage;
        }
        
        @Override
        public int getSlots() {
            return storage.getSlotCount();
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return storage.getSlot(slot);
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            storage.setSlot(slot, stack);
            onContentsChanged(slot);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            ItemStack slotStack = storage.getSlot(slot);
            
            if (slotStack.isEmpty()) {
                // Empty slot - can insert
                int maxInsert = Math.min(stack.getMaxStackSize(), stack.getCount());
                if (!simulate) {
                    storage.setSlot(slot, new ItemStack(stack.getItem(), maxInsert));
                    onContentsChanged(slot);
                }
                
                if (maxInsert < stack.getCount()) {
                    ItemStack remainder = stack.copy();
                    remainder.shrink(maxInsert);
                    return remainder;
                } else {
                    return ItemStack.EMPTY;
                }
            } else if (ItemStack.isSameItemSameTags(slotStack, stack)) {
                // Stackable - try to merge
                int maxStackSize = slotStack.getMaxStackSize();
                int currentCount = slotStack.getCount();
                int remainingSpace = maxStackSize - currentCount;
                int maxInsert = Math.min(remainingSpace, stack.getCount());
                
                if (maxInsert > 0 && !simulate) {
                    ItemStack newStack = slotStack.copy();
                    newStack.setCount(currentCount + maxInsert);
                    storage.setSlot(slot, newStack);
                    onContentsChanged(slot);
                }
                
                if (maxInsert < stack.getCount()) {
                    ItemStack remainder = stack.copy();
                    remainder.shrink(maxInsert);
                    return remainder;
                } else {
                    return ItemStack.EMPTY;
                }
            } else {
                // Not stackable - cannot insert
                return stack;
            }
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            
            ItemStack slotStack = storage.getSlot(slot);
            if (slotStack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            
            int extractCount = Math.min(amount, slotStack.getCount());
            ItemStack extracted = new ItemStack(slotStack.getItem(), extractCount);
            
            if (!simulate) {
                ItemStack remaining = slotStack.copy();
                remaining.shrink(extractCount);
                storage.setSlot(slot, remaining);
                onContentsChanged(slot);
            }
            
            return extracted;
        }
        
        @Override
        public int getSlotLimit(int slot) {
            ItemStack slotStack = storage.getSlot(slot);
            return slotStack.isEmpty() ? 64 : slotStack.getMaxStackSize();
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true; // Allow all items by default
        }
    }
}