package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.UUID;
import java.util.List;

/**
 * Platform-agnostic interface for accessing town data.
 * Uses long for resource counts to support large-scale economies.
 */
public interface ITownDataProvider {
    UUID getTownId();

    String getTownName();

    // New generic resource methods
    void addResource(Item item, long count);

    long getResourceCount(Item item);

    Map<Item, Long> getAllResources();

    // Communal storage methods
    boolean addToCommunalStorage(Item item, long count);

    long getCommunalStorageCount(Item item);

    Map<Item, Long> getAllCommunalStorageItems();

    // Personal storage methods
    boolean addToPersonalStorage(UUID playerId, Item item, long count);

    long getPersonalStorageCount(UUID playerId, Item item);

    Map<Item, Long> getPersonalStorageItems(UUID playerId);

    // Population methods
    long getPopulation();

    // Tourist methods
    long getTouristCount();

    long getMaxTourists();

    boolean canAddMoreTourists();

    // Work Units
    long getWorkUnits();

    long getWorkUnitCap();

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

    BlockPos getPosition();

    void addVisitor(UUID fromTownId);

    int getTotalVisitors();

    // Visit history methods
    /**
     * Record a visit from another town with additional information
     *
     * @param originTownId UUID of the town visitors are from
     * @param count        Number of visitors
     * @param originPos    Position the visitors originated from
     */
    void recordVisit(UUID originTownId, int count, BlockPos originPos);

    /**
     * Get all the visit history records
     *
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

        public long getTimestamp() {
            return timestamp;
        }

        public UUID getOriginTownId() {
            return originTownId;
        }

        public int getCount() {
            return count;
        }

        public BlockPos getOriginPos() {
            return originPos;
        }
    }
}
