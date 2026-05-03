package com.quackers29.businesscraft.api;

public interface ItemHandlerHelper {
    Object createItemStackHandler(int size);

    int getSlots(Object itemHandler);

    void setStackInSlot(Object itemHandler, int slot, Object stack);

    Object getStackInSlot(Object itemHandler, int slot);

    Object createSlot(Object itemHandler, int index, int x, int y);

    Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y);

    Object createLazyOptional(Object itemHandler);

    boolean isItemHandlerCapability(Object capability);

    Object getEmptyLazyOptional();

    Object castLazyOptional(Object lazyOptional, Object capability);

    Object createCustomItemStackHandler(int size, Runnable onContentsChanged);

    void invalidateLazyOptional(Object lazyOptional);

    Object serializeNBT(Object itemHandler);

    void deserializeNBT(Object itemHandler, Object nbt);

    Object extractItem(Object itemHandler, int slot, int amount, boolean simulate);

    Object insertItem(Object itemHandler, int slot, Object stack, boolean simulate);

    Object createStorageWrapper(SlotBasedStorageAccess storage);
}
