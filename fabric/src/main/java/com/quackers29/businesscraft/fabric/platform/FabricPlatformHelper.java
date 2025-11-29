package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.PlatformHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric implementation of PlatformHelper
 */
public class FabricPlatformHelper implements PlatformHelper {
    @Override
    public String getModId() {
        return "businesscraft";
    }

    @Override
    public boolean isClientSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }

    @Override
    public String getPlatformName() {
        return "fabric";
    }

    @Override
    public java.nio.file.Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }
}
