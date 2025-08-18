package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.PlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of the PlatformHelper interface.
 */
public class FabricPlatformHelper implements PlatformHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricPlatformHelper.class);
    
    @Override
    public String getPlatformName() {
        return "fabric";
    }
    
    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }
    
    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
    
    @Override
    public String getModVersion() {
        return FabricLoader.getInstance()
            .getModContainer("businesscraft")
            .map(container -> container.getMetadata().getVersion().getFriendlyString())
            .orElse("Unknown");
    }
    
    @Override
    public String getModDisplayName() {
        return FabricLoader.getInstance()
            .getModContainer("businesscraft")
            .map(container -> container.getMetadata().getName())
            .orElse("BusinessCraft");
    }
    
    @Override
    public void sendPlayerMessage(Object player, String message, String color) {
        LOGGER.debug("sendPlayerMessage not yet implemented for Fabric");
    }
    
    @Override
    public void forceBlockUpdate(Object player, int x, int y, int z) {
        LOGGER.debug("forceBlockUpdate not yet implemented for Fabric");
    }
    
    @Override
    public void clearClientCaches() {
        LOGGER.debug("clearClientCaches not yet implemented for Fabric");
    }
    
    @Override
    public boolean isClientSide() {
        return true; // Placeholder
    }
    
    @Override
    public boolean isServerSide() {
        return true; // Placeholder  
    }
    
    @Override
    public String getLogicalSide() {
        return "SERVER"; // Placeholder
    }
    
    @Override
    public void clearTownPlatformCache(String townId) {
        LOGGER.debug("clearTownPlatformCache not yet implemented for Fabric");
    }
    
    @Override
    public void refreshPlatformManagementScreen() {
        LOGGER.debug("refreshPlatformManagementScreen not yet implemented for Fabric");
    }
    
    @Override
    public void updateTradeScreenOutput(Object itemStack) {
        LOGGER.debug("updateTradeScreenOutput not yet implemented for Fabric");
    }
    
    @Override
    public void executeClientTask(Runnable task) {
        LOGGER.debug("executeClientTask not yet implemented for Fabric");
    }
    
    @Override
    public void setActivePlatformForPathCreation(int x, int y, int z, String platformId) {
        LOGGER.debug("setActivePlatformForPathCreation not yet implemented for Fabric");
    }
    
    @Override
    public void clearActivePlatformForPathCreation() {
        LOGGER.debug("clearActivePlatformForPathCreation not yet implemented for Fabric");
    }
    
    @Override
    public Object createBlockPos(int x, int y, int z) {
        // Use Fabric's BlockPos (needs proper import)
        return new net.minecraft.util.math.BlockPos(x, y, z);
    }
    
    @Override
    public Object getCurrentScreen() {
        // Get the current Minecraft screen on client side
        LOGGER.debug("getCurrentScreen not yet implemented for Fabric");
        return null;
    }
    
    @Override
    public void updatePaymentBoardScreen(Object screen, java.util.List<Object> rewards) {
        if (screen == null) {
            LOGGER.debug("Cannot update payment board screen - screen is null");
            return;
        }
        
        LOGGER.debug("updatePaymentBoardScreen not yet implemented for Fabric - would update {} rewards", rewards.size());
    }
    
    @Override
    public String serializeRewardEntry(Object reward) {
        // For now, use simple toString serialization
        // Full implementation would mirror the Forge structured serialization
        LOGGER.debug("serializeRewardEntry not yet fully implemented for Fabric - using toString()");
        return reward.toString();
    }
}