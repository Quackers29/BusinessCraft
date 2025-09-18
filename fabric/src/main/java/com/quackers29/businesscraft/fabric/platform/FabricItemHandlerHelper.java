package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric implementation of ItemHandlerHelper using direct Fabric Transfer API
 */
public class FabricItemHandlerHelper implements ItemHandlerHelper {

    @Override
    public Object createItemStackHandler(int size) {
        // Use SimpleContainer for basic item storage in Fabric
        return new SimpleContainer(size);
    }

    @Override
    public int getSlots(Object itemHandler) {
        if (itemHandler instanceof Container container) {
            return container.getContainerSize();
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public void setStackInSlot(Object itemHandler, int slot, Object stack) {
        if (itemHandler instanceof Container container &&
            stack instanceof ItemStack itemStack) {
            // Direct Fabric container operation
            container.setItem(slot, itemStack);
        } else {
            throw new IllegalArgumentException("Invalid item handler or stack type");
        }
    }

    @Override
    public Object getStackInSlot(Object itemHandler, int slot) {
        if (itemHandler instanceof Container container) {
            // Direct Fabric container operation
            return container.getItem(slot);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createSlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof Container container) {
            // Direct Fabric slot creation
            return new Slot(container, index, x, y);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof Container container) {
            // Direct Fabric withdrawal-only slot creation
            return new WithdrawalOnlySlot(container, index, x, y);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createLazyOptional(Object itemHandler) {
        // Fabric doesn't use LazyOptional, return the handler directly
        return itemHandler;
    }

    @Override
    public boolean isItemHandlerCapability(Object capability) {
        // Fabric uses Storage interface for item capabilities
        return capability instanceof Storage;
    }

    @Override
    public Object getEmptyLazyOptional() {
        // Return null for empty in Fabric (no LazyOptional equivalent)
        return null;
    }

    @Override
    public Object castLazyOptional(Object lazyOptional, Object capability) {
        // Return the capability directly in Fabric
        return capability;
    }

    @Override
    public Object createCustomItemStackHandler(int size, Runnable onContentsChanged) {
        // Create SimpleContainer with callback
        return new SimpleContainer(size) {
            @Override
            public void setChanged() {
                super.setChanged();
                if (onContentsChanged != null) {
                    onContentsChanged.run();
                }
            }
        };
    }

    @Override
    public void invalidateLazyOptional(Object lazyOptional) {
        // No-op in Fabric (no LazyOptional equivalent)
    }

    @Override
    public Object serializeNBT(Object itemHandler) {
        if (itemHandler instanceof Container container) {
            // Direct Fabric NBT serialization
            CompoundTag tag = new CompoundTag();
            ListTag listTag = new ListTag();

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty()) {
                    CompoundTag itemTag = new CompoundTag();
                    itemTag.putInt("Slot", i);
                    stack.save(itemTag);
                    listTag.add(itemTag);
                }
            }

            tag.put("Items", listTag);
            tag.putInt("Size", container.getContainerSize());
            return tag;
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public void deserializeNBT(Object itemHandler, Object nbt) {
        if (itemHandler instanceof Container container && nbt instanceof CompoundTag tag) {
            // Direct Fabric NBT deserialization
            ListTag listTag = tag.getList("Items", Tag.TAG_COMPOUND);

            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag itemTag = listTag.getCompound(i);
                int slot = itemTag.getInt("Slot");
                ItemStack stack = ItemStack.of(itemTag);
                container.setItem(slot, stack);
            }
        } else {
            throw new IllegalArgumentException("Invalid item handler or NBT type");
        }
    }

    @Override
    public Object extractItem(Object itemHandler, int slot, int amount, boolean simulate) {
        if (itemHandler instanceof Container container) {
            // Direct Fabric item extraction
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int toExtract = Math.min(amount, stack.getCount());
            ItemStack extracted = stack.copy();
            extracted.setCount(toExtract);

            if (!simulate) {
                stack.shrink(toExtract);
                if (stack.isEmpty()) {
                    container.setItem(slot, ItemStack.EMPTY);
                }
                container.setChanged();
            }

            return extracted;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public Object insertItem(Object itemHandler, int slot, Object stack, boolean simulate) {
        if (itemHandler instanceof Container container &&
            stack instanceof ItemStack itemStack) {
            // Direct Fabric item insertion
            ItemStack existingStack = container.getItem(slot);

            if (existingStack.isEmpty()) {
                if (!simulate) {
                    container.setItem(slot, itemStack.copy());
                    container.setChanged();
                }
                return ItemStack.EMPTY;
            } else if (ItemStack.isSameItemSameTags(existingStack, itemStack)) {
                int space = existingStack.getMaxStackSize() - existingStack.getCount();
                int toInsert = Math.min(space, itemStack.getCount());

                if (!simulate) {
                    existingStack.grow(toInsert);
                    container.setChanged();
                }

                if (toInsert < itemStack.getCount()) {
                    ItemStack remaining = itemStack.copy();
                    remaining.shrink(toInsert);
                    return remaining;
                }
                return ItemStack.EMPTY;
            }
        }
        return stack;
    }

    /**
     * Custom slot that only allows withdrawal, not placement (for buffer storage)
     */
    private static class WithdrawalOnlySlot extends Slot {
        public WithdrawalOnlySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // Users cannot place items in buffer slots
        }
    }
}
