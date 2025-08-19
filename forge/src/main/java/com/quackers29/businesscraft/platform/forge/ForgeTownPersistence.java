package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.data.TownSavedData;
import com.quackers29.businesscraft.town.data.ITownPersistence;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
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
        // CRITICAL FIX: Properly synchronize TownManager data with TownSavedData
        try {
            // First, update the TownSavedData's towns map with the current TownManager data
            if (townData.containsKey("towns")) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> townsData = (Map<String, Map<String, Object>>) townData.get("towns");
                
                // Clear existing towns and rebuild from TownManager data
                Map<UUID, com.quackers29.businesscraft.town.Town> savedTowns = savedData.getTowns();
                savedTowns.clear();
                
                // Convert each town from platform-agnostic format back to Town objects
                for (Map.Entry<String, Map<String, Object>> entry : townsData.entrySet()) {
                    try {
                        UUID townId = UUID.fromString(entry.getKey());
                        com.quackers29.businesscraft.town.Town town = com.quackers29.businesscraft.town.Town.fromDataMap(entry.getValue());
                        savedTowns.put(townId, town);
                    } catch (Exception e) {
                        LOGGER.error("Failed to convert town data for ID {}: {}", entry.getKey(), e.getMessage());
                    }
                }
                
                LOGGER.debug("Synchronized {} towns from TownManager to TownSavedData", savedTowns.size());
            }
            
            // UNIFIED ARCHITECTURE: Payment board data is now included in Town.toDataMap() 
            // No separate payment board saving needed
            
            // Mark as dirty to trigger Forge's SavedData persistence
            savedData.setDirty();
            this.isDirty = false;
            
            LOGGER.debug("Saved town data and payment boards to persistent storage");
            
        } catch (Exception e) {
            LOGGER.error("Failed to save town data: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    /*
     * UNIFIED ARCHITECTURE: Payment board persistence is now handled directly by Town class.
     * The savePaymentBoardData() method has been removed because payment board data is 
     * automatically included in Town.toDataMap() and handled by the save() method above.
     */
    
    @Override
    public Map<String, Object> load() {
        // CRITICAL FIX: The TownSavedData already handles loading through Forge's SavedData system
        // We don't need to duplicate the loading logic here - just return empty data and let
        // the Enhanced MultiLoader persistence system work through the standard save/load cycle
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // The savedData instance is automatically loaded from disk by Forge's SavedData system
            // when computeIfAbsent calls TownSavedData::load
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
                LOGGER.debug("Loaded {} towns from SavedData successfully", savedTowns.size());
            } else {
                LOGGER.debug("No existing town data found in SavedData - fresh world or clean start");
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