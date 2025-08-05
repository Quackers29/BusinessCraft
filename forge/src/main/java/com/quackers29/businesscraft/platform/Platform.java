package com.quackers29.businesscraft.platform;

import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a tourist platform in BusinessCraft
 */
public class Platform {
    private UUID id;
    private String name;
    private boolean enabled;
    private BlockPos startPos;
    private BlockPos endPos;
    private Set<UUID> enabledDestinations = new HashSet<>();
    
    /**
     * Create a new platform with a random ID
     */
    public Platform() {
        this.id = UUID.randomUUID();
        this.name = "New Platform";
        this.enabled = true;
    }
    
    /**
     * Create a new platform with the given ID
     */
    public Platform(UUID id) {
        this.id = id;
        this.name = "New Platform";
        this.enabled = true;
    }
    
    /**
     * Create a new platform with the given name, enabled state, and path
     */
    public Platform(String name, boolean enabled, BlockPos startPos, BlockPos endPos) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.enabled = enabled;
        this.startPos = startPos;
        this.endPos = endPos;
    }
    
    /**
     * Create a platform from NBT data
     */
    public Platform(CompoundTag tag) {
        this.id = tag.getUUID("Id");
        this.name = tag.getString("Name");
        this.enabled = tag.getBoolean("Enabled");
        
        if (tag.contains("StartX")) {
            this.startPos = new BlockPos(
                tag.getInt("StartX"),
                tag.getInt("StartY"),
                tag.getInt("StartZ")
            );
        }
        
        if (tag.contains("EndX")) {
            this.endPos = new BlockPos(
                tag.getInt("EndX"),
                tag.getInt("EndY"),
                tag.getInt("EndZ")
            );
        }
        
        // Load enabled destinations
        if (tag.contains("Destinations")) {
            CompoundTag destTag = tag.getCompound("Destinations");
            int count = destTag.getInt("Count");
            for (int i = 0; i < count; i++) {
                if (destTag.contains("Dest" + i)) {
                    enabledDestinations.add(destTag.getUUID("Dest" + i));
                }
            }
        }
    }
    
    /**
     * Save this platform to NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putString("Name", name);
        tag.putBoolean("Enabled", enabled);
        
        if (startPos != null) {
            tag.putInt("StartX", startPos.getX());
            tag.putInt("StartY", startPos.getY());
            tag.putInt("StartZ", startPos.getZ());
        }
        
        if (endPos != null) {
            tag.putInt("EndX", endPos.getX());
            tag.putInt("EndY", endPos.getY());
            tag.putInt("EndZ", endPos.getZ());
        }
        
        // Save enabled destinations
        CompoundTag destTag = new CompoundTag();
        destTag.putInt("Count", enabledDestinations.size());
        int i = 0;
        for (UUID destId : enabledDestinations) {
            destTag.putUUID("Dest" + i, destId);
            i++;
        }
        tag.put("Destinations", destTag);
        
        return tag;
    }
    
    /**
     * Get the unique ID of this platform
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Get the name of this platform
     */
    public String getName() {
        return name;
    }
    
    /**
     * Set the name of this platform
     */
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * Check if this platform is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Set whether this platform is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * Get the start position of this platform
     */
    public BlockPos getStartPos() {
        return startPos;
    }
    
    /**
     * Set the start position of this platform
     */
    public void setStartPos(BlockPos startPos) {
        this.startPos = startPos;
    }
    
    /**
     * Get the end position of this platform
     */
    public BlockPos getEndPos() {
        return endPos;
    }
    
    /**
     * Set the end position of this platform
     */
    public void setEndPos(BlockPos endPos) {
        this.endPos = endPos;
    }
    
    /**
     * Check if this platform is complete (has both start and end positions)
     */
    public boolean isComplete() {
        return startPos != null && endPos != null;
    }
    
    /**
     * Check if this platform has no enabled destinations (accepts any)
     */
    public boolean hasNoEnabledDestinations() {
        return enabledDestinations.isEmpty();
    }
    
    /**
     * Enable a destination for this platform
     */
    public void enableDestination(UUID destinationId) {
        enabledDestinations.add(destinationId);
    }
    
    /**
     * Disable a destination for this platform
     */
    public void disableDestination(UUID destinationId) {
        enabledDestinations.remove(destinationId);
    }
    
    /**
     * Check if a destination is enabled for this platform
     */
    public boolean isDestinationEnabled(UUID destinationId) {
        return enabledDestinations.contains(destinationId);
    }
    
    /**
     * Get all enabled destinations
     */
    public Set<UUID> getEnabledDestinations() {
        return new HashSet<>(enabledDestinations);
    }
    
    /**
     * Clear all enabled destinations
     */
    public void clearEnabledDestinations() {
        enabledDestinations.clear();
    }
    
    /**
     * Get destinations as a map (for compatibility with existing code)
     * @return Map of destination IDs to enabled status
     */
    public Map<UUID, Boolean> getDestinations() {
        Map<UUID, Boolean> result = new HashMap<>();
        for (UUID dest : enabledDestinations) {
            result.put(dest, true);
        }
        return result;
    }
    
    /**
     * Set destination enabled status
     * @param destinationId The destination ID
     * @param enabled Whether the destination is enabled
     */
    public void setDestinationEnabled(UUID destinationId, boolean enabled) {
        if (enabled) {
            enabledDestinations.add(destinationId);
        } else {
            enabledDestinations.remove(destinationId);
        }
    }
    
    /**
     * Convert this platform to an NBT tag
     * @return The NBT tag
     */
    public CompoundTag toNBT() {
        return save();
    }
    
    /**
     * Create a platform from an NBT tag
     * @param tag The NBT tag
     * @return The platform
     */
    public static Platform fromNBT(CompoundTag tag) {
        return new Platform(tag);
    }
} 