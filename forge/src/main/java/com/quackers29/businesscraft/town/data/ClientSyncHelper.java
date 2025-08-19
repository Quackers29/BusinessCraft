package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.util.PositionConverter;

import java.util.*;

/**
 * Helper class for client-server synchronization and data management.
 * Extracted from TownBlockEntity to improve code organization.
 * 
 * This class handles the complex synchronization of visit history, resources,
 * town name resolution, and client-side caching.
 */
public class ClientSyncHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSyncHelper.class);
    
    // Client-side caches
    private final Map<Item, Integer> clientResources = new HashMap<>();
    private final Map<Item, Integer> clientCommunalStorage = new HashMap<>();
    private final Map<UUID, Map<Item, Integer>> clientPersonalStorage = new HashMap<>();
    private final List<ITownDataProvider.VisitHistoryRecord> clientVisitHistory = new ArrayList<>();
    private final Map<UUID, String> townNameCache = new HashMap<>();
    
    /**
     * Adds resource data to the provided tag for client-side rendering
     * This centralizes our resource serialization logic in one place
     */
    public void syncResourcesForClient(CompoundTag tag, ITownDataProvider provider) {
        if (provider == null) return;
        
        // Create a resources tag
        CompoundTag resourcesTag = new CompoundTag();
        
        // Add all resources to the tag
        provider.getAllResources().forEach((item, count) -> {
            // provider.getAllResources() returns Map<Object, Integer> for platform independence
            if (item instanceof Item) {
                String itemKey = ForgeRegistries.ITEMS.getKey((Item) item).toString();
                resourcesTag.putInt(itemKey, count);
            }
        });
        
        // Add resources tag to the update tag
        tag.put("clientResources", resourcesTag);
        
        // Add communal storage data
        CompoundTag communalTag = new CompoundTag();
        provider.getAllCommunalStorageItems().forEach((item, count) -> {
            // provider.getAllCommunalStorageItems() returns Map<Object, Integer> for platform independence
            if (item instanceof Item) {
                String itemKey = ForgeRegistries.ITEMS.getKey((Item) item).toString();
                communalTag.putInt(itemKey, count);
            }
        });
        tag.put("clientCommunalStorage", communalTag);
    }
    
    /**
     * Loads resources from the provided tag into the client-side cache
     * This centralizes our resource deserialization logic in one place
     */
    public void loadResourcesFromTag(CompoundTag tag) {
        if (tag.contains("clientResources")) {
            CompoundTag resourcesTag = tag.getCompound("clientResources");
            
            // Clear previous resources
            clientResources.clear();
            
            // Load all resources from the tag
            for (String key : resourcesTag.getAllKeys()) {
                try {
                    ResourceLocation resourceLocation = new ResourceLocation(key);
                    Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                    if (item != null && item != Items.AIR) {
                        int count = resourcesTag.getInt(key);
                        clientResources.put(item, count);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading client resource: {}", key, e);
                }
            }
        }
        
        // Load communal storage data
        if (tag.contains("clientCommunalStorage")) {
            CompoundTag communalTag = tag.getCompound("clientCommunalStorage");
            
            // Clear previous communal storage
            clientCommunalStorage.clear();
            
            // Load all communal storage items from the tag
            for (String key : communalTag.getAllKeys()) {
                try {
                    ResourceLocation resourceLocation = new ResourceLocation(key);
                    Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                    if (item != null && item != Items.AIR) {
                        int count = communalTag.getInt(key);
                        clientCommunalStorage.put(item, count);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading client communal storage item: {}", key, e);
                }
            }
        }
    }
    
    /**
     * Adds visit history data to the provided tag for client-side rendering
     */
    public void syncVisitHistoryForClient(CompoundTag tag, ITownDataProvider provider, Level level) {
        if (provider == null) return;
        
        List<ITownDataProvider.VisitHistoryRecord> history = provider.getVisitHistory();
        // Always sync visit history data, even if empty, to keep client informed
        
        ListTag historyTag = new ListTag();
        for (ITownDataProvider.VisitHistoryRecord record : history) {
            CompoundTag visitTag = new CompoundTag();
            visitTag.putLong("timestamp", record.getTimestamp());
            
            // Store both UUID and resolved name for client display
            if (record.getOriginTownId() != null) {
                UUID townId = record.getOriginTownId();
                visitTag.putUUID("townId", townId);
                
                // Resolve town name using the centralized method
                String townName = resolveTownName(townId, true, level);
                visitTag.putString("townName", townName);
            }
            
            visitTag.putInt("count", record.getCount());
            
            // Add origin position
            if (record.getOriginPos() != null && record.getOriginPos() != BlockPos.ZERO) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", record.getOriginPos().getX());
                posTag.putInt("y", record.getOriginPos().getY());
                posTag.putInt("z", record.getOriginPos().getZ());
                visitTag.put("pos", posTag);
            }
            
            historyTag.add(visitTag);
        }
        tag.put("visitHistory", historyTag);
    }
    
    /**
     * Loads visit history from the provided tag into the client-side cache
     */
    public void loadVisitHistoryFromTag(CompoundTag tag) {
        if (tag.contains("visitHistory")) {
            ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
            
            // Clear previous history
            clientVisitHistory.clear();
            
            // Load all history entries
            for (int i = 0; i < historyTag.size(); i++) {
                CompoundTag visitTag = historyTag.getCompound(i);
                
                long timestamp = visitTag.getLong("timestamp");
                int count = visitTag.getInt("count");
                
                // Handle both old and new formats
                UUID townId = null;
                if (visitTag.contains("townId")) {
                    townId = visitTag.getUUID("townId");
                } else {
                    // Fallback to legacy format
                    LOGGER.warn("Found legacy visit history format without townId");
                    continue;
                }
                
                // Store the pre-resolved town name from the server in a client field
                if (visitTag.contains("townName")) {
                    String townName = visitTag.getString("townName");
                    // Only log when a town name is added for the first time
                    if (!townNameCache.containsKey(townId)) {
                        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, "Loaded town name for {}: {}", townId, townName);
                    }
                    // Store the name in a map for client-side lookup
                    townNameCache.put(townId, townName);
                } else {
                    LOGGER.warn("Missing town name for visit record with ID {}", townId);
                    // If no name is provided, use a fallback
                    townNameCache.put(townId, "Town-" + townId.toString().substring(0, 8));
                }
                
                BlockPos originPos = BlockPos.ZERO;
                if (visitTag.contains("pos")) {
                    CompoundTag posTag = visitTag.getCompound("pos");
                    originPos = new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z")
                    );
                }
                
                // Create the visit record - convert BlockPos to Position using converter
                ITownDataProvider.Position position = PositionConverter.toPosition(originPos);
                clientVisitHistory.add(new ITownDataProvider.VisitHistoryRecord(timestamp, townId, count, position));
            }
        }
    }
    
    /**
     * Resolves a town name from its UUID with flexible behavior for client/server contexts
     * @param townId The UUID of the town to resolve
     * @param logResolveFailure Whether to log when resolution fails
     * @param level The level for server-side lookups
     * @return The resolved name or a fallback
     */
    public String resolveTownName(UUID townId, boolean logResolveFailure, Level level) {
        if (townId == null) return "Unknown";
        
        // For server-side or forced server-side lookup
        if (level != null && !level.isClientSide()) {
            if (level instanceof ServerLevel serverLevel) {
                Town town = TownManager.get(serverLevel).getTown(townId);
                if (town != null) {
                    return town.getName();
                } else if (logResolveFailure) {
                    DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, "Could not resolve town name for {}", townId);
                }
            }
            return "Unknown Town";
        }
        
        // For client-side lookup
        if (townNameCache.containsKey(townId)) {
            String cachedName = townNameCache.get(townId);
            if (cachedName != null && !cachedName.isEmpty()) {
                return cachedName;
            }
        }
        
        // Fallback for client-side with no cache
        return "Town-" + townId.toString().substring(0, 8);
    }
    
    /**
     * Helper method to resolve town name from UUID - simplified version for backward compatibility
     */
    public String resolveTownName(UUID townId, Level level) {
        return resolveTownName(townId, false, level);
    }
    
    /**
     * Helper method to get town name from client cache or resolve from server
     */
    public String getTownNameFromId(UUID townId, Level level) {
        if (townId == null) return "Unknown";
        
        return resolveTownName(townId, level != null && !level.isClientSide(), level);
    }
    
    /**
     * Updates the client-side personal storage cache for a player
     * @param playerId UUID of the player
     * @param items Map of items in the player's personal storage
     */
    public void updateClientPersonalStorage(UUID playerId, Map<Item, Integer> items) {
        if (playerId == null) return;
        
        // Clear existing items for this player
        Map<Item, Integer> playerItems = clientPersonalStorage.computeIfAbsent(playerId, k -> new HashMap<>());
        playerItems.clear();
        
        // Add all the new items
        playerItems.putAll(items);
        
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, "Updated client personal storage cache for player {} with {} items", 
            playerId, items.size());
    }
    
    /**
     * Updates client resources from town data during sync operations
     */
    public void updateClientResourcesFromTown(Town town) {
        if (town == null) return;
        
        // Update client resources from the town (make sure emeralds are properly reflected)
        clientResources.clear();
        town.getAllResources().forEach((item, count) -> {
            // The common Town.getAllResources() returns Map<Object, Integer> for platform independence
            if (item instanceof Item) {
                clientResources.put((Item) item, count);
            }
        });
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, "Updated client resources from town during sync, resources count: {}", clientResources.size());
        
        // Update client communal storage from the town
        clientCommunalStorage.clear();
        // TODO: Communal storage needs to be implemented in common Town class
        // town.getAllCommunalStorageItemsForge().forEach((item, count) -> {
        //     clientCommunalStorage.put(item, count);
        // });
        // Placeholder - communal storage functionality disabled
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, "Updated client communal storage from town during sync, storage count: {}", clientCommunalStorage.size());
    }
    
    // Getters for client-side data access
    
    /**
     * Gets the client-side cached resources
     * @return Map of resources
     */
    public Map<Item, Integer> getClientResources() {
        return clientResources;
    }
    
    /**
     * Gets the client-side cached communal storage items
     * @return Map of communal storage items
     */
    public Map<Item, Integer> getClientCommunalStorage() {
        return clientCommunalStorage;
    }
    
    /**
     * Gets the client-side cached personal storage items for a specific player
     * @param playerId UUID of the player
     * @return Map of personal storage items for that player
     */
    public Map<Item, Integer> getClientPersonalStorage(UUID playerId) {
        return clientPersonalStorage.getOrDefault(playerId, Collections.emptyMap());
    }
    
    /**
     * Gets the visit history for client-side display
     */
    public List<ITownDataProvider.VisitHistoryRecord> getClientVisitHistory() {
        return Collections.unmodifiableList(clientVisitHistory);
    }
    
    /**
     * Gets the visit history, choosing between client cache and server data
     */
    public List<ITownDataProvider.VisitHistoryRecord> getVisitHistory(Level level, ITownDataProvider provider) {
        if (level != null && level.isClientSide()) {
            return Collections.unmodifiableList(clientVisitHistory);
        } else {
            if (provider != null) {
                return provider.getVisitHistory();
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * Clears all client-side caches (useful for cleanup)
     */
    public void clearAll() {
        clientResources.clear();
        clientCommunalStorage.clear();
        clientPersonalStorage.clear();
        clientVisitHistory.clear();
        townNameCache.clear();
    }
    
    /**
     * Gets the size of all cached data for debugging
     */
    public String getCacheStats() {
        return String.format("Resources: %d, Communal: %d, Personal: %d, History: %d, Names: %d",
            clientResources.size(),
            clientCommunalStorage.size(),
            clientPersonalStorage.size(),
            clientVisitHistory.size(),
            townNameCache.size());
    }
    
    /**
     * Notifies all nearby players of buffer storage changes for real-time UI updates
     * This is a static method for easy access from TownBlockEntity
     */
    public static void notifyBufferStorageChange(ServerLevel level, UUID townId, Map<Item, Integer> bufferItems) {
        if (level == null || townId == null) return;
        
        // TODO: Migrate BufferStorageResponsePacket to common module
        // Send legacy BufferStorageResponsePacket to all players in the area
        // This ensures Payment Board UI updates in real-time when hoppers extract items
        // com.quackers29.businesscraft.network.packets.storage.BufferStorageResponsePacket packet = 
        //     new com.quackers29.businesscraft.network.packets.storage.BufferStorageResponsePacket(bufferItems);
        
        // Send to all players within a reasonable distance of any town blocks
        level.players().forEach(player -> {
            // Send to all players - the client will filter based on which UI is open
            // TODO: Migrate BufferStorageResponsePacket to common module
            // com.quackers29.businesscraft.network.ModMessages.sendToPlayer(packet, player);
        });
        
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
            "Sent legacy buffer storage update for town {} to {} players", townId, level.players().size());
    }
    
    /**
     * Notifies all nearby players of slot-based buffer storage changes for real-time UI updates
     * This is the new method that preserves exact slot positions
     */
    public static void notifyBufferSlotStorageChange(ServerLevel level, UUID townId, SlotBasedStorage slotStorage) {
        if (level == null || townId == null || slotStorage == null) return;
        
        // Send new BufferSlotStorageResponsePacket to all players in the area
        // This ensures Payment Board UI updates in real-time with exact slot preservation
        // TODO: Migrate BufferSlotStorageResponsePacket to common module
        // com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket packet = 
        //     new com.quackers29.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket(slotStorage);
        
        // Send to all players within a reasonable distance of any town blocks
        level.players().forEach(player -> {
            // Send to all players - the client will filter based on which UI is open
            // TODO: Migrate BufferStorageResponsePacket to common module
            // com.quackers29.businesscraft.network.ModMessages.sendToPlayer(packet, player);
        });
        
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
            "Sent slot-based buffer storage update for town {} to {} players", townId, level.players().size());
    }
} 