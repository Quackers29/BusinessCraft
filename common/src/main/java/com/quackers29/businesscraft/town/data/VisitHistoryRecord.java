package com.quackers29.businesscraft.town.data;

import java.util.UUID;

/**
 * Platform-agnostic visit history record for town tourism tracking.
 * This replaces ForgeVisitHistoryRecord and avoids platform-specific dependencies.
 * 
 * Uses primitive coordinates instead of BlockPos for platform independence.
 * 
 * Enhanced MultiLoader approach: Common business logic uses this class,
 * platform modules can convert to/from their specific position types.
 */
public class VisitHistoryRecord {
    private final long timestamp;
    private final UUID originTownId;
    private final int count;
    private final int originX, originY, originZ;

    /**
     * Create a new visit history record.
     * 
     * @param timestamp When the visit occurred (System.currentTimeMillis())
     * @param originTownId UUID of the town where tourists originated
     * @param count Number of tourists in this visit
     * @param originX X coordinate of origin town
     * @param originY Y coordinate of origin town  
     * @param originZ Z coordinate of origin town
     */
    public VisitHistoryRecord(long timestamp, UUID originTownId, int count, int originX, int originY, int originZ) {
        this.timestamp = timestamp;
        this.originTownId = originTownId;
        this.count = count;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
    }

    /**
     * Get the timestamp when this visit occurred.
     * 
     * @return Timestamp in milliseconds since epoch
     */
    public long getTimestamp() { 
        return timestamp; 
    }
    
    /**
     * Get the UUID of the town where tourists originated.
     * 
     * @return Origin town UUID, or null if unknown
     */
    public UUID getOriginTownId() { 
        return originTownId; 
    }
    
    /**
     * Get the number of tourists in this visit.
     * 
     * @return Tourist count (positive integer)
     */
    public int getCount() { 
        return count; 
    }
    
    /**
     * Get the X coordinate of the origin town.
     * 
     * @return X coordinate in world space
     */
    public int getOriginX() { 
        return originX; 
    }
    
    /**
     * Get the Y coordinate of the origin town.
     * 
     * @return Y coordinate in world space
     */
    public int getOriginY() { 
        return originY; 
    }
    
    /**
     * Get the Z coordinate of the origin town.
     * 
     * @return Z coordinate in world space
     */
    public int getOriginZ() { 
        return originZ; 
    }
    
    /**
     * Get origin coordinates as an array for easy platform conversion.
     * 
     * @return int array [x, y, z]
     */
    public int[] getOriginCoordinates() {
        return new int[]{originX, originY, originZ};
    }
    
    /**
     * Create a VisitHistoryRecord from platform-specific position data.
     * This is a helper method for platform modules to easily create records.
     * 
     * @param timestamp Visit timestamp
     * @param originTownId Origin town UUID
     * @param count Tourist count
     * @param coordinates Position coordinates as [x, y, z] array
     * @return New VisitHistoryRecord instance
     */
    public static VisitHistoryRecord fromCoordinates(long timestamp, UUID originTownId, int count, int[] coordinates) {
        if (coordinates.length != 3) {
            throw new IllegalArgumentException("Coordinates array must have exactly 3 elements [x, y, z]");
        }
        return new VisitHistoryRecord(timestamp, originTownId, count, coordinates[0], coordinates[1], coordinates[2]);
    }
    
    @Override
    public String toString() {
        return String.format("VisitHistoryRecord{timestamp=%d, originTownId=%s, count=%d, origin=(%d,%d,%d)}", 
            timestamp, originTownId, count, originX, originY, originZ);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        VisitHistoryRecord that = (VisitHistoryRecord) obj;
        
        return timestamp == that.timestamp &&
               count == that.count &&
               originX == that.originX &&
               originY == that.originY &&
               originZ == that.originZ &&
               (originTownId != null ? originTownId.equals(that.originTownId) : that.originTownId == null);
    }
    
    @Override
    public int hashCode() {
        int result = (int) (timestamp ^ (timestamp >>> 32));
        result = 31 * result + (originTownId != null ? originTownId.hashCode() : 0);
        result = 31 * result + count;
        result = 31 * result + originX;
        result = 31 * result + originY;
        result = 31 * result + originZ;
        return result;
    }
}