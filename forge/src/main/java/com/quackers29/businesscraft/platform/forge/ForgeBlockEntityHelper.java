package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.BlockEntityHelper;
import com.quackers29.businesscraft.platform.InventoryHelper;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.ITownManagerService;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import java.util.Set;
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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.util.HashSet;

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
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Setting destination {} to {} for platform {} on block entity {}", townId, enabled, platformId, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                UUID platformUUID = UUID.fromString(platformId);
                UUID townUUID = UUID.fromString(townId);
                
                Platform platform = townInterface.getPlatform(platformUUID);
                if (platform == null) {
                    LOGGER.warn("Platform not found with ID: {}", platformId);
                    return false;
                }
                
                if (enabled) {
                    platform.enableDestination(townUUID);
                } else {
                    platform.disableDestination(townUUID);
                }
                
                // Mark the block entity as changed to trigger NBT save
                townInterface.setChanged();
                
                LOGGER.info("FORGE BLOCK ENTITY HELPER: Successfully set destination {} to {} for platform {}", townId, enabled, platformId);
                return true;
                
            } catch (IllegalArgumentException e) {
                LOGGER.error("FORGE BLOCK ENTITY HELPER: Invalid UUID format - platformId: {}, townId: {}", platformId, townId);
                return false;
            }
        }
        
        LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
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
        try {
            if (!(town instanceof ITownDataProvider)) {
                LOGGER.warn("getTownPosition called with non-ITownDataProvider: {}", town.getClass());
                return null;
            }
            
            ITownDataProvider townData = (ITownDataProvider) town;
            ITownDataProvider.Position position = townData.getPosition();
            
            if (position == null) {
                LOGGER.warn("Town position is null for town: {}", townData.getTownName());
                return null;
            }
            
            return new int[]{position.getX(), position.getY(), position.getZ()};
            
        } catch (Exception e) {
            LOGGER.error("Exception in getTownPosition: {}", e.getMessage());
            return null;
        }
    }
    
    // @Override
    public @Nullable Object getTownById(Object player, String townId) {
        try {
            if (!(player instanceof ServerPlayer)) {
                LOGGER.warn("getTownById called with non-ServerPlayer: {}", player.getClass());
                return null;
            }
            
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel level = serverPlayer.serverLevel();
            
            // Get TownManagerService using platform services
            ITownManagerService townManagerService = PlatformServices.getTownManagerService();
            if (townManagerService == null) {
                LOGGER.warn("TownManagerService not available for getTownById");
                return null;
            }
            
            // Convert string to UUID
            UUID townUUID;
            try {
                townUUID = UUID.fromString(townId);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid town ID format in getTownById: {}", townId);
                return null;
            }
            
            // Get all towns and find the matching one
            try {
                Map<UUID, Object> allTowns = townManagerService.getAllTowns(level);
                
                if (allTowns != null) {
                    Object town = allTowns.get(townUUID);
                    if (town != null) {
                        LOGGER.debug("Found town by ID {}: {}", townId, town.getClass().getSimpleName());
                        return town;
                    } else {
                        LOGGER.debug("Town not found by ID: {}", townId);
                        return null;
                    }
                } else {
                    LOGGER.warn("getAllTowns returned null");
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to access getAllTowns method: {}", e.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("Exception in getTownById for {}: {}", townId, e.getMessage());
            return null;
        }
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
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Setting platform {} enabled state to {} on block entity {}", platformId, enabled, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                UUID platformUUID = UUID.fromString(platformId);
                
                Platform platform = townInterface.getPlatform(platformUUID);
                if (platform == null) {
                    LOGGER.warn("Platform not found with ID: {}", platformId);
                    return false;
                }
                
                platform.setEnabled(enabled);
                
                // Mark the block entity as changed to trigger NBT save
                townInterface.setChanged();
                
                LOGGER.info("FORGE BLOCK ENTITY HELPER: Successfully set platform {} enabled state to {}", platformId, enabled);
                return true;
                
            } catch (IllegalArgumentException e) {
                LOGGER.error("FORGE BLOCK ENTITY HELPER: Invalid platform ID format: {}", platformId);
                return false;
            }
        }
        
        LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
        return false;
    }
    
    // @Override
    public boolean setPlatformCreationMode(Object blockEntity, boolean mode, String platformId) {
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Setting platform creation mode to {} for platform {} on block entity {}", mode, platformId, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                UUID platformUUID = UUID.fromString(platformId);
                townInterface.setPlatformCreationMode(mode, platformUUID);
                LOGGER.info("FORGE BLOCK ENTITY HELPER: Successfully set platform creation mode to {} for platform {}", mode, platformId);
                return true;
            } catch (IllegalArgumentException e) {
                LOGGER.error("FORGE BLOCK ENTITY HELPER: Invalid platform ID format: {}", platformId);
                return false;
            }
        }
        
        LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
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
            
            // Generate structured map data for sophisticated map features
            Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> structuredMapData = 
                generateStructuredMapData(townEntity, serverPlayer, zoomLevel, includeStructures);
            
            // Send structured town data to client using updated packet
            sendStructuredMapDataToClient(serverPlayer, townEntity, structuredMapData, zoomLevel);
            
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
                
                // Parse the JSON map data to structured format for sophisticated features
                Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> structuredData = 
                    parseJsonToStructuredMapData(mapData);
                
                // Directly set the parsed town data on the sophisticated map
                mapModal.setTownData(structuredData);
                LOGGER.debug("Set parsed town data directly on sophisticated map: {} towns", structuredData.size());
                
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
     * Parse JSON map data back to structured format for client-side sophisticated features.
     */
    private Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> parseJsonToStructuredMapData(String jsonData) {
        Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> result = new HashMap<>();
        
        try {
            // Basic JSON parsing to extract town data
            if (jsonData == null || !jsonData.contains("\"towns\":{")) {
                return result;
            }
            
            // Extract the towns section - need to find the matching closing brace
            int townsStart = jsonData.indexOf("\"towns\":{") + 9;
            int townsEnd = findMatchingClosingBrace(jsonData, townsStart - 1); // -1 to include the opening brace
            
            if (townsStart > 9 && townsEnd > townsStart) {
                String townsSection = jsonData.substring(townsStart, townsEnd);
                
                LOGGER.debug("Extracted towns section: {}", townsSection);
                
                // Parse individual town entries
                String[] townEntries = townsSection.split("},");
                LOGGER.debug("Split into {} town entries", townEntries.length);
                for (String townEntry : townEntries) {
                    try {
                        if (!townEntry.endsWith("}")) {
                            townEntry += "}";
                        }
                        
                        TownMapDataResponsePacket.TownMapInfo townInfo = parseTownEntry(townEntry);
                        if (townInfo != null) {
                            result.put(townInfo.townId, townInfo);
                        }
                        
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse town entry: {}", e.getMessage());
                    }
                }
            }
            
            LOGGER.debug("Parsed {} towns from JSON map data", result.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse JSON to structured map data: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Find the matching closing brace for a given opening brace position.
     */
    private int findMatchingClosingBrace(String json, int openBracePos) {
        int braceCount = 0;
        for (int i = openBracePos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1; // No matching brace found
    }
    
    /**
     * Parse a single town entry from JSON format.
     */
    private TownMapDataResponsePacket.TownMapInfo parseTownEntry(String townEntry) {
        try {
            // Extract town UUID (the key)
            int colonIndex = townEntry.indexOf(":");
            if (colonIndex == -1) return null;
            
            String townIdStr = townEntry.substring(0, colonIndex).trim().replaceAll("\"", "");
            java.util.UUID townId = java.util.UUID.fromString(townIdStr);
            
            // Extract town data
            String townDataJson = townEntry.substring(colonIndex + 1).trim();
            if (!townDataJson.startsWith("{") || !townDataJson.endsWith("}")) {
                return null;
            }
            
            // Parse individual fields
            String name = extractJsonString(townDataJson, "name");
            int x = extractJsonInt(townDataJson, "x");
            int y = extractJsonInt(townDataJson, "y");
            int z = extractJsonInt(townDataJson, "z");
            int population = extractJsonInt(townDataJson, "population");
            int visitCount = extractJsonInt(townDataJson, "visitCount");
            long lastVisited = extractJsonLong(townDataJson, "lastVisited");
            boolean isCurrentTown = extractJsonBoolean(townDataJson, "isCurrentTown");
            
            return new TownMapDataResponsePacket.TownMapInfo(
                townId, name, x, y, z, population, visitCount, lastVisited, isCurrentTown
            );
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse town entry '{}': {}", townEntry, e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper methods for JSON parsing.
     */
    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    private int extractJsonInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private long extractJsonLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0L;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    private boolean extractJsonBoolean(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return false;
        start += pattern.length();
        return json.substring(start).startsWith("true");
    }
    
    /**
     * Generate structured map data for sophisticated map features.
     * This creates TownMapInfo objects for all towns in the area.
     */
    private Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> generateStructuredMapData(
            TownInterfaceEntity townEntity, ServerPlayer player, int zoomLevel, boolean includeStructures) {
        
        Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> townMapData = new HashMap<>();
        
        try {
            ServerLevel level = player.serverLevel();
            BlockPos currentPos = townEntity.getBlockPos();
            
            // Get town manager and find all towns
            Object townManager = getTownManager(level);
            if (townManager == null) {
                LOGGER.warn("No town manager found for map data generation");
                return townMapData;
            }
            
            // Debug: Check total towns available before radius filtering
            try {
                Map<UUID, Object> debugAllTowns = PlatformServices.getTownManagerService().getAllTowns((ServerLevel) townManager);
                LOGGER.warn("DEBUG: TownManagerService.getAllTowns() returned {} total towns before radius filtering", debugAllTowns.size());
                for (Map.Entry<UUID, Object> entry : debugAllTowns.entrySet()) {
                    Object townObj = entry.getValue();
                    if (townObj instanceof ITownDataProvider townData) {
                        LOGGER.warn("DEBUG: Town found: '{}' at {}", townData.getTownName(), getTownPosition(townData));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("DEBUG: Failed to check total towns: {}", e.getMessage());
            }
            
            // Calculate search radius based on zoom level
            int searchRadius = calculateMapSearchRadius(zoomLevel);
            
            // Find all towns within the search radius
            List<Object> nearbyTowns = findTownsInRadius(townManager, currentPos, searchRadius);
            
            for (Object townObj : nearbyTowns) {
                try {
                    ITownDataProvider townData = (ITownDataProvider) townObj;
                    
                    if (townData.getTownName() == null || townData.getTownId() == null) {
                        continue;
                    }
                    
                    // Get town position
                    BlockPos townPos = getTownPosition(townData);
                    if (townPos == null) {
                        continue;
                    }
                    
                    // Check if this is the current town
                    boolean isCurrentTown = townPos.equals(currentPos);
                    
                    // Create TownMapInfo object with structured data
                    TownMapDataResponsePacket.TownMapInfo townInfo = new TownMapDataResponsePacket.TownMapInfo(
                        townData.getTownId(),
                        townData.getTownName(),
                        townPos.getX(),
                        townPos.getY(), 
                        townPos.getZ(),
                        getTownPopulation(townData),
                        getTownVisitCount(townData),
                        getTownLastVisited(townData),
                        isCurrentTown
                    );
                    
                    townMapData.put(townData.getTownId(), townInfo);
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to process town for map data: {}", e.getMessage());
                }
            }
            
            LOGGER.debug("Generated structured map data for {} towns within radius {}", 
                        townMapData.size(), searchRadius);
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate structured map data: {}", e.getMessage());
        }
        
        return townMapData;
    }
    
    /**
     * Send structured map data to client using the sophisticated map modal system.
     */
    private void sendStructuredMapDataToClient(ServerPlayer player, TownInterfaceEntity townEntity, 
                                             Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> mapData, 
                                             int zoomLevel) {
        try {
            // For now, we'll convert the structured data to JSON format for compatibility
            // In the future, we could extend the packet system to send structured data directly
            String jsonMapData = convertStructuredDataToJson(mapData, zoomLevel);
            
            TownMapDataResponsePacket responsePacket = new TownMapDataResponsePacket(
                townEntity.getBlockPos().getX(),
                townEntity.getBlockPos().getY(), 
                townEntity.getBlockPos().getZ(),
                jsonMapData,
                zoomLevel
            );
            
            ModMessages.sendToPlayer(responsePacket, player);
            
            LOGGER.debug("Sent structured map data for {} towns to client", mapData.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to send structured map data to client: {}", e.getMessage());
        }
    }
    
    /**
     * Convert structured map data to JSON format for packet transmission.
     */
    private String convertStructuredDataToJson(Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> mapData, int zoomLevel) {
        StringBuilder json = new StringBuilder();
        json.append("{\"towns\":{");
        
        boolean first = true;
        for (Map.Entry<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> entry : mapData.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            TownMapDataResponsePacket.TownMapInfo town = entry.getValue();
            json.append("\"").append(entry.getKey().toString()).append("\":{");
            json.append("\"id\":\"").append(town.townId.toString()).append("\",");
            json.append("\"name\":\"").append(escapeJson(town.name)).append("\",");
            json.append("\"x\":").append(town.x).append(",");
            json.append("\"y\":").append(town.y).append(",");
            json.append("\"z\":").append(town.z).append(",");
            json.append("\"population\":").append(town.population).append(",");
            json.append("\"visitCount\":").append(town.visitCount).append(",");
            json.append("\"lastVisited\":").append(town.lastVisited).append(",");
            json.append("\"isCurrentTown\":").append(town.isCurrentTown);
            json.append("}");
        }
        
        json.append("},\"zoomLevel\":").append(zoomLevel);
        json.append(",\"generatedAt\":").append(System.currentTimeMillis());
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Calculate map search radius based on zoom level.
     */
    private int calculateMapSearchRadius(int zoomLevel) {
        // Larger radius for lower zoom levels to show more towns
        int baseRadius = 1000; // 1km base radius
        return baseRadius * Math.max(1, 5 - zoomLevel); // Increase radius for lower zoom
    }
    
    /**
     * Helper methods for accessing town data.
     */
    private Object getTownManager(ServerLevel level) {
        try {
            // The town manager service handles level access internally
            // Return the level so we can pass it to service methods
            return level;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town manager for level: {}", e.getMessage());
            return null;
        }
    }
    
    private List<Object> findTownsInRadius(Object townManager, BlockPos center, int radius) {
        List<Object> nearbyTowns = new ArrayList<>();
        
        try {
            if (townManager == null) {
                return nearbyTowns;
            }
            
            // townManager is actually the ServerLevel
            ServerLevel level = (ServerLevel) townManager;
            
            // Get all towns from the town manager service
            Map<UUID, Object> allTowns = PlatformServices.getTownManagerService().getAllTowns(level);
            
            LOGGER.debug("findTownsInRadius: Retrieved {} total towns from TownManagerService", allTowns.size());
            
            for (Object townObj : allTowns.values()) {
                try {
                    ITownDataProvider townData = (ITownDataProvider) townObj;
                    BlockPos townPos = getTownPosition(townData);
                    
                    LOGGER.debug("Processing town '{}' at position {}", townData.getTownName(), townPos);
                    
                    if (townPos != null) {
                        // Calculate distance
                        double distance = Math.sqrt(
                            Math.pow(townPos.getX() - center.getX(), 2) + 
                            Math.pow(townPos.getZ() - center.getZ(), 2)
                        );
                        
                        LOGGER.debug("Town '{}' distance: {} (radius: {})", townData.getTownName(), distance, radius);
                        
                        // Include towns within radius
                        if (distance <= radius) {
                            nearbyTowns.add(townObj);
                            LOGGER.debug("Town '{}' INCLUDED in radius", townData.getTownName());
                        } else {
                            LOGGER.debug("Town '{}' EXCLUDED from radius", townData.getTownName());
                        }
                    } else {
                        LOGGER.warn("Town '{}' has null position", townData.getTownName());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to process town for radius check: {}", e.getMessage());
                }
            }
            
            LOGGER.debug("Found {} towns within radius {} of position {}", nearbyTowns.size(), radius, center);
            
        } catch (Exception e) {
            LOGGER.error("Failed to find towns in radius: {}", e.getMessage());
        }
        
        return nearbyTowns;
    }
    
    private BlockPos getTownPosition(ITownDataProvider townData) {
        try {
            // Get the town's position from the town data directly
            // The Town class should have position data
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                // Get position from the town - this may need to be implemented in Town class
                return new BlockPos(town.getX(), town.getY(), town.getZ());
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town position: {}", e.getMessage());
            return null;
        }
    }
    
    private int getTownPopulation(ITownDataProvider townData) {
        try {
            // Get town population from town data
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                return town.getPopulation();
            }
            return 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town population: {}", e.getMessage());
            return 0;
        }
    }
    
    private int getTownVisitCount(ITownDataProvider townData) {
        try {
            // Get visit count from town data
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                return town.getVisitHistory().size(); // Assuming visit history is available
            }
            return 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town visit count: {}", e.getMessage());
            return 0;
        }
    }
    
    private long getTownLastVisited(ITownDataProvider townData) {
        try {
            // Get last visited timestamp from town data  
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                // Get the most recent visit from visit history
                List<ITownDataProvider.VisitHistoryRecord> history = town.getVisitHistory();
                if (!history.isEmpty()) {
                    // Get the last visit record
                    ITownDataProvider.VisitHistoryRecord lastVisit = history.get(history.size() - 1);
                    return lastVisit.getTimestamp();
                }
                return 0; // No visits yet
            }
            return 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town last visited: {}", e.getMessage());
            return 0;
        }
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
        // Delegate to the overloaded method with null targetTownId for coordinate-based lookup
        return processPlatformDataRequest(player, x, y, z, includePlatformConnections, 
                                        includeDestinationTowns, maxRadius, null);
    }
    
    /**
     * Process platform data request with target town ID (UUID-based lookup).
     * This method handles UUID-based town lookups and uses the actual town coordinates.
     */
    public boolean processPlatformDataRequest(Object player, int x, int y, int z, 
                                            boolean includePlatformConnections, 
                                            boolean includeDestinationTowns, 
                                            int maxRadius, String targetTownId) {
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel level = serverPlayer.serverLevel();
            
            UUID townId;
            int actualX = x, actualY = y, actualZ = z;
            
            // Handle UUID-based lookup if targetTownId is provided
            if (targetTownId != null && !targetTownId.isEmpty()) {
                LOGGER.debug("Processing UUID-based platform data request for town ID: {}", targetTownId);
                
                // Convert string to UUID
                try {
                    townId = UUID.fromString(targetTownId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid town ID format: {}", targetTownId);
                    return false;
                }
                
                // Get town by UUID using existing method
                Object town = getTownById(player, targetTownId);
                if (town == null) {
                    LOGGER.warn("Could not find town with ID {} for platform data request", targetTownId);
                    return false;
                }
                
                // Get actual town coordinates
                int[] townPosition = getTownPosition(town);
                if (townPosition != null) {
                    actualX = townPosition[0];
                    actualY = townPosition[1];
                    actualZ = townPosition[2];
                    LOGGER.debug("Found town {} at actual position ({}, {}, {})", targetTownId, actualX, actualY, actualZ);
                } else {
                    LOGGER.warn("Could not get position for town {}, using default coordinates", targetTownId);
                }
                
            } else {
                // Coordinate-based lookup (original behavior)
                townId = findTownIdByPosition(x, y, z);
                if (townId == null) {
                    LOGGER.warn("Could not find town at position ({}, {}, {}) for platform data request", x, y, z);
                    return false;
                }
            }
            
            // Create structured response packet with actual coordinates
            TownPlatformDataResponsePacket response = 
                new TownPlatformDataResponsePacket(actualX, actualY, actualZ, townId, maxRadius);
            
            // Add platform data if requested
            if (includePlatformConnections) {
                generateStructuredPlatformData(level, townId, response);
                LOGGER.debug("Generated structured platform data for town {}", townId);
            }
            
            // Add town info if requested
            if (includeDestinationTowns) {
                generateStructuredTownInfo(level, townId, response);
                LOGGER.debug("Generated structured town info for town {}", townId);
            }
            
            // Send packet using platform services
            LOGGER.warn("SERVER PLATFORM DATA SEND: Sending TownPlatformDataResponsePacket to client - townId: {}, platforms: {}, townInfo: {}", 
                       response.getTownId(), response.getPlatforms().size(), response.getTownInfo() != null ? response.getTownInfo().name : "null");
            
            try {
                PlatformServices.getNetworkHelper().sendToClient(response, serverPlayer);
                LOGGER.warn("SERVER PLATFORM DATA SEND: Successfully sent TownPlatformDataResponsePacket to client");
            } catch (Exception e) {
                LOGGER.error("SERVER PLATFORM DATA SEND: Failed to send TownPlatformDataResponsePacket to client: {}", e.getMessage());
                e.printStackTrace();
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to process platform data request (UUID-based) for town {}: {}", targetTownId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Generate destination town data as JSON string for sophisticated map display.
     * This method finds all towns within the specified radius and formats them for the client cache.
     */
    private String generateDestinationTownData(ServerLevel level, BlockPos centerPos, int maxRadius) {
        try {
            // Use platform service to get TownManager
            ITownManagerService townManagerService = PlatformServices.getTownManagerService();
            
            if (townManagerService == null) {
                LOGGER.debug("TownManagerService not available, returning empty town data");
                return "{}";
            }
            
            // Get all towns from TownManagerService
            Map<UUID, Object> allTowns = townManagerService.getAllTowns(level);
            
            if (allTowns == null || allTowns.isEmpty()) {
                LOGGER.debug("getAllTowns returned empty data");
                return "{}";
            }
            
            // Build JSON manually for simplicity (could use Gson if available)
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            boolean first = true;
            
            for (Object town : allTowns.values()) {
                try {
                    // Get town position through ITownDataProvider
                    if (!(town instanceof com.quackers29.businesscraft.api.ITownDataProvider)) {
                        continue;
                    }
                    
                    com.quackers29.businesscraft.api.ITownDataProvider townData = 
                        (com.quackers29.businesscraft.api.ITownDataProvider) town;
                    
                    // Get town position through ITownDataProvider interface
                    com.quackers29.businesscraft.api.ITownDataProvider.Position townPos = townData.getPosition();
                    if (townPos == null) {
                        LOGGER.warn("Town position is null for town: {}", townData.getTownName());
                        continue;
                    }
                    
                    // Convert to BlockPos for distance calculation
                    BlockPos pos = new BlockPos(townPos.getX(), townPos.getY(), townPos.getZ());
                    
                    // Check distance
                    double distance = Math.sqrt(centerPos.distSqr(pos));
                    if (distance > maxRadius) {
                        continue;
                    }
                    
                    String townName = townData.getTownName();
                    java.util.UUID townId = townData.getTownId();
                    
                    if (townName == null || townId == null) {
                        LOGGER.warn("Town data missing: name={}, id={}", townName, townId);
                        continue;
                    }
                    
                    LOGGER.debug("Successfully processed town '{}' at ({}, {}, {}) - distance: {}", 
                        townName, pos.getX(), pos.getY(), pos.getZ(), (int)distance);
                    
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
                    LOGGER.warn("Failed to process town data for town: {}", 
                        town instanceof ITownDataProvider ? ((ITownDataProvider) town).getTownName() : "unknown", e);
                }
            }
            
            jsonBuilder.append("}");
            String result = jsonBuilder.toString();
            
            LOGGER.debug("Generated destination data with {} towns within {}m radius", 
                        allTowns.size(), maxRadius);
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
                // Parse the JSON data and update the sophisticated map modal
                try {
                    // Find the town ID that corresponds to this position
                    UUID townIdAtPosition = findTownIdByPosition(x, y, z);
                    if (townIdAtPosition != null) {
                        // Parse platform data into structured format
                        Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms = parsePlatformData(platformData);
                        
                        // Parse town info from destination data  
                        TownPlatformDataResponsePacket.TownInfo townInfo = parseTownInfo(destinationData, townIdAtPosition);
                        
                        // Update the modal with the parsed data
                        mapModal.refreshPlatformData(townIdAtPosition, platforms);
                        if (townInfo != null) {
                            mapModal.refreshTownData(townIdAtPosition, townInfo);
                        }
                        
                        LOGGER.debug("Updated town platform UI with {} platforms for town {} at ({}, {}, {})", 
                            platforms.size(), townIdAtPosition, x, y, z);
                        return true;
                    } else {
                        LOGGER.warn("Could not find town ID for position ({}, {}, {})", x, y, z);
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to parse platform data: {}", e.getMessage());
                    return false;
                }
            } else {
                LOGGER.debug("No town map modal open to update with platform data");
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update town platform UI at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return false;
        }
    }

    /**
     * Find town ID by position coordinates using client-side town map cache.
     */
    private UUID findTownIdByPosition(int x, int y, int z) {
        try {
            // Use client-side cache to find town at this position
            com.quackers29.businesscraft.client.cache.ClientTownMapCache cache = 
                com.quackers29.businesscraft.client.cache.ClientTownMapCache.getInstance();
            
            // Get all cached towns and find one at this position
            Map<UUID, com.quackers29.businesscraft.client.cache.ClientTownMapCache.CachedTownData> cachedTowns = 
                cache.getAllTowns();
            
            for (Map.Entry<UUID, com.quackers29.businesscraft.client.cache.ClientTownMapCache.CachedTownData> entry : cachedTowns.entrySet()) {
                com.quackers29.businesscraft.client.cache.ClientTownMapCache.CachedTownData town = entry.getValue();
                if (town.getX() == x && town.getY() == y && town.getZ() == z) {
                    LOGGER.debug("Found town {} at position ({}, {}, {})", town.getName(), x, y, z);
                    return entry.getKey();
                }
            }
            
            LOGGER.debug("No cached town found at position ({}, {}, {})", x, y, z);
        } catch (Exception e) {
            LOGGER.debug("Failed to find town ID by position ({}, {}, {}): {}", x, y, z, e.getMessage());
        }
        return null;
    }

    /**
     * Parse platform data JSON into structured PlatformInfo objects.
     */
    private Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> parsePlatformData(String platformData) {
        Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms = new HashMap<>();
        
        if (platformData == null || platformData.trim().equals("{}") || platformData.trim().isEmpty()) {
            return platforms;
        }
        
        // For now, return empty map as the sophisticated map expects structured data
        // The current implementation uses JSON strings, but the sophisticated map 
        // expects PlatformInfo objects with specific fields
        LOGGER.debug("Platform data parsing not yet implemented for JSON: {}", platformData);
        
        return platforms;
    }

    /**
     * Parse town info from destination data JSON.
     */
    private TownPlatformDataResponsePacket.TownInfo parseTownInfo(String destinationData, UUID townId) {
        if (destinationData == null || destinationData.trim().equals("{}") || destinationData.trim().isEmpty()) {
            return null;
        }
        
        // For now, return null as the sophisticated map expects structured data
        // The current implementation uses JSON strings, but the sophisticated map
        // expects TownInfo objects with specific fields  
        LOGGER.debug("Town info parsing not yet implemented for JSON: {}", destinationData);
        
        return null;
    }
    
    /**
     * Generate structured platform data for the response packet.
     */
    private void generateStructuredPlatformData(net.minecraft.server.level.ServerLevel level, UUID townId, TownPlatformDataResponsePacket response) {
        try {
            // Use platform service to get TownManager
            ITownManagerService townManagerService = PlatformServices.getTownManagerService();
            if (townManagerService == null) {
                LOGGER.debug("TownManagerService not available for platform data generation");
                return;
            }
            
            // Get the town object
            Object townObject = townManagerService.getTown(level, townId);
            if (townObject == null) {
                LOGGER.debug("Town {} not found for platform data generation", townId);
                return;
            }
            
            // Get town data provider
            if (!(townObject instanceof com.quackers29.businesscraft.api.ITownDataProvider)) {
                LOGGER.debug("Town {} does not implement ITownDataProvider", townId);
                return;
            }
            
            com.quackers29.businesscraft.api.ITownDataProvider townData = 
                (com.quackers29.businesscraft.api.ITownDataProvider) townObject;
            
            // Get the town's position to find the TownInterfaceEntity
            ITownDataProvider.Position townPos = townData.getPosition();
            if (townPos == null) {
                LOGGER.debug("Town {} has no position data", townId);
                return;
            }
            
            // Find the TownInterfaceEntity at the town's position
            BlockPos townBlockPos = new BlockPos(townPos.getX(), townPos.getY(), townPos.getZ());
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(townBlockPos);
            
            if (!(blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity)) {
                LOGGER.debug("No TownInterfaceEntity found at town {} position {}", townId, townBlockPos);
                return;
            }
            
            com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterface = 
                (com.quackers29.businesscraft.block.entity.TownInterfaceEntity) blockEntity;
            
            // Get the platforms from the TownInterfaceEntity
            List<com.quackers29.businesscraft.platform.Platform> platforms = townInterface.getPlatforms();
            
            if (platforms == null || platforms.isEmpty()) {
                LOGGER.debug("Town {} has no platforms created yet", townId);
                return;
            }
            
            LOGGER.debug("Found {} platforms for town {}", platforms.size(), townId);
            
            // Convert platforms to structured data
            for (com.quackers29.businesscraft.platform.Platform platform : platforms) {
                try {
                    UUID platformId = platform.getId();
                    String platformName = platform.getName() != null ? platform.getName() : "Platform " + platformId.toString().substring(0, 8);
                    boolean enabled = platform.isEnabled();
                    
                    // Get platform path coordinates
                    BlockPos startPos = platform.getStartPos();
                    BlockPos endPos = platform.getEndPos();
                    
                    if (startPos == null || endPos == null) {
                        LOGGER.debug("Platform {} has no path set (startPos={}, endPos={})", platformId, startPos, endPos);
                        continue;
                    }
                    
                    // Convert BlockPos to int arrays for Enhanced MultiLoader compatibility
                    int[] startCoords = new int[]{startPos.getX(), startPos.getY(), startPos.getZ()};
                    int[] endCoords = new int[]{endPos.getX(), endPos.getY(), endPos.getZ()};
                    
                    // Get enabled destinations (platform system should track these)
                    Set<UUID> enabledDestinations = new HashSet<>(); // TODO: Get actual destinations from platform
                    
                    // Add platform to response packet
                    response.addPlatform(platformId, platformName, enabled, startCoords, endCoords, enabledDestinations);
                    
                    LOGGER.debug("Added platform {} '{}' with path from {} to {}", 
                               platformId, platformName, startPos, endPos);
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to process platform {}: {}", platform.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate structured platform data for town {}: {}", townId, e.getMessage());
        }
    }
    
    /**
     * Generate structured town info for the response packet.
     */
    private void generateStructuredTownInfo(net.minecraft.server.level.ServerLevel level, UUID townId, TownPlatformDataResponsePacket response) {
        try {
            // Use platform service to get TownManager
            ITownManagerService townManagerService = PlatformServices.getTownManagerService();
            if (townManagerService == null) {
                LOGGER.debug("TownManagerService not available for town info generation");
                return;
            }
            
            // Get the town object
            Object townObject = townManagerService.getTown(level, townId);
            if (townObject == null) {
                LOGGER.debug("Town {} not found for town info generation", townId);
                return;
            }
            
            // Get town data provider
            if (!(townObject instanceof com.quackers29.businesscraft.api.ITownDataProvider)) {
                LOGGER.debug("Town {} does not implement ITownDataProvider", townId);
                return;
            }
            
            com.quackers29.businesscraft.api.ITownDataProvider townData = 
                (com.quackers29.businesscraft.api.ITownDataProvider) townObject;
            
            // Extract town information
            String townName = townData.getTownName();
            int population = townData.getPopulation();
            int touristCount = townData.getTouristCount();
            
            // Get actual boundary radius from town (matches main branch behavior)
            int boundaryRadius = 10; // Default fallback
            if (townObject instanceof com.quackers29.businesscraft.town.Town) {
                boundaryRadius = ((com.quackers29.businesscraft.town.Town) townObject).getBoundaryRadius();
            }
            
            // Get town center coordinates
            BlockPos townPos = getTownPosition(townData);
            if (townPos != null) {
                int[] townPosition = new int[]{townPos.getX(), townPos.getY(), townPos.getZ()};
                // Set town info with actual center coordinates
                response.setTownInfo(townName, population, touristCount, boundaryRadius, 
                                   townPosition[0], townPosition[1], townPosition[2]);
                LOGGER.warn("TOWNINFO COORD DEBUG: Generated town info for {} at center ({},{},{}): population={}, tourists={}, boundary={} (calculated from town)", 
                           townName, townPosition[0], townPosition[1], townPosition[2], population, touristCount, boundaryRadius);
            } else {
                // Fallback to original method without coordinates (will use defaults)
                response.setTownInfo(townName, population, touristCount, boundaryRadius);
                LOGGER.warn("Could not get town center for {}, using default coordinates", townName);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate structured town info for town {}: {}", townId, e.getMessage());
        }
    }
    
    /**
     * Update client-side town platform UI with structured data packet.
     * This method handles sophisticated map modal updates with structured PlatformInfo data.
     */
    public boolean updateTownPlatformUIStructured(Object player, int x, int y, int z, Object packet) {
        try {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: updateTownPlatformUIStructured called at ({}, {}, {})", x, y, z);
            
            // Cast to the structured packet type
            if (!(packet instanceof com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket)) {
                LOGGER.warn("FORGE BLOCK ENTITY HELPER: Invalid packet type for structured platform UI update: {}", packet.getClass());
                return false;
            }
            
            com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket structuredPacket = 
                (com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket) packet;
            
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Structured packet received - townId: {}, platforms: {}", 
                       structuredPacket.getTownId(), structuredPacket.getPlatforms().size());
            
            // Get town map modal from current screen
            Minecraft mc = Minecraft.getInstance();
            Screen currentScreen = mc.screen;
            
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Current screen type: {}", 
                       currentScreen != null ? currentScreen.getClass().getSimpleName() : "null");
            
            if (currentScreen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) {
                LOGGER.warn("FORGE BLOCK ENTITY HELPER: TownMapModal detected! Updating with platform data...");
                
                com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
                    (com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) currentScreen;
                
                // Update the modal with structured data directly
                UUID townId = structuredPacket.getTownId();
                Map<UUID, com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket.PlatformInfo> platforms = structuredPacket.getPlatforms();
                com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket.TownInfo townInfo = structuredPacket.getTownInfo();
                
                LOGGER.warn("FORGE BLOCK ENTITY HELPER: Calling mapModal.refreshPlatformData with {} platforms for town {}", 
                           platforms.size(), townId);
                
                // The PlatformInfo class now has all the fields that TownMapModal expects
                // Pass the structured data directly to the sophisticated map
                mapModal.refreshPlatformData(townId, platforms);
                if (townInfo != null) {
                    LOGGER.warn("FORGE BLOCK ENTITY HELPER: Calling mapModal.refreshTownData with townInfo: {}", townInfo.name);
                    mapModal.refreshTownData(townId, townInfo);
                }
                
                LOGGER.warn("FORGE BLOCK ENTITY HELPER: Successfully updated sophisticated map with structured data: {} platforms for town {} at ({}, {}, {})", 
                    platforms.size(), townId, x, y, z);
                return true;
            } else {
                LOGGER.warn("FORGE BLOCK ENTITY HELPER: Town map modal not currently open, structured update skipped. Current screen: {}", 
                           currentScreen != null ? currentScreen.getClass().getSimpleName() : "null");
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("FORGE BLOCK ENTITY HELPER: Failed to update platform UI with structured data at ({}, {}, {}): {}", 
                x, y, z, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // @Override
    public boolean openDestinationsUI(Object blockEntity, Object player, String platformId) {
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Opening destinations UI for platform: {} at block entity: {}", platformId, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                // Get the platform by ID
                UUID platformUUID = UUID.fromString(platformId);
                Platform platform = townInterface.getPlatform(platformUUID);
                
                if (platform == null) {
                    LOGGER.warn("Platform not found with ID: {}", platformId);
                    return false;
                }
                
                // Get all available town destinations from TownManager
                Map<UUID, String> townNames = new HashMap<>();
                Map<UUID, Boolean> enabledState = new HashMap<>();
                Map<UUID, Integer> townDistances = new HashMap<>();
                Map<UUID, String> townDirections = new HashMap<>();
                
                // Get TownManagerService to access all towns
                ITownManagerService townManagerService = PlatformServices.getTownManagerService();
                if (townManagerService != null && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                    try {
                        Map<UUID, Object> allTowns = townManagerService.getAllTowns(serverPlayer.serverLevel());
                        BlockPos currentPos = townInterface.getBlockPos();
                        
                        if (allTowns != null) {
                            for (Map.Entry<UUID, Object> entry : allTowns.entrySet()) {
                                UUID townId = entry.getKey();
                                Object townObj = entry.getValue();
                                
                                if (townObj instanceof com.quackers29.businesscraft.town.Town town) {
                                    // Skip the current town
                                    if (townId.equals(townInterface.getTownId())) continue;
                                    
                                    townNames.put(townId, town.getName());
                                    
                                    // Calculate distance
                                    com.quackers29.businesscraft.api.ITownDataProvider.Position townPos = town.getPosition();
                                    if (townPos != null) {
                                        double distance = currentPos.distManhattan(new BlockPos(townPos.getX(), townPos.getY(), townPos.getZ()));
                                        townDistances.put(townId, (int) distance);
                                        
                                        // Calculate direction
                                        int dx = townPos.getX() - currentPos.getX();
                                        int dz = townPos.getZ() - currentPos.getZ();
                                        String direction = getCardinalDirection(dx, dz);
                                        townDirections.put(townId, direction);
                                    }
                                    
                                    // Get enabled state from platform destinations
                                    enabledState.put(townId, platform.isDestinationEnabled(townId));
                                }
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to get town data for destinations: {}", e.getMessage());
                        // Fall back to empty data
                    }
                }
                
                // Open the destinations screen on client side
                BlockPos blockPos = townInterface.getBlockPos();
                String platformName = "Platform #" + (townInterface.getPlatforms().indexOf(platform) + 1);
                
                LOGGER.info("FORGE BLOCK ENTITY HELPER: Sending RefreshDestinationsPacket for platform {} at position {}", platformName, blockPos);
                
                // Send RefreshDestinationsPacket back to client with the real data
                com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket responsePacket = 
                    new com.quackers29.businesscraft.network.packets.ui.RefreshDestinationsPacket(
                        blockPos.getX(), blockPos.getY(), blockPos.getZ(), platformId, 
                        serializeDestinationData(townNames, enabledState, townDistances, townDirections, platformName));
                
                // Send to the specific player
                PlatformServices.getNetworkHelper().sendToClient(responsePacket, player);
                
                return true;
                
            } catch (Exception e) {
                LOGGER.error("Failed to open destinations UI for platform {}: {}", platformId, e.getMessage());
                e.printStackTrace();
                return false;
            }
        }
        
        LOGGER.warn("Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
        return false;
    }
    
    /**
     * Calculate cardinal direction from coordinate differences
     */
    private String getCardinalDirection(int dx, int dz) {
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? "East" : "West";
        } else {
            return dz > 0 ? "South" : "North";
        }
    }
    
    /**
     * Serialize destination data for RefreshDestinationsPacket
     */
    private String serializeDestinationData(Map<UUID, String> townNames, Map<UUID, Boolean> enabledState, 
                                          Map<UUID, Integer> townDistances, Map<UUID, String> townDirections, 
                                          String platformName) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"platformName\":\"").append(platformName).append("\",\"towns\":[");
        
        boolean first = true;
        for (Map.Entry<UUID, String> entry : townNames.entrySet()) {
            if (!first) sb.append(",");
            UUID townId = entry.getKey();
            sb.append("{\"id\":\"").append(townId.toString()).append("\"");
            sb.append(",\"name\":\"").append(entry.getValue()).append("\"");
            sb.append(",\"enabled\":").append(enabledState.getOrDefault(townId, false));
            sb.append(",\"distance\":").append(townDistances.getOrDefault(townId, 0));
            sb.append(",\"direction\":\"").append(townDirections.getOrDefault(townId, "")).append("\"");
            sb.append("}");
            first = false;
        }
        
        sb.append("]}");
        return sb.toString();
    }
    
    /**
     * Handle RefreshDestinationsPacket on client side to open destinations UI
     */
    public boolean refreshDestinationData(Object player, int x, int y, int z, String platformId, String destinationData) {
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Refreshing destination data for platform {} at [{}, {}, {}]", platformId, x, y, z);
        
        try {
            // Parse the JSON data (simple JSON parsing)
            Map<UUID, String> townNames = new HashMap<>();
            Map<UUID, Boolean> enabledState = new HashMap<>();
            Map<UUID, Integer> townDistances = new HashMap<>();
            Map<UUID, String> townDirections = new HashMap<>();
            String platformName = "Platform";
            
            // Simple JSON parsing (this is basic but functional)
            if (destinationData.contains("\"platformName\":")) {
                int nameStart = destinationData.indexOf("\"platformName\":\"") + 16;
                int nameEnd = destinationData.indexOf("\"", nameStart);
                if (nameEnd > nameStart) {
                    platformName = destinationData.substring(nameStart, nameEnd);
                }
            }
            
            // Parse towns array
            if (destinationData.contains("\"towns\":[")) {
                String townsSection = destinationData.substring(destinationData.indexOf("\"towns\":[") + 9);
                townsSection = townsSection.substring(0, townsSection.lastIndexOf("]"));
                
                // Split by objects (simple approach)
                String[] townObjects = townsSection.split("\\},\\{");
                for (String townObj : townObjects) {
                    townObj = townObj.replace("{", "").replace("}", "");
                    String[] pairs = townObj.split(",");
                    
                    String townIdStr = null, townNameStr = null, directionStr = "";
                    boolean enabled = false;
                    int distance = 0;
                    
                    for (String pair : pairs) {
                        String[] keyValue = pair.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].replace("\"", "").trim();
                            String value = keyValue[1].replace("\"", "").trim();
                            
                            switch (key) {
                                case "id": townIdStr = value; break;
                                case "name": townNameStr = value; break;
                                case "enabled": enabled = Boolean.parseBoolean(value); break;
                                case "distance": distance = Integer.parseInt(value); break;
                                case "direction": directionStr = value; break;
                            }
                        }
                    }
                    
                    if (townIdStr != null && townNameStr != null) {
                        UUID townId = UUID.fromString(townIdStr);
                        townNames.put(townId, townNameStr);
                        enabledState.put(townId, enabled);
                        townDistances.put(townId, distance);
                        townDirections.put(townId, directionStr);
                    }
                }
            }
            
            LOGGER.info("FORGE BLOCK ENTITY HELPER: Parsed {} towns for destinations UI", townNames.size());
            
            // Open the destinations UI on client side
            BlockPos blockPos = new BlockPos(x, y, z);
            UUID platformUUID = UUID.fromString(platformId);
            
            com.quackers29.businesscraft.ui.screens.platform.DestinationsScreenV2.open(
                blockPos, platformUUID, platformName, townNames, enabledState, townDistances, townDirections);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse destination data: {}", e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}