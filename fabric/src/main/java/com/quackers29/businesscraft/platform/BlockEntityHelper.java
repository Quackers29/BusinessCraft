package com.quackers29.businesscraft.platform;

import net.minecraft.block.entity.BlockEntity;

/**
 * Platform-agnostic block entity helper interface using Yarn mappings.
 * Provides cross-platform block entity operations for the Enhanced MultiLoader approach.
 */
public interface BlockEntityHelper {
    
    /**
     * Attach a primary inventory to a block entity
     * @param blockEntity The block entity
     * @param slotCount Number of inventory slots
     * @return Platform-specific inventory attachment
     */
    Object attachPrimaryInventory(BlockEntity blockEntity, int slotCount);
    
    /**
     * Attach a buffer inventory to a block entity
     * @param blockEntity The block entity
     * @param slotCount Number of inventory slots
     * @return Platform-specific inventory attachment
     */
    Object attachBufferInventory(BlockEntity blockEntity, int slotCount);
    
    /**
     * Get the primary inventory from a block entity
     * @param blockEntity The block entity
     * @return Platform inventory interface
     */
    InventoryHelper.PlatformInventory getPrimaryInventory(BlockEntity blockEntity);
    
    /**
     * Get the buffer inventory from a block entity
     * @param blockEntity The block entity
     * @return Platform inventory interface
     */
    InventoryHelper.PlatformInventory getBufferInventory(BlockEntity blockEntity);
    
    /**
     * Helper class for block entity data operations
     */
    class BlockEntityDataHelper {
        
        /**
         * Attach a primary inventory to a block entity
         * @param helper The block entity helper
         * @param blockEntity The block entity
         * @param slotCount Number of slots
         * @return Inventory attachment
         */
        public static Object attachPrimaryInventory(BlockEntityHelper helper, BlockEntity blockEntity, 
                                                   int slotCount) {
            return helper.attachPrimaryInventory(blockEntity, slotCount);
        }
        
        /**
         * Attach a buffer inventory to a block entity
         * @param helper The block entity helper
         * @param blockEntity The block entity
         * @param slotCount Number of slots
         * @return Inventory attachment
         */
        public static Object attachBufferInventory(BlockEntityHelper helper, BlockEntity blockEntity,
                                                  int slotCount) {
            return helper.attachBufferInventory(blockEntity, slotCount);
        }
        
        /**
         * Get the primary inventory from a block entity
         * @param helper The block entity helper
         * @param blockEntity The block entity
         * @return Platform inventory
         */
        public static InventoryHelper.PlatformInventory getPrimaryInventory(BlockEntityHelper helper, BlockEntity blockEntity) {
            return helper.getPrimaryInventory(blockEntity);
        }
        
        /**
         * Get the buffer inventory from a block entity
         * @param helper The block entity helper
         * @param blockEntity The block entity
         * @return Platform inventory
         */
        public static InventoryHelper.PlatformInventory getBufferInventory(BlockEntityHelper helper, BlockEntity blockEntity) {
            return helper.getBufferInventory(blockEntity);
        }
    }
}