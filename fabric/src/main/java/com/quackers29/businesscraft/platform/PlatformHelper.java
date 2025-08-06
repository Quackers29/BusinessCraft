package com.quackers29.businesscraft.platform;

import net.minecraft.item.ItemStack;

/**
 * Platform helper interface for Fabric-specific operations using Yarn mappings.
 * This interface provides access to platform-specific functionality.
 */
public interface PlatformHelper {
    
    /**
     * Get the name of the current mod platform (e.g., "Fabric", "Forge")
     * @return Platform name
     */
    String getPlatformName();
    
    /**
     * Check if this is a development environment
     * @return true if in development environment
     */
    boolean isDevelopmentEnvironment();
    
    /**
     * Check if this is the physical client
     * @return true if on physical client
     */
    boolean isPhysicalClient();
    
    /**
     * Get the mod version
     * @return Mod version string
     */
    String getModVersion();
    
    /**
     * Get the mod ID
     * @return Mod ID string
     */
    String getModId();
    
    /**
     * Check if a mod is loaded
     * @param modId The mod ID to check
     * @return true if mod is loaded
     */
    boolean isModLoaded(String modId);
    
    /**
     * Get the display name of an item stack for platform-specific formatting
     * @param stack The item stack
     * @return Display name string
     */
    String getItemDisplayName(ItemStack stack);
}