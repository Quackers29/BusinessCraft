package com.quackers29.businesscraft.town.utils;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import com.quackers29.businesscraft.town.data.DistanceMilestoneHelper;

/**
 * STUB: Town notification utils for unified architecture.
 * TODO: Move full implementation from forge module.
 */
public class TownNotificationUtils {
    
    public static void notifyTouristArrivals(ServerLevel serverLevel, BlockPos townBlockPos, 
                                           String originTownName, String townName, 
                                           int count, int payment, double averageDistance, 
                                           DistanceMilestoneHelper.MilestoneResult milestoneResult) {
        // STUB: No-op for now
    }
}