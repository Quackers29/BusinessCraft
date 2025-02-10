package com.yourdomain.businesscraft.town;

import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import com.yourdomain.businesscraft.config.ConfigLoader;
import java.util.Map;
import java.util.HashMap;

public class Town {
    private final UUID id;
    private final BlockPos position;
    private String name;
    private int breadCount;
    private int population;
    private boolean touristSpawningEnabled;
    private Map<UUID, Integer> visitors = new HashMap<>();
    
    public Town(UUID id, BlockPos pos, String name) {
        this.id = id;
        this.position = pos;
        this.name = name;
        this.touristSpawningEnabled = true;
    }
    
    public void addBread(int count) {
        this.breadCount += count;
        if (this.breadCount >= ConfigLoader.breadPerPop) {
            this.breadCount -= ConfigLoader.breadPerPop;
            this.population++;
        }
    }
    
    public boolean canSpawnTourists() {
        return touristSpawningEnabled && 
               population >= ConfigLoader.minPopForTourists;
    }
    
    public void removeTourist() {
        if (population > 0) {
            population--;
            breadCount -= ConfigLoader.breadPerPop;
            if (breadCount < 0) breadCount = 0;
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
        CompoundTag visitorsTag = new CompoundTag();
        visitors.forEach((visitorId, count) -> {
            visitorsTag.putInt(visitorId.toString(), count);
        });
        tag.put("visitors", visitorsTag);
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
        if (tag.contains("visitors")) {
            CompoundTag visitorsTag = tag.getCompound("visitors");
            visitorsTag.getAllKeys().forEach(key -> {
                town.visitors.put(UUID.fromString(key), visitorsTag.getInt(key));
            });
        }
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
    
    public void addVisitor(UUID fromTownId) {
        visitors.merge(fromTownId, 1, Integer::sum);
    }
    
    public int getTotalVisitors() {
        return visitors.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public boolean isTouristSpawningEnabled() {
        return touristSpawningEnabled;
    }
    
    // Getters and setters
} 