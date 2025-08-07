package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.InventoryHelper;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import org.jetbrains.annotations.NotNull;

/**
 * Fabric implementation of InventoryHelper using Yarn mappings.
 * Implements cross-platform inventory operations using Fabric Transfer API and SimpleInventory.
 */
public class FabricInventoryHelper implements InventoryHelper {
    
    @Override
    public PlatformInventory createInventory(int size) {
        return new FabricPlatformInventory(size);
    }
    
    @Override
    public PlatformInventory createInventory(int size, ItemValidator validator) {
        return new FabricPlatformInventory(size, validator);
    }
    
    @Override
    public PlatformInventory createExtractionOnlyView(PlatformInventory inventory) {
        return new ExtractionOnlyInventoryView((FabricPlatformInventory) inventory);
    }
    
    @Override
    public boolean canCombineStacks(@NotNull ItemStack stack1, @NotNull ItemStack stack2) {
        if (stack1.isEmpty() || stack2.isEmpty()) return true;
        return ItemStack.canCombine(stack1, stack2);
    }
    
    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return stack.getMaxCount();
    }
    
    @Override
    public @NotNull ItemStack copyStack(@NotNull ItemStack stack) {
        return stack.copy();
    }
    
    @Override
    public @NotNull ItemStack emptyStack() {
        return ItemStack.EMPTY;
    }
    
    /**
     * Fabric-based platform inventory implementation using SimpleInventory
     */
    public static class FabricPlatformInventory implements PlatformInventory {
        private final SimpleInventory inventory;
        private final ItemValidator validator;
        private boolean dirty = false;
        
        public FabricPlatformInventory(int size) {
            this(size, null);
        }
        
        public FabricPlatformInventory(int size, ItemValidator validator) {
            this.inventory = new SimpleInventory(size);
            this.validator = validator;
            this.inventory.addListener(inv -> markDirty());
        }
        
        @Override
        public int getSlots() {
            return inventory.size();
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= inventory.size()) return ItemStack.EMPTY;
            return inventory.getStack(slot);
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot >= 0 && slot < inventory.size()) {
                inventory.setStack(slot, stack);
            }
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return 64; // Default Minecraft stack limit
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot < 0 || slot >= inventory.size() || stack.isEmpty()) return stack;
            
            if (!isItemValid(slot, stack)) return stack;
            
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) {
                int toInsert = Math.min(stack.getCount(), getSlotLimit(slot));
                if (!simulate) {
                    setStackInSlot(slot, stack.copyWithCount(toInsert));
                }
                return stack.copyWithCount(stack.getCount() - toInsert);
            } else if (canCombineStacks(existing, stack)) {
                int availableSpace = Math.min(getSlotLimit(slot), existing.getMaxCount()) - existing.getCount();
                int toInsert = Math.min(stack.getCount(), availableSpace);
                if (toInsert > 0 && !simulate) {
                    existing.increment(toInsert);
                }
                return stack.copyWithCount(stack.getCount() - toInsert);
            }
            
            return stack;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot < 0 || slot >= inventory.size() || amount <= 0) return ItemStack.EMPTY;
            
            ItemStack existing = getStackInSlot(slot);
            if (existing.isEmpty()) return ItemStack.EMPTY;
            
            int toExtract = Math.min(amount, existing.getCount());
            if (!simulate) {
                existing.decrement(toExtract);
                if (existing.isEmpty()) {
                    setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
            
            return existing.copyWithCount(toExtract);
        }
        
        @Override
        public boolean isSlotEmpty(int slot) {
            return getStackInSlot(slot).isEmpty();
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (validator != null) {
                return validator.isItemValid(slot, stack);
            }
            return true;
        }
        
        @Override
        public NbtCompound serializeNBT() {
            NbtCompound nbt = new NbtCompound();
            NbtList items = new NbtList();
            
            for (int i = 0; i < getSlots(); i++) {
                ItemStack stack = getStackInSlot(i);
                if (!stack.isEmpty()) {
                    NbtCompound item = new NbtCompound();
                    item.putInt("Slot", i);
                    stack.writeNbt(item);
                    items.add(item);
                }
            }
            
            nbt.put("Items", items);
            nbt.putInt("Size", getSlots());
            return nbt;
        }
        
        @Override
        public void deserializeNBT(NbtCompound nbt) {
            clear();
            NbtList items = nbt.getList("Items", 10);
            
            for (int i = 0; i < items.size(); i++) {
                NbtCompound item = items.getCompound(i);
                int slot = item.getInt("Slot");
                if (slot >= 0 && slot < getSlots()) {
                    setStackInSlot(slot, ItemStack.fromNbt(item));
                }
            }
        }
        
        @Override
        public void markDirty() {
            this.dirty = true;
        }
        
        @Override
        public boolean isDirty() {
            return dirty;
        }
        
        @Override
        public void clearDirty() {
            this.dirty = false;
        }
        
        @Override
        public boolean isEmpty() {
            for (int i = 0; i < getSlots(); i++) {
                if (!isSlotEmpty(i)) return false;
            }
            return true;
        }
        
        @Override
        public void clear() {
            for (int i = 0; i < getSlots(); i++) {
                setStackInSlot(i, ItemStack.EMPTY);
            }
        }
        
        @Override
        public int getTotalItems() {
            int total = 0;
            for (int i = 0; i < getSlots(); i++) {
                total += getStackInSlot(i).getCount();
            }
            return total;
        }
        
        @Override
        public boolean contains(@NotNull ItemStack stack) {
            return findSlot(stack) != -1;
        }
        
        @Override
        public int findSlot(@NotNull ItemStack stack) {
            for (int i = 0; i < getSlots(); i++) {
                if (canCombineStacks(getStackInSlot(i), stack)) {
                    return i;
                }
            }
            return -1;
        }
        
        @Override
        public int getFirstEmptySlot() {
            for (int i = 0; i < getSlots(); i++) {
                if (isSlotEmpty(i)) return i;
            }
            return -1;
        }
        
        private boolean canCombineStacks(@NotNull ItemStack stack1, @NotNull ItemStack stack2) {
            if (stack1.isEmpty() || stack2.isEmpty()) return true;
            return ItemStack.canCombine(stack1, stack2);
        }
    }
    
    /**
     * Extraction-only view of an inventory
     */
    public static class ExtractionOnlyInventoryView implements PlatformInventory {
        private final FabricPlatformInventory delegate;
        
        public ExtractionOnlyInventoryView(FabricPlatformInventory delegate) {
            this.delegate = delegate;
        }
        
        @Override
        public int getSlots() {
            return delegate.getSlots();
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return delegate.getStackInSlot(slot);
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            // No-op for extraction-only view
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return stack; // Cannot insert into extraction-only view
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate.extractItem(slot, amount, simulate);
        }
        
        @Override
        public boolean isSlotEmpty(int slot) {
            return delegate.isSlotEmpty(slot);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false; // Cannot insert into extraction-only view
        }
        
        @Override
        public NbtCompound serializeNBT() {
            return delegate.serializeNBT();
        }
        
        @Override
        public void deserializeNBT(NbtCompound nbt) {
            // No-op for extraction-only view
        }
        
        @Override
        public void markDirty() {
            delegate.markDirty();
        }
        
        @Override
        public boolean isDirty() {
            return delegate.isDirty();
        }
        
        @Override
        public void clearDirty() {
            delegate.clearDirty();
        }
        
        @Override
        public boolean isEmpty() {
            return delegate.isEmpty();
        }
        
        @Override
        public void clear() {
            // No-op for extraction-only view
        }
        
        @Override
        public int getTotalItems() {
            return delegate.getTotalItems();
        }
        
        @Override
        public boolean contains(@NotNull ItemStack stack) {
            return delegate.contains(stack);
        }
        
        @Override
        public int findSlot(@NotNull ItemStack stack) {
            return delegate.findSlot(stack);
        }
        
        @Override
        public int getFirstEmptySlot() {
            return delegate.getFirstEmptySlot();
        }
    }
}