package com.yourdomain.businesscraft.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side manager for platform visualization state.
 * Tracks which towns should show platform visualization and for how long.
 */
public class PlatformVisualizationManager {
    private static final PlatformVisualizationManager INSTANCE = new PlatformVisualizationManager();
    
    // Constants matching the server-side values
    private static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks
    
    // Track which town blocks should show platform visualization and when it expires
    private final Map<BlockPos, Long> activeTownVisualization = new HashMap<>();
    
    public static PlatformVisualizationManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers that a player has exited a town UI at the given position.
     * This enables platform visualization for 30 seconds.
     * 
     * @param townBlockPos The position of the town block
     * @param gameTime The current game time
     */
    public void registerPlayerExitUI(BlockPos townBlockPos, long gameTime) {
        activeTownVisualization.put(townBlockPos, gameTime);
    }
    
    /**
     * Checks if platform visualization should be shown for a specific town block.
     * 
     * @param townBlockPos The position of the town block
     * @param currentGameTime The current game time
     * @return true if visualization should be shown
     */
    public boolean shouldShowVisualization(BlockPos townBlockPos, long currentGameTime) {
        Long exitTime = activeTownVisualization.get(townBlockPos);
        if (exitTime == null) {
            return false;
        }
        
        // Check if the 30-second timer has expired
        if (currentGameTime - exitTime > EXTENDED_INDICATOR_DURATION) {
            // Clean up expired entry
            activeTownVisualization.remove(townBlockPos);
            return false;
        }
        
        return true;
    }
    
    /**
     * Cleans up expired visualization entries.
     * Should be called periodically to prevent memory leaks.
     * 
     * @param currentGameTime The current game time
     */
    public void cleanupExpired(long currentGameTime) {
        Iterator<Map.Entry<BlockPos, Long>> iterator = activeTownVisualization.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Long> entry = iterator.next();
            if (currentGameTime - entry.getValue() > EXTENDED_INDICATOR_DURATION) {
                iterator.remove();
            }
        }
    }
    
    /**
     * Gets the number of active town visualizations.
     * Useful for debugging.
     * 
     * @return The number of towns currently showing visualization
     */
    public int getActiveVisualizationCount() {
        return activeTownVisualization.size();
    }
    
    /**
     * Clears all visualization state.
     * Useful for cleanup when changing worlds.
     */
    public void clearAll() {
        activeTownVisualization.clear();
    }
}