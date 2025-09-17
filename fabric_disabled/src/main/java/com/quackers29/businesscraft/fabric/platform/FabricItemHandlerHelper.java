package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ItemHandlerHelper;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.minecraft.world.item.ItemStack;

/**
 * Fabric implementation of ItemHandlerHelper using Fabric Transfer API
 */
public class FabricItemHandlerHelper implements ItemHandlerHelper {

    @Override
    public Object createItemStackHandler(int size) {
        // For Fabric, we'll create a simple inventory storage
        // This is a simplified implementation - may need enhancement
        return new FabricSimpleInventory(size);
    }

    @Override
    public int getSlots(Object itemHandler) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return inventory.getSlots();
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public void setStackInSlot(Object itemHandler, int slot, ItemStack stack) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            inventory.setStackInSlot(slot, stack);
        } else {
            throw new IllegalArgumentException("Invalid item handler type");
        }
    }

    @Override
    public ItemStack getStackInSlot(Object itemHandler, int slot) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return inventory.getStackInSlot(slot);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createSlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return new FabricSlot(inventory, index, x, y);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return new FabricWithdrawalOnlySlot(inventory, index, x, y);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public Object createLazyOptional(Object itemHandler) {
        // Fabric doesn't have LazyOptional, but we can return the handler directly
        return itemHandler;
    }

    @Override
    public boolean isItemHandlerCapability(Object capability) {
        // Fabric uses different capability system
        return capability instanceof Storage;
    }

    @Override
    public Object getEmptyLazyOptional() {
        // Return null for empty in Fabric
        return null;
    }

    @Override
    public Object castLazyOptional(Object lazyOptional, Object capability) {
        // Return the capability directly in Fabric
        return capability;
    }

    @Override
    public Object createCustomItemStackHandler(int size, Runnable onContentsChanged) {
        return new FabricSimpleInventory(size, onContentsChanged);
    }

    @Override
    public void invalidateLazyOptional(Object lazyOptional) {
        // No-op in Fabric
    }

    @Override
    public Object serializeNBT(Object itemHandler) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return inventory.serializeNBT();
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public void deserializeNBT(Object itemHandler, Object nbt) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            inventory.deserializeNBT(nbt);
        } else {
            throw new IllegalArgumentException("Invalid item handler type");
        }
    }

    @Override
    public ItemStack extractItem(Object itemHandler, int slot, int amount, boolean simulate) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return inventory.extractItem(slot, amount, simulate);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    @Override
    public ItemStack insertItem(Object itemHandler, int slot, ItemStack stack, boolean simulate) {
        if (itemHandler instanceof FabricSimpleInventory inventory) {
            return inventory.insertItem(slot, stack, simulate);
        }
        throw new IllegalArgumentException("Invalid item handler type");
    }

    // Inner classes for Fabric-specific implementations
    private static class FabricSimpleInventory {
        private final ItemStack[] stacks;
        private final Runnable onContentsChanged;

        public FabricSimpleInventory(int size) {
            this(size, null);
        }

        public FabricSimpleInventory(int size, Runnable onContentsChanged) {
            this.stacks = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                this.stacks[i] = ItemStack.EMPTY;
            }
            this.onContentsChanged = onContentsChanged;
        }

        public int getSlots() {
            return stacks.length;
        }

        public void setStackInSlot(int slot, ItemStack stack) {
            stacks[slot] = stack;
            if (onContentsChanged != null) {
                onContentsChanged.run();
            }
        }

        public ItemStack getStackInSlot(int slot) {
            return stacks[slot];
        }

        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack stack = stacks[slot];
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            int toExtract = Math.min(amount, stack.getCount());
            ItemStack result = stack.copy();
            result.setCount(toExtract);

            if (!simulate) {
                stack.shrink(toExtract);
                if (onContentsChanged != null) {
                    onContentsChanged.run();
                }
            }

            return result;
        }

        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack existing = stacks[slot];
            if (!existing.isEmpty() && !ItemStack.isSameItemSameTags(existing, stack)) {
                return stack; // Cannot insert into occupied slot with different item
            }

            int maxStackSize = existing.getMaxStackSize();
            int canInsert = maxStackSize - existing.getCount();
            int toInsert = Math.min(canInsert, stack.getCount());

            if (toInsert <= 0) {
                return stack; // No space
            }

            if (!simulate) {
                if (existing.isEmpty()) {
                    stacks[slot] = stack.copy();
                    stacks[slot].setCount(toInsert);
                } else {
                    existing.grow(toInsert);
                }
                if (onContentsChanged != null) {
                    onContentsChanged.run();
                }
            }

            ItemStack remainder = stack.copy();
            remainder.shrink(toInsert);
            return remainder.isEmpty() ? ItemStack.EMPTY : remainder;
        }

        public Object serializeNBT() {
            // TODO: Implement NBT serialization
            return null;
        }

        public void deserializeNBT(Object nbt) {
            // TODO: Implement NBT deserialization
        }
    }

    private static class FabricSlot {
        private final FabricSimpleInventory inventory;
        private final int index;
        private final int x;
        private final int y;

        public FabricSlot(FabricSimpleInventory inventory, int index, int x, int y) {
            this.inventory = inventory;
            this.index = index;
            this.x = x;
            this.y = y;
        }

        // TODO: Implement slot methods for Fabric
    }

    private static class FabricWithdrawalOnlySlot extends FabricSlot {
        public FabricWithdrawalOnlySlot(FabricSimpleInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        // TODO: Implement withdrawal-only slot methods for Fabric
    }
}
