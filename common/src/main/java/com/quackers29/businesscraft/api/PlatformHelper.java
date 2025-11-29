package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for basic platform operations.
 * Implementations will be platform-specific (Forge, Fabric, etc.)
 */
public interface PlatformHelper {
    /**
     * Get the mod ID
     */
    String getModId();

    /**
     * Check if we're running on the client side
     */
    boolean isClientSide();

    /**
     * Check if we're running on the server side
     */
    boolean isServerSide();

    /**
     * Get the current platform name (e.g., "forge", "fabric")
     */
    String getPlatformName();

    /**
     * Get the configuration directory
     */
    java.nio.file.Path getConfigDirectory();
}
