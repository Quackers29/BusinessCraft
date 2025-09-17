package com.yourdomain.businesscraft.api;

/**
 * Platform-agnostic interface for item handler operations.
 * Implementations will provide access to platform-specific item handler classes.
 */
public interface ItemHandlerHelper {
    /**
     * Create a new ItemStackHandler with the given size
     */
    Object createItemStackHandler(int size);
}
