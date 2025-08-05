package com.quackers29.businesscraft.platform;

import com.quackers29.businesscraft.api.ITownDataProvider;
import net.minecraft.core.BlockPos;

/**
 * Forge-specific implementation of the Position interface.
 * Wraps Minecraft's BlockPos for use with the common API.
 */
public class ForgePosition implements ITownDataProvider.Position {
    private final BlockPos blockPos;
    
    public ForgePosition(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
    
    public ForgePosition(int x, int y, int z) {
        this.blockPos = new BlockPos(x, y, z);
    }
    
    @Override
    public int getX() {
        return blockPos.getX();
    }
    
    @Override
    public int getY() {
        return blockPos.getY();
    }
    
    @Override
    public int getZ() {
        return blockPos.getZ();
    }
    
    /**
     * Get the underlying BlockPos for Forge-specific operations
     */
    public BlockPos getBlockPos() {
        return blockPos;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ForgePosition that = (ForgePosition) obj;
        return blockPos.equals(that.blockPos);
    }
    
    @Override
    public int hashCode() {
        return blockPos.hashCode();
    }
    
    @Override
    public String toString() {
        return "ForgePosition{" + blockPos + "}";
    }
}