package com.quackers29.businesscraft.town.data;

// Platform-agnostic imports - Minecraft types abstracted through platform helpers
import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Represents a reward entry in the town payment board system.
 * This replaces the simple communal storage with a structured reward system.
 */
public class RewardEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(RewardEntry.class);
    
    private final UUID id;
    private final long timestamp;
    private long expirationTime;
    private final RewardSource source;
    private final List<ItemStack> rewards;
    private ClaimStatus status;
    private String eligibility; // "ALL" for now, expandable for player tracking
    private final Map<String, String> metadata; // Source-specific data as strings
    
    /**
     * Constructor for creating a new reward entry
     */
    public RewardEntry(RewardSource source, List<ItemStack> rewards, String eligibility) {
        this.id = UUID.randomUUID();
        this.timestamp = System.currentTimeMillis();
        this.expirationTime = timestamp + (24 * 60 * 60 * 1000L); // 24 hours default
        this.source = source;
        this.rewards = new ArrayList<>(rewards);
        this.status = ClaimStatus.UNCLAIMED;
        this.eligibility = eligibility != null ? eligibility : "ALL";
        this.metadata = new HashMap<>();
    }
    
    /**
     * Constructor for loading from NBT
     */
    private RewardEntry(UUID id, long timestamp, long expirationTime, RewardSource source, 
                       List<ItemStack> rewards, ClaimStatus status, String eligibility, 
                       Map<String, String> metadata) {
        this.id = id;
        this.timestamp = timestamp;
        this.expirationTime = expirationTime;
        this.source = source;
        this.rewards = rewards;
        this.status = status;
        this.eligibility = eligibility;
        this.metadata = metadata;
    }
    
    // Getters
    public UUID getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public long getExpirationTime() { return expirationTime; }
    public RewardSource getSource() { return source; }
    public List<ItemStack> getRewards() { return Collections.unmodifiableList(rewards); }
    public ClaimStatus getStatus() { return status; }
    public String getEligibility() { return eligibility; }
    public Map<String, String> getMetadata() { return Collections.unmodifiableMap(metadata); }
    
    // Setters
    public void setStatus(ClaimStatus status) { this.status = status; }
    public void setExpirationTime(long expirationTime) { this.expirationTime = expirationTime; }
    
    /**
     * Add metadata to this reward entry
     */
    public void addMetadata(String key, String value) {
        this.metadata.put(key, value);
    }
    
    /**
     * Check if this reward has expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }
    
    /**
     * Check if this reward can be claimed by the given eligibility criteria
     */
    public boolean canBeClaimed(String claimerEligibility) {
        if (status != ClaimStatus.UNCLAIMED) return false;
        if (isExpired()) return false;
        return "ALL".equals(eligibility) || eligibility.equals(claimerEligibility);
    }
    
    /**
     * Get a formatted string representation of the rewards
     * Uses LinkedHashMap to maintain consistent order and prevent text flipping
     */
    public String getRewardsDisplay() {
        if (rewards.isEmpty()) return "No rewards";
        
        // Use LinkedHashMap to maintain insertion order and prevent text flipping
        Map<String, Integer> combinedRewards = new LinkedHashMap<>();
        
        // Combine identical items by item name (maintains order)
        for (ItemStack stack : rewards) {
            String itemName = stack.getHoverName().getString();
            combinedRewards.merge(itemName, stack.getCount(), Integer::sum);
        }
        
        // Format the display in consistent order
        List<String> rewardStrings = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : combinedRewards.entrySet()) {
            String itemName = entry.getKey();
            int count = entry.getValue();
            rewardStrings.add(count > 1 ? count + "x " + itemName : itemName);
        }
        
        return String.join(", ", rewardStrings);
    }
    
    /**
     * Get a time ago string for display
     */
    public String getTimeAgoDisplay() {
        long diff = System.currentTimeMillis() - timestamp;
        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);
        
        if (days > 0) return days + "d ago";
        if (hours > 0) return hours + "h ago";
        if (minutes > 0) return minutes + "m ago";
        return "Just now";
    }
    
    /**
     * Get timestamp in HH:mm:ss format for display
     */
    public String getTimeDisplay() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
    
    /**
     * Get full date and time string for tooltip display
     */
    public String getFullDateTimeDisplay() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(timestamp), 
            ZoneId.systemDefault()
        );
        return dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss"));
    }
    
    /**
     * Serialize this reward entry to NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        
        tag.putUUID("id", id);
        tag.putLong("timestamp", timestamp);
        tag.putLong("expirationTime", expirationTime);
        tag.putString("source", source.name());
        tag.putString("status", status.name());
        tag.putString("eligibility", eligibility);
        
        // Save rewards
        ListTag rewardsList = new ListTag();
        for (ItemStack stack : rewards) {
            CompoundTag stackTag = new CompoundTag();
            stack.save(stackTag);
            rewardsList.add(stackTag);
        }
        tag.put("rewards", rewardsList);
        
        // Save metadata
        CompoundTag metadataTag = new CompoundTag();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            metadataTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("metadata", metadataTag);
        
        return tag;
    }
    
    /**
     * Create a reward entry from network data (for client-side sync)
     */
    public static RewardEntry fromNetwork(UUID id, long timestamp, long expirationTime, 
                                        RewardSource source, List<ItemStack> rewards, 
                                        ClaimStatus status, String eligibility) {
        return new RewardEntry(id, timestamp, expirationTime, source, rewards, status, eligibility, new HashMap<>());
    }
    
    /**
     * Create a reward entry from network data with metadata (for client-side sync)
     */
    public static RewardEntry fromNetworkWithMetadata(UUID id, long timestamp, long expirationTime, 
                                        RewardSource source, List<ItemStack> rewards, 
                                        ClaimStatus status, String eligibility, Map<String, String> metadata) {
        return new RewardEntry(id, timestamp, expirationTime, source, rewards, status, eligibility, metadata);
    }

    /**
     * Deserialize a reward entry from NBT
     */
    public static RewardEntry fromNBT(CompoundTag tag) {
        try {
            UUID id = tag.getUUID("id");
            long timestamp = tag.getLong("timestamp");
            long expirationTime = tag.getLong("expirationTime");
            
            RewardSource source;
            try {
                source = RewardSource.valueOf(tag.getString("source"));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown reward source: {}, defaulting to OTHER", tag.getString("source"));
                source = RewardSource.OTHER;
            }
            
            ClaimStatus status;
            try {
                status = ClaimStatus.valueOf(tag.getString("status"));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Unknown claim status: {}, defaulting to UNCLAIMED", tag.getString("status"));
                status = ClaimStatus.UNCLAIMED;
            }
            
            String eligibility = tag.getString("eligibility");
            if (eligibility.isEmpty()) eligibility = "ALL";
            
            // Load rewards
            List<ItemStack> rewards = new ArrayList<>();
            if (tag.contains("rewards")) {
                ListTag rewardsList = tag.getList("rewards", 10); // 10 = CompoundTag
                for (int i = 0; i < rewardsList.size(); i++) {
                    CompoundTag stackTag = rewardsList.getCompound(i);
                    ItemStack stack = ItemStack.of(stackTag);
                    if (!stack.isEmpty()) {
                        rewards.add(stack);
                    }
                }
            }
            
            // Load metadata
            Map<String, String> metadata = new HashMap<>();
            if (tag.contains("metadata")) {
                CompoundTag metadataTag = tag.getCompound("metadata");
                for (String key : metadataTag.getAllKeys()) {
                    metadata.put(key, metadataTag.getString(key));
                }
            }
            
            return new RewardEntry(id, timestamp, expirationTime, source, rewards, status, eligibility, metadata);
            
        } catch (Exception e) {
            LOGGER.error("Error deserializing RewardEntry from NBT", e);
            return null;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        RewardEntry that = (RewardEntry) obj;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("RewardEntry{id=%s, source=%s, status=%s, rewards=%d items}", 
                id, source, status, rewards.size());
    }
}
