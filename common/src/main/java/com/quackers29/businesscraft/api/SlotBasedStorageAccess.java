package com.quackers29.businesscraft.api;

import net.minecraft.world.item.ItemStack;

public interface SlotBasedStorageAccess {
    int getSlotCount();

    ItemStack getSlot(int index);

    void setSlot(int index, ItemStack stack);

    void onContentsChanged(int slot);
}
