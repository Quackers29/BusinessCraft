package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.town.data.ITownPersistence;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Fabric implementation of ITownPersistence interface.
 * 
 * This class provides platform-agnostic persistence for town data
 * using Fabric's native data storage systems.
 * 
 * Enhanced MultiLoader approach: Wraps Fabric persistence infrastructure
 * to provide the same interface as Forge for the common module.
 */
public class FabricTownPersistence implements ITownPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/FabricTownPersistence");
    
    private final ServerWorld level;
    private final String identifier;
    private boolean isDirty = false;
    private Map<String, Object> data = new HashMap<>();
    
    public FabricTownPersistence(ServerWorld level, String identifier) {
        this.level = level;
        this.identifier = identifier;
        LOGGER.debug("FabricTownPersistence initialized for level: {} with identifier: {}", 
            level.getRegistryKey().getValue(), identifier);
    }
    
    @Override
    public void save(Map<String, Object> townData) {
        // Store the data for Fabric-specific persistence
        this.data.clear();
        this.data.putAll(townData);
        
        // In a full implementation, this would write to Fabric's PersistentState
        // For now, we'll use the in-memory storage and let Fabric's save system handle it
        
        this.isDirty = false;
        LOGGER.debug("Saved town data with {} entries", townData.size());
    }
    
    @Override
    public Map<String, Object> load() {
        // Return the stored data
        // In a full implementation, this would load from Fabric's PersistentState
        
        Map<String, Object> result = new HashMap<>(this.data);
        LOGGER.debug("Loaded town data - {} entries", result.size());
        return result;
    }
    
    @Override
    public void markDirty() {
        this.isDirty = true;
        // In a full implementation, this would mark Fabric's PersistentState as dirty
    }
    
    @Override
    public String getIdentifier() {
        return this.identifier;
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
     * Get the underlying level for Fabric-specific operations.
     */
    public ServerWorld getServerWorld() {
        return this.level;
    }
    
    /**
     * Helper method to convert Map to NBT for Fabric persistence.
     * This will be used when implementing full Fabric PersistentState integration.
     */
    private NbtCompound mapToNbt(Map<String, Object> map) {
        NbtCompound tag = new NbtCompound();
        
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
                NbtCompound subTag = mapToNbt(subMap);
                tag.put(key, subTag);
            }
            // Add more type conversions as needed
        }
        
        return tag;
    }
}