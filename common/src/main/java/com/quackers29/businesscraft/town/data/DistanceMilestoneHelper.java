package com.quackers29.businesscraft.town.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import com.quackers29.businesscraft.api.PlatformAccess;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Helper class for processing distance-based milestone rewards.
 * Checks if tourist distances meet milestone thresholds and processes reward delivery.
 */
public class DistanceMilestoneHelper {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    /**
     * Represents a milestone achievement result
     */
    public static class MilestoneResult {
        public final double actualDistance;    // The actual distance traveled
        public final int milestoneAchieved;   // The milestone threshold achieved
        public final List<ItemStack> rewards;
        public final int touristCount;
        
        public MilestoneResult(double actualDistance, int milestoneAchieved, List<ItemStack> rewards, int touristCount) {
            this.actualDistance = actualDistance;
            this.milestoneAchieved = milestoneAchieved;
            this.rewards = rewards;
            this.touristCount = touristCount;
        }
        
        public boolean hasRewards() {
            return !rewards.isEmpty();
        }
    }
    
    /**
     * Checks if the given distance meets any milestone thresholds and returns applicable rewards.
     * 
     * @param distance The travel distance in blocks
     * @param touristCount Number of tourists that traveled this distance
     * @return MilestoneResult containing rewards if milestone achieved, or empty result if not
     */
    public static MilestoneResult checkMilestones(double distance, int touristCount) {
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "MILESTONE CHECK - Starting check for distance: {} blocks, tourists: {}", distance, touristCount);
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "MILESTONE CHECK - enableMilestones: {}", ConfigLoader.enableMilestones);
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "MILESTONE CHECK - Available milestones: {}", ConfigLoader.milestoneRewards.keySet());
        
        // Return empty result if milestones are disabled
        if (!ConfigLoader.enableMilestones) {
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Milestones disabled in config, skipping check for distance: {}", distance);
            return new MilestoneResult(distance, -1, new ArrayList<>(), touristCount);
        }
        
        // Find the highest milestone that this distance achieves
        int achievedMilestone = -1;
        for (int milestoneDistance : ConfigLoader.milestoneRewards.keySet()) {
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "MILESTONE CHECK - Checking milestone: {} blocks, distance >= milestone: {}", 
                            milestoneDistance, distance >= milestoneDistance);
            if (distance >= milestoneDistance && milestoneDistance > achievedMilestone) {
                achievedMilestone = milestoneDistance;
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "MILESTONE CHECK - New achieved milestone: {}", achievedMilestone);
            }
        }
        
        // No milestone achieved
        if (achievedMilestone == -1) {
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "No milestone achieved for distance: {} blocks", distance);
            return new MilestoneResult(distance, -1, new ArrayList<>(), touristCount);
        }
        
        // Process rewards for achieved milestone
        List<String> rewardStrings = ConfigLoader.milestoneRewards.get(achievedMilestone);
        List<ItemStack> rewards = parseRewards(rewardStrings, touristCount);
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Milestone achieved! Distance: {} blocks, Milestone: {} blocks, Rewards: {}", 
                        distance, achievedMilestone, rewards.size());
        
        return new MilestoneResult(distance, achievedMilestone, rewards, touristCount);
    }
    
    /**
     * Parses reward strings from config into ItemStacks.
     * 
     * @param rewardStrings List of "item:count" format strings from config
     * @param touristCount Number of tourists (for scaling rewards if needed)
     * @return List of ItemStacks representing the rewards
     */
    private static List<ItemStack> parseRewards(List<String> rewardStrings, int touristCount) {
        List<ItemStack> rewards = new ArrayList<>();
        
        if (rewardStrings == null || rewardStrings.isEmpty()) {
            return rewards;
        }
        
        for (String rewardStr : rewardStrings) {
            ItemStack reward = parseRewardString(rewardStr.trim());
            if (!reward.isEmpty()) {
                // Scale reward by tourist count (e.g., 5 tourists = 5x rewards)
                reward.setCount(reward.getCount() * touristCount);
                rewards.add(reward);
            }
        }
        
        return rewards;
    }
    
    /**
     * Parses a single reward string into an ItemStack.
     * 
     * @param rewardStr Format: "minecraft:bread:1" or "minecraft:bread" (count defaults to 1)
     * @return ItemStack representing the reward, or empty stack if parsing fails
     */
    private static ItemStack parseRewardString(String rewardStr) {
        try {
            String[] parts = rewardStr.split(":");
            if (parts.length < 2) {
                LOGGER.warn("Invalid reward format: '{}' - expected 'namespace:item' or 'namespace:item:count'", rewardStr);
                return ItemStack.EMPTY;
            }
            
            // Build resource location (namespace:item)
            String namespace = parts[0];
            String itemName = parts[1];
            ResourceLocation itemId = new ResourceLocation(namespace, itemName);
            
            // Get item from registry
            Object itemObj = PlatformAccess.getRegistry().getItem(itemId);
            if (itemObj instanceof net.minecraft.world.item.Item item) {
                if (item == null) {
                    LOGGER.warn("Unknown item in milestone reward: '{}'", itemId);
                    return ItemStack.EMPTY;
                }

                // Parse count (defaults to 1)
                int count = 1;
                if (parts.length >= 3) {
                    try {
                        count = Integer.parseInt(parts[2]);
                    if (count <= 0) {
                        LOGGER.warn("Invalid reward count '{}' in '{}' - using count 1", parts[2], rewardStr);
                        count = 1;
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid reward count '{}' in '{}' - using count 1", parts[2], rewardStr);
                }
            }

            ItemStack reward = new ItemStack(item, count);
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Parsed reward: {} x{}", itemId, count);

            return reward;
            } else {
                LOGGER.warn("Failed to cast item from registry: '{}'", itemId);
                return ItemStack.EMPTY;
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to parse reward string '{}': {}", rewardStr, e.getMessage());
            return ItemStack.EMPTY;
        }
    }
    
    /**
     * Delivers milestone rewards to the destination town's payment board.
     * 
     * @param town The destination town to receive the rewards
     * @param milestoneResult The milestone achievement result containing rewards
     * @return true if rewards were successfully delivered
     */
    public static boolean deliverRewards(Town town, MilestoneResult milestoneResult) {
        if (!milestoneResult.hasRewards()) {
            return false;
        }
        
        try {
            // Create a reward entry for the milestone achievement
            List<ItemStack> rewardList = new ArrayList<>(milestoneResult.rewards);
            java.util.UUID rewardId = town.getPaymentBoard().addReward(
                RewardSource.MILESTONE, 
                rewardList, 
                "ALL"
            );
            
            if (rewardId != null) {
                // Add metadata about the milestone
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, 
                    "Created milestone reward entry {} for town {}: {}m journey, {} tourists, {} items", 
                    rewardId, town.getName(), (int) Math.round(milestoneResult.actualDistance), 
                    milestoneResult.touristCount, milestoneResult.rewards.size());
                
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Added milestone reward to payment board for town '{}': {}m journey with {} tourists resulted in {} reward items", 
                    town.getName(), (int) Math.round(milestoneResult.actualDistance), 
                    milestoneResult.touristCount, milestoneResult.rewards.size());
                return true;
            } else {
                LOGGER.warn("Failed to create milestone reward entry for town '{}'", town.getName());
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to deliver milestone rewards to town '{}': {}", town.getName(), e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets all configured milestone distances for UI display purposes.
     * 
     * @return List of milestone distances in ascending order
     */
    public static List<Integer> getMilestoneDistances() {
        return ConfigLoader.milestoneRewards.keySet().stream()
                .sorted()
                .toList();
    }
    
    /**
     * Gets the reward strings for a specific milestone distance.
     * 
     * @param distance The milestone distance
     * @return List of reward strings, or empty list if milestone doesn't exist
     */
    public static List<String> getMilestoneRewards(int distance) {
        return ConfigLoader.milestoneRewards.getOrDefault(distance, new ArrayList<>());
    }
}
