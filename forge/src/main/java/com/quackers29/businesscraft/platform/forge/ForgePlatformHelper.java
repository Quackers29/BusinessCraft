package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.PlatformHelper;
import com.quackers29.businesscraft.event.PlatformPathHandler;
import com.quackers29.businesscraft.debug.DebugConfig;
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
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Setting active platform for path creation: {} at {}", platformId, pos);
            PlatformPathHandler.setActivePlatform(pos, uuid);
        } catch (IllegalArgumentException e) {
            // Invalid UUID format
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Invalid platform ID format: {}", platformId);
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
        LOGGER.debug("clearClientCaches called - refreshing UI data (UNIFIED ARCHITECTURE: no cache clearing needed)");
        
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        
        // UNIFIED ARCHITECTURE: No client-side town name caches to clear
        // Names are now resolved fresh from server (like map view system)
        
        // Refresh payment board if open (this will get fresh data from server with current town names)
        if (minecraft.screen instanceof com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen paymentScreen) {
            paymentScreen.refreshPaymentBoardData();
        }
        
        LOGGER.debug("Refreshed UI components (no cache clearing needed with unified architecture)");
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
        if (!(itemStack instanceof net.minecraft.world.item.ItemStack paymentItem)) {
            LOGGER.warn("updateTradeScreenOutput called with invalid itemStack type: {}", 
                itemStack != null ? itemStack.getClass().getSimpleName() : "null");
            return;
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "updateTradeScreenOutput called with: {}", paymentItem);
        
        // Get the current screen and update the output slot on either TradeScreen or BCModalInventoryScreen
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        
        client.execute(() -> {
            try {
                // Handle traditional TradeScreen first
                if (client.screen instanceof com.quackers29.businesscraft.ui.screens.town.TradeScreen tradeScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Setting payment item in TradeScreen: {}", paymentItem);
                    tradeScreen.setOutputItem(paymentItem);
                } 
                // Also check for our new modal inventory screen
                else if (client.screen instanceof com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen<?> modalScreen) {
                    // Check if the container is a TradeMenu
                    if (modalScreen.getMenu() instanceof com.quackers29.businesscraft.menu.TradeMenu tradeMenu) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "Setting payment item in BCModalInventoryScreen: {}", paymentItem);
                        tradeMenu.setOutputItem(paymentItem.copy());
                    }
                }
                else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Current screen is not a trade screen: {}", 
                        client.screen != null ? client.screen.getClass().getSimpleName() : "null");
                }
            } catch (Exception e) {
                LOGGER.error("Error updating trade screen output: {}", e.getMessage(), e);
            }
        });
    }
    
    @Override
    public void executeClientTask(Runnable task) {
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        client.execute(task);
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
                    if (reward instanceof com.quackers29.businesscraft.town.data.RewardEntry entry) {
                        // RewardEntry objects come directly from PaymentBoardResponsePacket now
                        // These preserve all original data including UUIDs
                        rewardEntries.add(entry);
                        LOGGER.debug("Added RewardEntry: ID={}, source={}, items={}", 
                            entry.getId(), entry.getSource(), entry.getRewards().size());
                    } else {
                        LOGGER.warn("Expected RewardEntry but got: {} - {}", 
                            reward.getClass().getName(), reward);
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
            LOGGER.debug("Parsing reward string: {}", rewardString);
            
            // Check if this is our structured format
            if (rewardString.startsWith("REWARD_ENTRY|")) {
                return parseStructuredRewardEntry(rewardString);
            }
            
            // Legacy fallback parsing for older format or toString() output
            if (rewardString.contains("Tourist Payment") || rewardString.contains("TOURIST_ARRIVAL")) {
                // Create tourist payment reward
                java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
                items.add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 5));
                
                return new com.quackers29.businesscraft.town.data.RewardEntry(
                    com.quackers29.businesscraft.town.data.RewardSource.TOURIST_ARRIVAL,
                    items,
                    "ALL"
                );
            } else if (rewardString.contains("Distance Milestone") || rewardString.contains("MILESTONE")) {
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
    
    /**
     * Parse structured reward entry format: REWARD_ENTRY|id=...|source=...|status=...|timestamp=...|itemCount=...|item0=...|...
     */
    private com.quackers29.businesscraft.town.data.RewardEntry parseStructuredRewardEntry(String rewardString) {
        try {
            String[] parts = rewardString.split("\\|");
            
            java.util.UUID id = null;
            com.quackers29.businesscraft.town.data.RewardSource source = null;
            com.quackers29.businesscraft.town.data.ClaimStatus status = null;
            long timestamp = System.currentTimeMillis();
            int itemCount = 0;
            
            // Parse metadata
            for (String part : parts) {
                if (part.startsWith("id=")) {
                    id = java.util.UUID.fromString(part.substring(3));
                } else if (part.startsWith("source=")) {
                    source = com.quackers29.businesscraft.town.data.RewardSource.valueOf(part.substring(7));
                } else if (part.startsWith("status=")) {
                    status = com.quackers29.businesscraft.town.data.ClaimStatus.valueOf(part.substring(7));
                } else if (part.startsWith("timestamp=")) {
                    timestamp = Long.parseLong(part.substring(10));
                } else if (part.startsWith("itemCount=")) {
                    itemCount = Integer.parseInt(part.substring(10));
                }
            }
            
            // Parse items
            java.util.List<net.minecraft.world.item.ItemStack> items = new java.util.ArrayList<>();
            for (int i = 0; i < itemCount; i++) {
                String itemKey = "item" + i + "=";
                for (String part : parts) {
                    if (part.startsWith(itemKey)) {
                        String itemData = part.substring(itemKey.length());
                        String[] itemParts = itemData.split(":");
                        if (itemParts.length >= 2) {
                            try {
                                // Simple item parsing - in real implementation would need proper item registry lookup
                                net.minecraft.world.item.Item item = net.minecraft.world.item.Items.BREAD; // Fallback
                                if (itemParts[0].contains("emerald")) item = net.minecraft.world.item.Items.EMERALD;
                                else if (itemParts[0].contains("gold")) item = net.minecraft.world.item.Items.GOLD_INGOT;
                                else if (itemParts[0].contains("diamond")) item = net.minecraft.world.item.Items.DIAMOND;
                                else if (itemParts[0].contains("experience_bottle")) item = net.minecraft.world.item.Items.EXPERIENCE_BOTTLE;
                                
                                int count = Integer.parseInt(itemParts[1]);
                                items.add(new net.minecraft.world.item.ItemStack(item, count));
                            } catch (Exception e) {
                                LOGGER.warn("Failed to parse item: {}", itemData, e);
                            }
                        }
                        break;
                    }
                }
            }
            
            if (source != null && !items.isEmpty()) {
                LOGGER.debug("Successfully parsed structured reward entry: id={}, source={}, items={}", 
                    id, source, items.size());
                // Use the public constructor to create a new reward entry
                // Note: This creates a new UUID and timestamp, but preserves the items and source
                return new com.quackers29.businesscraft.town.data.RewardEntry(source, items, "ALL");
            } else {
                LOGGER.warn("Missing required fields in structured reward entry: source={}, items={}", 
                    source, items.size());
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse structured reward entry: {}", rewardString, e);
        }
        return null;
    }
    
    @Override
    public String serializeRewardEntry(Object reward) {
        try {
            if (reward instanceof com.quackers29.businesscraft.town.data.RewardEntry entry) {
                // Create a structured serialization format that can be reliably parsed
                StringBuilder sb = new StringBuilder();
                sb.append("REWARD_ENTRY|");
                sb.append("id=").append(entry.getId().toString()).append("|");
                sb.append("source=").append(entry.getSource().name()).append("|");
                sb.append("status=").append(entry.getStatus().name()).append("|");
                sb.append("timestamp=").append(entry.getTimestamp()).append("|");
                sb.append("itemCount=").append(entry.getRewards().size());
                
                // Add item serialization
                for (int i = 0; i < entry.getRewards().size(); i++) {
                    net.minecraft.world.item.ItemStack stack = entry.getRewards().get(i);
                    sb.append("|item").append(i).append("=").append(stack.getItem().toString())
                      .append(":").append(stack.getCount());
                }
                
                String result = sb.toString();
                LOGGER.debug("Serialized RewardEntry: {}", result);
                return result;
            } else {
                // Fallback to toString for other types
                String result = reward.toString();
                LOGGER.debug("Serialized non-RewardEntry object: {}", result);
                return result;
            }
        } catch (Exception e) {
            LOGGER.error("Error serializing reward entry", e);
            return reward.toString();
        }
    }
    
    @Override
    public boolean enablePlatformVisualization(int x, int y, int z) {
        try {
            // Only execute on client side
            if (!isClientSide()) {
                LOGGER.warn("enablePlatformVisualization called on server side");
                return false;
            }
            
            // Get current game time from client level
            net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
            net.minecraft.world.level.Level level = minecraft.level;
            if (level == null) {
                LOGGER.warn("No client level available for platform visualization");
                return false;
            }
            
            long currentTime = level.getGameTime();
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            
            // Use the visualization manager from the main branch approach
            com.quackers29.businesscraft.client.render.world.VisualizationManager.getInstance()
                .showVisualization(com.quackers29.businesscraft.client.render.world.VisualizationManager.TYPE_PLATFORM, pos, null);
            
            com.quackers29.businesscraft.client.render.world.VisualizationManager.getInstance()
                .showVisualization(com.quackers29.businesscraft.client.render.world.VisualizationManager.TYPE_TOWN_BOUNDARY, pos, null);
            
            // Immediately request boundary data for instant visualization (main branch approach)
            com.quackers29.businesscraft.network.ModMessages.sendToServer(
                new com.quackers29.businesscraft.network.packets.ui.BoundarySyncRequestPacket(x, y, z, true, 32));
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Successfully enabled platform and boundary visualization at [{}, {}, {}] and requested immediate boundary sync", x, y, z);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to enable platform visualization at [{}, {}, {}]: {}", x, y, z, e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean updateBoundaryVisualization(int x, int y, int z, int boundaryRadius) {
        try {
            // Only execute on client side
            if (!isClientSide()) {
                LOGGER.warn("updateBoundaryVisualization called on server side");
                return false;
            }
            
            // Update boundary visualization radius through the renderer
            net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, y, z);
            com.quackers29.businesscraft.client.render.world.TownBoundaryVisualizationRenderer
                .updateBoundaryRadius(pos, boundaryRadius);
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Successfully updated boundary visualization at [{}, {}, {}] to radius={}", x, y, z, boundaryRadius);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to update boundary visualization at [{}, {}, {}]: {}", x, y, z, e.getMessage(), e);
            return false;
        }
    }
}