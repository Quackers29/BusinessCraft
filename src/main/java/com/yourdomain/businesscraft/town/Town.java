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
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

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
    
    // Visit history storage - moved from TownBlockEntity
    private final List<VisitHistoryRecord> visitHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 50; // Maximum history entries to keep
    
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
        
        // Save visit history
        if (!visitHistory.isEmpty()) {
            ListTag historyTag = new ListTag();
            for (VisitHistoryRecord record : visitHistory) {
                CompoundTag visitTag = new CompoundTag();
                visitTag.putLong("timestamp", record.getTimestamp());
                visitTag.putUUID("townId", record.getOriginTownId());
                visitTag.putInt("count", record.getCount());
                
                // Save origin position
                if (record.getOriginPos() != null && record.getOriginPos() != BlockPos.ZERO) {
                    CompoundTag posTag = new CompoundTag();
                    posTag.putInt("x", record.getOriginPos().getX());
                    posTag.putInt("y", record.getOriginPos().getY());
                    posTag.putInt("z", record.getOriginPos().getZ());
                    visitTag.put("pos", posTag);
                }
                
                historyTag.add(visitTag);
            }
            tag.put("visitHistory", historyTag);
        }
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
        
        // Load visit history
        if (tag.contains("visitHistory")) {
            ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < historyTag.size(); i++) {
                CompoundTag visitTag = historyTag.getCompound(i);
                
                long timestamp = visitTag.getLong("timestamp");
                UUID originTownId;
                
                // Support both new UUID format and legacy string format
                if (visitTag.contains("townId")) {
                    originTownId = visitTag.getUUID("townId");
                } else if (visitTag.contains("town")) {
                    // Legacy format - convert town name to a deterministic UUID
                    // This ensures backward compatibility
                    String townName = visitTag.getString("town");
                    originTownId = UUID.nameUUIDFromBytes(("town:" + townName).getBytes());
                    LOGGER.info("Converting legacy town name '{}' to UUID: {}", townName, originTownId);
                } else {
                    // No valid identifier, use a placeholder UUID
                    originTownId = new UUID(0, 0);
                }
                
                int count = visitTag.getInt("count");
                
                BlockPos originPos = BlockPos.ZERO;
                if (visitTag.contains("pos")) {
                    CompoundTag posTag = visitTag.getCompound("pos");
                    originPos = new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z")
                    );
                }
                
                town.visitHistory.add(new VisitHistoryRecord(timestamp, originTownId, count, originPos));
            }
        }
        
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
    
    // Visit history implementation
    @Override
    public void recordVisit(UUID originTownId, int count, BlockPos originPos) {
        long timestamp = System.currentTimeMillis();
        
        // Create the visit record
        VisitHistoryRecord record = new VisitHistoryRecord(timestamp, originTownId, count, originPos);
        
        // Add to the beginning of the list (newest first)
        visitHistory.add(0, record);
        
        // Trim if we exceed the maximum history size
        while (visitHistory.size() > MAX_HISTORY_SIZE) {
            visitHistory.remove(visitHistory.size() - 1);
        }
        
        // Mark as dirty to ensure it's saved
        markDirty();
    }
    
    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        return Collections.unmodifiableList(visitHistory);
    }
} 