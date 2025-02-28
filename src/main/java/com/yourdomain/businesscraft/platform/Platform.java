package com.yourdomain.businesscraft.platform;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

/**
 * Represents a tourist platform in a town
 */
public class Platform {
    private String name;
    private boolean enabled;
    private BlockPos startPos;
    private BlockPos endPos;
    private UUID id;
    
    /**
     * Creates a new platform with default values
     */
    public Platform() {
        this.name = "New Platform";
        this.enabled = true;
        this.id = UUID.randomUUID();
    }
    
    /**
     * Creates a new platform with specified values
     */
    public Platform(String name, boolean enabled, BlockPos startPos, BlockPos endPos) {
        this.name = name;
        this.enabled = enabled;
        this.startPos = startPos;
        this.endPos = endPos;
        this.id = UUID.randomUUID();
    }
    
    /**
     * Creates a platform from NBT data
     */
    public static Platform fromNBT(CompoundTag tag) {
        Platform platform = new Platform();
        
        if (tag.contains("name")) {
            platform.name = tag.getString("name");
        }
        
        platform.enabled = tag.getBoolean("enabled");
        
        if (tag.contains("id")) {
            platform.id = tag.getUUID("id");
        }
        
        if (tag.contains("startPos")) {
            CompoundTag startPosTag = tag.getCompound("startPos");
            int x = startPosTag.getInt("x");
            int y = startPosTag.getInt("y");
            int z = startPosTag.getInt("z");
            platform.startPos = new BlockPos(x, y, z);
        }
        
        if (tag.contains("endPos")) {
            CompoundTag endPosTag = tag.getCompound("endPos");
            int x = endPosTag.getInt("x");
            int y = endPosTag.getInt("y");
            int z = endPosTag.getInt("z");
            platform.endPos = new BlockPos(x, y, z);
        }
        
        return platform;
    }
    
    /**
     * Saves platform data to NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        
        tag.putString("name", name);
        tag.putBoolean("enabled", enabled);
        tag.putUUID("id", id);
        
        if (startPos != null) {
            CompoundTag startPosTag = new CompoundTag();
            startPosTag.putInt("x", startPos.getX());
            startPosTag.putInt("y", startPos.getY());
            startPosTag.putInt("z", startPos.getZ());
            tag.put("startPos", startPosTag);
        }
        
        if (endPos != null) {
            CompoundTag endPosTag = new CompoundTag();
            endPosTag.putInt("x", endPos.getX());
            endPosTag.putInt("y", endPos.getY());
            endPosTag.putInt("z", endPos.getZ());
            tag.put("endPos", endPosTag);
        }
        
        return tag;
    }
    
    /**
     * Checks if this platform has both start and end positions defined
     */
    public boolean isComplete() {
        return startPos != null && endPos != null;
    }
    
    // Getters and Setters
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public BlockPos getStartPos() {
        return startPos;
    }
    
    public void setStartPos(BlockPos startPos) {
        this.startPos = startPos;
    }
    
    public BlockPos getEndPos() {
        return endPos;
    }
    
    public void setEndPos(BlockPos endPos) {
        this.endPos = endPos;
    }
    
    public UUID getId() {
        return id;
    }
} 