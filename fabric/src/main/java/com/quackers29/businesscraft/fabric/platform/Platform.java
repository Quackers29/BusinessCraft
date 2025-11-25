package com.quackers29.businesscraft.fabric.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Fabric implementation of Platform with full NBT support matching common
 */
public class Platform {
    private UUID id;
    private String name;
    private boolean enabled;
    private BlockPos startPos;
    private BlockPos endPos;
    private Set<UUID> enabledDestinations = new HashSet<>();

    public Platform() {
        this.id = UUID.randomUUID();
        this.name = "New Platform";
        this.enabled = true;
    }

    public Platform(UUID id) {
        this.id = id;
        this.name = "New Platform";
        this.enabled = true;
    }

    public Platform(String name, boolean enabled, BlockPos startPos, BlockPos endPos) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.enabled = enabled;
        this.startPos = startPos;
        this.endPos = endPos;
    }

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

    public static Platform fromNBT(CompoundTag tag) {
        return new Platform(tag);
    }

    public UUID getId() { return id; }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    public boolean isEnabled() { return enabled; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public BlockPos getStartPos() { return startPos; }

    public void setStartPos(BlockPos startPos) { this.startPos = startPos; }

    public BlockPos getEndPos() { return endPos; }

    public void setEndPos(BlockPos endPos) { this.endPos = endPos; }

    public Set<UUID> getEnabledDestinations() { return new HashSet<>(enabledDestinations); }

    public boolean isComplete() { return startPos != null && endPos != null; }

    public boolean hasNoEnabledDestinations() { return enabledDestinations.isEmpty(); }
}
