package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.BlockEntityHelper;
import com.quackers29.businesscraft.platform.InventoryHelper;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Forge implementation of the BlockEntityHelper interface using the capability system.
 * This class provides cross-platform block entity capability management for
 * inventory attachment and custom data storage.
 */
public class ForgeBlockEntityHelper implements BlockEntityHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeBlockEntityHelper.class);
    
    // Thread-safe storage for custom data attachments
    private final Map<BlockEntity, Map<String, Object>> customDataStorage = new ConcurrentHashMap<>();
    
    // Track capability attachments for proper cleanup
    private final Map<BlockEntity, Map<String, LazyOptional<?>>> capabilityAttachments = new ConcurrentHashMap<>();
    
    @Override
    public Object attachInventory(BlockEntity blockEntity, InventoryHelper.PlatformInventory inventory, @Nullable Direction side) {
        // For Forge, we expect the inventory to wrap an IItemHandler
        
        // Get the ItemStackHandler from the inventory wrapper
        Object platformInventory = inventory.getPlatformInventory();
        if (!(platformInventory instanceof IItemHandler itemHandler)) {
            throw new IllegalArgumentException("Platform inventory must be an IItemHandler for Forge");
        }
        
        // Create LazyOptional capability
        LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(() -> itemHandler);
        
        // Store capability attachment for cleanup
        capabilityAttachments
            .computeIfAbsent(blockEntity, k -> new HashMap<>())
            .put(getInventoryCapabilityKey() + (side != null ? "_" + side.getName() : ""), lazyOptional);
        
        return lazyOptional;
    }
    
    @Override
    public @Nullable InventoryHelper.PlatformInventory getInventory(BlockEntity blockEntity, @Nullable Direction side) {
        // This would typically query the block entity's getCapability method
        // For now, we'll return null as we need the block entity to implement the capability system
        // This will be properly implemented when we abstract the TownInterfaceEntity
        return null;
    }
    
    @Override
    public <T> Object attachData(BlockEntity blockEntity, String key, T data) {
        customDataStorage
            .computeIfAbsent(blockEntity, k -> new ConcurrentHashMap<>())
            .put(key, data);
        
        // Return a handle that can be used for removal
        return new DataHandle(blockEntity, key);
    }
    
    @Override
    public @Nullable <T> T getData(BlockEntity blockEntity, String key, Class<T> dataClass) {
        Map<String, Object> entityData = customDataStorage.get(blockEntity);
        if (entityData != null) {
            Object data = entityData.get(key);
            if (dataClass.isInstance(data)) {
                return dataClass.cast(data);
            }
        }
        return null;
    }
    
    @Override
    public void removeData(BlockEntity blockEntity, String key) {
        Map<String, Object> entityData = customDataStorage.get(blockEntity);
        if (entityData != null) {
            entityData.remove(key);
            if (entityData.isEmpty()) {
                customDataStorage.remove(blockEntity);
            }
        }
    }
    
    @Override
    public void invalidateAllAttachments(BlockEntity blockEntity) {
        // Invalidate all capability attachments
        Map<String, LazyOptional<?>> attachments = capabilityAttachments.remove(blockEntity);
        if (attachments != null) {
            attachments.values().forEach(LazyOptional::invalidate);
        }
        
        // Remove custom data
        customDataStorage.remove(blockEntity);
    }
    
    @Override
    public boolean hasCapability(BlockEntity blockEntity, String capabilityKey, @Nullable Direction side) {
        // Check if we have a stored capability attachment
        Map<String, LazyOptional<?>> attachments = capabilityAttachments.get(blockEntity);
        if (attachments != null) {
            String fullKey = capabilityKey + (side != null ? "_" + side.getName() : "");
            LazyOptional<?> capability = attachments.get(fullKey);
            return capability != null && capability.isPresent();
        }
        return false;
    }
    
    @Override
    public String getInventoryCapabilityKey() {
        return "inventory";
    }
    
    // ==== NEW METHODS FOR ENHANCED MULTILOADER PACKET SUPPORT ====
    
    // @Override
    public @Nullable Object getBlockEntity(Object player, int x, int y, int z) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("Player object is not a ServerPlayer: {}", player);
            return null;
        }
        
        Level level = serverPlayer.level();
        BlockPos pos = new BlockPos(x, y, z);
        return level.getBlockEntity(pos);
    }
    
    // @Override
    public @Nullable Object getTownDataProvider(Object blockEntity) {
        if (blockEntity instanceof ITownDataProvider provider) {
            return provider;
        }
        return null;
    }
    
    // @Override
    public boolean isTouristSpawningEnabled(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.isTouristSpawningEnabled();
        }
        return false;
    }
    
    // @Override
    public void setTouristSpawningEnabled(Object townDataProvider, boolean enabled) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.setTouristSpawningEnabled(enabled);
        }
    }
    
    // @Override
    public void markTownDataDirty(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.markDirty();
        }
    }
    
    // @Override
    public void syncTownData(Object blockEntity) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            townInterface.syncTownData();
        }
    }
    
    // @Override
    public @Nullable String getTownName(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.getTownName();
        }
        return null;
    }
    
    // @Override
    public void setTownName(Object townDataProvider, String townName) {
        // TODO: Implement setTownName - method doesn't exist in current ITownDataProvider
        LOGGER.warn("setTownName not yet implemented for Forge");
    }
    
    // @Override
    public @Nullable String getTownId(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.getTownId() != null ? provider.getTownId().toString() : null;
        }
        return null;
    }
    
    // @Override
    public boolean isTownDataInitialized(Object townDataProvider) {
        // TODO: Implement isTownDataInitialized - method doesn't exist in current ITownDataProvider
        LOGGER.warn("isTownDataInitialized not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public int getSearchRadius(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.getSearchRadius();
        }
        return 100; // Default fallback
    }
    
    // @Override
    public void setSearchRadius(Object townDataProvider, int radius) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.setSearchRadius(radius);
        }
    }
    
    // @Override
    public boolean canAddMorePlatforms(Object blockEntity) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            return townInterface.canAddMorePlatforms();
        }
        return false;
    }
    
    // @Override
    public boolean addPlatform(Object blockEntity) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            return townInterface.addPlatform();
        }
        return false;
    }
    
    // @Override
    public void markBlockEntityChanged(Object blockEntity) {
        if (blockEntity instanceof BlockEntity be) {
            be.setChanged();
        }
    }
    
    // @Override
    public boolean deletePlatform(Object blockEntity, int platformIndex) {
        // TODO: Implement deletePlatform - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("deletePlatform not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean removePlatform(Object blockEntity, String platformId) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(platformId);
                return townInterface.removePlatform(uuid);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid platform ID format: {}", platformId);
                return false;
            }
        }
        return false;
    }
    
    // @Override
    public int getPlatformCount(Object blockEntity) {
        // TODO: Implement getPlatformCount - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("getPlatformCount not yet implemented for Forge");
        return 0;
    }
    
    // @Override
    public void setPlatformEnabled(Object blockEntity, int platformIndex, boolean enabled) {
        // TODO: Implement setPlatformEnabled - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("setPlatformEnabled not yet implemented for Forge");
    }
    
    // @Override
    public boolean isPlatformEnabled(Object blockEntity, int platformIndex) {
        // TODO: Implement isPlatformEnabled - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("isPlatformEnabled not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public @Nullable Object getClientBlockEntity(int x, int y, int z) {
        // TODO: Implement client-side block entity access
        // This would require access to Minecraft.getInstance().level
        LOGGER.warn("getClientBlockEntity not yet implemented for Forge");
        return null;
    }
    
    // ==== PLACEHOLDER IMPLEMENTATIONS FOR NEW PACKET-RELATED METHODS ====
    // These need full implementation based on existing TownInterfaceEntity methods
    
    // @Override
    public boolean setPlatformDestinationEnabled(Object blockEntity, String platformId, String townId, boolean enabled) {
        // TODO: Implement platform destination management
        LOGGER.warn("setPlatformDestinationEnabled not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public Map<String, String> getAllTownsForDestination(Object blockEntity) {
        // TODO: Implement town destination retrieval
        LOGGER.warn("getAllTownsForDestination not yet implemented for Forge");
        return new HashMap<>();
    }
    
    // @Override
    public Map<String, Boolean> getPlatformDestinations(Object blockEntity, String platformId) {
        // TODO: Implement platform destination state retrieval
        LOGGER.warn("getPlatformDestinations not yet implemented for Forge");
        return new HashMap<>();
    }
    
    // @Override
    public @Nullable Object getOriginTown(Object blockEntity) {
        // TODO: Implement origin town retrieval
        LOGGER.warn("getOriginTown not yet implemented for Forge");
        return null;
    }
    
    // @Override
    public @Nullable int[] getTownPosition(Object town) {
        // TODO: Implement town position retrieval
        LOGGER.warn("getTownPosition not yet implemented for Forge");
        return null;
    }
    
    // @Override
    public @Nullable Object getTownById(Object player, String townId) {
        // TODO: Implement town lookup by ID
        LOGGER.warn("getTownById not yet implemented for Forge");
        return null;
    }
    
    // @Override
    public boolean setPlatformPath(Object blockEntity, String platformId, int startX, int startY, int startZ, int endX, int endY, int endZ) {
        // TODO: Implement platform path setting
        LOGGER.warn("setPlatformPath not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean resetPlatformPath(Object blockEntity, String platformId) {
        // TODO: Implement platform path reset
        LOGGER.warn("resetPlatformPath not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean setPlatformEnabledById(Object blockEntity, String platformId, boolean enabled) {
        // TODO: Implement platform enabled state by ID
        LOGGER.warn("setPlatformEnabledById not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean setPlatformCreationMode(Object blockEntity, boolean mode, String platformId) {
        // TODO: Implement platform creation mode setting
        LOGGER.warn("setPlatformCreationMode not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public Object processResourceTrade(Object blockEntity, Object player, Object itemStack, int slotId) {
        // TODO: Implement resource trading logic
        LOGGER.warn("processResourceTrade not yet implemented for Forge");
        return null;
    }
    
    // @Override
    public List<Object> getUnclaimedRewards(Object blockEntity) {
        // TODO: Implement payment board reward retrieval
        LOGGER.warn("getUnclaimedRewards not yet implemented for Forge");
        return new ArrayList<>();
    }
    
    // @Override
    public Object claimPaymentBoardReward(Object blockEntity, Object player, String rewardId, boolean toBuffer) {
        // TODO: Implement reward claiming logic
        LOGGER.warn("claimPaymentBoardReward not yet implemented for Forge");
        return null;
    }
    
    // @Override
    public boolean openPaymentBoardUI(Object blockEntity, Object player) {
        // TODO: Implement Payment Board UI opening
        LOGGER.warn("openPaymentBoardUI not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean openTownInterfaceUI(Object blockEntity, Object player) {
        // TODO: Implement Town Interface UI opening
        LOGGER.warn("openTownInterfaceUI not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean processTownMapDataRequest(Object blockEntity, Object player, int zoomLevel, boolean includeStructures) {
        if (!(blockEntity instanceof TownInterfaceEntity) || !(player instanceof ServerPlayer)) {
            return false;
        }
        
        try {
            TownInterfaceEntity townEntity = (TownInterfaceEntity) blockEntity;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            LOGGER.debug("Processing town map data request for zoom level: {} at position: {}", 
                        zoomLevel, townEntity.getBlockPos());
            
            // Get map data - simplified implementation for now
            String mapData = generateMapData(townEntity, zoomLevel, includeStructures);
            
            // Send response packet to client
            TownMapDataResponsePacket responsePacket = new TownMapDataResponsePacket(
                townEntity.getBlockPos().getX(),
                townEntity.getBlockPos().getY(), 
                townEntity.getBlockPos().getZ(),
                mapData,
                zoomLevel
            );
            
            ModMessages.sendToPlayer(responsePacket, serverPlayer);
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to process town map data request", e);
            return false;
        }
    }
    
    public boolean updateTownMapUI(Object player, int x, int y, int z, String mapData, int zoomLevel) {
        try {
            // Find the currently open map modal and update it
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) {
                com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
                    (com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) minecraft.screen;
                mapModal.updateMapData(mapData, zoomLevel);
                return true;
            }
            
            LOGGER.debug("No town map modal currently open to update");
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to update town map UI", e);
            return false;
        }
    }
    
    /**
     * Generate map data for the requested town area.
     * This is a simplified implementation - in a full implementation this would
     * collect actual world data, town boundaries, structures, etc.
     */
    private String generateMapData(TownInterfaceEntity townEntity, int zoomLevel, boolean includeStructures) {
        // Simplified map data generation
        StringBuilder mapData = new StringBuilder();
        mapData.append("{");
        mapData.append("\"townPos\":{\"x\":").append(townEntity.getBlockPos().getX())
               .append(",\"y\":").append(townEntity.getBlockPos().getY())
               .append(",\"z\":").append(townEntity.getBlockPos().getZ()).append("},");
        mapData.append("\"zoomLevel\":").append(zoomLevel).append(",");
        mapData.append("\"includeStructures\":").append(includeStructures).append(",");
        mapData.append("\"mapSize\":").append(64 * zoomLevel).append(",");
        mapData.append("\"generatedAt\":").append(System.currentTimeMillis());
        mapData.append("}");
        
        return mapData.toString();
    }
    
    /**
     * Simple data handle for tracking custom data attachments.
     */
    private static class DataHandle {
        private final BlockEntity blockEntity;
        private final String key;
        
        public DataHandle(BlockEntity blockEntity, String key) {
            this.blockEntity = blockEntity;
            this.key = key;
        }
        
        public BlockEntity getBlockEntity() {
            return blockEntity;
        }
        
        public String getKey() {
            return key;
        }
    }
    
    public boolean processPlatformDataRequest(Object player, int x, int y, int z, 
                                            boolean includePlatformConnections, 
                                            boolean includeDestinationTowns, 
                                            int maxRadius) {
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel level = serverPlayer.serverLevel();
            BlockPos pos = new BlockPos(x, y, z);
            
            // Generate platform connection data
            String platformData = "{}"; // Placeholder - implement actual platform data generation
            if (includePlatformConnections) {
                // TODO: Implement platform connection data generation
                // This would gather platform layout, connections, and transportation network info
            }
            
            // Generate destination town data with actual town information
            String destinationData = "{}";
            if (includeDestinationTowns) {
                destinationData = generateDestinationTownData(level, pos, maxRadius);
                LOGGER.debug("Generated destination data: {}", destinationData);
            }
            
            // Send response packet to client
            TownPlatformDataResponsePacket response = 
                new TownPlatformDataResponsePacket(x, y, z, platformData, destinationData, maxRadius);
            
            // Send packet using platform services
            PlatformServices.getNetworkHelper().sendToClient(response, serverPlayer);
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to process platform data request at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate destination town data as JSON string for sophisticated map display.
     * This method finds all towns within the specified radius and formats them for the client cache.
     */
    private String generateDestinationTownData(ServerLevel level, BlockPos centerPos, int maxRadius) {
        try {
            // Try to get TownManager - this is currently in forge module but should be in common
            Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
            Object townManagerInstance = townManagerClass.getMethod("get", ServerLevel.class).invoke(null, level);
            
            if (townManagerInstance == null) {
                LOGGER.debug("TownManager not available, returning empty town data");
                return "{}";
            }
            
            // Get all towns from TownManager
            Object allTowns = townManagerClass.getMethod("getAllTowns").invoke(townManagerInstance);
            
            if (!(allTowns instanceof java.util.Collection)) {
                LOGGER.debug("getAllTowns did not return a collection, returning empty data");
                return "{}";
            }
            
            @SuppressWarnings("unchecked")
            java.util.Collection<Object> towns = (java.util.Collection<Object>) allTowns;
            
            // Build JSON manually for simplicity (could use Gson if available)
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            boolean first = true;
            
            for (Object town : towns) {
                try {
                    // Get town position through ITownDataProvider
                    if (!(town instanceof com.quackers29.businesscraft.api.ITownDataProvider)) {
                        continue;
                    }
                    
                    com.quackers29.businesscraft.api.ITownDataProvider townData = 
                        (com.quackers29.businesscraft.api.ITownDataProvider) town;
                    
                    // Get town information
                    Object townPos = town.getClass().getMethod("getPos").invoke(town);
                    if (!(townPos instanceof BlockPos)) {
                        continue;
                    }
                    
                    BlockPos pos = (BlockPos) townPos;
                    
                    // Check distance
                    double distance = Math.sqrt(centerPos.distSqr(pos));
                    if (distance > maxRadius) {
                        continue;
                    }
                    
                    String townName = townData.getTownName();
                    java.util.UUID townId = townData.getTownId();
                    
                    if (townName == null || townId == null) {
                        continue;
                    }
                    
                    if (!first) {
                        jsonBuilder.append(",");
                    }
                    first = false;
                    
                    // Add town data as JSON
                    jsonBuilder.append("\"").append(townId.toString()).append("\":{");
                    jsonBuilder.append("\"id\":\"").append(townId.toString()).append("\",");
                    jsonBuilder.append("\"name\":\"").append(escapeJson(townName)).append("\",");
                    jsonBuilder.append("\"x\":").append(pos.getX()).append(",");
                    jsonBuilder.append("\"y\":").append(pos.getY()).append(",");
                    jsonBuilder.append("\"z\":").append(pos.getZ()).append(",");
                    jsonBuilder.append("\"distance\":").append((int)distance);
                    jsonBuilder.append("}");
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to process town data: {}", e.getMessage());
                }
            }
            
            jsonBuilder.append("}");
            String result = jsonBuilder.toString();
            
            LOGGER.debug("Generated destination data with {} towns within {}m radius", 
                        towns.size(), maxRadius);
            return result;
            
        } catch (Exception e) {
            LOGGER.warn("Failed to generate destination town data: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Simple JSON string escaping for town names.
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    public boolean updateTownPlatformUI(Object player, int x, int y, int z, String platformData, String destinationData) {
        try {
            // Find the currently open map modal and update it with platform data
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) {
                com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
                    (com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) minecraft.screen;
                
                // Update the modal with platform and destination data
                // TODO: Implement sophisticated map modal platform data update
                // This would update town markers, connections, and interactive features
                
                LOGGER.debug("Updated town platform UI with platform data at ({}, {}, {})", x, y, z);
                return true;
            } else {
                LOGGER.debug("No town map modal open to update with platform data");
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update town platform UI at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return false;
        }
    }
}