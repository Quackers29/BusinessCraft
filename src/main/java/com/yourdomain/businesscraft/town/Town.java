package com.yourdomain.businesscraft.town;

import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import com.yourdomain.businesscraft.config.ConfigLoader;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.town.components.TownEconomyComponent;

public class Town {
    private static final Logger LOGGER = LoggerFactory.getLogger(Town.class);
    private final UUID id;
    private final BlockPos position;
    private String name;
    private final TownEconomyComponent economy = new TownEconomyComponent();
    private final Map<UUID, Integer> visitors = new HashMap<>();
    private boolean touristSpawningEnabled;
    private boolean cachedResult;
    
    public Town(UUID id, BlockPos pos, String name) {
        this.id = id;
        this.position = pos;
        this.name = name;
        this.touristSpawningEnabled = true;
    }
    
    public void addBread(int count) {
        economy.addBread(count);
    }
    
    public boolean canSpawnTourists() {
        boolean result = touristSpawningEnabled && economy.getPopulation() >= ConfigLoader.minPopForTourists;
        if (result != cachedResult) {
            LOGGER.info("SPAWN STATE CHANGE [{}] - Enabled: {}, Population: {}/{}, Result: {}",
                id, touristSpawningEnabled, economy.getPopulation(), ConfigLoader.minPopForTourists, result);
            cachedResult = result;
        }
        return result;
    }
    
    public void removeTourist() {
        economy.removePopulation(1);
    }
    
    public void save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("posX", position.getX());
        tag.putInt("posY", position.getY());
        tag.putInt("posZ", position.getZ());
        CompoundTag visitorsTag = new CompoundTag();
        visitors.forEach((visitorId, count) -> {
            visitorsTag.putInt(visitorId.toString(), count);
        });
        tag.put("visitors", visitorsTag);
        CompoundTag economyTag = new CompoundTag();
        economy.save(economyTag);
        tag.put("economy", economyTag);
    }
    
    public static Town load(CompoundTag tag) {
        BlockPos pos = new BlockPos(
            tag.getInt("posX"),
            tag.getInt("posY"),
            tag.getInt("posZ")
        );
        UUID id = tag.getUUID("id");
        Town town = new Town(id, pos, tag.getString("name"));
        if (tag.contains("visitors")) {
            CompoundTag visitorsTag = tag.getCompound("visitors");
            visitorsTag.getAllKeys().forEach(key -> {
                town.visitors.put(UUID.fromString(key), visitorsTag.getInt(key));
            });
        }
        town.economy.load(tag.getCompound("economy"));
        return town;
    }
    
    public String getName() {
        return name;
    }
    
    public int getBreadCount() {
        return economy.getBreadCount();
    }
    
    public int getPopulation() {
        return economy.getPopulation();
    }
    
    public UUID getId() {
        return id;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setTouristSpawningEnabled(boolean enabled) {
        LOGGER.info("TOGGLE [{}] - Changing from {} to {}", 
            id, touristSpawningEnabled, enabled);
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