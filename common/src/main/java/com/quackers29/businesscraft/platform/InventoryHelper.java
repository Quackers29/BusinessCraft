package com.quackers29.businesscraft.platform;

/**
 * Platform-agnostic interface for inventory operations.
 * Enhanced for TownInterfaceEntity migration to support Forge capability system.
 *
 * This interface abstracts complex Forge-specific patterns:
 * - IItemHandler for inventory management
 * - LazyOptional for capability system
 * - Capability querying and management
 */
public interface InventoryHelper {

    /**
     * Check if an ItemStack is valid (not empty).
     * Platform implementations handle ItemStack validation.
     *
     * @param itemStack Platform-specific ItemStack object
     * @return True if the ItemStack is valid (not empty)
     */
    boolean isItemStackValid(Object itemStack);

    /**
     * Create an item handler for inventory management.
     * Used by TownInterfaceEntity for capability system integration.
     *
     * @param size Number of slots in the inventory
     * @return Platform-specific item handler object (IItemHandler equivalent)
     */
    Object createItemHandler(int size);

    /**
     * Create a lazy optional wrapper for capability system.
     * Used by Forge implementations for LazyOptional pattern.
     *
     * @param capability Platform-specific capability object
     * @return Lazy optional wrapper or null if not supported
     */
    Object createLazyOptional(Object capability);

    /**
     * Check if an object has a specific capability.
     * Used for capability system queries in TownInterfaceEntity.
     *
     * @param object Object to check for capability
     * @param capability Platform-specific capability object
     * @return true if object has the capability, false otherwise
     */
    boolean hasCapability(Object object, Object capability);

    /**
     * Get a capability from an object.
     * Used for retrieving capabilities like ITEM_HANDLER from block entities.
     *
     * @param object Object to get capability from
     * @param capability Platform-specific capability object
     * @return Capability instance or null if not available
     */
    Object getCapability(Object object, Object capability);

    /**
     * Get the ITEM_HANDLER capability constant.
     * Platform implementations provide their specific capability objects.
     *
     * @return Platform-specific ITEM_HANDLER capability
     */
    Object getItemHandlerCapability();

    /**
     * Check if lazy optional is present (not empty).
     * Used for null-safe capability checking.
     *
     * @param lazyOptional Platform-specific lazy optional object
     * @return true if lazy optional has a value, false otherwise
     */
    boolean isLazyOptionalPresent(Object lazyOptional);

    /**
     * Get the value from a lazy optional.
     * Used to unwrap LazyOptional values safely.
     *
     * @param lazyOptional Platform-specific lazy optional object
     * @return The wrapped value or null if empty
     */
    Object getLazyOptionalValue(Object lazyOptional);

    /**
     * Invalidate a lazy optional.
     * Used for cleanup when block entities are removed.
     *
     * @param lazyOptional Platform-specific lazy optional object
     */
    void invalidateLazyOptional(Object lazyOptional);

    /**
     * Create an empty lazy optional.
     * Used when capabilities are not available.
     *
     * @return Empty lazy optional instance
     */
    Object createEmptyLazyOptional();
}