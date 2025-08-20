package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.components.TownComponent;
// Using ITownDataProvider.VisitHistoryRecord instead
import com.quackers29.businesscraft.town.service.TownBusinessLogic;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
import com.quackers29.businesscraft.town.data.RewardSource;
import com.quackers29.businesscraft.town.data.RewardEntry;
import com.quackers29.businesscraft.town.data.ClaimStatus;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-agnostic Town class for BusinessCraft mod.
 * 
 * This class has been migrated from the Forge module to the common module
 * to enable full Enhanced MultiLoader Template compatibility. All platform-specific
 * operations use direct Minecraft API access (Unified Architecture).
 * 
 * Key design principles:
 * - Zero platform dependencies (no BlockPos, Item, etc.)
 * - Uses primitive coordinates instead of platform-specific position types
 * - Platform services for all Minecraft API interactions
 * - NBT-compatible serialization format for cross-platform saves
 * 
 * Enhanced MultiLoader approach: This common business logic works identically
 * on both Forge and Fabric platforms.
 */
public class Town implements ITownDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Town.class);
    
    // Core identity data (platform-agnostic)
    private final UUID id;
    private final int[] position; // [x, y, z] - replaces BlockPos
    private String name;
    
    // Economy and resource management (using platform services)
    private final Map<String, Integer> resources = new ConcurrentHashMap<>(); // item ID string -> count
    private int population;
    private int touristCount = 0;
    private boolean touristSpawningEnabled = true;
    
    // Tourism and visitor tracking
    private final Map<UUID, Integer> visitors = new ConcurrentHashMap<>();
    private int touristsReceivedCounter = 0;
    private final List<ITownDataProvider.VisitHistoryRecord> visitHistory = Collections.synchronizedList(new ArrayList<>());
    private static final int MAX_HISTORY_SIZE = 50;
    
    // Platform management
    private int[] pathStart; // [x, y, z] - replaces BlockPos
    private int[] pathEnd;   // [x, y, z] - replaces BlockPos  
    private int searchRadius = 10;
    
    // UNIFIED ARCHITECTURE: Direct payment board ownership (enables natural database queries)
    private final TownPaymentBoard paymentBoard = new TownPaymentBoard();
    
    // Legacy payment board data (for migration compatibility)
    private final Map<String, Object> paymentBoardData = new ConcurrentHashMap<>();
    
    // Business logic service reference
    private transient TownBusinessLogic businessLogic;
    
    /**
     * Create a new Town with platform-agnostic coordinates.
     * 
     * @param id Unique town identifier
     * @param x X coordinate in world space
     * @param y Y coordinate in world space 
     * @param z Z coordinate in world space
     * @param name Display name for the town
     */
    public Town(UUID id, int x, int y, int z, String name) {
        this.id = id;
        this.position = new int[]{x, y, z};
        this.name = name;
        this.population = ConfigLoader.defaultStartingPopulation;
        this.businessLogic = new TownBusinessLogic();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Created new Town: {} at ({}, {}, {}) with population: {}", 
            name, x, y, z, population);
    }
    
    // ================================
    // Core Identity Methods
    // ================================
    
    public UUID getId() { 
        return id; 
    }
    
    // Internal method for coordinate access
    public int[] getPositionArray() { 
        return position.clone(); // Return copy to prevent mutation
    }
    
    public int getX() { return position[0]; }
    public int getY() { return position[1]; }  
    public int getZ() { return position[2]; }
    
    public String getName() { 
        return name; 
    }
    
    public void setName(String name) {
        this.name = name;
        markDirty();
    }
    
    // ================================
    // Resource Management (Platform-Agnostic)
    // ================================
    
    /**
     * Add resources using item resource location string.
     * Platform modules convert Items to/from resource location strings.
     * Handles bread-to-population conversion matching main branch behavior.
     */
    public void addResource(String itemId, int count) {
        resources.merge(itemId, count, Integer::sum);
        
        // Handle bread-to-population conversion (matches main branch TownEconomyComponent logic)
        if ("minecraft:bread".equals(itemId) && count > 0) {
            int breadCount = resources.get("minecraft:bread");
            int popToAdd = breadCount / ConfigLoader.breadPerPop;
            
            if (popToAdd > 0) {
                // Consume the bread used for population (matches main branch logic)
                resources.put("minecraft:bread", breadCount - (popToAdd * ConfigLoader.breadPerPop));
                if (resources.get("minecraft:bread") == 0) {
                    resources.remove("minecraft:bread");
                }
                
                this.population += popToAdd;
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                    "Town {} population increased by {} to {} (consumed {} bread)", 
                    name, popToAdd, population, popToAdd * ConfigLoader.breadPerPop);
            }
        }
        
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
            "Town {} added {} x{} (total: {})", name, itemId, count, resources.get(itemId));
    }
    
    /**
     * Get resource count by item resource location string.
     */
    public int getResourceCount(String itemId) {
        return resources.getOrDefault(itemId, 0);
    }
    
    /**
     * Remove resources from town storage.
     * 
     * @param itemId Item resource location string
     * @param count Amount to remove
     * @return true if sufficient resources were available and removed
     */
    public boolean removeResource(String itemId, int count) {
        int currentCount = resources.getOrDefault(itemId, 0);
        if (currentCount >= count) {
            resources.put(itemId, currentCount - count);
            if (resources.get(itemId) == 0) {
                resources.remove(itemId);
            }
            markDirty();
            return true;
        }
        return false;
    }
    
    /**
     * Get all resources as platform-agnostic map.
     * Keys are item resource location strings, values are counts.
     */
    public Map<String, Integer> getAllResourcesAsStrings() {
        return new HashMap<>(resources);
    }
    
    // ITownDataProvider interface implementation
    @Override
    public Map<Object, Integer> getAllResources() {
        // Convert string keys to Items via direct registry access (Unified Architecture)
        Map<Object, Integer> result = new HashMap<>(); 
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(entry.getKey()));
            if (item != null) {
                result.put(item, entry.getValue());
            }
        }
        return result;
    }
    
    // Additional ITownDataProvider interface methods
    @Override
    public UUID getTownId() { return id; }
    
    @Override
    public String getTownName() { return name; }
    
    @Override
    public int getBreadCount() {
        return getResourceCount("minecraft:bread");
    }
    
    @Override
    public void addResource(Object item, int count) {
        if (item instanceof Item minecraftItem) {
            String itemId = BuiltInRegistries.ITEM.getKey(minecraftItem).toString();
            addResource(itemId, count); // This will call the bread-to-population logic
        }
    }
    
    @Override
    public int getResourceCount(Object item) {
        if (item instanceof Item minecraftItem) {
            String itemId = BuiltInRegistries.ITEM.getKey(minecraftItem).toString();
            return getResourceCount(itemId);
        }
        return 0;
    }
    
    // Communal storage - redirect to payment board for now
    @Override
    public boolean addToCommunalStorage(Object item, int count) {
        addResource(item, count);
        return true;
    }
    
    @Override
    public int getCommunalStorageCount(Object item) {
        return getResourceCount(item);
    }
    
    @Override
    public Map<Object, Integer> getAllCommunalStorageItems() {
        return getAllResources();
    }
    
    // Personal storage - placeholder implementation
    @Override
    public boolean addToPersonalStorage(UUID playerId, Object item, int count) {
        // TODO: Implement personal storage system
        return false;
    }
    
    @Override
    public int getPersonalStorageCount(UUID playerId, Object item) {
        // TODO: Implement personal storage system
        return 0;
    }
    
    @Override
    public Map<Object, Integer> getPersonalStorageItems(UUID playerId) {
        // TODO: Implement personal storage system
        return new HashMap<>();
    }
    
    @Override
    public int getMaxTourists() {
        if (businessLogic == null) {
            businessLogic = new TownBusinessLogic();
        }
        return businessLogic.calculateMaxTourists(population);
    }
    
    @Override
    public boolean canAddMoreTourists() {
        return touristCount < getMaxTourists();
    }
    
    @Override
    public boolean canSpawnTourists() {
        return touristSpawningEnabled && canAddMoreTourists();
    }
    
    // Position interface implementation
    private static class TownPosition implements ITownDataProvider.Position {
        private final int x, y, z;
        
        public TownPosition(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
        
        @Override
        public int getX() { return x; }
        
        @Override
        public int getY() { return y; }
        
        @Override
        public int getZ() { return z; }
    }
    
    @Override
    public ITownDataProvider.Position getPosition() {
        return new TownPosition(position[0], position[1], position[2]);
    }
    
    @Override
    public ITownDataProvider.Position getPathStart() {
        return pathStart != null ? new TownPosition(pathStart[0], pathStart[1], pathStart[2]) : null;
    }
    
    @Override
    public void setPathStart(ITownDataProvider.Position pos) {
        if (pos != null) {
            setPathStart(pos.getX(), pos.getY(), pos.getZ());
        } else {
            this.pathStart = null;
            markDirty();
        }
    }
    
    @Override
    public ITownDataProvider.Position getPathEnd() {
        return pathEnd != null ? new TownPosition(pathEnd[0], pathEnd[1], pathEnd[2]) : null;
    }
    
    @Override
    public void setPathEnd(ITownDataProvider.Position pos) {
        if (pos != null) {
            setPathEnd(pos.getX(), pos.getY(), pos.getZ());
        } else {
            this.pathEnd = null;
            markDirty();
        }
    }
    
    @Override
    public void addVisitor(UUID fromTownId) {
        visitors.merge(fromTownId, 1, Integer::sum);
        
        // Increment counter for tourists received
        touristsReceivedCounter++;
        
        // Check if population growth from tourism is enabled and if we should increase population  
        // Use populationPerTourist (default 10) - every 10 tourists increases population by 1
        if (ConfigLoader.populationPerTourist > 0 && 
            touristsReceivedCounter >= ConfigLoader.populationPerTourist) {
            // Increase population by 1
            setPopulation(population + 1);
            
            // Reset counter, subtracting any excess tourists
            touristsReceivedCounter -= ConfigLoader.populationPerTourist;
            
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Town [{}] population increased to {} after receiving {} tourists", 
                name, population, ConfigLoader.populationPerTourist);
        }
        
        markDirty();
    }
    
    @Override
    public int getTotalVisitors() {
        return visitors.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    @Override
    public void recordVisit(UUID originTownId, int count, ITownDataProvider.Position originPos) {
        // Convert Position to coordinates and call our internal method
        int[] coords = new int[]{originPos.getX(), originPos.getY(), originPos.getZ()};
        addVisitToHistory(originTownId, count, coords);
    }
    
    @Override
    public List<ITownDataProvider.VisitHistoryRecord> getVisitHistory() {
        // Return copy to prevent external mutation
        List<ITownDataProvider.VisitHistoryRecord> result = new ArrayList<>(visitHistory);
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
            "VISIT HISTORY DEBUG - Town {} getVisitHistory() returning {} records", 
            name, result.size());
        for (int i = 0; i < result.size() && i < 3; i++) { // Log first 3 records
            ITownDataProvider.VisitHistoryRecord record = result.get(i);
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                "  Record {}: {} tourists from {} at timestamp {}", 
                i, record.getCount(), record.getOriginTownId(), record.getTimestamp());
        }
        return result;
    }
    
    // ================================
    // Population and Tourism Management
    // ================================
    
    public int getPopulation() { 
        return population; 
    }
    
    public void setPopulation(int population) {
        this.population = Math.max(1, population); // Minimum population of 1
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
            "Town {} population set to: {}", name, this.population);
    }
    
    public int getTouristCount() { 
        return touristCount; 
    }
    
    public void setTouristCount(int count) {
        this.touristCount = Math.max(0, count);
        markDirty();
    }
    
    /**
     * Adds a tourist to the town count
     */
    public void addTourist() {
        this.touristCount++;
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
            "Tourist added to town {}. New count: {}", name, touristCount);
    }
    
    /**
     * Removes a tourist from the town count
     */
    public void removeTourist() {
        if (this.touristCount > 0) {
            this.touristCount--;
            markDirty();
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Tourist removed from town {}. New count: {}", name, touristCount);
        }
    }
    
    public boolean isTouristSpawningEnabled() { 
        return touristSpawningEnabled; 
    }
    
    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
            "Town {} tourist spawning: {}", name, enabled ? "enabled" : "disabled");
    }
    
    // ================================
    // Tourism Business Logic Integration
    // ================================
    
    /**
     * Process tourist arrival and calculate rewards.
     * Uses simplified reward calculation for now.
     */
    public void processTouristArrival(int touristCount, UUID originTownId, int[] originCoordinates) {
        // Add visit to history
        addVisitToHistory(originTownId, touristCount, originCoordinates);
        
        // Simplified reward calculation - 2 emeralds per tourist
        int emeraldReward = touristCount * 2;
        addResource("minecraft:emerald", emeraldReward);
        
        // Note: Population increase is handled per-tourist in addVisitor() method
        
        markDirty();
    }
    
    // ================================  
    // Visit History Management
    // ================================
    
    /**
     * Add a visit to the town's history using platform-agnostic coordinates.
     */
    public void addVisitToHistory(UUID originTownId, int count, int[] originCoordinates) {
        ITownDataProvider.Position originPos = new TownPosition(
            originCoordinates[0], originCoordinates[1], originCoordinates[2]);
        
        ITownDataProvider.VisitHistoryRecord record = new ITownDataProvider.VisitHistoryRecord(
            System.currentTimeMillis(), originTownId, count, originPos
        );
        
        visitHistory.add(record);
        
        // Maintain maximum history size
        while (visitHistory.size() > MAX_HISTORY_SIZE) {
            visitHistory.remove(0);
        }
        
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
            "Town {} recorded visit: {} tourists from town {} at ({}, {}, {})", 
            name, count, originTownId, originCoordinates[0], originCoordinates[1], originCoordinates[2]);
    }
    
    // Internal method for getting visit history
    public List<ITownDataProvider.VisitHistoryRecord> getVisitHistoryInternal() {
        return new ArrayList<>(visitHistory); // Return copy to prevent external mutation
    }
    
    public void clearVisitHistory() {
        visitHistory.clear();
        markDirty();
    }
    
    // ================================
    // Platform Management (Platform-Agnostic)
    // ================================
    
    // Internal method for coordinate access
    public int[] getPathStartArray() { 
        return pathStart != null ? pathStart.clone() : null; 
    }
    
    public void setPathStart(int x, int y, int z) {
        this.pathStart = new int[]{x, y, z};
        markDirty();
    }
    
    // Internal method for coordinate access
    public int[] getPathEndArray() { 
        return pathEnd != null ? pathEnd.clone() : null; 
    }
    
    public void setPathEnd(int x, int y, int z) {
        this.pathEnd = new int[]{x, y, z};
        markDirty();
    }
    
    public int getSearchRadius() { 
        return searchRadius; 
    }
    
    public void setSearchRadius(int radius) {
        this.searchRadius = Math.max(1, radius);
        markDirty();
    }
    
    // ================================
    // Town Boundary System (Business Logic)
    // ================================
    
    /**
     * Gets the boundary radius for this town based on population.
     * Uses simple 1:1 population to radius ratio.
     */
    public int getBoundaryRadius() {
        // Simple business rule: 1 block radius per population point, minimum 5
        return Math.max(5, population);
    }
    
    /**
     * Check if coordinates are within town boundary.
     */
    public boolean isWithinBoundary(int x, int y, int z) {
        int radius = getBoundaryRadius();
        int dx = x - position[0];
        int dz = z - position[2];
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        return distance <= radius;
    }
    
    // ================================
    // Payment Board System
    // ================================
    
    public Map<String, Object> getPaymentBoardData() {
        return new HashMap<>(paymentBoardData);
    }
    
    public void setPaymentBoardData(String key, Object value) {
        paymentBoardData.put(key, value);
        markDirty();
    }
    
    /**
     * Gets the payment board for this town.
     * 
     * UNIFIED ARCHITECTURE: Direct access enables natural database queries!
     * Now you can call: town.getPaymentBoard().getUnclaimedVisitorRewards()
     * 
     * @return The town's payment board with direct access to all reward data
     */
    public TownPaymentBoard getPaymentBoard() {
        // UNIFIED ARCHITECTURE: Direct ownership - no bridge pattern needed!
        return paymentBoard;
    }
    
    // ================================
    // UNIFIED ARCHITECTURE: Natural Database-Style Queries
    // ================================
    
    /**
     * Gets all unclaimed visitor rewards for this town.
     * 
     * This is the exact natural database query that was requested!
     * Example usage: town.getUnclaimedVisitorRewards()
     * 
     * @return List of unclaimed tourist arrival rewards
     */
    public List<RewardEntry> getUnclaimedVisitorRewards() {
        return paymentBoard.getRewards().stream()
            .filter(r -> r.getSource() == RewardSource.TOURIST_ARRIVAL)
            .filter(r -> r.getStatus() == ClaimStatus.UNCLAIMED)
            .toList();
    }
    
    /**
     * Gets all unclaimed rewards from a specific source.
     * Example: town.getUnclaimedRewards(RewardSource.MILESTONE)
     * 
     * @param source The reward source to filter by
     * @return List of unclaimed rewards from that source
     */
    public List<RewardEntry> getUnclaimedRewards(RewardSource source) {
        return paymentBoard.getRewards().stream()
            .filter(r -> r.getSource() == source)
            .filter(r -> r.getStatus() == ClaimStatus.UNCLAIMED)
            .toList();
    }
    
    /**
     * Gets total unclaimed emerald value across all rewards.
     * Example: int emeralds = town.getTotalUnclaimedEmeralds()
     * 
     * @return Total emerald count from all unclaimed rewards
     */
    public int getTotalUnclaimedEmeralds() {
        return paymentBoard.getRewards().stream()
            .filter(r -> r.getStatus() == ClaimStatus.UNCLAIMED)
            .flatMap(r -> r.getRewards().stream())
            .filter(stack -> stack.getItem().toString().contains("emerald"))
            .mapToInt(ItemStack::getCount)
            .sum();
    }
    
    /**
     * Gets all rewards received from a specific origin town.
     * Example: town.getRewardsFromTown(originTownId)
     * 
     * @param originTownId UUID of the origin town
     * @return List of rewards from that town
     */
    public List<RewardEntry> getRewardsFromTown(UUID originTownId) {
        return paymentBoard.getRewards().stream()
            .filter(r -> r.getMetadata().containsKey("originTown"))
            .filter(r -> r.getMetadata().get("originTown").equals(originTownId.toString()))
            .toList();
    }
    
    // ================================
    // Data Persistence Support
    // ================================
    
    /**
     * Convert town to platform-agnostic data map for persistence.
     * Uses only NBT-compatible data types.
     */
    public Map<String, Object> toDataMap() {
        Map<String, Object> data = new HashMap<>();
        
        // Core identity
        data.put("id", id.toString());
        data.put("position", position.clone());
        data.put("name", name);
        
        // Economy and population
        data.put("resources", new HashMap<>(resources));
        data.put("population", population);
        data.put("touristCount", touristCount);
        data.put("touristSpawningEnabled", touristSpawningEnabled);
        data.put("touristsReceivedCounter", touristsReceivedCounter);
        
        // Platform management
        if (pathStart != null) data.put("pathStart", pathStart.clone());
        if (pathEnd != null) data.put("pathEnd", pathEnd.clone());
        data.put("searchRadius", searchRadius);
        
        // Visit history (convert to serializable format)
        List<Map<String, Object>> historyData = new ArrayList<>();
        for (ITownDataProvider.VisitHistoryRecord record : visitHistory) {
            Map<String, Object> recordData = new HashMap<>();
            recordData.put("timestamp", record.getTimestamp());
            recordData.put("originTownId", record.getOriginTownId() != null ? record.getOriginTownId().toString() : null);
            recordData.put("count", record.getCount());
            recordData.put("originCoordinates", new int[]{record.getOriginPos().getX(), record.getOriginPos().getY(), record.getOriginPos().getZ()});
            historyData.add(recordData);
        }
        data.put("visitHistory", historyData);
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
            "PERSISTENCE DEBUG - Saving {} visit history records for town {}", 
            historyData.size(), name);
        
        // UNIFIED ARCHITECTURE: Serialize payment board directly
        data.put("paymentBoard", paymentBoard.toNBT());
        
        // Legacy compatibility
        data.put("paymentBoardData", new HashMap<>(paymentBoardData));
        
        return data;
    }
    
    /**
     * Create Town from platform-agnostic data map.
     * Used for loading from persistent storage.
     */
    @SuppressWarnings("unchecked")
    public static Town fromDataMap(Map<String, Object> data) {
        UUID id = UUID.fromString((String) data.get("id"));
        int[] position = (int[]) data.get("position");
        String name = (String) data.get("name");
        
        Town town = new Town(id, position[0], position[1], position[2], name);
        
        // Load economy and population data
        if (data.containsKey("resources")) {
            town.resources.putAll((Map<String, Integer>) data.get("resources"));
        }
        town.population = (Integer) data.getOrDefault("population", ConfigLoader.defaultStartingPopulation);
        town.touristCount = (Integer) data.getOrDefault("touristCount", 0);
        town.touristSpawningEnabled = (Boolean) data.getOrDefault("touristSpawningEnabled", true);
        town.touristsReceivedCounter = (Integer) data.getOrDefault("touristsReceivedCounter", 0);
        
        // Load platform management data
        if (data.containsKey("pathStart")) {
            town.pathStart = ((int[]) data.get("pathStart")).clone();
        }
        if (data.containsKey("pathEnd")) {
            town.pathEnd = ((int[]) data.get("pathEnd")).clone();
        }
        town.searchRadius = (Integer) data.getOrDefault("searchRadius", 10);
        
        // Load visit history
        if (data.containsKey("visitHistory")) {
            List<Map<String, Object>> historyData = (List<Map<String, Object>>) data.get("visitHistory");
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                "PERSISTENCE DEBUG - Loading {} visit history records for town {}", 
                historyData.size(), town.name);
            for (Map<String, Object> recordData : historyData) {
                UUID originTownId = recordData.get("originTownId") != null ? 
                    UUID.fromString((String) recordData.get("originTownId")) : null;
                int[] originCoords = (int[]) recordData.get("originCoordinates");
                
                ITownDataProvider.Position originPos = new TownPosition(
                    originCoords[0], originCoords[1], originCoords[2]);
                
                ITownDataProvider.VisitHistoryRecord record = new ITownDataProvider.VisitHistoryRecord(
                    (Long) recordData.get("timestamp"),
                    originTownId,
                    (Integer) recordData.get("count"),
                    originPos
                );
                town.visitHistory.add(record);
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                    "PERSISTENCE DEBUG - Loaded visit record: {} tourists from {} at timestamp {}", 
                    record.getCount(), originTownId, record.getTimestamp());
            }
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                "PERSISTENCE DEBUG - No visitHistory data found for town {}", town.name);
        }
        
        // UNIFIED ARCHITECTURE: Load payment board directly
        if (data.containsKey("paymentBoard")) {
            Object paymentBoardData = data.get("paymentBoard");
            if (paymentBoardData instanceof CompoundTag) {
                // New unified format
                town.paymentBoard.fromNBT((CompoundTag) paymentBoardData);
            } else if (paymentBoardData instanceof Map) {
                // Legacy compatibility - load old format into legacy map
                town.paymentBoardData.putAll((Map<String, Object>) paymentBoardData);
            }
        }
        
        // Legacy compatibility
        if (data.containsKey("paymentBoardData")) {
            town.paymentBoardData.putAll((Map<String, Object>) data.get("paymentBoardData"));
        }
        
        return town;
    }
    
    /**
     * Mark town data as dirty for persistence.
     * Notifies all active TownManager instances to save changes.
     */
    @Override
    public void markDirty() {
        // UNIFIED ARCHITECTURE FIX: Properly mark persistence as dirty
        // Since Town doesn't have direct access to ServerLevel, mark all active TownManagers
        Collection<TownManager> managers = TownManager.getAllInstances();
        for (TownManager manager : managers) {
            manager.markDirty();
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
            "Town {} marked dirty across {} TownManager instances", name, managers.size());
    }
    
    @Override
    public String toString() {
        return String.format("Town{id=%s, name='%s', position=(%d,%d,%d), population=%d}", 
            id, name, position[0], position[1], position[2], population);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Town town = (Town) obj;
        return Objects.equals(id, town.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}