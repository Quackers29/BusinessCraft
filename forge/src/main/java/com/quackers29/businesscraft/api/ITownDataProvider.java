package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.UUID;
import java.util.List;

/**
 * Platform-agnostic interface for accessing town data
 */
public interface ITownDataProvider {
    UUID getTownId();
    String getTownName();
    
    // Legacy resource methods
    int getBreadCount();
    
    // New generic resource methods
    void addResource(Item item, int count);
    int getResourceCount(Item item);
    Map<Item, Integer> getAllResources();
    
    // Communal storage methods
    boolean addToCommunalStorage(Item item, int count);
    int getCommunalStorageCount(Item item);
    Map<Item, Integer> getAllCommunalStorageItems();
    
    // Personal storage methods
    boolean addToPersonalStorage(UUID playerId, Item item, int count);
    int getPersonalStorageCount(UUID playerId, Item item);
    Map<Item, Integer> getPersonalStorageItems(UUID playerId);
    
    // Population methods
    int getPopulation();
    
    // Tourist methods
    int getTouristCount();
    int getMaxTourists();
    boolean canAddMoreTourists();
    
    // Other town data
    boolean isTouristSpawningEnabled();
    void setTouristSpawningEnabled(boolean enabled);
    BlockPos getPathStart();
    void setPathStart(BlockPos pos);
    BlockPos getPathEnd();
    void setPathEnd(BlockPos pos);
    int getSearchRadius();
    void setSearchRadius(int radius);
    boolean canSpawnTourists();
    void markDirty();
    
    // Legacy method - delegate to addResource
    default void addBread(int count) {
        addResource(net.minecraft.world.item.Items.BREAD, count);
    }
    
    BlockPos getPosition();
    void addVisitor(UUID fromTownId);
    int getTotalVisitors();
    
    // Visit history methods
    /**
     * Record a visit from another town with additional information
     * @param originTownId UUID of the town visitors are from
     * @param count Number of visitors
     * @param originPos Position the visitors originated from
     */
    void recordVisit(UUID originTownId, int count, BlockPos originPos);
    
    /**
     * Get all the visit history records
     * @return List of visit records
     */
    List<VisitHistoryRecord> getVisitHistory();
    
    /**
     * Record for storing visit history information.
     * This replaces the TownBlockEntity.VisitRecord class.
     */
    class VisitHistoryRecord {
        private final long timestamp;
        private final UUID originTownId;
        private final int count;
        private final BlockPos originPos;

        public VisitHistoryRecord(long timestamp, UUID originTownId, int count, BlockPos originPos) {
            this.timestamp = timestamp;
            this.originTownId = originTownId;
            this.count = count;
            this.originPos = originPos;
        }

        public long getTimestamp() { return timestamp; }
        public UUID getOriginTownId() { return originTownId; }
        public int getCount() { return count; }
        public BlockPos getOriginPos() { return originPos; }
    }
} 