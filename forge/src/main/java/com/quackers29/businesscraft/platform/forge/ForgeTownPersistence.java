package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.data.TownSavedData;
import com.quackers29.businesscraft.town.data.ITownPersistence;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

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
        // Convert platform-agnostic format to NBT and save via TownSavedData
        CompoundTag tag = new CompoundTag();
        
        // Convert Map<String, Object> to NBT format
        // The townData contains serialized town information from common module
        for (Map.Entry<String, Object> entry : townData.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof CompoundTag) {
                tag.put(key, (CompoundTag) value);
            } else if (value instanceof String) {
                tag.putString(key, (String) value);
            } else if (value instanceof Integer) {
                tag.putInt(key, (Integer) value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mapValue = (Map<String, Object>) value;
                CompoundTag subTag = new CompoundTag();
                saveMapToNbt(mapValue, subTag);
                tag.put(key, subTag);
            }
            // Add more type conversions as needed
        }
        
        // Force TownSavedData to save
        savedData.setDirty();
        this.isDirty = false;
        
        LOGGER.debug("Saved town data with {} entries", townData.size());
    }
    
    @Override
    public Map<String, Object> load() {
        // The common TownManager will call this to get the serialized data
        // For the initial implementation, we start with an empty data structure
        // and let the common TownManager populate it through save() calls
        
        Map<String, Object> result = new HashMap<>();
        
        // If there are existing towns in the SavedData, we could convert them here
        // But for now, we'll let the migration happen through normal save operations
        LOGGER.debug("Loaded town data - {} entries", result.size());
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