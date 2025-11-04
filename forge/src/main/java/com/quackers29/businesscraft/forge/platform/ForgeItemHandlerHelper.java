package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.ItemHandlerHelper;
import com.quackers29.businesscraft.api.SlotBasedStorageAccess;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

/**
 * Forge implementation of ItemHandlerHelper
 */
public class ForgeItemHandlerHelper implements ItemHandlerHelper {
    @Override
    public Object createItemStackHandler(int size) {
        return new ItemStackHandler(size);
    }

    @Override
    public int getSlots(Object itemHandler) {
        if (itemHandler instanceof ItemStackHandler handler) {
            return handler.getSlots();
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public void setStackInSlot(Object itemHandler, int slot, Object stack) {
        if (itemHandler instanceof ItemStackHandler handler &&
            stack instanceof net.minecraft.world.item.ItemStack itemStack) {
            handler.setStackInSlot(slot, itemStack);
        } else {
            throw new IllegalArgumentException("Invalid item handler or stack type");
        }
    }

    @Override
    public Object getStackInSlot(Object itemHandler, int slot) {
        if (itemHandler instanceof ItemStackHandler handler) {
            return handler.getStackInSlot(slot);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createSlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof ItemStackHandler handler) {
            return new SlotItemHandler(handler, index, x, y);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof ItemStackHandler handler) {
            return new WithdrawalOnlySlotItemHandler(handler, index, x, y);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createLazyOptional(Object itemHandler) {
        if (itemHandler instanceof IItemHandler handler) {
            return LazyOptional.of(() -> handler);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public boolean isItemHandlerCapability(Object capability) {
        return capability == ForgeCapabilities.ITEM_HANDLER;
    }

    @Override
    public Object getEmptyLazyOptional() {
        return LazyOptional.empty();
    }

    @Override
    public Object castLazyOptional(Object lazyOptional, Object capability) {
        if (lazyOptional instanceof LazyOptional<?> lazy && capability instanceof Capability<?> cap) {
            return lazy.cast();
        }
        return lazyOptional;
    }

    @Override
    public Object createCustomItemStackHandler(int size, Runnable onContentsChanged) {
        return new ItemStackHandler(size) {
            @Override
            protected void onContentsChanged(int slot) {
                if (onContentsChanged != null) {
                    onContentsChanged.run();
                }
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                // Buffer is extraction-only for hoppers, no insertion allowed
                return false;
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                // Allow extraction for hopper automation
                ItemStack extracted = super.extractItem(slot, amount, simulate);

                // If not simulating and we actually extracted something, trigger callback
                if (!simulate && !extracted.isEmpty() && onContentsChanged != null) {
                    onContentsChanged.run();
                }

                return extracted;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Block insertion - buffer is managed internally
                return stack;
            }
        };
    }

    @Override
    public void invalidateLazyOptional(Object lazyOptional) {
        if (lazyOptional instanceof LazyOptional<?> lazy) {
            lazy.invalidate();
        }
    }

    @Override
    public Object serializeNBT(Object itemHandler) {
        if (itemHandler instanceof ItemStackHandler handler) {
            return handler.serializeNBT();
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public void deserializeNBT(Object itemHandler, Object nbt) {
        if (itemHandler instanceof ItemStackHandler handler && nbt instanceof CompoundTag tag) {
            handler.deserializeNBT(tag);
        } else {
            throw new IllegalArgumentException("Invalid item handler or NBT type");
        }
    }

    @Override
    public Object extractItem(Object itemHandler, int slot, int amount, boolean simulate) {
        if (itemHandler instanceof net.minecraftforge.items.IItemHandler handler) {
            return handler.extractItem(slot, amount, simulate);
        }
        return net.minecraft.world.item.ItemStack.EMPTY;
    }

    @Override
    public Object insertItem(Object itemHandler, int slot, Object stack, boolean simulate) {
        if (itemHandler instanceof net.minecraftforge.items.IItemHandler handler &&
            stack instanceof net.minecraft.world.item.ItemStack itemStack) {
            return handler.insertItem(slot, itemStack, simulate);
        }
        return stack;
    }
    
    @Override
    public Object createStorageWrapper(SlotBasedStorageAccess storage) {
        return new SlotBasedItemStackHandler(storage);
    }

    /**
     * Custom slot that only allows withdrawal, not placement (for buffer storage)
     */
    private static class WithdrawalOnlySlotItemHandler extends SlotItemHandler {
        public WithdrawalOnlySlotItemHandler(ItemStackHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // Users cannot place items in buffer slots
        }
    }
    
    /**
     * ItemStackHandler wrapper for SlotBasedStorage UI integration
     * This allows existing UI code to work with SlotBasedStorage without changes
     */
    private static class SlotBasedItemStackHandler extends ItemStackHandler {
        private final SlotBasedStorageAccess storage;
        
        public SlotBasedItemStackHandler(SlotBasedStorageAccess storage) {
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
        
        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            storage.onContentsChanged(slot);
        }
    }
}
