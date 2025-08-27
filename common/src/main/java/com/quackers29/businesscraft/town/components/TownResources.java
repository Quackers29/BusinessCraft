package com.quackers29.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.quackers29.businesscraft.platform.PlatformServices;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Stores various resources in a town with their quantities
 * Uses unified architecture with platform services for cross-platform compatibility
 */
public class TownResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownResources.class);
    private final Map<Item, Integer> resources = new HashMap<>();
    private final String instanceId = java.util.UUID.randomUUID().toString().substring(0, 8);
    
    public TownResources() {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Created new TownResources instance {}", instanceId);
    }
    
    /**
     * Add a specific resource to the town
     * 
     * @param item The item that represents the resource
     * @param count The amount to add (can be negative to remove resources)
     */
    public void addResource(Item item, int count) {
        if (item == null) return;
        
        if (count > 0) {
            // Adding resources
            resources.merge(item, count, Integer::sum);
            String itemId = PlatformServices.getRegistryHelper().getItemId(item);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Added {} of resource {}", count, itemId);
        } else if (count < 0) {
            // Removing resources (count is negative)
            int currentAmount = resources.getOrDefault(item, 0);
            int newAmount = Math.max(0, currentAmount + count); // Ensure we don't go below 0
            
            // Special logging for emeralds (more important for debugging)
            boolean isEmerald = item == Items.EMERALD;
            
            if (currentAmount > 0) {
                String itemId = PlatformServices.getRegistryHelper().getItemId(item);
                if (isEmerald && count <= -5) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Emerald change: {} -> {} (removed {})", 
                        currentAmount, newAmount, -count);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Removed {} of resource {} (new amount: {})", 
                        -count, itemId, newAmount);
                }
                
                // Store the updated amount
                resources.put(item, newAmount);
            } else {
                if (isEmerald) {
                    LOGGER.warn("Failed emerald reduction: Attempted to remove {} emeralds, but current amount is 0", -count);
                }
            }
        }
    }
    
    /**
     * Get the count of a specific resource
     * 
     * @param item The resource item to check
     * @return The amount of the resource
     */
    public int getResourceCount(Item item) {
        return resources.getOrDefault(item, 0);
    }
    
    /**
     * Consume a specific amount of resources
     * 
     * @param item The resource item to consume
     * @param count The amount to consume
     * @return True if the resources were successfully consumed
     */
    public boolean consumeResource(Item item, int count) {
        if (item == null || count <= 0) return false;
        
        int currentCount = resources.getOrDefault(item, 0);
        if (currentCount < count) return false;
        
        resources.put(item, currentCount - count);
        return true;
    }
    
    /**
     * Gets all resources as an unmodifiable map
     * 
     * @return Map of all resources
     */
    public Map<Item, Integer> getAllResources() {
        return Collections.unmodifiableMap(resources);
    }
    
    /**
     * Save resources to NBT using unified platform services
     * 
     * @param tag The tag to save to
     */
    public void save(CompoundTag tag) {
        CompoundTag resourcesTag = new CompoundTag();
        
        for (Map.Entry<Item, Integer> entry : resources.entrySet()) {
            String itemId = PlatformServices.getRegistryHelper().getItemId(entry.getKey());
            if (itemId != null) {
                resourcesTag.putInt(itemId, entry.getValue());
            }
        }
        
        tag.put("resources", resourcesTag);
    }
    
    /**
     * Load resources from NBT using unified platform services
     * 
     * @param tag The tag to load from
     */
    public void load(CompoundTag tag) {
        resources.clear();
        
        if (tag.contains("resources")) {
            CompoundTag resourcesTag = tag.getCompound("resources");
            
            for (String itemId : resourcesTag.getAllKeys()) {
                Object itemObj = PlatformServices.getRegistryHelper().getItem(itemId);
                
                if (itemObj instanceof Item item && item != Items.AIR) {
                    resources.put(item, resourcesTag.getInt(itemId));
                }
            }
        }
        
        // Legacy support - if bread count exists in old format, migrate it
        if (tag.contains("breadCount")) {
            int breadCount = tag.getInt("breadCount");
            if (breadCount > 0) {
                resources.put(Items.BREAD, resources.getOrDefault(Items.BREAD, 0) + breadCount);
            }
        }
    }
}