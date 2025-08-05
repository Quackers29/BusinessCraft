package com.quackers29.businesscraft.util;

import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.HashMap;

/**
 * Utility class for converting between platform-specific Item types and Object types.
 */
public class ItemConverter {
    
    /**
     * Convert a Map<Object, Integer> to Map<Item, Integer> for Forge-specific usage
     */
    @SuppressWarnings("unchecked")
    public static Map<Item, Integer> toItemMap(Map<Object, Integer> objectMap) {
        if (objectMap == null) return new HashMap<>();
        
        Map<Item, Integer> itemMap = new HashMap<>();
        objectMap.forEach((obj, count) -> {
            if (obj instanceof Item item) {
                itemMap.put(item, count);
            }
        });
        return itemMap;
    }
    
    /**
     * Convert a Map<Item, Integer> to Map<Object, Integer> for interface compatibility
     */
    public static Map<Object, Integer> toObjectMap(Map<Item, Integer> itemMap) {
        if (itemMap == null) return new HashMap<>();
        
        Map<Object, Integer> objectMap = new HashMap<>();
        itemMap.forEach((item, count) -> objectMap.put(item, count));
        return objectMap;
    }
    
    /**
     * Safe cast Object to Item
     */
    public static Item toItem(Object obj) {
        return obj instanceof Item item ? item : null;
    }
}