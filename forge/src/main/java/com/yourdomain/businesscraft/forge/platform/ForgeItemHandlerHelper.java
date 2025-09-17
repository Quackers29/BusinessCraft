package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraft.world.item.ItemStack;

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
    public void setStackInSlot(Object itemHandler, int slot, ItemStack stack) {
        if (itemHandler instanceof ItemStackHandler handler) {
            handler.setStackInSlot(slot, stack);
        } else {
            throw new IllegalArgumentException("Invalid item handler type");
        }
    }

    @Override
    public ItemStack getStackInSlot(Object itemHandler, int slot) {
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
}
