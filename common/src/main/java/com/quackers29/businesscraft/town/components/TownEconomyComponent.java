package com.quackers29.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import com.quackers29.businesscraft.config.ConfigLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

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
        if (item == null) return;
        
        // Special logging for emerald deductions
        boolean isEmerald = item == net.minecraft.world.item.Items.EMERALD;
        
        // Log only significant emerald deductions (> 1)
        if (count < 0 && isEmerald && count <= -5) {
            LOGGER.info("Deducting {} emeralds from town economy", -count);
        }
        
        // Actually add/remove the resource
        resources.addResource(item, count);
        
        // Special handling for bread which still drives population
        if (item == Items.BREAD && count > 0) {
            int breadCount = resources.getResourceCount(Items.BREAD);
            int popToAdd = breadCount / ConfigLoader.breadPerPop;
            
            if (popToAdd > 0) {
                // Consume the bread used for population
                resources.consumeResource(Items.BREAD, popToAdd * ConfigLoader.breadPerPop);
                this.population += popToAdd;
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Population increased by {} to {}", popToAdd, population);
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
    
    /**
     * Set the population of the town directly
     * 
     * @param amount The new population value
     */
    public void setPopulation(int amount) {
        if (amount >= 0) {
            population = amount;
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Population set to {}", population);
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
