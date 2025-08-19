package com.quackers29.businesscraft.town.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import com.quackers29.businesscraft.town.data.DistanceMilestoneHelper;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Town notification utilities for unified architecture.
 * Contains cross-platform compatible notification and town management methods.
 */
public class TownNotificationUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownNotificationUtils.class);
    
    public static void notifyTouristArrivals(ServerLevel serverLevel, BlockPos townBlockPos, 
                                           String originTownName, String townName, 
                                           int count, int payment, double averageDistance, 
                                           DistanceMilestoneHelper.MilestoneResult milestoneResult) {
        // STUB: No-op for now
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
}