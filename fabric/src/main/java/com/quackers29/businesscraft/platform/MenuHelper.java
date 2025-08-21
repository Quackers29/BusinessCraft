package com.quackers29.businesscraft.platform;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

/**
 * Platform-agnostic menu helper interface using Yarn mappings.
 * Provides cross-platform menu operations for the Enhanced MultiLoader approach.
 */
public interface MenuHelper {
    
    /**
     * Create an extended screen handler factory for complex data
     * @param factory The screen handler factory
     * @return Platform-specific extended factory
     */
    Object createExtendedScreenHandlerFactory(Object factory);
    
    /**
     * Helper class for menu data operations using Yarn mappings
     */
    class MenuDataHelper {
        
        /**
         * Write a block position to packet buffer
         * @param buffer The packet buffer
         * @param pos The block position
         */
        public static void writeBlockPos(PacketByteBuf buffer, BlockPos pos) {
            buffer.writeBlockPos(pos);
        }
        
        /**
         * Read a block position from packet buffer
         * @param buffer The packet buffer
         * @return The block position
         */
        public static BlockPos readBlockPos(PacketByteBuf buffer) {
            return buffer.readBlockPos();
        }
    }
}