package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.InventoryHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Forge implementation of the InventoryHelper interface using ItemStackHandler and capabilities.
 * This class provides all the functionality needed to abstract Forge's inventory system
 * for cross-platform compatibility.
 */
public class ForgeInventoryHelper implements InventoryHelper {
    
    @Override
    public PlatformInventory createInventory(int slots, Runnable changeCallback) {
        return new ForgeInventoryWrapper(slots, changeCallback, null);
    }
    
    @Override
    public PlatformInventory createInventory(int slots, Runnable changeCallback, ItemValidator itemValidator) {
        return new ForgeInventoryWrapper(slots, changeCallback, itemValidator);
    }
    
    @Override
    public Object createInventoryCapability(PlatformInventory inventory) {
        if (inventory instanceof ForgeInventoryWrapper wrapper) {
            return LazyOptional.of(() -> wrapper.getItemStackHandler());
        }
        throw new IllegalArgumentException("PlatformInventory must be a ForgeInventoryWrapper for Forge platform");
    }
    
    @Override
    public void invalidateCapability(Object capability) {
        if (capability instanceof LazyOptional<?> lazyOptional) {
            lazyOptional.invalidate();
        }
    }
    
    @Override
    public PlatformInventory createCombinedInventory(PlatformInventory... inventories) {
        return new CombinedForgeInventory(inventories);
    }
    
    /**
     * Forge-specific implementation of PlatformInventory using ItemStackHandler.
     */
    private static class ForgeInventoryWrapper implements PlatformInventory {
        private final ItemStackHandler itemHandler;
        private Runnable changeCallback;
        private ItemValidator itemValidator;
        
        public ForgeInventoryWrapper(int slots, Runnable changeCallback, ItemValidator itemValidator) {
            this.changeCallback = changeCallback;
            this.itemValidator = itemValidator;
            
            // Create ItemStackHandler with custom behavior
            this.itemHandler = new ItemStackHandler(slots) {
                @Override
                protected void onContentsChanged(int slot) {
                    if (ForgeInventoryWrapper.this.changeCallback != null) {
                        ForgeInventoryWrapper.this.changeCallback.run();
                    }
                }
                
                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    if (ForgeInventoryWrapper.this.itemValidator != null) {
                        return ForgeInventoryWrapper.this.itemValidator.isItemValid(slot, stack);
                    }
                    return super.isItemValid(slot, stack);
                }
            };
        }
        
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return itemHandler.insertItem(slot, stack, simulate);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return itemHandler.extractItem(slot, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return itemHandler.isItemValid(slot, stack);
        }
        
        @Override
        public CompoundTag serializeNBT() {
            return itemHandler.serializeNBT();
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            itemHandler.deserializeNBT(nbt);
        }
        
        @Override
        public Object getPlatformInventory() {
            return itemHandler;
        }
        
        @Override
        public void setChangeCallback(Runnable callback) {
            this.changeCallback = callback;
        }
        
        @Override
        public void setItemValidator(ItemValidator validator) {
            this.itemValidator = validator;
        }
        
        @Override
        public PlatformInventory createExtractionOnlyView() {
            return new ExtractionOnlyForgeInventory(this);
        }
        
