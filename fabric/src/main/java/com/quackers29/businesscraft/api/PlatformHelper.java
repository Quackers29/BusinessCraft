package com.quackers29.businesscraft.api;

/**
 * Platform abstraction interface for cross-platform compatibility
 */
public interface PlatformHelper {
    String getModId();
    boolean isClientSide();
    boolean isServerSide();
    String getPlatformName();
}
