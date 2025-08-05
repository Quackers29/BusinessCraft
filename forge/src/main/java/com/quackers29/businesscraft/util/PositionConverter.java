package com.quackers29.businesscraft.util;

import com.quackers29.businesscraft.api.ITownDataProvider;
import net.minecraft.core.BlockPos;

/**
 * Utility class for converting between platform-specific and platform-agnostic position types.
 */
public class PositionConverter {
    
    /**
     * Convert a BlockPos to a platform-agnostic Position
     */
    public static ITownDataProvider.Position toPosition(BlockPos pos) {
        if (pos == null) return null;
        
        return new ITownDataProvider.Position() {
            @Override
            public int getX() { return pos.getX(); }
            
            @Override
            public int getY() { return pos.getY(); }
            
            @Override
            public int getZ() { return pos.getZ(); }
        };
    }
    
    /**
     * Convert a platform-agnostic Position to a BlockPos
     */
    public static BlockPos toBlockPos(ITownDataProvider.Position pos) {
        if (pos == null) return null;
        return new BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }
}