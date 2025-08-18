package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.PlatformHelper;
import com.quackers29.businesscraft.event.PlatformPathHandler;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Forge implementation of the PlatformHelper interface.
 */
public class ForgePlatformHelper implements PlatformHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgePlatformHelper.class);
    
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
    public String getModDisplayName() {
        return ModList.get().getModContainerById("businesscraft")
            .map(container -> container.getModInfo().getDisplayName())
            .orElse("BusinessCraft");
    }
    
    @Override
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
    
    @Override
    public void clearActivePlatformForPathCreation() {
        PlatformPathHandler.clearActivePlatform();
    }
    
    @Override
    public void sendPlayerMessage(Object player, String message, String color) {
        LOGGER.debug("sendPlayerMessage not yet implemented for Forge");
    }
    
    @Override
    public void forceBlockUpdate(Object player, int x, int y, int z) {
        LOGGER.debug("forceBlockUpdate not yet implemented for Forge");
    }
    
    @Override
    public void clearClientCaches() {
        LOGGER.debug("clearClientCaches not yet implemented for Forge");
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
        LOGGER.debug("clearTownPlatformCache not yet implemented for Forge");
    }
    
    @Override
    public void refreshPlatformManagementScreen() {
        LOGGER.debug("refreshPlatformManagementScreen not yet implemented for Forge");
    }
    
    @Override
    public void updateTradeScreenOutput(Object itemStack) {
        LOGGER.debug("updateTradeScreenOutput not yet implemented for Forge");
    }
    
    @Override
    public void executeClientTask(Runnable task) {
        LOGGER.debug("executeClientTask not yet implemented for Forge");
    }
    
    @Override
    public Object createBlockPos(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
    
    @Override
    public Object getCurrentScreen() {
        // Get the current Minecraft screen on client side
        if (net.minecraftforge.api.distmarker.Dist.CLIENT.isDedicatedServer()) {
            return null; // Server side has no screens
        }
        
        try {
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            return minecraft.screen;
        } catch (Exception e) {
            LOGGER.warn("Failed to get current screen: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    public void updatePaymentBoardScreen(Object screen, java.util.List<Object> rewards) {
        if (screen == null) {
            LOGGER.debug("Cannot update payment board screen - screen is null");
            return;
        }
        
        try {
            // Check if this is a PaymentBoardScreen
            String screenClassName = screen.getClass().getName();
            String simpleClassName = screen.getClass().getSimpleName();
            LOGGER.debug("Screen class name: {}, simple: {}", screenClassName, simpleClassName);
            
            if (simpleClassName.equals("PaymentBoardScreen")) {
                LOGGER.debug("Updating PaymentBoardScreen with {} rewards", rewards.size());
                
                // Convert Object rewards to RewardEntry objects for the screen
                java.util.List<com.quackers29.businesscraft.town.data.RewardEntry> rewardEntries = 
                    new java.util.ArrayList<>();
                
                for (Object reward : rewards) {
                    if (reward instanceof String rewardString) {
                        // Create a RewardEntry from the string data
                        com.quackers29.businesscraft.town.data.RewardEntry rewardEntry = 
                            createRewardEntryFromString(rewardString);
                        if (rewardEntry != null) {
                            rewardEntries.add(rewardEntry);
                        }
                    } else if (reward instanceof com.quackers29.businesscraft.town.data.RewardEntry entry) {
                        // Already a RewardEntry, add it directly
                        rewardEntries.add(entry);
                    }
                }
                
                LOGGER.debug("Converted {} rewards to {} RewardEntry objects", rewards.size(), rewardEntries.size());
                
                // List all available methods for debugging
                java.lang.reflect.Method[] methods = screen.getClass().getMethods();
                LOGGER.debug("Available methods in PaymentBoardScreen:");
                for (java.lang.reflect.Method method : methods) {
                    if (method.getName().contains("update") || method.getName().contains("Reward")) {
                        LOGGER.debug("  Method: {} with {} parameters", method.getName(), method.getParameterCount());
                    }
                }
                
                // Call updateRewardData with the correct parameter type
                java.lang.reflect.Method updateMethod = screen.getClass()
                    .getMethod("updateRewardData", java.util.List.class);
                LOGGER.debug("Found updateRewardData method: {}", updateMethod);
                
                updateMethod.invoke(screen, rewardEntries);
                
                LOGGER.debug("Successfully invoked PaymentBoardScreen.updateRewardData() with {} converted rewards", rewardEntries.size());
            } else {
                LOGGER.debug("Screen is not PaymentBoardScreen: {}", simpleClassName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update payment board screen: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Helper method to create RewardEntry from string data
     */
    private com.quackers29.businesscraft.town.data.RewardEntry createRewardEntryFromString(String rewardString) {
        try {
            // Parse the reward string and create appropriate RewardEntry
            if (rewardString.contains("Tourist Payment")) {
                // Create tourist payment reward
                java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
                items.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 5));
                
                return new com.quackers29.businesscraft.town.data.RewardEntry(
                    com.quackers29.businesscraft.town.data.RewardSource.TOURIST_PAYMENT,
                    items,
                    "ALL"
                );
            } else if (rewardString.contains("Distance Milestone")) {
                // Create milestone reward
                java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
                items.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BREAD, 1));
                items.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EXPERIENCE_BOTTLE, 2));
                
                return new com.quackers29.businesscraft.town.data.RewardEntry(
                    com.quackers29.businesscraft.town.data.RewardSource.MILESTONE,
                    items,
                    "ALL"
                );
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to create RewardEntry from string: {}", rewardString, e);
        }
        return null;
    }
}