package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ItemHandlerHelper;
import com.quackers29.businesscraft.api.SlotBasedStorageAccess;

/**
 * Fabric implementation of ItemHandlerHelper
 * Placeholder implementation - will be expanded when Fabric support is fully implemented
 */
public class FabricItemHandlerHelper implements ItemHandlerHelper {
    @Override
    public Object createItemStackHandler(int size) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public int getSlots(Object itemHandler) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public void setStackInSlot(Object itemHandler, int slot, Object stack) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object getStackInSlot(Object itemHandler, int slot) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object createSlot(Object itemHandler, int index, int x, int y) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object createWithdrawalOnlySlot(Object itemHandler, int index, int x, int y) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object createLazyOptional(Object itemHandler) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public boolean isItemHandlerCapability(Object capability) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object getEmptyLazyOptional() {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object castLazyOptional(Object lazyOptional, Object capability) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object createCustomItemStackHandler(int size, Runnable onContentsChanged) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public void invalidateLazyOptional(Object lazyOptional) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object serializeNBT(Object itemHandler) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public void deserializeNBT(Object itemHandler, Object nbt) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object extractItem(Object itemHandler, int slot, int amount, boolean simulate) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }

    @Override
    public Object insertItem(Object itemHandler, int slot, Object stack, boolean simulate) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }
    
    @Override
    public Object createStorageWrapper(SlotBasedStorageAccess storage) {
        throw new UnsupportedOperationException("Fabric item handler implementation not yet complete");
    }
}
