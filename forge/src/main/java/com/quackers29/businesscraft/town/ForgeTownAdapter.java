package com.quackers29.businesscraft.town;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.platform.ForgePosition;
import com.quackers29.businesscraft.util.PositionConverter;
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
    public void setTownName(String name) {
        forgeTown.setName(name);
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
        ITownDataProvider.Position pos = forgeTown.getPathStart();
        return pos != null ? new ForgePosition(pos.getX(), pos.getY(), pos.getZ()) : null;
    }
    
    @Override
    public void setPathStart(Position pos) {
        if (pos != null) {
            forgeTown.setPathStart(pos.getX(), pos.getY(), pos.getZ());
        } else {
            forgeTown.setPathStart(0, 0, 0); // Use a default position when null
        }
    }
    
    @Override
    public Position getPathEnd() {
        ITownDataProvider.Position pos = forgeTown.getPathEnd();
        return pos != null ? new ForgePosition(pos.getX(), pos.getY(), pos.getZ()) : null;
    }
    
    @Override
    public void setPathEnd(Position pos) {
        if (pos != null) {
            forgeTown.setPathEnd(pos.getX(), pos.getY(), pos.getZ());
        } else {
            forgeTown.setPathEnd(0, 0, 0); // Use a default position when null
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
        ITownDataProvider.Position pos = forgeTown.getPosition();
        return new ForgePosition(pos.getX(), pos.getY(), pos.getZ());
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
        // Convert BlockPos to Position using the utility converter
        ITownDataProvider.Position position = PositionConverter.toPosition(blockPos);
        forgeTown.recordVisit(originTownId, count, position);
    }
    
    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        return forgeTown.getVisitHistory().stream()
            .map(record -> new VisitHistoryRecord(
                record.getTimestamp(),
                record.getOriginTownId(),
                record.getCount(),
                new ForgePosition(record.getOriginPos().getX(), record.getOriginPos().getY(), record.getOriginPos().getZ())
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