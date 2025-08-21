package com.quackers29.businesscraft.util;

/**
 * Platform detection utility for unified architecture.
 * Determines which mod loader platform is currently running.
 */
public class PlatformUtil {
    
    private static Boolean isForge = null;
    
    /**
     * Check if running on Minecraft Forge.
     * @return true if running on Forge, false if running on Fabric
     */
    public static boolean isForge() {
        if (isForge == null) {
            try {
                Class.forName("net.minecraftforge.fml.ModList");
                isForge = true;
            } catch (ClassNotFoundException e) {
                isForge = false;
            }
        }
        return isForge;
    }
    
    /**
     * Check if running on Fabric.
     * @return true if running on Fabric, false if running on Forge
     */
    public static boolean isFabric() {
        return !isForge();
    }
    
    /**
     * Get the platform name as a string.
     * @return "forge" or "fabric"
     */
    public static String getPlatformName() {
        return isForge() ? "forge" : "fabric";
    }
}