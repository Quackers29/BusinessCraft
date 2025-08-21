package com.quackers29.businesscraft.data;

import java.util.List;
import java.util.UUID;

/**
 * Display data wrapper for rewards that preserves the original server-side UUID
 * for proper claim operations while providing display information for the UI.
 * 
 * This solves the UUID mismatch issue where client-side RewardEntry objects
 * get new UUIDs due to constructor limitations.
 */
public class RewardDisplayData {
    private final UUID originalId;  // Server-side UUID for claiming
    private final String source;
    private final String status;
    private final long timestamp;
    private final List<String> itemDisplayNames;
    private final List<Integer> itemCounts;
    
    public RewardDisplayData(UUID originalId, String source, String status, long timestamp,
                           List<String> itemDisplayNames, List<Integer> itemCounts) {
        this.originalId = originalId;
        this.source = source;
        this.status = status;
        this.timestamp = timestamp;
        this.itemDisplayNames = itemDisplayNames;
        this.itemCounts = itemCounts;
    }
    
    /**
     * Get the original server-side UUID for claim operations
     */
    public UUID getOriginalId() {
        return originalId;
    }
    
    /**
     * Get the reward source (e.g., "TOURIST_ARRIVAL", "MILESTONE")
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Get the reward status (e.g., "UNCLAIMED", "CLAIMED", "EXPIRED")
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Get the timestamp when the reward was created
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the display names of the items in this reward
     */
    public List<String> getItemDisplayNames() {
        return itemDisplayNames;
    }
    
    /**
     * Get the counts of the items in this reward
     */
    public List<Integer> getItemCounts() {
        return itemCounts;
    }
    
    @Override
    public String toString() {
        return String.format("RewardDisplayData{id=%s, source=%s, status=%s, items=%d}", 
            originalId, source, status, itemDisplayNames.size());
    }
}