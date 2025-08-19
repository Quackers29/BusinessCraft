package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.data.ITownPersistence;
import com.quackers29.businesscraft.town.service.TownBusinessLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-agnostic TownManager for BusinessCraft mod.
 * 
 * This class has been migrated from the Forge module to the common module
 * to enable full Enhanced MultiLoader Template compatibility. All platform-specific
 * operations are abstracted through PlatformServices.
 * 
 * Key design changes from Forge version:
 * - Uses DataStorageHelper instead of direct SavedData
 * - Platform-agnostic level references (Object instead of ServerLevel)
 * - Coordinates stored as primitive arrays instead of BlockPos
 * - Item operations through RegistryHelper string-based API
 * 
 * Enhanced MultiLoader approach: This common business logic works identically
 * on both Forge and Fabric platforms.
 */
public class TownManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownManager");
    private static final String PERSISTENCE_NAME = "businesscraft_towns";
    
    // Platform-agnostic level reference
    private final Object level; // ServerLevel (platform-specific)
    private final ITownPersistence persistence;
    private final TownBusinessLogic businessLogic;
    
    // In-memory town storage
    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();
    
    // Static instances per level (platform-agnostic)
    private static final Map<Object, TownManager> INSTANCES = new ConcurrentHashMap<>();
    
    /**
     * Private constructor - use get(ServerLevel) to obtain instances.
     */
    private TownManager(Object level) {
        this.level = level;
        this.businessLogic = new TownBusinessLogic();
        
        // Create persistence layer using platform services
        this.persistence = createPersistence(level);
        
        // Load existing town data
        loadTowns();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, 
            "TownManager initialized for level with {} towns loaded", towns.size());
    }
    
    /**
     * Get or create TownManager instance for a level.
     * 
     * @param level ServerLevel (platform-specific)
     * @return TownManager instance for this level
     */
    public static TownManager get(Object level) {
        return INSTANCES.computeIfAbsent(level, TownManager::new);
    }
    
    /**
     * Remove TownManager instance for a level (cleanup on server stop).
     */
    public static void remove(Object level) {
        TownManager manager = INSTANCES.remove(level);
        if (manager != null) {
            manager.saveTowns(); // Ensure data is saved before cleanup
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, 
                "TownManager removed and saved for level");
        }
    }
    
    /**
     * Get all active TownManager instances.
     * Used for cleanup and global operations.
     */
    public static Collection<TownManager> getAllInstances() {
        return new ArrayList<>(INSTANCES.values());
    }
    
    // ================================
    // Core Town Management
    // ================================
    
    /**
     * Create a new town at the specified coordinates.
     * 
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     * @param z Z coordinate in world space
     * @param name Town display name
     * @return Created Town instance, or null if location is invalid
     */
    public Town createTown(int x, int y, int z, String name) {
        // Validate town placement using business logic
        if (!isValidTownLocation(x, y, z, getAllTowns(), ConfigLoader.minDistanceBetweenTowns)) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER,
                "Town creation failed: Invalid location ({}, {}, {}) - too close to existing town", x, y, z);
            return null;
        }
        
        UUID townId = UUID.randomUUID();
        Town town = new Town(townId, x, y, z, name);
        towns.put(townId, town);
        
        // Mark persistence as dirty (triggers save through platform abstraction)
        persistence.markDirty();
        // NOTE: Enhanced MultiLoader requires explicit save vs main branch's automatic save
        saveTowns(); // Ensure immediate persistence for reliability
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER,
            "Created new town: {} at ({}, {}, {}) with ID: {}", name, x, y, z, townId);
        
        return town;
    }
    
    /**
     * Check if a town can be placed at the specified coordinates.
     * Used by platform services for validation.
     * 
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     * @param z Z coordinate in world space
     * @return true if location is valid for town placement
     */
    public boolean canPlaceTownAt(int x, int y, int z) {
        return isValidTownLocation(x, y, z, getAllTowns(), ConfigLoader.minDistanceBetweenTowns);
    }
    
    /**
     * Get error message for town placement at specified coordinates.
     * Used by platform services for user feedback.
     * 
     * @param x X coordinate in world space
     * @param y Y coordinate in world space
     * @param z Z coordinate in world space
     * @return Error message, or null if location is valid
     */
    public String getTownPlacementError(int x, int y, int z) {
        if (isValidTownLocation(x, y, z, getAllTowns(), ConfigLoader.minDistanceBetweenTowns)) {
            return null; // No error, location is valid
        }
        
        // Find the closest town to provide specific error message
        Town closestTown = null;
        double closestDistance = Double.MAX_VALUE;
        
        for (Town existingTown : getAllTowns()) {
            int[] existingPos = existingTown.getPositionArray();
            double distance = calculateDistance(x, y, z, existingPos);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestTown = existingTown;
            }
        }
        
        if (closestTown != null) {
            return String.format("Too close to existing town '%s' (%.1f blocks away, minimum %d required)", 
                closestTown.getName(), closestDistance, ConfigLoader.minDistanceBetweenTowns);
        }
        
        return "Invalid location for town placement";
    }
    
    /**
     * Get town by UUID.
     */
    public Town getTown(UUID townId) {
        return towns.get(townId);
    }
    
    /**
     * Get all towns in this level.
     */
    public Collection<Town> getAllTowns() {
        return new ArrayList<>(towns.values());
    }
    
    /**
     * Get towns by name (case-insensitive search).
     */
    public List<Town> getTownsByName(String name) {
        return towns.values().stream()
            .filter(town -> town.getName().toLowerCase().contains(name.toLowerCase()))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Remove a town from the world.
     * 
     * @param townId UUID of town to remove
     * @return true if town was removed, false if not found
     */
    public boolean removeTown(UUID townId) {
        Town removed = towns.remove(townId);
        if (removed != null) {
            persistence.markDirty();
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER,
                "Removed town: {} (ID: {})", removed.getName(), townId);
            return true;
        }
        return false;
    }
    
    /**
     * Clear all towns from this level.
     * Used for debug/admin commands.
     */
    public void clearAllTowns() {
        int count = towns.size();
        towns.clear();
        persistence.markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER,
            "Cleared {} towns from level", count);
    }
    
    // ================================
    // Town Location and Search
    // ================================
    
    /**
     * Find town at specific coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     * @return Town at those coordinates, or null if none found
     */
    public Town getTownAt(int x, int y, int z) {
        return towns.values().stream()
            .filter(town -> {
                int[] pos = town.getPositionArray();
                return pos[0] == x && pos[1] == y && pos[2] == z;
            })
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Find closest town to given coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     * @return Closest town, or null if no towns exist
     */
    public Town getClosestTown(int x, int y, int z) {
        return towns.values().stream()
            .min((a, b) -> {
                double distA = calculateDistance(x, y, z, a.getPositionArray());
                double distB = calculateDistance(x, y, z, b.getPositionArray());
                return Double.compare(distA, distB);
            })
            .orElse(null);
    }
    
    /**
     * Find all towns within a certain radius of coordinates.
     * 
     * @param x X coordinate
     * @param y Y coordinate  
     * @param z Z coordinate
     * @param radius Search radius in blocks
     * @return List of towns within radius
     */
    public List<Town> getTownsWithinRadius(int x, int y, int z, double radius) {
        return towns.values().stream()
            .filter(town -> calculateDistance(x, y, z, town.getPositionArray()) <= radius)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Calculate 3D distance between coordinates and town position.
     */
    private double calculateDistance(int x, int y, int z, int[] townPos) {
        int dx = x - townPos[0];
        int dy = y - townPos[1];
        int dz = z - townPos[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Check if a location is valid for town placement.
     * Ensures minimum distance from existing towns.
     */
    private boolean isValidTownLocation(int x, int y, int z, Collection<Town> existingTowns, int minDistance) {
        for (Town existingTown : existingTowns) {
            int[] existingPos = existingTown.getPositionArray();
            double distance = calculateDistance(x, y, z, existingPos);
            if (distance < minDistance) {
                return false;
            }
        }
        return true;
    }
    
    // ================================
    // Tourism and Business Logic Integration
    // ================================
    
    /**
     * Process tourist arrival at a town.
     * Delegates to Town's business logic processing.
     * 
     * @param destinationTownId UUID of destination town
     * @param touristCount Number of tourists arriving
     * @param originTownId UUID of origin town (can be null)
     * @param originX Origin X coordinate
     * @param originY Origin Y coordinate
     * @param originZ Origin Z coordinate
     */
    public void processTouristArrival(UUID destinationTownId, int touristCount, 
                                    UUID originTownId, int originX, int originY, int originZ) {
        Town town = getTown(destinationTownId);
        if (town != null) {
            int[] originCoordinates = new int[]{originX, originY, originZ};
            town.processTouristArrival(touristCount, originTownId, originCoordinates);
            persistence.markDirty();
            
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING,
                "Processed {} tourists arriving at town {} from coordinates ({}, {}, {})",
                touristCount, town.getName(), originX, originY, originZ);
        }
    }
    
    /**
     * Get the business logic service for advanced operations.
     */
    public TownBusinessLogic getBusinessLogic() {
        return businessLogic;
    }
    
    // ================================
    // Resource Management (Platform-Agnostic)
    // ================================
    
    /**
     * Add resources to a town using item resource location string.
     * Platform modules convert Items to resource location strings.
     * 
     * @param townId UUID of target town
     * @param itemId Item resource location string (e.g., "minecraft:bread")
     * @param count Amount to add
     * @return true if town exists and resources were added
     */
    public boolean addResourceToTown(UUID townId, String itemId, int count) {
        Town town = getTown(townId);
        if (town != null) {
            town.addResource(itemId, count);
            persistence.markDirty();
            return true;
        }
        return false;
    }
    
    /**
     * Remove resources from a town.
     * 
     * @param townId UUID of target town
     * @param itemId Item resource location string
     * @param count Amount to remove
     * @return true if town exists and sufficient resources were available
     */
    public boolean removeResourceFromTown(UUID townId, String itemId, int count) {
        Town town = getTown(townId);
        if (town != null) {
            boolean success = town.removeResource(itemId, count);
            if (success) {
                persistence.markDirty();
            }
            return success;
        }
        return false;
    }
    
    // ================================
    // Data Persistence (Platform-Agnostic)
    // ================================
    
    /**
     * Create persistence layer using platform services.
     */
    private ITownPersistence createPersistence(Object level) {
        // Use platform services to create appropriate persistence implementation
        return PlatformServices.getDataStorageHelper().createTownPersistence(level, PERSISTENCE_NAME);
    }
    
    /**
     * Load towns from persistence layer.
     */
    private void loadTowns() {
        try {
            Map<String, Object> data = persistence.load();
            
            if (data.containsKey("towns")) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> townsData = (Map<String, Map<String, Object>>) data.get("towns");
                
                for (Map.Entry<String, Map<String, Object>> entry : townsData.entrySet()) {
                    try {
                        UUID townId = UUID.fromString(entry.getKey());
                        Town town = Town.fromDataMap(entry.getValue());
                        towns.put(townId, town);
                    } catch (Exception e) {
                        LOGGER.error("Failed to load town with ID {}: {}", entry.getKey(), e.getMessage());
                    }
                }
                
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER,
                    "Loaded {} towns from persistent storage", towns.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load town data: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save all towns to persistence layer.
     */
    public void saveTowns() {
        try {
            Map<String, Object> data = new HashMap<>();
            Map<String, Map<String, Object>> townsData = new HashMap<>();
            
            for (Map.Entry<UUID, Town> entry : towns.entrySet()) {
                townsData.put(entry.getKey().toString(), entry.getValue().toDataMap());
            }
            
            data.put("towns", townsData);
            persistence.save(data);
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER,
                "Saved {} towns to persistent storage", towns.size());
                
        } catch (Exception e) {
            LOGGER.error("Failed to save town data: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Mark town data as dirty for persistence.
     * Enhanced MultiLoader requires explicit save for reliability.
     */
    public void markDirty() {
        persistence.markDirty();
        // NOTE: Enhanced MultiLoader requires explicit save vs main branch's automatic save
        saveTowns(); // Ensure immediate persistence for reliability
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, 
            "Town data marked dirty and saved immediately");
    }
    
    // ================================
    // Persistence Interface Implementation
    // ================================
    
    
    // ================================
    // Debug and Admin Methods
    // ================================
    
    /**
     * Get statistics about town management.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalTowns", towns.size());
        stats.put("totalPopulation", towns.values().stream().mapToInt(Town::getPopulation).sum());
        stats.put("averagePopulation", towns.isEmpty() ? 0 : 
            towns.values().stream().mapToInt(Town::getPopulation).average().orElse(0));
        stats.put("persistenceId", PERSISTENCE_NAME);
        stats.put("levelContext", level.getClass().getSimpleName());
        
        return stats;
    }
    
    @Override
    public String toString() {
        return String.format("TownManager{level=%s, towns=%d, persistence=%s}", 
            level.getClass().getSimpleName(), towns.size(), PERSISTENCE_NAME);
    }
    
    // ================================
    // Internal Empty Persistence Implementation
    // ================================
    
    /**
     * Minimal persistence implementation for fallback scenarios.
     * Platform-specific implementations should provide proper persistence.
     */
    private static class EmptyTownPersistence implements ITownPersistence {
        private final Object level;
        private final Map<String, Object> data = new HashMap<>();
        private boolean dirty = false;
        
        public EmptyTownPersistence(Object level) {
            this.level = level;
        }
        
        @Override
        public void save(Map<String, Object> townData) {
            this.data.clear();
            this.data.putAll(townData);
            this.dirty = false;
        }
        
        @Override
        public Map<String, Object> load() {
            return new HashMap<>(data);
        }
        
        @Override
        public void markDirty() {
            this.dirty = true;
        }
        
        @Override
        public String getIdentifier() {
            return PERSISTENCE_NAME;
        }
        
        @Override
        public boolean isDirty() {
            return dirty;
        }
        
        @Override
        public void clearDirty() {
            this.dirty = false;
        }
        
        @Override
        public Object getLevel() {
            return level;
        }
    }
}