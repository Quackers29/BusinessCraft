package com.quackers29.businesscraft.scoreboard;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.network.chat.Component;

public class TownScoreboardManager {
    private static final String OBJECTIVE_NAME = "townstats";
    
    public static void updateScoreboard(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();
        
        // Remove old objective if it exists
        Objective objective = scoreboard.getObjective(OBJECTIVE_NAME);
        if (objective != null) {
            scoreboard.removeObjective(objective);
        }
        
        // Create new objective with correct parameter count
        final Objective finalObjective = scoreboard.addObjective(
            OBJECTIVE_NAME,
            ObjectiveCriteria.DUMMY,
            Component.literal("Town Population"),
            ObjectiveCriteria.RenderType.INTEGER
        );
        
        // Set display slot (sidebar)
        scoreboard.setDisplayObjective(1, finalObjective);
        
        // Update scores for each town
        TownManager.get(level).getAllTowns().forEach((id, town) -> {
            String status = town.canSpawnTourists() ? ":ON" : ":OFF";
            String displayName = town.getName() + status + " [" + town.getTotalVisitors() + "]";
            scoreboard.getOrCreatePlayerScore(displayName, finalObjective).setScore(town.getPopulation());
        });
    }
} 
