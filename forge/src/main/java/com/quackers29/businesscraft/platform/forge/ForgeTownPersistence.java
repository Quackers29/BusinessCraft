package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.data.TownSavedData;
import com.quackers29.businesscraft.town.data.ITownPersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Forge implementation of ITownPersistence interface.
 * 
 * This class bridges the platform-agnostic ITownPersistence interface
 * with Forge's native SavedData system via TownSavedData.
 * 
 * Enhanced MultiLoader approach: Wraps existing Forge persistence
 * infrastructure to provide platform-agnostic interface for common module.
 */
public class ForgeTownPersistence implements ITownPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/ForgeTownPersistence");
    
    private final ServerLevel level;
    private final TownSavedData savedData;
    private boolean isDirty = false;
    
    public ForgeTownPersistence(ServerLevel level) {
        this.level = level;
        this.savedData = level.getDataStorage().computeIfAbsent(
            TownSavedData::load,
            TownSavedData::create,
            TownSavedData.NAME
        );
        LOGGER.debug("ForgeTownPersistence initialized for level: {}", level.dimension().location());
    }
    
    @Override
    public void save(Map<String, Object> townData) {
        // CRITICAL FIX: Properly save town data to TownSavedData for persistence
        // Convert platform-agnostic format from common module back to TownSavedData format
        
        try {
            // Clear existing towns in SavedData
            savedData.getTowns().clear();
            
            if (townData.containsKey("towns")) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> townsData = (Map<String, Map<String, Object>>) townData.get("towns");
                
                // Convert each town from platform-agnostic format back to Town objects
                for (Map.Entry<String, Map<String, Object>> entry : townsData.entrySet()) {
                    try {
                        UUID townId = UUID.fromString(entry.getKey());
                        Map<String, Object> townDataMap = entry.getValue();
                        
                        // Create Town from data map (platform-agnostic to common Town)
                        com.quackers29.businesscraft.town.Town town = 
                            com.quackers29.businesscraft.town.Town.fromDataMap(townDataMap);
                        
                        // Store in TownSavedData for actual persistence
                        savedData.getTowns().put(townId, town);
                        
                    } catch (Exception e) {
                        LOGGER.error("Failed to save town with ID {}: {}", entry.getKey(), e.getMessage());
                    }
                }
                
                LOGGER.debug("Saved {} towns to TownSavedData", savedData.getTowns().size());
            }
            
            // Mark TownSavedData as dirty to trigger NBT save
            savedData.setDirty();
            this.isDirty = false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to save town data: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public Map<String, Object> load() {
        // CRITICAL FIX: Properly restore town data from TownSavedData
        // This was the root cause of the persistence bug - towns were never restored on world reload
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get the towns map directly from TownSavedData
            Map<UUID, com.quackers29.businesscraft.town.Town> savedTowns = savedData.getTowns();
            
            if (!savedTowns.isEmpty()) {
                // Convert saved towns to the platform-agnostic format expected by common TownManager
                Map<String, Map<String, Object>> townsData = new HashMap<>();
                
                for (Map.Entry<UUID, com.quackers29.businesscraft.town.Town> entry : savedTowns.entrySet()) {
                    UUID townId = entry.getKey();
                    com.quackers29.businesscraft.town.Town town = entry.getValue();
                    
                    // Convert town to data map (platform-agnostic format)
                    Map<String, Object> townData = town.toDataMap();
                    townsData.put(townId.toString(), townData);
                }
                
                result.put("towns", townsData);
                LOGGER.debug("Loaded town data - {} towns restored from SavedData", savedTowns.size());
            } else {
                LOGGER.debug("No existing town data found in SavedData");
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load town data from SavedData: {}", e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    @Override
    public void markDirty() {
        this.isDirty = true;
        savedData.setDirty();
    }
    
    @Override
    public String getIdentifier() {
        return TownSavedData.NAME;
    }
    
    @Override
    public boolean isDirty() {
        return this.isDirty;
    }
    
    @Override
    public void clearDirty() {
        this.isDirty = false;
    }
    
    @Override
    public Object getLevel() {
        return this.level;
    }
    
    /**
     * Get the underlying TownSavedData for backward compatibility.
     * This allows existing Forge code to access the SavedData system.
     */
    public TownSavedData getSavedData() {
        return this.savedData;
    }
    
    /**
     * Helper method to recursively convert Map to NBT.
     */
    private void saveMapToNbt(Map<String, Object> map, CompoundTag tag) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                tag.putString(key, (String) value);
            } else if (value instanceof Integer) {
                tag.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                tag.putLong(key, (Long) value);
            } else if (value instanceof Double) {
                tag.putDouble(key, (Double) value);
            } else if (value instanceof Boolean) {
                tag.putBoolean(key, (Boolean) value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                CompoundTag subTag = new CompoundTag();
                saveMapToNbt(subMap, subTag);
                tag.put(key, subTag);
            }
            // Add more type conversions as needed
        }
    }
}