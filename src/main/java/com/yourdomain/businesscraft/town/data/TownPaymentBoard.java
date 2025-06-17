package com.yourdomain.businesscraft.town.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages the payment board system for a town, replacing the simple communal storage
 * with a structured reward system that tracks sources, timestamps, and claim status.
 */
public class TownPaymentBoard {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownPaymentBoard.class);
    
    private final List<RewardEntry> rewards = new ArrayList<>();
    private final Map<Item, Integer> bufferStorage = new HashMap<>(); // 2x9 buffer storage
    
    // Configuration
    private static final int MAX_REWARDS = 100; // Maximum number of rewards to keep
    private static final long DEFAULT_EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L; // 7 days
    
    /**
     * Add a new reward to the payment board
     */
    public UUID addReward(RewardSource source, List<ItemStack> rewardItems, String eligibility) {
        if (rewardItems == null || rewardItems.isEmpty()) {
            LOGGER.warn("Attempted to add empty reward to payment board");
            return null;
        }
        
        RewardEntry entry = new RewardEntry(source, rewardItems, eligibility);
        entry.setExpirationTime(System.currentTimeMillis() + DEFAULT_EXPIRATION_TIME);
        
        rewards.add(entry);
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Added reward to payment board: {} from {} with {} items", 
            entry.getId(), source.getDisplayName(), rewardItems.size());
        
        // Clean up old rewards if we have too many
        if (rewards.size() > MAX_REWARDS) {
            cleanupExpiredRewards();
            if (rewards.size() > MAX_REWARDS) {
                // Remove oldest rewards if still over limit
                rewards.sort(Comparator.comparing(RewardEntry::getTimestamp));
                while (rewards.size() > MAX_REWARDS) {
                    RewardEntry removed = rewards.remove(0);
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                        "Removed old reward due to size limit: {}", removed.getId());
                }
            }
        }
        
        return entry.getId();
    }
    
    /**
     * Get all unclaimed rewards
     */
    public List<RewardEntry> getUnclaimedRewards() {
        cleanupExpiredRewards();
        return rewards.stream()
                .filter(entry -> entry.getStatus() == ClaimStatus.UNCLAIMED && !entry.isExpired())
                .sorted(Comparator.comparing(RewardEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get all rewards (for display purposes)
     */
    public List<RewardEntry> getAllRewards() {
        return rewards.stream()
                .sorted(Comparator.comparing(RewardEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Get rewards by source type
     */
    public List<RewardEntry> getRewardsBySource(RewardSource source) {
        return rewards.stream()
                .filter(entry -> entry.getSource() == source)
                .sorted(Comparator.comparing(RewardEntry::getTimestamp).reversed())
                .collect(Collectors.toList());
    }
    
    /**
     * Claim a reward by ID, attempting to place items in player inventory first,
     * then in buffer storage if inventory is full
     */
    public ClaimResult claimReward(UUID rewardId, String claimerEligibility, boolean toBuffer) {
        RewardEntry entry = findRewardById(rewardId);
        if (entry == null) {
            return new ClaimResult(false, "Reward not found");
        }
        
        if (!entry.canBeClaimed(claimerEligibility)) {
            String reason = entry.getStatus() == ClaimStatus.CLAIMED ? "already claimed" :
                           entry.getStatus() == ClaimStatus.EXPIRED ? "expired" :
                           entry.isExpired() ? "expired" : "not eligible";
            return new ClaimResult(false, "Reward cannot be claimed: " + reason);
        }
        
        if (toBuffer) {
            // Claim directly to buffer
            boolean success = addToBufferStorage(entry.getRewards());
            if (success) {
                entry.setStatus(ClaimStatus.CLAIMED);
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                    "Claimed reward {} to buffer storage", rewardId);
                return new ClaimResult(true, "Claimed to buffer storage", entry.getRewards());
            } else {
                return new ClaimResult(false, "Buffer storage is full");
            }
        } else {
            // For claiming to inventory, we'll return the items and let the caller handle inventory placement
            entry.setStatus(ClaimStatus.CLAIMED);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                "Claimed reward {} to inventory", rewardId);
            return new ClaimResult(true, "Claimed to inventory", entry.getRewards());
        }
    }
    
    /**
     * Add items to buffer storage (2x9 grid)
     */
    private boolean addToBufferStorage(List<ItemStack> items) {
        // Calculate available space in buffer (18 slots total)
        int usedSlots = 0;
        for (Integer count : bufferStorage.values()) {
            usedSlots += (count + 63) / 64; // Ceiling division for stack sizes
        }
        
        if (usedSlots >= 18) {
            return false; // Buffer is full
        }
        
        // Add items to buffer storage
        for (ItemStack stack : items) {
            bufferStorage.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        
        return true;
    }
    
    /**
     * Get buffer storage items
     */
    public Map<Item, Integer> getBufferStorage() {
        return Collections.unmodifiableMap(bufferStorage);
    }
    
    /**
     * Add items to buffer storage directly
     */
    public boolean addToBuffer(Item item, int count) {
        if (count <= 0) return true;
        
        bufferStorage.merge(item, count, Integer::sum);
        return true;
    }
    
    /**
     * Remove items from buffer storage
     */
    public boolean removeFromBuffer(Item item, int count) {
        if (count <= 0) return true;
        
        int currentAmount = bufferStorage.getOrDefault(item, 0);
        if (currentAmount < count) {
            return false; // Not enough items
        }
        
        int newAmount = currentAmount - count;
        if (newAmount > 0) {
            bufferStorage.put(item, newAmount);
        } else {
            bufferStorage.remove(item);
        }
        
        return true;
    }
    
    /**
     * Clean up expired and old rewards
     */
    public void cleanupExpiredRewards() {
        int removedCount = 0;
        Iterator<RewardEntry> iterator = rewards.iterator();
        while (iterator.hasNext()) {
            RewardEntry entry = iterator.next();
            if (entry.isExpired() && entry.getStatus() != ClaimStatus.CLAIMED) {
                entry.setStatus(ClaimStatus.EXPIRED);
            }
            
            // Remove very old expired rewards (older than 30 days)
            long thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L);
            if (entry.getTimestamp() < thirtyDaysAgo && entry.getStatus() == ClaimStatus.EXPIRED) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                "Cleaned up {} old expired rewards", removedCount);
        }
    }
    
    /**
     * Find a reward by ID
     */
    private RewardEntry findRewardById(UUID rewardId) {
        return rewards.stream()
                .filter(entry -> entry.getId().equals(rewardId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Get statistics about the payment board
     */
    public PaymentBoardStats getStats() {
        cleanupExpiredRewards();
        
        long unclaimed = rewards.stream().filter(r -> r.getStatus() == ClaimStatus.UNCLAIMED).count();
        long claimed = rewards.stream().filter(r -> r.getStatus() == ClaimStatus.CLAIMED).count();
        long expired = rewards.stream().filter(r -> r.getStatus() == ClaimStatus.EXPIRED).count();
        
        return new PaymentBoardStats(unclaimed, claimed, expired, rewards.size());
    }
    
    /**
     * Serialize to NBT
     */
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        
        // Save rewards
        ListTag rewardsList = new ListTag();
        for (RewardEntry entry : rewards) {
            rewardsList.add(entry.toNBT());
        }
        tag.put("rewards", rewardsList);
        
        // Save buffer storage
        CompoundTag bufferTag = new CompoundTag();
        for (Map.Entry<Item, Integer> entry : bufferStorage.entrySet()) {
            String itemKey = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(entry.getKey()).toString();
            bufferTag.putInt(itemKey, entry.getValue());
        }
        tag.put("bufferStorage", bufferTag);
        
        return tag;
    }
    
    /**
     * Deserialize from NBT
     */
    public void fromNBT(CompoundTag tag) {
        rewards.clear();
        bufferStorage.clear();
        
        // Load rewards
        if (tag.contains("rewards")) {
            ListTag rewardsList = tag.getList("rewards", 10); // 10 = CompoundTag
            for (int i = 0; i < rewardsList.size(); i++) {
                RewardEntry entry = RewardEntry.fromNBT(rewardsList.getCompound(i));
                if (entry != null) {
                    rewards.add(entry);
                }
            }
        }
        
        // Load buffer storage
        if (tag.contains("bufferStorage")) {
            CompoundTag bufferTag = tag.getCompound("bufferStorage");
            for (String key : bufferTag.getAllKeys()) {
                try {
                    net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(key);
                    Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null) {
                        int count = bufferTag.getInt(key);
                        if (count > 0) {
                            bufferStorage.put(item, count);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading buffer storage item: {}", key, e);
                }
            }
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Loaded payment board with {} rewards and {} buffer items", 
            rewards.size(), bufferStorage.size());
    }
    
    /**
     * Result of a claim operation
     */
    public static class ClaimResult {
        private final boolean success;
        private final String message;
        private final List<ItemStack> claimedItems;
        
        public ClaimResult(boolean success, String message) {
            this(success, message, Collections.emptyList());
        }
        
        public ClaimResult(boolean success, String message, List<ItemStack> claimedItems) {
            this.success = success;
            this.message = message;
            this.claimedItems = claimedItems != null ? new ArrayList<>(claimedItems) : Collections.emptyList();
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public List<ItemStack> getClaimedItems() { return Collections.unmodifiableList(claimedItems); }
    }
    
    /**
     * Statistics about the payment board
     */
    public static class PaymentBoardStats {
        private final long unclaimedCount;
        private final long claimedCount;
        private final long expiredCount;
        private final long totalCount;
        
        public PaymentBoardStats(long unclaimedCount, long claimedCount, long expiredCount, long totalCount) {
            this.unclaimedCount = unclaimedCount;
            this.claimedCount = claimedCount;
            this.expiredCount = expiredCount;
            this.totalCount = totalCount;
        }
        
        public long getUnclaimedCount() { return unclaimedCount; }
        public long getClaimedCount() { return claimedCount; }
        public long getExpiredCount() { return expiredCount; }
        public long getTotalCount() { return totalCount; }
    }
}