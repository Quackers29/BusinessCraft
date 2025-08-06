package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;

/**
 * Fabric implementation of PlatformHelper using Yarn mappings.
 */
public class FabricPlatformHelper implements PlatformHelper {
    
    @Override
    public String getPlatformName() {
        return "Fabric";
    }
    
    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
    
    @Override
    public boolean isPhysicalClient() {
        return FabricLoader.getInstance().getEnvironmentType().name().equals("CLIENT");
    }
    
    @Override
    public String getModVersion() {
        return FabricLoader.getInstance()
            .getModContainer("businesscraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }
    
    @Override
    public String getModId() {
        return "businesscraft";
    }
    
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }
    
    @Override
    public String getItemDisplayName(ItemStack stack) {
        return stack.getName().getString();
    }
}