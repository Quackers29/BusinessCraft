package com.yourdomain.businesscraft.town.data;

import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Buffers visitor arrivals to group them together and process them in batches.
 * This prevents spam notifications and allows for better visitor tracking.
 * 
 * Extracted from TownBlockEntity to improve code organization.
 */
public class VisitBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitBuffer.class);
    private static final long BUFFER_TIMEOUT_MS = 1000; // 1 second timeout
    
    // Single value object to store visitor count and origin position
    private static class VisitorInfo {
        final int count;
        final BlockPos originPos;
        double totalDistance;
        
        public VisitorInfo(int count, BlockPos originPos) {
            this.count = count;
            this.originPos = originPos;
            this.totalDistance = 0;
        }
        
        public VisitorInfo incrementCount() {
            VisitorInfo newInfo = new VisitorInfo(count + 1, originPos);
            newInfo.totalDistance = this.totalDistance; // Preserve the accumulated distance
            return newInfo;
        }
        
        public VisitorInfo addDistance(double distance) {
            this.totalDistance += distance;
            return this;
        }
        
        public double getAverageDistance() {
            return count > 0 ? totalDistance / count : 0;
        }
        
        @Override
        public String toString() {
            return "VisitorInfo{count=" + count + 
                ", originPos=" + originPos + 
                ", totalDistance=" + totalDistance + 
                ", avgDistance=" + getAverageDistance() + "}";
        }
    }
    
    private final Map<UUID, VisitorInfo> visitors = new HashMap<>();
    // Add a map to store distances that persists after processing
    private final Map<UUID, Double> distanceMap = new HashMap<>();
    private long lastVisitTime = 0;

    /**
     * Adds a visitor to the buffer
     * @param townId The ID of the town the visitor is from
     * @param originPos The origin position of the visitor
     */
    public void addVisitor(UUID townId, BlockPos originPos) {
        // Update or insert a new VisitorInfo
        visitors.compute(townId, (id, info) -> {
            if (info == null) {
                return new VisitorInfo(1, originPos);
            } else {
                return info.incrementCount();
            }
        });
        
        lastVisitTime = System.currentTimeMillis();
    }
    
    /**
     * Updates the distance information for a visitor
     * @param townId The ID of the town
     * @param distance The distance traveled
     */
    public void updateVisitorDistance(UUID townId, double distance) {
        visitors.computeIfPresent(townId, (id, info) -> info.addDistance(distance));
    }
    
    /**
     * Gets the average distance for visitors from a specific town
     * @param townId The town ID
     * @return The average distance, or 0 if no data available
     */
    public double getAverageDistance(UUID townId) {
        // First check the visitor buffer
        VisitorInfo info = visitors.get(townId);
        if (info != null && info.getAverageDistance() > 0) {
            return info.getAverageDistance();
        }
        // If not found or zero, check the persistent distance map
        return distanceMap.getOrDefault(townId, 0.0);
    }
    
    /**
     * Gets the number of different towns with visitors in the buffer
     * @return The visitor count
     */
    public int getVisitorCount() {
        return visitors.size();
    }
    
    /**
     * Checks if the buffer should be processed (has visitors and timeout reached)
     * @return true if the buffer should be processed
     */
    public boolean shouldProcess() {
        return !visitors.isEmpty() && 
               System.currentTimeMillis() - lastVisitTime > BUFFER_TIMEOUT_MS;
    }

    /**
     * Processes all visitors in the buffer and returns visit history records
     * @return List of visit history records
     */
    public List<VisitHistoryRecord> processVisits() {
        if (visitors.isEmpty()) return Collections.emptyList();
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "VISIT BUFFER - Processing visits: {}", this.toString());
        
        // Store distances in the persistent map before clearing the buffer
        visitors.forEach((townId, info) -> {
            double avgDistance = info.getAverageDistance();
            if (avgDistance > 0) {
                distanceMap.put(townId, avgDistance);
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "VISIT BUFFER - Storing distance for townId {}: {}", townId, avgDistance);
            }
        });
        
        long now = System.currentTimeMillis();
        List<VisitHistoryRecord> records = visitors.entrySet().stream()
            .map(entry -> new VisitHistoryRecord(
                now, 
                entry.getKey(), 
                entry.getValue().count,
                entry.getValue().originPos
            ))
            .collect(Collectors.toList());
            
        // Clear the visitors buffer but keep the distanceMap
        visitors.clear();
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "VISIT BUFFER - After clearing: {}", this.toString());
        
        return records;
    }
    
    /**
     * Clears saved distance data for a specific town
     * @param townId The town ID to clear distance data for
     */
    public void clearSavedDistance(UUID townId) {
        Double removed = distanceMap.remove(townId);
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "VISIT BUFFER - Cleared saved distance for townId {}: {}", townId, removed != null ? removed : 0);
    }
    
    /**
     * Detailed toString method for debugging
     */
    @Override
    public String toString() {
        return "VisitBuffer{visitors=" + 
            visitors.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue().count + 
                    ", distance=" + e.getValue().totalDistance + 
                    ", avgDist=" + e.getValue().getAverageDistance())
                .collect(Collectors.joining("; ")) +
            ", distanceMap=" + distanceMap +
            ", lastVisitTime=" + lastVisitTime + "}";
    }
} 