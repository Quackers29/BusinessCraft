package com.yourdomain.businesscraft.platform.forge;

import com.yourdomain.businesscraft.platform.PlatformHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;

/**
 * Forge implementation of the PlatformHelper interface.
 */
public class ForgePlatformHelper implements PlatformHelper {
    
    @Override
    public String getPlatformName() {
        return "forge";
    }
    
    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }
    
    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
    
    @Override
    public String getModVersion() {
        return ModList.get().getModContainerById("businesscraft")
            .map(container -> container.getModInfo().getVersion().toString())
            .orElse("Unknown");
    }
    
    @Override
    public CreativeModeTab.Builder createCreativeTab() {
        return CreativeModeTab.builder();
    }
    
    @Override
    public String getModDisplayName() {
        return ModList.get().getModContainerById("businesscraft")
            .map(container -> container.getModInfo().getDisplayName())
            .orElse("BusinessCraft");
    }
}