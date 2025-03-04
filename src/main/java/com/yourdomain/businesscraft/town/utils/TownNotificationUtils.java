package com.yourdomain.businesscraft.town.utils;

import com.yourdomain.businesscraft.entity.TouristEntity;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Utility class for sending notifications to towns and nearby players
 */
public class TownNotificationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownNotificationUtils.class);
    private static final int NOTIFICATION_RANGE = 64; // Range in blocks for player notifications
    
    /**
     * Handles both the town management and notification for a tourist departure
     */
    public static void notifyTouristDeparture(ServerLevel level, UUID originTownId, String originTownName, 
                                             String destinationName, boolean died, BlockPos lastPosition) {
        if (originTownId == null) {
            LOGGER.warn("Cannot notify tourist departure: origin town ID is null");
            return;
        }
        
        // Update the town's tourist count
        Town town = removeTouristFromOrigin(level, originTownId);
        if (town == null) return;
        
        // Display notification to nearby players
        displayTouristDepartureNotification(level, town, originTownName, destinationName, died, lastPosition);
    }
    
    /**
     * Updates the origin town's tourist count without displaying notifications
     * Returns the town object if successful, null otherwise
     */
    public static Town removeTouristFromOrigin(ServerLevel level, UUID originTownId) {
        if (originTownId == null) {
            LOGGER.warn("Cannot update tourist count: origin town ID is null");
            return null;
        }
        
        // Get town from manager
        Town town = TownManager.get(level).getTown(originTownId);
        if (town == null) {
            LOGGER.warn("Cannot update tourist count: town not found for ID {}", originTownId);
            return null;
        }
        
        // Decrement the tourist count in the origin town
        town.removeTourist();
        
        return town;
    }
    
    /**
     * Displays notification to nearby players about a tourist departure
     * Does not update the town's tourist count
     */
    public static void displayTouristDepartureNotification(ServerLevel level, Town town, String originTownName, 
                                                          String destinationName, boolean died, BlockPos lastPosition) {
        // Format message based on whether the tourist died or timed out
        String actionText = died ? "died" : "quit";
        Component message = Component.literal("A tourist heading to " + destinationName + " has " + actionText + "!")
            .withStyle(ChatFormatting.GOLD);
        
        // Get town block position
        BlockPos townPos = town.getPosition();
        if (townPos == null) {
            LOGGER.warn("Cannot display tourist departure notification: town position is null for {}", originTownName);
            return;
        }
        
        // Find nearby players to notify
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && isNearPosition(player.blockPosition(), townPos, NOTIFICATION_RANGE)) {
                player.sendSystemMessage(message);
            }
        }
        
        // Log the event
        LOGGER.info("Tourist from {} heading to {} has {}", originTownName, destinationName, actionText);
    }
    
    /**
     * Notifies nearby players when a tourist arrives at a platform
     */
    public static void notifyTouristArrival(ServerLevel level, BlockPos platformPos, 
                                           String originTownName, String destinationName) {
        // Format message
        Component message = Component.literal("A tourist from " + originTownName + " has arrived at " + destinationName + "!")
            .withStyle(ChatFormatting.GREEN);
        
        // Find nearby players to notify
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.level() == level && isNearPosition(player.blockPosition(), platformPos, NOTIFICATION_RANGE)) {
                player.sendSystemMessage(message);
            }
        }
    }
    
    /**
     * Checks if a position is within range of another position
     */
    private static boolean isNearPosition(BlockPos pos1, BlockPos pos2, int range) {
        return pos1.distSqr(pos2) <= (range * range);
    }
} 