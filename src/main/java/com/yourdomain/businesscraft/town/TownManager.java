package com.yourdomain.businesscraft.town;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import java.util.Collections;
import com.yourdomain.businesscraft.data.TownSavedData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TownManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownManager");
    private static TownManager INSTANCE;
    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();
    private TownSavedData savedData;
    
    private TownManager() {}
    
    public static TownManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new TownManager();
        }
        return INSTANCE;
    }
    
    public static void init(ServerLevel level) {
        TownManager manager = getInstance();
        if (manager.savedData == null) {
            manager.savedData = level.getDataStorage().computeIfAbsent(
                TownSavedData::load,
                TownSavedData::create,
                TownSavedData.NAME
            );
        }
    }
    
    public UUID registerTown(BlockPos pos, String name) {
        UUID townId = UUID.randomUUID();
        LOGGER.info("Registering new town. ID: {}, Name: {}, Position: {}", townId, name, pos);
        towns.put(townId, new Town(townId, pos, name));
        if (savedData != null) {
            savedData.setDirty();
            LOGGER.info("Marked town data as dirty");
        } else {
            LOGGER.warn("SavedData is null when registering town");
        }
        return townId;
    }
    
    public Town getTown(UUID id) {
        Town town = towns.get(id);
        LOGGER.info("Getting town with ID: {}. Found: {}", id, town != null);
        return town;
    }
    
    public void updateResources(UUID townId, int breadCount) {
        Town town = towns.get(townId);
        if (town != null) {
            town.addBread(breadCount);
        }
    }
    
    public void saveAllTowns(CompoundTag worldData) {
        CompoundTag townsTag = new CompoundTag();
        towns.forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            town.save(townTag);
            townsTag.put(id.toString(), townTag);
        });
        worldData.put("towns", townsTag);
        
        if (savedData != null) {
            savedData.setDirty();
        }
    }
    
    public void loadAllTowns(CompoundTag worldData) {
        towns.clear();
        if (worldData.contains("towns")) {
            CompoundTag townsTag = worldData.getCompound("towns");
            townsTag.getAllKeys().forEach(key -> {
                UUID id = UUID.fromString(key);
                Town town = Town.load(townsTag.getCompound(key));
                towns.put(id, town);
            });
        }
    }
    
    public Map<UUID, Town> getAllTowns() {
        return Collections.unmodifiableMap(towns);
    }
    
    public void clearGhostTowns() {
        towns.entrySet().removeIf(entry -> {
            Town town = entry.getValue();
            // Define your logic to determine if a town is a "ghost town"
            return town.getPopulation() == 0; // Example: remove towns with zero population
        });
        if (savedData != null) {
            savedData.setDirty();
        }
    }
    
    public int clearAllTowns() {
        int count = towns.size();
        towns.clear();
        if (savedData != null) {
            savedData.setDirty();
        }
        return count;
    }
    
    public void onServerStopping() {
        if (savedData != null) {
            CompoundTag data = new CompoundTag();
            saveAllTowns(data);
            savedData.setDirty();
        }
    }
} 