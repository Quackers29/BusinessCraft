package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.platform.ForgePosition;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter that makes the Forge Town class compatible with the common ITownDataProvider interface.
 * This allows gradual migration without breaking existing functionality.
 */
public class ForgeTownAdapter implements ITownDataProvider {
    private final Town forgeTown;
    
    public ForgeTownAdapter(Town forgeTown) {
        this.forgeTown = forgeTown;
    }
    
    @Override
    public UUID getTownId() {
        return forgeTown.getId();
    }
    
    @Override
    public String getTownName() {
        return forgeTown.getName();
    }
    
    @Override
    public int getBreadCount() {
        return forgeTown.getBreadCount();
    }
    
    @Override
    public void addResource(Object item, int count) {
        if (item instanceof Item) {
            forgeTown.addResource((Item) item, count);
        } else {
            throw new IllegalArgumentException("Expected Minecraft Item, got: " + 
                (item != null ? item.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public int getResourceCount(Object item) {
        if (item instanceof Item) {
            return forgeTown.getResourceCount((Item) item);
        } else {
            throw new IllegalArgumentException("Expected Minecraft Item, got: " + 
                (item != null ? item.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public Map<Object, Integer> getAllResources() {
        return forgeTown.getAllResources().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> (Object) entry.getKey(),
                Map.Entry::getValue
            ));
    }
    
    @Override
    public boolean addToCommunalStorage(Object item, int count) {
        if (item instanceof Item) {
            return forgeTown.addToCommunalStorage((Item) item, count);
        } else {
            throw new IllegalArgumentException("Expected Minecraft Item, got: " + 
                (item != null ? item.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public int getCommunalStorageCount(Object item) {
        if (item instanceof Item) {
            return forgeTown.getCommunalStorageCount((Item) item);
        } else {
            throw new IllegalArgumentException("Expected Minecraft Item, got: " + 
                (item != null ? item.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public Map<Object, Integer> getAllCommunalStorageItems() {
        return forgeTown.getAllCommunalStorageItems().entrySet().stream()
            .collect(Collectors.toMap(
                entry -> (Object) entry.getKey(),
                Map.Entry::getValue
            ));
    }
    
    @Override
    public boolean addToPersonalStorage(UUID playerId, Object item, int count) {
        if (item instanceof Item) {
            return forgeTown.addToPersonalStorage(playerId, (Item) item, count);
        } else {
            throw new IllegalArgumentException("Expected Minecraft Item, got: " + 
                (item != null ? item.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public int getPersonalStorageCount(UUID playerId, Object item) {
        if (item instanceof Item) {
            return forgeTown.getPersonalStorageCount(playerId, (Item) item);
        } else {
            throw new IllegalArgumentException("Expected Minecraft Item, got: " + 
                (item != null ? item.getClass().getSimpleName() : "null"));
        }
    }
    
    @Override
    public Map<Object, Integer> getPersonalStorageItems(UUID playerId) {
        return forgeTown.getPersonalStorageItems(playerId).entrySet().stream()
            .collect(Collectors.toMap(
                entry -> (Object) entry.getKey(),
                Map.Entry::getValue
            ));
    }
    
    @Override
    public int getPopulation() {
        return forgeTown.getPopulation();
    }
    
    @Override
    public int getTouristCount() {
        return forgeTown.getTouristCount();
    }
    
    @Override
    public int getMaxTourists() {
        return forgeTown.getMaxTourists();
    }
    
    @Override
    public boolean canAddMoreTourists() {
        return forgeTown.canAddMoreTourists();
    }
    
    @Override
    public boolean isTouristSpawningEnabled() {
        return forgeTown.isTouristSpawningEnabled();
    }
    
    @Override
    public void setTouristSpawningEnabled(boolean enabled) {
        forgeTown.setTouristSpawningEnabled(enabled);
    }
    
    @Override
    public Position getPathStart() {
        BlockPos pos = forgeTown.getPathStartForge();
        return pos != null ? new ForgePosition(pos) : null;
    }
    
    @Override
    public void setPathStart(Position pos) {
        if (pos instanceof ForgePosition) {
            forgeTown.setPathStartForge(((ForgePosition) pos).getBlockPos());
        } else if (pos != null) {
            forgeTown.setPathStartForge(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        } else {
            forgeTown.setPathStartForge(null);
        }
    }
    
    @Override
    public Position getPathEnd() {
        BlockPos pos = forgeTown.getPathEndForge();
        return pos != null ? new ForgePosition(pos) : null;
    }
    
    @Override
    public void setPathEnd(Position pos) {
        if (pos instanceof ForgePosition) {
            forgeTown.setPathEndForge(((ForgePosition) pos).getBlockPos());
        } else if (pos != null) {
            forgeTown.setPathEndForge(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        } else {
            forgeTown.setPathEndForge(null);
        }
    }
    
    @Override
    public int getSearchRadius() {
        return forgeTown.getSearchRadius();
    }
    
    @Override
    public void setSearchRadius(int radius) {
        forgeTown.setSearchRadius(radius);
    }
    
    @Override
    public boolean canSpawnTourists() {
        return forgeTown.canSpawnTourists();
    }
    
    @Override
    public void markDirty() {
        forgeTown.markDirty();
    }
    
    @Override
    public Position getPosition() {
        return new ForgePosition(forgeTown.getPositionForge());
    }
    
    @Override
    public void addVisitor(UUID fromTownId) {
        forgeTown.addVisitor(fromTownId);
    }
    
    @Override
    public int getTotalVisitors() {
        return forgeTown.getTotalVisitors();
    }
    
    @Override
    public void recordVisit(UUID originTownId, int count, Position originPos) {
        BlockPos blockPos;
        if (originPos instanceof ForgePosition) {
            blockPos = ((ForgePosition) originPos).getBlockPos();
        } else {
            blockPos = new BlockPos(originPos.getX(), originPos.getY(), originPos.getZ());
        }
        forgeTown.recordVisit(originTownId, count, blockPos);
    }
    
    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        return forgeTown.getVisitHistoryForge().stream()
            .map(forgeRecord -> new VisitHistoryRecord(
                forgeRecord.getTimestamp(),
                forgeRecord.getOriginTownId(),
                forgeRecord.getCount(),
                new ForgePosition(forgeRecord.getOriginPos())
            ))
            .collect(Collectors.toList());
    }
    
    /**
     * Get the underlying Forge Town for platform-specific operations
     */
    public Town getForgeTown() {
        return forgeTown;
    }
}