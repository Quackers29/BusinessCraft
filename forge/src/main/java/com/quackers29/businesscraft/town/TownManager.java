package com.quackers29.businesscraft.town;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import java.util.Collections;
import com.quackers29.businesscraft.data.TownSavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import java.util.HashMap;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.service.TownBoundaryService;
import com.quackers29.businesscraft.town.service.TownBusinessLogic;
import com.quackers29.businesscraft.platform.PlatformServices;

public class TownManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownManager");
    private final TownSavedData savedData;
    private final TownBoundaryService boundaryService;
    private final TownBusinessLogic businessLogic;
    
    // Static reference to the current level for context
    private static final Map<ServerLevel, TownManager> INSTANCES = new HashMap<>();
    
    public TownSavedData getSavedData() {
        return this.savedData;
    }
    
    /**
     * Get the business logic service for town operations
     */
    public TownBusinessLogic getBusinessLogic() {
        return this.businessLogic;
    }

    private TownManager(ServerLevel level) {
        this.savedData = level.getDataStorage().computeIfAbsent(
            TownSavedData::load,
            TownSavedData::create,
            TownSavedData.NAME
        );
        this.boundaryService = new TownBoundaryService();
        this.businessLogic = new TownBusinessLogic(new com.quackers29.businesscraft.platform.ForgePlatformService());
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "TownManager initialized for level: {}", level.dimension().location());
    }

    public static TownManager get(ServerLevel level) {
        return INSTANCES.computeIfAbsent(level, key -> new TownManager(level));
    }
    
    /**
     * Cleans up instances that might be stale
     * Call this when server is stopping or restarting
     */
    public static void clearInstances() {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Clearing {} TownManager instances", INSTANCES.size());
        INSTANCES.clear();
    }

    public UUID registerTown(BlockPos pos, String name) {
        // Validate town name using common business logic
        com.quackers29.businesscraft.util.Result<String, com.quackers29.businesscraft.error.BCError> nameValidation = 
            businessLogic.validateTownName(name);
        
        if (nameValidation.isFailure()) {
            LOGGER.warn("Invalid town name '{}': {}", name, nameValidation.getError().getMessage());
            return null; // Return null to indicate failure
        }
        
        String validatedName = nameValidation.getValue();
        
        // Check minimum distance between towns
        if (!canPlaceTownAt(pos)) {
            LOGGER.warn("Attempted to place town too close to an existing town at position: {}", pos);
            return null; // Return null to indicate failure
        }
        
        UUID townId = UUID.randomUUID();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Registering new town. ID: {}, Name: {}, Position: {}", townId, validatedName, pos);
        savedData.getTowns().put(townId, new Town(townId, pos, validatedName));
        savedData.setDirty();
        return townId;
    }
    
    /**
     * Checks if a town can be placed at the specified position based on dynamic boundary calculations
     * 
     * @param pos The position to check
     * @return true if the town can be placed, false otherwise
     */
    public boolean canPlaceTownAt(BlockPos pos) {
        com.quackers29.businesscraft.util.Result<Void, com.quackers29.businesscraft.util.BCError.TownError> result = 
            boundaryService.checkTownPlacement(pos, savedData.getTowns().values());
        
        if (result.isFailure()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Town placement failed: {}", result.getError().getMessage());
            return false;
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Town placement validated at position: {}", pos);
        return true;
    }
    
    /**
     * Gets the detailed error message for why a town cannot be placed at a position
     * 
     * @param pos The position to check
     * @return Error message or null if placement is valid
     */
    public String getTownPlacementError(BlockPos pos) {
        com.quackers29.businesscraft.util.Result<Void, com.quackers29.businesscraft.util.BCError.TownError> result = 
            boundaryService.checkTownPlacement(pos, savedData.getTowns().values());
        
        return result.isFailure() ? result.getError().getMessage() : null;
    }

    public Town getTown(UUID id) {
        return savedData.getTowns().get(id);
    }

    public void updateResources(UUID townId, int breadCount) {
        Town town = savedData.getTowns().get(townId);
        if (town != null) {
            town.addBread(breadCount);
            savedData.setDirty();
        }
    }

    /**
     * Add a resource to a town
     * 
     * @param townId The town ID
     * @param item The resource item
     * @param count The amount to add
     */
    public void addResource(UUID townId, Item item, int count) {
        Town town = savedData.getTowns().get(townId);
        if (town != null) {
            town.addResource(item, count);
            savedData.setDirty();
        }
    }

    public void saveAllTowns(CompoundTag worldData) {
        CompoundTag townsTag = new CompoundTag();
        savedData.getTowns().forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            town.save(townTag);
            townsTag.put(id.toString(), townTag);
        });
        worldData.put("towns", townsTag);
        
        savedData.setDirty();
    }
    
    public void loadAllTowns(CompoundTag worldData) {
        savedData.getTowns().clear();
        if (worldData.contains("towns")) {
            CompoundTag townsTag = worldData.getCompound("towns");
            townsTag.getAllKeys().forEach(key -> {
                UUID id = UUID.fromString(key);
                Town town = Town.load(townsTag.getCompound(key));
                savedData.getTowns().put(id, town);
            });
        }
    }
    
    public Map<UUID, Town> getAllTowns() {
        return Collections.unmodifiableMap(savedData.getTowns());
    }
    
    public void clearGhostTowns() {
        savedData.getTowns().entrySet().removeIf(entry -> {
            Town town = entry.getValue();
            // Define your logic to determine if a town is a "ghost town"
            return town.getPopulation() == 0; // Example: remove towns with zero population
        });
        savedData.setDirty();
    }
    
    public int clearAllTowns() {
        int count = savedData.getTowns().size();
        savedData.getTowns().clear();
        savedData.setDirty();
        return count;
    }
    
    public void onServerStopping() {
        if (savedData != null) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Server stopping, marking {} towns as dirty", savedData.getTowns().size());
            savedData.setDirty();
        }
    }
    
    public void removeTown(UUID id) {
        if (savedData.getTowns().remove(id) != null) {
            savedData.setDirty();
        }
    }
    
    public void markDirty() {
        if (savedData != null) {
            savedData.setDirty();
        }
    }
} 