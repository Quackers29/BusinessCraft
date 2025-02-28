package com.yourdomain.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores various resources in a town with their quantities
 */
public class TownResources {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownResources.class);
    private final Map<Item, Integer> resources = new HashMap<>();
    
    /**
     * Add a specific resource to the town
     * 
     * @param item The item that represents the resource
     * @param count The amount to add
     */
    public void addResource(Item item, int count) {
        if (item == null || count <= 0) return;
        
        resources.merge(item, count, Integer::sum);
        LOGGER.debug("Added {} of resource {}", count, ForgeRegistries.ITEMS.getKey(item));
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
     * Save resources to NBT
     * 
     * @param tag The tag to save to
     */
    public void save(CompoundTag tag) {
        CompoundTag resourcesTag = new CompoundTag();
        
        for (Map.Entry<Item, Integer> entry : resources.entrySet()) {
            ResourceLocation key = ForgeRegistries.ITEMS.getKey(entry.getKey());
            if (key != null) {
                resourcesTag.putInt(key.toString(), entry.getValue());
            }
        }
        
        tag.put("resources", resourcesTag);
    }
    
    /**
     * Load resources from NBT
     * 
     * @param tag The tag to load from
     */
    public void load(CompoundTag tag) {
        resources.clear();
        
        if (tag.contains("resources")) {
            CompoundTag resourcesTag = tag.getCompound("resources");
            
            for (String key : resourcesTag.getAllKeys()) {
                ResourceLocation resourceLocation = new ResourceLocation(key);
                Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                
                if (item != null && item != Items.AIR) {
                    resources.put(item, resourcesTag.getInt(key));
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