package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.PlatformHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge implementation of PlatformHelper
 */
public class ForgePlatformHelper implements PlatformHelper {
    @Override
    public String getModId() {
        return "businesscraft";
    }

    @Override
    public boolean isClientSide() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    @Override
    public boolean isServerSide() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    @Override
    public String getPlatformName() {
        return "forge";
    }
}
