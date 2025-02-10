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
    private final TownSavedData savedData;
    
    public TownSavedData getSavedData() {
        return this.savedData;
    }

    private TownManager(ServerLevel level) {
        this.savedData = level.getDataStorage().computeIfAbsent(
            TownSavedData::load,
            TownSavedData::create,
            TownSavedData.NAME
        );
    }

    public static TownManager get(ServerLevel level) {
        return new TownManager(level);
    }

    public UUID registerTown(BlockPos pos, String name) {
        UUID townId = UUID.randomUUID();
        LOGGER.info("Registering new town. ID: {}, Name: {}, Position: {}", townId, name, pos);
        savedData.getTowns().put(townId, new Town(townId, pos, name));
        savedData.setDirty();
        return townId;
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
            CompoundTag data = new CompoundTag();
            saveAllTowns(data);
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