package com.quackers29.businesscraft.platform;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Platform abstraction interface for block entity capability/component operations.
 * This interface provides a common API for attaching and accessing data on block entities across mod loaders.
 * 
 * Abstracts:
 * - Forge: Capability system with LazyOptional<T> and getCapability()
 * - Fabric: Component system with ComponentKey<T> and component attachment API
 * 
 * Key Features Preserved:
 * - Inventory attachment to block entities (ItemStackHandler vs SimpleInventory)
 * - Data persistence and synchronization
 * - Side-specific capability/component access (null = any side)
 * - Proper lifecycle management and invalidation
 * - Type-safe capability/component retrieval
 * - Support for TownInterfaceEntity's complex inventory system
 */
public interface BlockEntityHelper {
    
    /**
     * Attach an inventory capability/component to a block entity.
     * This exposes the inventory to external systems like hoppers, pipes, etc.
     * 
     * @param blockEntity Target block entity
     * @param inventory Platform inventory to attach
     * @param side Optional side restriction (null = all sides)
     * @return Platform-specific attachment handle for later invalidation
     */
    Object attachInventory(BlockEntity blockEntity, InventoryHelper.PlatformInventory inventory, @Nullable Direction side);
    
    /**
     * Retrieve an inventory capability/component from a block entity.
     * 
     * @param blockEntity Source block entity
     * @param side Optional side specification (null = any side)
     * @return Platform inventory if present, null otherwise
     */
    @Nullable
    InventoryHelper.PlatformInventory getInventory(BlockEntity blockEntity, @Nullable Direction side);
    
    /**
     * Attach custom data to a block entity using platform-specific mechanisms.
     * This allows storing arbitrary data that persists with the block entity.
     * 
     * @param blockEntity Target block entity
     * @param key Unique identifier for the data
     * @param data Data to attach
     * @param <T> Data type
     * @return Platform-specific attachment handle for later access/invalidation
     */
    <T> Object attachData(BlockEntity blockEntity, String key, T data);
    
    /**
     * Retrieve custom data from a block entity.
     * 
     * @param blockEntity Source block entity
     * @param key Unique identifier for the data
     * @param dataClass Expected data class
     * @param <T> Data type
     * @return Attached data if present and correct type, null otherwise
     */
    @Nullable
    <T> T getData(BlockEntity blockEntity, String key, Class<T> dataClass);
    
    /**
     * Remove custom data from a block entity.
     * 
     * @param blockEntity Target block entity
     * @param key Unique identifier for the data to remove
     */
    void removeData(BlockEntity blockEntity, String key);
    
    /**
     * Invalidate all capabilities/components attached to a block entity.
     * This should be called when the block entity is removed to prevent memory leaks.
     * 
     * @param blockEntity Block entity being removed
     */
    void invalidateAllAttachments(BlockEntity blockEntity);
    
    /**
     * Check if a block entity has a specific capability/component.
     * 
     * @param blockEntity Source block entity
     * @param capabilityKey Platform-specific capability/component identifier
     * @param side Optional side specification (null = any side)
     * @return True if the capability/component is present
     */
    boolean hasCapability(BlockEntity blockEntity, String capabilityKey, @Nullable Direction side);
    
    /**
     * Create a platform-specific capability/component key for inventory access.
     * This provides a consistent way to identify inventory capabilities across platforms.
     * 
     * @return Platform-specific inventory capability key
     */
    String getInventoryCapabilityKey();
    
    /**
     * Helper class for common block entity operations.
     * Provides utilities for typical BusinessCraft block entity patterns.
     */
    class BlockEntityDataHelper {
        
        /**
         * Standard key for the primary inventory capability.
         * Used by TownInterfaceEntity's main item input slot.
         */
        public static final String PRIMARY_INVENTORY = "primary_inventory";
        
        /**
         * Standard key for buffer inventory capability.
         * Used by TownInterfaceEntity's payment buffer system.
         */
        public static final String BUFFER_INVENTORY = "buffer_inventory";
        
        /**
         * Standard key for town data attachment.
         * Used to associate town UUID with block entities.
         */
        public static final String TOWN_DATA = "town_data";
        
        /**
         * Standard key for platform data attachment.
         * Used to store Platform objects for visualization.
         */
        public static final String PLATFORM_DATA = "platform_data";
        
        /**
         * Attach a primary inventory to a block entity using standard conventions.
         * This is a convenience method for the most common use case.
         * 
         * @param helper BlockEntityHelper instance
         * @param blockEntity Target block entity
         * @param inventory Inventory to attach
         * @return Attachment handle for cleanup
         */
        public static Object attachPrimaryInventory(BlockEntityHelper helper, BlockEntity blockEntity, 
                                                  InventoryHelper.PlatformInventory inventory) {
            return helper.attachInventory(blockEntity, inventory, null);
        }
        
        /**
         * Attach a buffer inventory to a block entity using standard conventions.
         * Buffer inventories are typically extraction-only for hopper automation.
         * 
         * @param helper BlockEntityHelper instance
         * @param blockEntity Target block entity
         * @param bufferInventory Buffer inventory to attach
         * @return Attachment handle for cleanup
         */
        public static Object attachBufferInventory(BlockEntityHelper helper, BlockEntity blockEntity,
                                                 InventoryHelper.PlatformInventory bufferInventory) {
            return helper.attachInventory(blockEntity, bufferInventory, Direction.DOWN);
        }
        
        /**
         * Get the primary inventory from a block entity using standard conventions.
         * 
         * @param helper BlockEntityHelper instance
         * @param blockEntity Source block entity
         * @return Primary inventory if present, null otherwise
         */
        @Nullable
        public static InventoryHelper.PlatformInventory getPrimaryInventory(BlockEntityHelper helper, BlockEntity blockEntity) {
            return helper.getInventory(blockEntity, null);
        }
        
        /**
         * Get the buffer inventory from a block entity using standard conventions.
         * 
         * @param helper BlockEntityHelper instance  
         * @param blockEntity Source block entity
         * @return Buffer inventory if present, null otherwise
         */
        @Nullable
        public static InventoryHelper.PlatformInventory getBufferInventory(BlockEntityHelper helper, BlockEntity blockEntity) {
            return helper.getInventory(blockEntity, Direction.DOWN);
        }
    }
    
    /**
     * Handle visitor history request with server-side name resolution
     * Uses town UUID for direct town lookup instead of coordinates
     */
    void handleVisitorHistoryRequest(Object player, java.util.UUID townId);
}