package com.quackers29.businesscraft.town.components;

/**
 * Platform-agnostic interface for town components.
 * Components handle specific aspects of town functionality (economy, resources, etc.)
 */
public interface TownComponent {
    /**
     * Called periodically to update component state
     */
    void tick();
    
    /**
     * Save component data to a platform-specific data structure
     * @param data Platform-specific data container (CompoundTag for Minecraft)
     */
    void save(Object data);
    
    /**
     * Load component data from a platform-specific data structure
     * @param data Platform-specific data container (CompoundTag for Minecraft)
     */
    void load(Object data);
}