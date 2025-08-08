package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.components.TownComponent;
// Using ITownDataProvider.VisitHistoryRecord instead
import com.quackers29.businesscraft.town.service.TownBusinessLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-agnostic Town class for BusinessCraft mod.
 * 
 * This class has been migrated from the Forge module to the common module
 * to enable full Enhanced MultiLoader Template compatibility. All platform-specific
 * operations are abstracted through PlatformServices.
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
    
    // Payment board system (replaces storage systems)
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
     */
    public void addResource(String itemId, int count) {
        resources.merge(itemId, count, Integer::sum);
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
        // Convert string keys to platform-specific Item objects via PlatformServices
        Map<Object, Integer> result = new HashMap<>(); 
        for (Map.Entry<String, Integer> entry : resources.entrySet()) {
            Object item = PlatformServices.getRegistryHelper().getItem(entry.getKey());
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
        String itemId = PlatformServices.getRegistryHelper().getItemId(item);
        if (itemId != null) {
            addResource(itemId, count);
        }
    }
    
    @Override
    public int getResourceCount(Object item) {
        String itemId = PlatformServices.getRegistryHelper().getItemId(item);
        return itemId != null ? getResourceCount(itemId) : 0;
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
        return new ArrayList<>(visitHistory);
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
        
        // Increment tourists received counter
        this.touristsReceivedCounter += touristCount;
        
        // Simple population growth - every 10 tourists increases population by 1
        if (this.touristsReceivedCounter >= 10) {
            setPopulation(population + 1);
            this.touristsReceivedCounter = 0; // Reset counter after population increase
            
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                "Town {} reached population milestone! New population: {}", name, population);
        }
        
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
        
        // Payment board
        data.put("paymentBoard", new HashMap<>(paymentBoardData));
        
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
            }
        }
        
        // Load payment board data
        if (data.containsKey("paymentBoard")) {
            town.paymentBoardData.putAll((Map<String, Object>) data.get("paymentBoard"));
        }
        
        return town;
    }
    
    /**
     * Mark town data as dirty for persistence.
     * This is a placeholder - actual dirty marking will be handled by TownManager.
     */
    @Override
    public void markDirty() {
        // The TownManager will handle actual dirty marking through its persistence layer
        // This method exists for consistency with the existing codebase patterns
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