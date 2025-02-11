package com.yourdomain.businesscraft.town.components;

import net.minecraft.nbt.CompoundTag;
import com.yourdomain.businesscraft.config.ConfigLoader;

public class TownEconomyComponent implements TownComponent {
    private int breadCount;
    private int population;
    
    public void addBread(int count) {
        this.breadCount += count;
        if (this.breadCount >= ConfigLoader.breadPerPop) {
            this.breadCount -= ConfigLoader.breadPerPop;
            this.population++;
        }
    }

    public void removePopulation(int amount) {
        if (population >= amount) {
            population -= amount;
            breadCount = Math.max(0, breadCount - (ConfigLoader.breadPerPop * amount));
        }
    }

    @Override
    public void tick() {
        // Can implement daily economic updates later
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putInt("breadCount", breadCount);
        tag.putInt("population", population);
    }

    @Override
    public void load(CompoundTag tag) {
        breadCount = tag.getInt("breadCount");
        population = tag.getInt("population");
    }

    // Getters
    public int getBreadCount() { return breadCount; }
    public int getPopulation() { return population; }
} 