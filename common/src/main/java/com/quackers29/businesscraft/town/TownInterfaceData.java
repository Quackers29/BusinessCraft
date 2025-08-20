package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Platform-agnostic TownInterface data class for BusinessCraft mod.
 * 
 * This class represents the business logic and data of a TownInterface block
 * following the unified architecture pattern. All platform-specific operations
 * are handled through platform services or platform-specific wrapper entities.
 * 
 * Key design principles:
 * - Zero platform dependencies (no BlockPos, BlockEntity, etc.)
 * - Uses primitive coordinates instead of platform-specific position types
 * - Platform services for all Minecraft API interactions
 * - Direct access from common module packets (no BlockEntityHelper abstraction)
 * 
 * This replaces the over-abstracted BlockEntityHelper pattern for 60+ method calls.
 */
public class TownInterfaceData {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceData.class);
    
    // Core identity and position (platform-agnostic)
    private final int[] position; // [x, y, z] - replaces BlockPos
    private UUID townId;
    private String townName;
    
    // Platform management
    private final List<Platform> platforms = new ArrayList<>();
    private int searchRadius = ConfigLoader.vehicleSearchRadius;
    
    // State management
    private boolean touristSpawningEnabled = true;
    private boolean pathCreationMode = false;
    private String activePlatformId;
    
    // Town association
    private transient Town associatedTown; // Cached reference, not serialized
    
    // Dirty tracking for persistence
    private boolean isDirty = false;
    
    /**
     * Create new TownInterface data at the specified position.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate  
     * @param z Block Z coordinate
     */
    public TownInterfaceData(int x, int y, int z) {
        this.position = new int[]{x, y, z};
        this.townId = null; // Will be set when town is registered
        this.townName = "Unregistered Town";
    }
    
    /**
     * Get the block position coordinates.
     */
    public int[] getPosition() {
        return position.clone(); // Return copy to prevent modification
    }
    
    public int getX() { return position[0]; }
    public int getY() { return position[1]; }
    public int getZ() { return position[2]; }
    
    // === TOWN MANAGEMENT ===
    
    /**
     * Get the associated town UUID.
     */
    public UUID getTownId() {
        return townId;
    }
    
    /**
     * Set the associated town UUID.
     */
    public void setTownId(UUID townId) {
        this.townId = townId;
        this.associatedTown = null; // Clear cache
        markDirty();
    }
    
    /**
     * Get the town name.
     */
    public String getTownName() {
        return townName;
    }
    
    /**
     * Set the town name.
     */
    public void setTownName(String townName) {
        this.townName = townName;
        markDirty();
    }
    
    /**
     * Check if this interface has been registered with a town.
     */
    public boolean isTownRegistered() {
        return townId != null;
    }
    
    // === PLATFORM MANAGEMENT ===
    
    /**
     * Get all platforms for this town interface.
     */
    public List<Platform> getPlatforms() {
        return new ArrayList<>(platforms); // Return copy to prevent modification
    }
    
    /**
     * Get the number of platforms.
     */
    public int getPlatformCount() {
        return platforms.size();
    }
    
    /**
     * Check if more platforms can be added.
     */
    public boolean canAddMorePlatforms() {
        return platforms.size() < 10; // TODO: Add maxPlatformsPerTown to ConfigLoader
    }
    
    /**
     * Add a new platform.
     * 
     * @return True if platform was successfully added
     */
    public boolean addPlatform() {
        if (!canAddMorePlatforms()) {
            return false;
        }
        
        Platform newPlatform = new Platform();
        platforms.add(newPlatform);
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, 
            "Added new platform {} at position ({}, {}, {})", 
            newPlatform.getId(), position[0], position[1], position[2]);
        
        return true;
    }
    
    /**
     * Remove a platform by index.
     * 
     * @param platformIndex Index of the platform to remove
     * @return True if platform was successfully removed
     */
    public boolean removePlatform(int platformIndex) {
        if (platformIndex < 0 || platformIndex >= platforms.size()) {
            return false;
        }
        
        Platform removed = platforms.remove(platformIndex);
        markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Removed platform {} at index {} from position ({}, {}, {})",
            removed.getId(), platformIndex, position[0], position[1], position[2]);
        
        return true;
    }
    
    /**
     * Remove a platform by UUID.
     * 
     * @param platformId Platform UUID as string
     * @return True if platform was successfully removed
     */
    public boolean removePlatformById(String platformId) {
        UUID uuid = UUID.fromString(platformId);
        boolean removed = platforms.removeIf(platform -> platform.getId().equals(uuid));
        
        if (removed) {
            markDirty();
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "Removed platform {} from position ({}, {}, {})",
                platformId, position[0], position[1], position[2]);
        }
        
        return removed;
    }
    
    /**
     * Find a platform by UUID.
     * 
     * @param platformId Platform UUID as string
     * @return Platform object or null if not found
     */
    public Platform findPlatformById(String platformId) {
        UUID uuid = UUID.fromString(platformId);
        return platforms.stream()
            .filter(platform -> platform.getId().equals(uuid))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Set platform enabled state by index.
     * 
     * @param platformIndex Index of the platform
     * @param enabled New enabled state
     */
    public void setPlatformEnabled(int platformIndex, boolean enabled) {
        if (platformIndex >= 0 && platformIndex < platforms.size()) {
            platforms.get(platformIndex).setEnabled(enabled);
            markDirty();
        }
    }
    
    /**
     * Set platform enabled state by UUID.
     * 
     * @param platformId Platform UUID as string
     * @param enabled New enabled state
     * @return True if platform was found and updated
     */
    public boolean setPlatformEnabledById(String platformId, boolean enabled) {
        Platform platform = findPlatformById(platformId);
        if (platform != null) {
            platform.setEnabled(enabled);
            markDirty();
            return true;
        }
        return false;
    }
    
    /**
     * Check if a platform is enabled by index.
     * 
     * @param platformIndex Index of the platform
     * @return True if the platform is enabled
     */
    public boolean isPlatformEnabled(int platformIndex) {
        if (platformIndex >= 0 && platformIndex < platforms.size()) {
            return platforms.get(platformIndex).isEnabled();
        }
        return false;
    }
    
    // === TOURIST MANAGEMENT ===
    
    /**
     * Check if tourist spawning is enabled.
     */
    public boolean isTouristSpawningEnabled() {
        return touristSpawningEnabled;
    }
    
    /**
     * Set tourist spawning enabled state.
     */
    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
        markDirty();
    }
    
    // === SEARCH RADIUS ===
    
    /**
     * Get the search radius for platforms.
     */
    public int getSearchRadius() {
        return searchRadius;
    }
    
    /**
     * Set the search radius for platforms.
     */
    public void setSearchRadius(int radius) {
        this.searchRadius = Math.max(1, Math.min(radius, 50)); // TODO: Add maxSearchRadius to ConfigLoader
        markDirty();
    }
    
    // === PATH CREATION MODE ===
    
    /**
     * Check if path creation mode is active.
     */
    public boolean isPathCreationModeActive() {
        return pathCreationMode;
    }
    
    /**
     * Set path creation mode state.
     * 
     * @param mode True to enable path creation mode
     * @param platformId Platform UUID string for the active platform
     */
    public void setPathCreationMode(boolean mode, String platformId) {
        this.pathCreationMode = mode;
        this.activePlatformId = mode ? platformId : null;
        markDirty();
    }
    
    /**
     * Get the active platform ID for path creation.
     */
    public String getActivePlatformId() {
        return activePlatformId;
    }
    
    // === PLATFORM DESTINATIONS ===
    
    /**
     * Set destination enabled state for a platform.
     * 
     * @param platformId Platform UUID as string
     * @param townId Target town UUID as string
     * @param enabled New enabled state for this destination
     * @return True if destination state was successfully updated
     */
    public boolean setPlatformDestinationEnabled(String platformId, String townId, boolean enabled) {
        Platform platform = findPlatformById(platformId);
        if (platform != null) {
            UUID townUuid = UUID.fromString(townId);
            if (enabled) {
                platform.enableDestination(townUuid);
            } else {
                platform.disableDestination(townUuid);
            }
            markDirty();
            return true;
        }
        return false;
    }
    
    /**
     * Get destination enabled states for a platform.
     * 
     * @param platformId Platform UUID as string
     * @return Map of town UUID strings to enabled states, or empty map if platform not found
     */
    public Map<String, Boolean> getPlatformDestinations(String platformId) {
        Platform platform = findPlatformById(platformId);
        if (platform != null) {
            Map<String, Boolean> destinations = new HashMap<>();
            for (UUID destination : platform.getEnabledDestinations()) {
                destinations.put(destination.toString(), true);
            }
            return destinations;
        }
        return new HashMap<>();
    }
    
    // === PLATFORM PATHS ===
    
    /**
     * Set platform path coordinates (start and end positions).
     * 
     * @param platformId Platform UUID as string
     * @param startX Start position X coordinate
     * @param startY Start position Y coordinate
     * @param startZ Start position Z coordinate
     * @param endX End position X coordinate
     * @param endY End position Y coordinate
     * @param endZ End position Z coordinate
     * @return True if platform path was successfully updated
     */
    public boolean setPlatformPath(String platformId, int startX, int startY, int startZ,
                                  int endX, int endY, int endZ) {
        Platform platform = findPlatformById(platformId);
        if (platform != null) {
            platform.setStartPos(new net.minecraft.core.BlockPos(startX, startY, startZ));
            platform.setEndPos(new net.minecraft.core.BlockPos(endX, endY, endZ));
            markDirty();
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "Set path for platform {} from ({}, {}, {}) to ({}, {}, {})",
                platformId, startX, startY, startZ, endX, endY, endZ);
            
            return true;
        }
        return false;
    }
    
    /**
     * Reset platform path coordinates (clear start and end positions).
     * 
     * @param platformId Platform UUID as string
     * @return True if platform path was successfully reset
     */
    public boolean resetPlatformPath(String platformId) {
        Platform platform = findPlatformById(platformId);
        if (platform != null) {
            platform.setStartPos(null);
            platform.setEndPos(null);
            markDirty();
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "Reset path for platform {}", platformId);
            
            return true;
        }
        return false;
    }
    
    // === TOWN DATA PROVIDER INTEGRATION ===
    
    /**
     * Get the associated Town object (cached).
     * This provides ITownDataProvider functionality.
     */
    public Town getAssociatedTown() {
        // Note: This method would need platform services to access TownManager
        // Implementation depends on how platform services are structured
        return associatedTown;
    }
    
    /**
     * Set the associated Town object (for caching).
     */
    public void setAssociatedTown(Town town) {
        this.associatedTown = town;
    }
    
    // === PERSISTENCE ===
    
    /**
     * Check if the data has been modified and needs saving.
     */
    public boolean isDirty() {
        return isDirty;
    }
    
    /**
     * Mark the data as dirty for persistence.
     */
    public void markDirty() {
        this.isDirty = true;
    }
    
    /**
     * Clear the dirty flag (called after saving).
     */
    public void clearDirty() {
        this.isDirty = false;
    }
    
    // === SERIALIZATION ===
    
    /**
     * Save data to NBT format for cross-platform persistence.
     * This replaces the platform-specific NBT handling in TownInterfaceEntity.
     */
    public Map<String, Object> saveToNBT() {
        Map<String, Object> nbt = new HashMap<>();
        
        // Position
        nbt.put("x", position[0]);
        nbt.put("y", position[1]);
        nbt.put("z", position[2]);
        
        // Town data
        if (townId != null) {
            nbt.put("townId", townId.toString());
        }
        nbt.put("townName", townName);
        
        // Settings
        nbt.put("touristSpawningEnabled", touristSpawningEnabled);
        nbt.put("searchRadius", searchRadius);
        nbt.put("pathCreationMode", pathCreationMode);
        if (activePlatformId != null) {
            nbt.put("activePlatformId", activePlatformId);
        }
        
        // Platforms
        List<Map<String, Object>> platformList = new ArrayList<>();
        for (Platform platform : platforms) {
            // Convert CompoundTag to Map for NBT compatibility
            net.minecraft.nbt.CompoundTag tag = platform.save();
            Map<String, Object> platformNbt = convertTagToMap(tag);
            platformList.add(platformNbt);
        }
        nbt.put("platforms", platformList);
        
        return nbt;
    }
    
    /**
     * Load data from NBT format.
     */
    @SuppressWarnings("unchecked")
    public void loadFromNBT(Map<String, Object> nbt) {
        // Position is immutable after creation
        
        // Town data
        if (nbt.containsKey("townId")) {
            this.townId = UUID.fromString((String) nbt.get("townId"));
        }
        this.townName = (String) nbt.getOrDefault("townName", "Unregistered Town");
        
        // Settings
        this.touristSpawningEnabled = (Boolean) nbt.getOrDefault("touristSpawningEnabled", true);
        this.searchRadius = (Integer) nbt.getOrDefault("searchRadius", ConfigLoader.vehicleSearchRadius);
        this.pathCreationMode = (Boolean) nbt.getOrDefault("pathCreationMode", false);
        this.activePlatformId = (String) nbt.get("activePlatformId");
        
        // Platforms
        platforms.clear();
        if (nbt.containsKey("platforms")) {
            List<Map<String, Object>> platformList = (List<Map<String, Object>>) nbt.get("platforms");
            for (Map<String, Object> platformNbt : platformList) {
                Platform platform = new Platform();
                // Convert Map to CompoundTag for Platform loading
                net.minecraft.nbt.CompoundTag tag = convertMapToTag(platformNbt);
                platform = Platform.fromNBT(tag);
                platforms.add(platform);
            }
        }
        
        clearDirty(); // Data is fresh from storage
    }
    
    // === NBT CONVERSION HELPERS ===
    
    /**
     * Convert CompoundTag to Map for cross-platform NBT compatibility.
     */
    private Map<String, Object> convertTagToMap(net.minecraft.nbt.CompoundTag tag) {
        Map<String, Object> map = new HashMap<>();
        
        // Convert all tag entries to map entries
        for (String key : tag.getAllKeys()) {
            net.minecraft.nbt.Tag value = tag.get(key);
            if (value instanceof net.minecraft.nbt.StringTag) {
                map.put(key, tag.getString(key));
            } else if (value instanceof net.minecraft.nbt.IntTag) {
                map.put(key, tag.getInt(key));
            } else if (value instanceof net.minecraft.nbt.ByteTag) {
                map.put(key, tag.getBoolean(key)); // ByteTag is used for booleans
            } else if (value instanceof net.minecraft.nbt.CompoundTag) {
                map.put(key, convertTagToMap((net.minecraft.nbt.CompoundTag) value));
            }
            // Add other tag types as needed
        }
        
        return map;
    }
    
    /**
     * Convert Map to CompoundTag for platform-specific NBT handling.
     */
    @SuppressWarnings("unchecked")
    private net.minecraft.nbt.CompoundTag convertMapToTag(Map<String, Object> map) {
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                tag.putString(key, (String) value);
            } else if (value instanceof Integer) {
                tag.putInt(key, (Integer) value);
            } else if (value instanceof Boolean) {
                tag.putBoolean(key, (Boolean) value);
            } else if (value instanceof Map) {
                tag.put(key, convertMapToTag((Map<String, Object>) value));
            }
            // Add other types as needed
        }
        
        return tag;
    }
}