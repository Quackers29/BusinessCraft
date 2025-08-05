package com.quackers29.businesscraft.platform;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Platform abstraction interface for loader-specific operations.
 * This interface provides a common API for operations that differ between mod loaders.
 */
public interface PlatformHelper {
    
    /**
     * Gets the name of the current mod loader platform.
     * @return "forge" or "fabric"
     */
    String getPlatformName();
    
    /**
     * Checks if the mod is currently running on the specified platform.
     * @param platformName The platform name to check against
     * @return true if running on the specified platform
     */
    boolean isModLoaded(String modid);
    
    /**
     * Checks if we're running in a development environment.
     * @return true if in development environment
     */
    boolean isDevelopmentEnvironment();
    
    /**
     * Gets the mod version.
     * @return The current mod version
     */
    String getModVersion();
    
    /**
     * Creates a creative mode tab builder with platform-specific handling.
     * @return A creative mode tab builder
     */
    CreativeModeTab.Builder createCreativeTab();
    
    /**
     * Gets the display name for the mod.
     * @return The mod's display name
     */
    String getModDisplayName();
}