        /**
         * Get the underlying ItemStackHandler for Forge-specific operations.
         * @return The ItemStackHandler instance
         */
        public ItemStackHandler getItemStackHandler() {
            return itemHandler;
        }
    }
    
    /**
     * Extraction-only wrapper for buffer inventories.
     * Blocks insertion but allows extraction for hopper automation.
     */
    private static class ExtractionOnlyForgeInventory implements PlatformInventory {
        private final ForgeInventoryWrapper delegate;
        
        public ExtractionOnlyForgeInventory(ForgeInventoryWrapper delegate) {
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
            // Allow setting for internal operations
            delegate.setStackInSlot(slot, stack);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // Block all insertion
            return stack;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            // Allow extraction
            return delegate.extractItem(slot, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Block validation for insertion
            return false;
        }
        
        @Override
        public CompoundTag serializeNBT() {
            return delegate.serializeNBT();
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            delegate.deserializeNBT(nbt);
        }
        
        @Override
        public Object getPlatformInventory() {
            // Return wrapper that blocks insertion
            return new ExtractionOnlyItemHandler(delegate.getItemStackHandler());
        }
        
        @Override
        public void setChangeCallback(Runnable callback) {
            delegate.setChangeCallback(callback);
        }
        
        @Override
        public void setItemValidator(ItemValidator validator) {
            delegate.setItemValidator(validator);
        }
        
        @Override
        public PlatformInventory createExtractionOnlyView() {
            return this; // Already extraction-only
        }
    }
    
    /**
     * ItemStackHandler wrapper that blocks insertion for buffer inventories.
     */
    private static class ExtractionOnlyItemHandler implements IItemHandler {
        private final ItemStackHandler delegate;
        
        public ExtractionOnlyItemHandler(ItemStackHandler delegate) {
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
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            // Block all insertion
            return stack;
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return delegate.extractItem(slot, amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            return delegate.getSlotLimit(slot);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return false; // Block insertion
        }
    }
    
    /**
     * Combined inventory that presents multiple inventories as a single interface.
     * Used for menu systems that need unified inventory access.
     */
    private static class CombinedForgeInventory implements PlatformInventory {
        private final PlatformInventory[] inventories;
        private final int[] slotOffsets;
        private final int totalSlots;
        
        public CombinedForgeInventory(PlatformInventory[] inventories) {
            this.inventories = inventories;
            this.slotOffsets = new int[inventories.length];
            
            int offset = 0;
            for (int i = 0; i < inventories.length; i++) {
                slotOffsets[i] = offset;
                offset += inventories[i].getSlots();
            }
            this.totalSlots = offset;
        }
        
        private int[] getInventoryAndSlot(int slot) {
            for (int i = 0; i < inventories.length; i++) {
                if (slot >= slotOffsets[i] && 
                    (i == inventories.length - 1 || slot < slotOffsets[i + 1])) {
                    return new int[]{i, slot - slotOffsets[i]};
                }
            }
            throw new IndexOutOfBoundsException("Invalid slot: " + slot);
        }
        
        @Override
        public int getSlots() {
            return totalSlots;
        }
        
        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            int[] pos = getInventoryAndSlot(slot);
            return inventories[pos[0]].getStackInSlot(pos[1]);
        }
        
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            int[] pos = getInventoryAndSlot(slot);
            inventories[pos[0]].setStackInSlot(pos[1], stack);
        }
        
        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            int[] pos = getInventoryAndSlot(slot);
            return inventories[pos[0]].insertItem(pos[1], stack, simulate);
        }
        
        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            int[] pos = getInventoryAndSlot(slot);
            return inventories[pos[0]].extractItem(pos[1], amount, simulate);
        }
        
        @Override
        public int getSlotLimit(int slot) {
            int[] pos = getInventoryAndSlot(slot);
            return inventories[pos[0]].getSlotLimit(pos[1]);
        }
        
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            int[] pos = getInventoryAndSlot(slot);
            return inventories[pos[0]].isItemValid(pos[1], stack);
        }
        
        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            for (int i = 0; i < inventories.length; i++) {
                tag.put("inventory_" + i, inventories[i].serializeNBT());
            }
            return tag;
        }
        
        @Override
        public void deserializeNBT(CompoundTag nbt) {
            for (int i = 0; i < inventories.length; i++) {
                if (nbt.contains("inventory_" + i)) {
                    inventories[i].deserializeNBT(nbt.getCompound("inventory_" + i));
                }
            }
        }
        
        @Override
        public Object getPlatformInventory() {
            // Return the first inventory's platform object as primary
            return inventories.length > 0 ? inventories[0].getPlatformInventory() : null;
        }
        
        @Override
        public void setChangeCallback(Runnable callback) {
            // Set callback on all constituent inventories
            for (PlatformInventory inventory : inventories) {
                inventory.setChangeCallback(callback);
            }
        }
        
        @Override
        public void setItemValidator(ItemValidator validator) {
            // Set validator on all constituent inventories
            for (PlatformInventory inventory : inventories) {
                inventory.setItemValidator(validator);
            }
        }
        
        @Override
        public PlatformInventory createExtractionOnlyView() {
            PlatformInventory[] extractionOnlyInventories = new PlatformInventory[inventories.length];
            for (int i = 0; i < inventories.length; i++) {
                extractionOnlyInventories[i] = inventories[i].createExtractionOnlyView();
            }
            return new CombinedForgeInventory(extractionOnlyInventories);
        }
    }
}