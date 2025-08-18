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
        // NOTE: This class is deprecated - new persistence uses ForgeTownPersistence
        // This method is maintained for backward compatibility only
        CompoundTag townsTag = new CompoundTag();
        towns.forEach((id, town) -> {
            CompoundTag townTag = new CompoundTag();
            // Convert common Town's data format to NBT
            Map<String, Object> townData = town.toDataMap();
            saveMapToNbt(townData, townTag);
            townsTag.put(id.toString(), townTag);
        });
        tag.put("towns", townsTag);
        
        // Save payment board data
        savePaymentBoardData(tag);
        
        return tag;
    }
    
    /**
     * Save payment board data to NBT
     */
    private void savePaymentBoardData(CompoundTag tag) {
        try {
            // Access the payment boards from ForgeTownManagerService
            Map<java.util.UUID, com.quackers29.businesscraft.town.data.TownPaymentBoard> paymentBoards = 
                com.quackers29.businesscraft.platform.forge.ForgeTownManagerService.getPaymentBoards();
            
            if (!paymentBoards.isEmpty()) {
                CompoundTag paymentBoardsTag = new CompoundTag();
                
                for (Map.Entry<java.util.UUID, com.quackers29.businesscraft.town.data.TownPaymentBoard> entry : paymentBoards.entrySet()) {
                    java.util.UUID townId = entry.getKey();
                    com.quackers29.businesscraft.town.data.TownPaymentBoard paymentBoard = entry.getValue();
                    
                    // Save each payment board's NBT data
                    CompoundTag boardTag = paymentBoard.toNBT();
                    paymentBoardsTag.put(townId.toString(), boardTag);
                }
                
                tag.put("paymentBoards", paymentBoardsTag);
                System.out.println("TownSavedData: Saved " + paymentBoards.size() + " payment boards to NBT");
            }
        } catch (Exception e) {
            System.err.println("TownSavedData: Failed to save payment board data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
        
        // Load payment board data
        loadPaymentBoardData(tag);
    }
    
    /**
     * Load payment board data from NBT
     */
    private void loadPaymentBoardData(CompoundTag tag) {
        try {
            if (tag.contains("paymentBoards")) {
                CompoundTag paymentBoardsTag = tag.getCompound("paymentBoards");
                
                for (String key : paymentBoardsTag.getAllKeys()) {
                    try {
                        java.util.UUID townId = java.util.UUID.fromString(key);
                        CompoundTag boardTag = paymentBoardsTag.getCompound(key);
                        
                        // Create a new payment board and load its data
                        com.quackers29.businesscraft.town.data.TownPaymentBoard paymentBoard = 
                            new com.quackers29.businesscraft.town.data.TownPaymentBoard();
                        paymentBoard.fromNBT(boardTag);
                        
                        // Store in ForgeTownManagerService
                        com.quackers29.businesscraft.platform.forge.ForgeTownManagerService.setPaymentBoard(townId, paymentBoard);
                        
                    } catch (Exception e) {
                        System.err.println("TownSavedData: Failed to load payment board for town " + key + ": " + e.getMessage());
                    }
                }
                
                System.out.println("TownSavedData: Loaded " + paymentBoardsTag.getAllKeys().size() + " payment boards from NBT");
            }
        } catch (Exception e) {
            System.err.println("TownSavedData: Failed to load payment board data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
                result.put(key, nbtToMap(subTag));
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