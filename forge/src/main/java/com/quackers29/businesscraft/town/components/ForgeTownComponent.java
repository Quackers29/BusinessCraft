package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.town.components.TownComponent;
import net.minecraft.nbt.CompoundTag;

/**
 * Forge-specific adapter for TownComponent that bridges between the platform-agnostic
 * common interface and Minecraft Forge's CompoundTag system.
 */
public interface ForgeTownComponent extends TownComponent {
    
    /**
     * Save component data to CompoundTag (Forge-specific)
     */
    void save(CompoundTag tag);
    
    /**
     * Load component data from CompoundTag (Forge-specific)
     */
    void load(CompoundTag tag);
    
    /**
     * Bridge method to satisfy common TownComponent interface
     */
    @Override
    default void save(Object data) {
        if (data instanceof CompoundTag) {
            save((CompoundTag) data);
        } else {
            throw new IllegalArgumentException("Expected CompoundTag for Forge platform, got: " + 
                (data != null ? data.getClass().getSimpleName() : "null"));
        }
    }
    
    /**
     * Bridge method to satisfy common TownComponent interface
     */
    @Override
    default void load(Object data) {
        if (data instanceof CompoundTag) {
            load((CompoundTag) data);
        } else {
            throw new IllegalArgumentException("Expected CompoundTag for Forge platform, got: " + 
                (data != null ? data.getClass().getSimpleName() : "null"));
        }
    }
}