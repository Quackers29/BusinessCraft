package com.quackers29.businesscraft.api;

public interface PlatformHelper {
    String getModId();

    boolean isClientSide();

    boolean isServerSide();

    String getPlatformName();

    java.nio.file.Path getConfigDirectory();
}
