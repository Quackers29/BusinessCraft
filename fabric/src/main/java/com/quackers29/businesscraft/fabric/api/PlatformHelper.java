package com.quackers29.businesscraft.fabric.api;

/**
 * Platform abstraction interface for cross-platform compatibility
 */
public interface PlatformHelper {
    String getPlatformName();
    boolean isDevelopmentEnvironment();
}
