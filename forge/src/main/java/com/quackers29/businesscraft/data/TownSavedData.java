package com.quackers29.businesscraft.data;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TownSavedData extends SavedData {
    public static final String NAME = BusinessCraft.MOD_ID + "_towns";
    
    private final Map<UUID, Town> towns = new ConcurrentHashMap<>();
    
    public Map<UUID, Town> getTowns() {
        return towns;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        // UNIFIED ARCHITECTURE: Simple town persistence - payment boards included in Town data
        CompoundTag townsTag = new CompoundTag();
        towns.forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            // Convert common Town's data format to NBT - includes payment board data automatically
            Map<String, Object> townData = town.toDataMap();
            saveMapToNbt(townData, townTag);
            townsTag.put(id.toString(), townTag);
        });
        tag.put("towns", townsTag);
        
        // NOTE: Payment board data is now included in Town.toDataMap() - no separate saving needed
        
        return tag;
    }
    
    /*
     * UNIFIED ARCHITECTURE: Payment board persistence is now handled directly by Town class.
     * The savePaymentBoardData() method has been removed because payment board data is 
     * automatically included in Town.toDataMap().
     */
    
    public void loadFromNbt(CompoundTag tag) {
        // NOTE: This class is deprecated - new persistence uses ForgeTownPersistence
        towns.clear();
        if (tag.contains("towns")) {
            CompoundTag townsTag = tag.getCompound("towns");
            townsTag.getAllKeys().forEach(key -> {
                UUID id = UUID.fromString(key);
                CompoundTag townTag = townsTag.getCompound(key);
                // Convert NBT to common Town's data format
                Map<String, Object> townData = nbtToMap(townTag);
                Town town = Town.fromDataMap(townData);
                towns.put(id, town);
            });
        }
        
        // NOTE: Payment board data is now included in Town.fromDataMap() - no separate loading needed
    }
    
    /*
     * UNIFIED ARCHITECTURE: Payment board loading is now handled directly by Town class.
     * The loadPaymentBoardData() method has been removed because payment board data is
     * automatically included in Town.fromDataMap().
     */
    
    /**
     * Helper method to convert Map to NBT.
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
            } else if (value instanceof int[]) {
                tag.putIntArray(key, (int[]) value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subMap = (Map<String, Object>) value;
                CompoundTag subTag = new CompoundTag();
                saveMapToNbt(subMap, subTag);
                tag.put(key, subTag);
            } else if (value instanceof CompoundTag) {
                // UNIFIED ARCHITECTURE FIX: Handle CompoundTag objects directly
                CompoundTag compoundValue = (CompoundTag) value;
                tag.put(key, compoundValue);
            }
            // Add more type conversions as needed
        }
    }
    
    /**
     * Helper method to convert NBT to Map.
     */
    private Map<String, Object> nbtToMap(CompoundTag tag) {
        Map<String, Object> result = new java.util.HashMap<>();
        
        for (String key : tag.getAllKeys()) {
            if (tag.contains(key, 8)) { // String
                result.put(key, tag.getString(key));
            } else if (tag.contains(key, 3)) { // Int
                result.put(key, tag.getInt(key));
            } else if (tag.contains(key, 4)) { // Long
                result.put(key, tag.getLong(key));
            } else if (tag.contains(key, 6)) { // Double
                result.put(key, tag.getDouble(key));
            } else if (tag.contains(key, 1)) { // Boolean
                result.put(key, tag.getBoolean(key));
            } else if (tag.contains(key, 11)) { // Int Array
                result.put(key, tag.getIntArray(key));
            } else if (tag.contains(key, 10)) { // Compound
                CompoundTag subTag = tag.getCompound(key);
                // UNIFIED ARCHITECTURE FIX: For paymentBoard, preserve as CompoundTag
                if ("paymentBoard".equals(key)) {
                    result.put(key, subTag);
                } else {
                    result.put(key, nbtToMap(subTag));
                }
            }
            // Add more type conversions as needed
        }
        
        return result;
    }
    
    public static TownSavedData create() {
        return new TownSavedData();
    }
    
    public static TownSavedData load(CompoundTag tag) {
        TownSavedData data = create();
        data.loadFromNbt(tag);
        return data;
    }
} 