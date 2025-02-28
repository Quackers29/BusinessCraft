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
import com.yourdomain.businesscraft.api.ITownDataProvider;
import net.minecraft.world.item.Item;

public class Town implements ITownDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Town.class);
    private final UUID id;
    private final BlockPos position;
    private String name;
    private final TownEconomyComponent economy = new TownEconomyComponent();
    private final Map<UUID, Integer> visitors = new HashMap<>();
    private boolean touristSpawningEnabled;
    private boolean cachedResult;
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private int searchRadius = 10;
    
    public Town(UUID id, BlockPos pos, String name) {
        this.id = id;
        this.position = pos;
        this.name = name;
        this.touristSpawningEnabled = true;
    }
    
    public void addBread(int count) {
        economy.addBread(count);
    }
    
    @Override
    public void addResource(Item item, int count) {
        economy.addResource(item, count);
    }
    
    @Override
    public int getResourceCount(Item item) {
        return economy.getResourceCount(item);
    }
    
    @Override
    public Map<Item, Integer> getAllResources() {
        return economy.getResources().getAllResources();
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
        
        if (pathStart != null) {
            CompoundTag startPos = new CompoundTag();
            startPos.putInt("x", pathStart.getX());
            startPos.putInt("y", pathStart.getY());
            startPos.putInt("z", pathStart.getZ());
            tag.put("PathStart", startPos);
        }
        
        if (pathEnd != null) {
            CompoundTag endPos = new CompoundTag();
            endPos.putInt("x", pathEnd.getX());
            endPos.putInt("y", pathEnd.getY());
            endPos.putInt("z", pathEnd.getZ());
            tag.put("PathEnd", endPos);
        }
        
        tag.putInt("searchRadius", searchRadius);
        tag.putBoolean("touristSpawningEnabled", touristSpawningEnabled);
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
        
        if (tag.contains("PathStart")) {
            CompoundTag startPos = tag.getCompound("PathStart");
            town.pathStart = new BlockPos(
                startPos.getInt("x"),
                startPos.getInt("y"),
                startPos.getInt("z")
            );
        }
        
        if (tag.contains("PathEnd")) {
            CompoundTag endPos = tag.getCompound("PathEnd");
            town.pathEnd = new BlockPos(
                endPos.getInt("x"),
                endPos.getInt("y"),
                endPos.getInt("z")
            );
        }
        
        town.searchRadius = tag.contains("searchRadius") ? 
            tag.getInt("searchRadius") : 10;
        
        town.touristSpawningEnabled = !tag.contains("touristSpawningEnabled") || 
            tag.getBoolean("touristSpawningEnabled");
        
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
    
    public void setPathStart(BlockPos pathStart) {
        this.pathStart = pathStart;
    }
    
    public void setPathEnd(BlockPos pathEnd) {
        this.pathEnd = pathEnd;
    }
    
    public BlockPos getPathStart() {
        return pathStart;
    }
    
    public BlockPos getPathEnd() {
        return pathEnd;
    }
    
    public int getSearchRadius() {
        return searchRadius;
    }
    
    public void setSearchRadius(int searchRadius) {
        this.searchRadius = searchRadius;
    }
    
    @Override
    public UUID getTownId() {
        return id;
    }
    
    @Override
    public void markDirty() {
        // Find the TownManager for all loaded levels and mark the town data as dirty
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.getAllLevels().forEach(level -> {
                if (level instanceof net.minecraft.server.level.ServerLevel) {
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
                    TownManager manager = TownManager.get(serverLevel);
                    if (manager.getTown(id) == this) {
                        LOGGER.debug("Marking town {} as dirty", id);
                        manager.markDirty();
                    }
                }
            });
        }
    }
    
    @Override
    public String getTownName() {
        return getName();
    }
    
    // Getters and setters
} 