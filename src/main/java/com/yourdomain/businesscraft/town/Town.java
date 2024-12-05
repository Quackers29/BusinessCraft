package com.yourdomain.businesscraft.town;

import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import com.yourdomain.businesscraft.config.ConfigLoader;

public class Town {
    private final UUID id;
    private final BlockPos position;
    private String name;
    private int breadCount;
    private int population;
    private boolean touristSpawningEnabled;
    
    public Town(UUID id, BlockPos pos, String name) {
        this.id = id;
        this.position = pos;
        this.name = name;
        this.touristSpawningEnabled = true;
    }
    
    public void addBread(int count) {
        this.breadCount += count;
        if (this.breadCount >= ConfigLoader.breadForNewVillager) {
            this.breadCount -= ConfigLoader.breadForNewVillager;
            this.population++;
        }
    }
    
    public boolean canSpawnTourists() {
        return touristSpawningEnabled && population >= ConfigLoader.minPopForTourists;
    }
    
    public void removeTourist() {
        if (population > 0) {
            population--;
        }
    }
    
    public void save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("breadCount", breadCount);
        tag.putInt("population", population);
        tag.putBoolean("touristSpawning", touristSpawningEnabled);
        tag.putInt("posX", position.getX());
        tag.putInt("posY", position.getY());
        tag.putInt("posZ", position.getZ());
    }
    
    public static Town load(CompoundTag tag) {
        BlockPos pos = new BlockPos(
            tag.getInt("posX"),
            tag.getInt("posY"),
            tag.getInt("posZ")
        );
        UUID id = tag.getUUID("id");
        Town town = new Town(id, pos, tag.getString("name"));
        town.breadCount = tag.getInt("breadCount");
        town.population = tag.getInt("population");
        town.touristSpawningEnabled = tag.getBoolean("touristSpawning");
        return town;
    }
    
    public String getName() {
        return name;
    }
    
    public int getBreadCount() {
        return breadCount;
    }
    
    public int getPopulation() {
        return population;
    }
    
    public UUID getId() {
        return id;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
    }
    
    // Getters and setters
} 