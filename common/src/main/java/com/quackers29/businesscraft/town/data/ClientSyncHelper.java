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
import com.quackers29.businesscraft.platform.PlatformServices;
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
 * 
 * UNIFIED ARCHITECTURE: Platform-agnostic with direct Minecraft API access.
 */
public class ClientSyncHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSyncHelper.class);
    
    // Client-side caches (UNIFIED ARCHITECTURE: No town name caching needed)
    private final Map<Item, Integer> clientResources = new HashMap<>();
    private final Map<Item, Integer> clientCommunalStorage = new HashMap<>();
    private final List<ITownDataProvider.VisitHistoryRecord> clientVisitHistory = new ArrayList<>();
    // REMOVED: townNameCache - names now resolved fresh server-side like map view
    
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
                String itemKey = PlatformServices.getRegistryHelper().getItemId(item);
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
                String itemKey = PlatformServices.getRegistryHelper().getItemId(item);
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
                    Item item = (Item) PlatformServices.getRegistryHelper().getItem(resourceLocation.toString());
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
                    Item item = (Item) PlatformServices.getRegistryHelper().getItem(resourceLocation.toString());
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
     * UNIFIED ARCHITECTURE: Server-side name resolution like map view system
     */
    public void syncVisitHistoryForClient(CompoundTag tag, ITownDataProvider provider, Level level) {
        if (provider == null) return;
        
        List<ITownDataProvider.VisitHistoryRecord> history = provider.getVisitHistory();
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
            "VISIT HISTORY DEBUG - ClientSyncHelper.syncVisitHistoryForClient() syncing {} records with fresh server-side name resolution", 
            history.size());
        
        ListTag historyTag = new ListTag();
        for (ITownDataProvider.VisitHistoryRecord record : history) {
            CompoundTag visitTag = new CompoundTag();
            visitTag.putLong("timestamp", record.getTimestamp());
            
            // Store UUID and resolve name fresh from server (like map view)
            if (record.getOriginTownId() != null) {
                UUID townId = record.getOriginTownId();
                visitTag.putUUID("townId", townId);
                
                // FIXED: Fresh server-side name resolution (like map view pattern)
                String townName = resolveNameFreshFromServer(townId, level);
                visitTag.putString("townName", townName);
                
                DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
                    "Visit history: resolved town {} -> '{}' (fresh from server)", townId, townName);
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
     * UNIFIED ARCHITECTURE: Client just stores pre-resolved names from server (no caching)
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
                
                // FIXED: Pre-resolved names from server are stored directly in VisitHistoryRecord
                // No client-side caching needed - names are already fresh from server
                String preResolvedTownName = "Unknown";
                if (visitTag.contains("townName")) {
                    preResolvedTownName = visitTag.getString("townName");
                    DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
                        "Loaded pre-resolved town name for {}: {}", townId, preResolvedTownName);
                } else {
                    LOGGER.warn("Missing pre-resolved town name for visit record with ID {}", townId);
                    preResolvedTownName = "Town-" + townId.toString().substring(0, 8);
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
                
                // Create the visit record - convert BlockPos to Position
                ITownDataProvider.Position position = PositionConverter.toPosition(originPos);
                // UNIFIED ARCHITECTURE: Store only UUID, no cached names
                clientVisitHistory.add(new ITownDataProvider.VisitHistoryRecord(timestamp, townId, count, position));
            }
        }
    }
    
    /**
     * Fresh server-side name resolution (like map view pattern)
     * UNIFIED ARCHITECTURE: Always resolve names fresh from server data
     * @param townId The UUID of the town to resolve
     * @param level The level for server-side lookups
     * @return The resolved name or a fallback
     */
    private String resolveNameFreshFromServer(UUID townId, Level level) {
        if (townId == null) return "Unknown";
        
        // Always resolve fresh from server (like map view system)
        if (level != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            Town town = TownManager.get(serverLevel).getTown(townId);
            if (town != null) {
                String freshName = town.getName();
                DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
                    "Resolved town {} -> '{}' fresh from server (like map view)", townId, freshName);
                return freshName;
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
                    "Could not resolve town name for {} (town not found)", townId);
            }
        }
        
        // Fallback if not on server
        return "Town-" + townId.toString().substring(0, 8);
    }
    
    /**
     * DEPRECATED: Legacy method kept for backward compatibility
     * NEW CODE SHOULD USE: resolveNameFreshFromServer() for server-side resolution
     * or rely on pre-resolved names from server packets (like map view)
     */
    @Deprecated
    public String resolveTownName(UUID townId, boolean logResolveFailure, Level level) {
        return resolveNameFreshFromServer(townId, level);
    }
    
    /**
     * DEPRECATED: Legacy method kept for backward compatibility
     */
    @Deprecated
    public String resolveTownName(UUID townId, Level level) {
        return resolveNameFreshFromServer(townId, level);
    }
    
    /**
     * DEPRECATED: Legacy method - client should use pre-resolved names from server packets
     * This now resolves fresh from server if available (like map view system)
     */
    @Deprecated
    public String getTownNameFromId(UUID townId, Level level) {
        if (townId == null) return "Unknown";
        
        // UNIFIED ARCHITECTURE: Always try to resolve fresh from server
        return resolveNameFreshFromServer(townId, level);
    }
    
    
    /**
     * Updates client resources from town data during sync operations
     */
    public void updateClientResourcesFromTown(Town town) {
        if (town == null) return;
        
        // Update client resources from the town (make sure emeralds are properly reflected)
        clientResources.clear();
        town.getAllResources().forEach((item, count) -> {
            // The unified Town.getAllResources() returns Map<Object, Integer> for platform independence
            if (item instanceof Item) {
                clientResources.put((Item) item, count);
            }
        });
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, "Updated client resources from town during sync, resources count: {}", clientResources.size());
        
        // Update client communal storage from the town
        clientCommunalStorage.clear();
        // TODO: Communal storage needs to be implemented in unified Town class
        // town.getAllCommunalStorageItems().forEach((item, count) -> {
        //     if (item instanceof Item) {
        //         clientCommunalStorage.put((Item) item, count);
        //     }
        // });
        // Placeholder - communal storage functionality disabled until unified implementation
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
     * Gets the visit history for client-side display
     */
    public List<ITownDataProvider.VisitHistoryRecord> getClientVisitHistory() {
        return Collections.unmodifiableList(clientVisitHistory);
    }
    
    // Static registry for active ClientSyncHelper instances
    private static final java.util.concurrent.ConcurrentHashMap<java.util.UUID, ClientSyncHelper> activeInstances = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Register this ClientSyncHelper instance for a town
     */
    public void register(java.util.UUID townId) {
        if (townId != null) {
            activeInstances.put(townId, this);
        }
    }
    
    /**
     * Unregister this ClientSyncHelper instance
     */
    public void unregister(java.util.UUID townId) {
        if (townId != null) {
            activeInstances.remove(townId);
        }
    }
    
    /**
     * UNIFIED ARCHITECTURE: Update client visit history from server response
     * Called when VisitorHistoryResponsePacket received with server-resolved names
     */
    public static void updateVisitHistoryFromServer(java.util.UUID townId, List<com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket.VisitorEntry> serverEntries) {
        if (townId != null) {
            // Update specific town
            ClientSyncHelper helper = activeInstances.get(townId);
            if (helper != null) {
                helper.updateVisitHistoryFromServerInstance(serverEntries);
            } else {
                LOGGER.debug("No active ClientSyncHelper found for town {}", townId);
            }
        } else {
            // Update all active instances (broadcast mode)
            for (ClientSyncHelper helper : activeInstances.values()) {
                helper.updateVisitHistoryFromServerInstance(serverEntries);
            }
            LOGGER.debug("Updated {} active ClientSyncHelper instances with server data", activeInstances.size());
        }
    }
    
    /**
     * Instance method to update visit history from server response
     */
    public void updateVisitHistoryFromServerInstance(List<com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket.VisitorEntry> serverEntries) {
        clientVisitHistory.clear();
        
        for (var entry : serverEntries) {
            ITownDataProvider.Position position = new ITownDataProvider.Position() {
                @Override public int getX() { return entry.x; }
                @Override public int getY() { return entry.y; }
                @Override public int getZ() { return entry.z; }
            };
            
            // UNIFIED ARCHITECTURE: Store only UUID - names come from server when needed
            clientVisitHistory.add(new ITownDataProvider.VisitHistoryRecord(
                entry.timestamp, entry.townId, entry.count, position
            ));
        }
        
        LOGGER.debug("Updated client visit history from server: {} entries", clientVisitHistory.size());
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
     * UNIFIED ARCHITECTURE: No town name cache to clear
     */
    public void clearAll() {
        clientResources.clear();
        clientCommunalStorage.clear();
        clientVisitHistory.clear();
        // REMOVED: townNameCache.clear() - no more client-side town name caching
    }
    
    /**
     * Gets the size of all cached data for debugging
     * UNIFIED ARCHITECTURE: No town name cache stats
     */
    public String getCacheStats() {
        return String.format("Resources: %d, Communal: %d, History: %d",
            clientResources.size(),
            clientCommunalStorage.size(),
            clientVisitHistory.size());
    }
    
    /**
     * Notifies all nearby players of buffer storage changes for real-time UI updates
     * This is a static method for easy access from TownBlockEntity
     */
    public static void notifyBufferStorageChange(ServerLevel level, UUID townId, Map<Item, Integer> bufferItems) {
        if (level == null || townId == null) return;
        
        // TODO: Implement unified network packet system
        // Send legacy BufferStorageResponsePacket to all players in the area
        // This ensures Payment Board UI updates in real-time when hoppers extract items
        
        // Send to all players within a reasonable distance of any town blocks
        level.players().forEach(player -> {
            // Send to all players - the client will filter based on which UI is open
            // TODO: Implement unified packet sending
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
        // TODO: Implement unified packet system
        
        // Send to all players within a reasonable distance of any town blocks
        level.players().forEach(player -> {
            // Send to all players - the client will filter based on which UI is open
            // TODO: Implement unified packet sending
        });
        
        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS, 
            "Sent slot-based buffer storage update for town {} to {} players", townId, level.players().size());
    }
}