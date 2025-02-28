package com.yourdomain.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.yourdomain.businesscraft.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TownEconomyComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownEconomyComponent.class);
    private final TownResources resources = new TownResources();
    private int population;
    
    /**
     * Legacy method for compatibility. Use addResource instead.
     */
    public void addBread(int count) {
        addResource(Items.BREAD, count);
    }
    
    /**
     * Add a specific resource to the town and update population if applicable
     * 
     * @param item The item resource to add
     * @param count The amount to add
     */
    public void addResource(Item item, int count) {
        if (item == null || count <= 0) return;
        
        resources.addResource(item, count);
        
        // Special handling for bread which still drives population
        if (item == Items.BREAD) {
            int breadCount = resources.getResourceCount(Items.BREAD);
            int popToAdd = breadCount / ConfigLoader.breadPerPop;
            
            if (popToAdd > 0) {
                // Consume the bread used for population
                resources.consumeResource(Items.BREAD, popToAdd * ConfigLoader.breadPerPop);
                this.population += popToAdd;
                LOGGER.debug("Population increased by {} to {}", popToAdd, population);
            }
        }
    }
    
    /**
     * Get a specific resource count
     * 
     * @param item The item to get count for
     * @return The count of that resource
     */
    public int getResourceCount(Item item) {
        return resources.getResourceCount(item);
    }
    
    /**
     * Legacy method for bread count
     */
    public int getBreadCount() {
        return resources.getResourceCount(Items.BREAD);
    }
    
    /**
     * Remove population from the town
     * 
     * @param amount The amount of population to remove
     */
    public void removePopulation(int amount) {
        if (population >= amount) {
            population -= amount;
        }
    }

    @Override
    public void tick() {
        // Can implement daily economic updates later
    }

    @Override
    public void save(CompoundTag tag) {
        // Save population separately
        tag.putInt("population", population);
        
        // Save all resources using the TownResources
        resources.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        // Load population
        population = tag.getInt("population");
        
        // Load resources
        resources.load(tag);
    }

    // Getters
    public int getPopulation() { return population; }
    
    /**
     * Get all resources in the town
     * 
     * @return The town's resources
     */
    public TownResources getResources() {
        return resources;
    }
} 