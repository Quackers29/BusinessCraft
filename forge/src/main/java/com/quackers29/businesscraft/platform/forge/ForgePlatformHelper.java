package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.PlatformHelper;
import com.quackers29.businesscraft.event.PlatformPathHandler;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import java.util.UUID;

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
    
    public void setActivePlatformForPathCreation(int x, int y, int z, String platformId) {
        try {
            BlockPos pos = new BlockPos(x, y, z);
            UUID uuid = UUID.fromString(platformId);
            System.out.println("FORGE PLATFORM HELPER: Setting active platform for path creation: " + platformId + " at " + pos);
            PlatformPathHandler.setActivePlatform(pos, uuid);
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            System.out.println("FORGE PLATFORM HELPER: Invalid platform ID format: " + platformId);
            throw new RuntimeException("Invalid platform ID format: " + platformId, e);
        }
    }
    
    public void clearActivePlatformForPathCreation() {
        PlatformPathHandler.clearActivePlatform();
    }
    
    // TODO: Implement the following PlatformHelper interface methods:
    // - sendPlayerMessage(Object player, String message, String color): Send formatted messages to players
    // - forceBlockUpdate(Object player, int x, int y, int z): Force block state updates to clients  
    // - clearClientCaches(): Clear client-side caches that might contain stale data
    // - isClientSide(): Check if current execution is on client side
    // - isServerSide(): Check if current execution is on server side  
    // - getLogicalSide(): Get current logical side as string ("CLIENT" or "SERVER")
    // - clearTownPlatformCache(String townId): Clear platform cache data for specific town
    // - refreshPlatformManagementScreen(): Refresh open platform management screens
    // - updateTradeScreenOutput(Object itemStack): Update trade UI with payment result
    // - executeClientTask(Runnable task): Execute tasks on client main thread
    // - createBlockPos(int x, int y, int z): Create platform-specific BlockPos objects
}