package com.quackers29.businesscraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ITownDataProvider {
    UUID getTownId();

    String getTownName();

    void addResource(Item item, long count);

    long getResourceCount(Item item);

    Map<Item, Long> getAllResources();

    boolean addToCommunalStorage(Item item, long count);

    long getCommunalStorageCount(Item item);

    Map<Item, Long> getAllCommunalStorageItems();

    boolean addToPersonalStorage(UUID playerId, Item item, long count);

    long getPersonalStorageCount(UUID playerId, Item item);

    Map<Item, Long> getPersonalStorageItems(UUID playerId);

    long getPopulation();

    long getTouristCount();

    long getMaxTourists();

    boolean canAddMoreTourists();

    long getWorkUnits();

    long getWorkUnitCap();

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

    void recordVisit(UUID originTownId, int count, BlockPos originPos);

    List<VisitHistoryRecord> getVisitHistory();

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
