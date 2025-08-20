package com.quackers29.businesscraft.api;

import java.util.Map;
import java.util.UUID;
import java.util.List;

/**
 * Platform-agnostic interface for accessing town data.
 * This interface abstracts town functionality across different mod platforms.
 */
public interface ITownDataProvider {
    UUID getTownId();
    String getTownName();
    void setTownName(String name);
    
    // Legacy resource methods
    int getBreadCount();
    
    // New generic resource methods - using Object instead of Item for platform independence
    void addResource(Object item, int count);
    int getResourceCount(Object item);
    Map<Object, Integer> getAllResources();
    
    // Communal storage methods
    boolean addToCommunalStorage(Object item, int count);
    int getCommunalStorageCount(Object item);
    Map<Object, Integer> getAllCommunalStorageItems();
    
    // Population methods
    int getPopulation();
    
    // Tourist methods
    int getTouristCount();
    int getMaxTourists();
    boolean canAddMoreTourists();
    
    // Other town data
    boolean isTouristSpawningEnabled();
    void setTouristSpawningEnabled(boolean enabled);
    Position getPathStart();
    void setPathStart(Position pos);
    Position getPathEnd();
    void setPathEnd(Position pos);
    int getSearchRadius();
    void setSearchRadius(int radius);
    boolean canSpawnTourists();
    void markDirty();
    
    // Legacy method - delegate to addResource
    default void addBread(int count) {
        // Platform implementations will need to provide the bread item
        // This is a marker for platform-specific implementation
    }
    
    Position getPosition();
    void addVisitor(UUID fromTownId);
    int getTotalVisitors();
    
    // Visit history methods
    /**
     * Record a visit from another town with additional information
     * @param originTownId UUID of the town visitors are from
     * @param count Number of visitors
     * @param originPos Position the visitors originated from
     */
    void recordVisit(UUID originTownId, int count, Position originPos);
    
    /**
     * Get all the visit history records
     * @return List of visit records
     */
    List<VisitHistoryRecord> getVisitHistory();
    
    /**
     * Platform-agnostic position representation
     */
    interface Position {
        int getX();
        int getY();
        int getZ();
        
        /**
         * Format position as a short string for debugging/display
         */
        default String toShortString() {
            return String.format("[%d, %d, %d]", getX(), getY(), getZ());
        }
        
        /**
         * Calculate squared distance to another position
         */
        default double distSqr(Position other) {
            double dx = getX() - other.getX();
            double dy = getY() - other.getY();
            double dz = getZ() - other.getZ();
            return dx * dx + dy * dy + dz * dz;
        }
        
        /**
         * Calculate distance to another position
         */
        default double distanceTo(Position other) {
            return Math.sqrt(distSqr(other));
        }
    }
    
    /**
     * Record for storing visit history information.
     * Platform-agnostic representation of town visit data.
     * UNIFIED ARCHITECTURE: Stores only UUIDs, names resolved fresh from server when needed
     */
    class VisitHistoryRecord {
        private final long timestamp;
        private final UUID originTownId;
        private final int count;
        private final Position originPos;

        public VisitHistoryRecord(long timestamp, UUID originTownId, int count, Position originPos) {
            this.timestamp = timestamp;
            this.originTownId = originTownId;
            this.count = count;
            this.originPos = originPos;
        }

        public long getTimestamp() { return timestamp; }
        public UUID getOriginTownId() { return originTownId; }
        public int getCount() { return count; }
        public Position getOriginPos() { return originPos; }
    }
}