package com.yourdomain.businesscraft.town.data;

import com.yourdomain.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Helper class for platform visualization and particle effects.
 * Extracted from TownBlockEntity to improve code organization.
 * 
 * This class handles the complex platform indicator spawning logic
 * while leaving the platform storage in TownBlockEntity.
 */
public class PlatformVisualizationHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVisualizationHelper.class);
    
    // Platform visualization constants
    private static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks
    private static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks
    
    // Platform visualization tracking
    private final Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    private final Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();
    
    /**
     * Spawns visual indicators at platform start and end points and along the line
     * @param level The level
     * @param platforms The list of platforms to visualize
     * @param searchRadius The search radius for particle effects
     */
    public void spawnPlatformIndicators(Level level, List<Platform> platforms, int searchRadius) {
        if (level.isClientSide || platforms.isEmpty()) return;
        
        long gameTime = level.getGameTime();
        
        // Clean up extended indicator players first
        Iterator<Map.Entry<UUID, Long>> iterator = extendedIndicatorPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > EXTENDED_INDICATOR_DURATION) {
                iterator.remove();
            }
        }
        
        // Only show indicators if there are players with extended indicators
        // or if we're in platform creation mode (this would need to be passed in)
        boolean shouldShowIndicators = !extendedIndicatorPlayers.isEmpty();
        
        if (!shouldShowIndicators) return;
        
        ServerLevel serverLevel = (ServerLevel) level;
        
        for (Platform platform : platforms) {
            if (!platform.isEnabled() || !platform.isComplete()) continue;
            
            UUID platformId = platform.getId();
            
            // Check if we should spawn indicators for this platform
            boolean shouldSpawn = 
                !platformIndicatorSpawnTimes.containsKey(platformId) ||
                gameTime - platformIndicatorSpawnTimes.get(platformId) >= INDICATOR_SPAWN_INTERVAL;
            
            if (shouldSpawn) {
                platformIndicatorSpawnTimes.put(platformId, gameTime);
                
                BlockPos startPos = platform.getStartPos();
                BlockPos endPos = platform.getEndPos();
                
                // Platform visualization is now handled client-side through solid line rendering
                // The ClientRenderEvents system automatically renders platforms based on 
                // synchronized platform data in TownBlockEntity
                
                // No server-side particle spawning needed - all visualization is client-side
            }
        }
    }
    
    /**
     * Registers a player as having exited the town UI, enabling extended indicators
     * @param playerId The UUID of the player who exited the UI
     * @param gameTime The current game time
     */
    public void registerPlayerExitUI(UUID playerId, long gameTime) {
        extendedIndicatorPlayers.put(playerId, gameTime);
    }
    
    /**
     * Cleans up platform indicator spawn times for platforms that no longer exist
     * @param platforms The current list of platforms
     */
    public void cleanupPlatformIndicators(List<Platform> platforms) {
        platformIndicatorSpawnTimes.keySet().removeIf(platformId ->
            platforms.stream().noneMatch(p -> p.getId().equals(platformId))
        );
    }
    
    /**
     * This method has been replaced by client-side solid line rendering.
     * Platform paths are now rendered as solid lines through ClientRenderEvents.
     * 
     * @deprecated Use client-side PlatformLineRenderer.renderPath() instead
     */
    @Deprecated
    private void spawnPathParticles(ServerLevel level, BlockPos startPos, BlockPos endPos) {
        // Method body removed - platform paths are now rendered client-side as solid lines
        // See: ClientRenderEvents.renderPlatformsForTown() and PlatformLineRenderer.renderPath()
    }
    
    /**
     * This method has been replaced by client-side solid line rendering.
     * Platform boundaries are now rendered as solid lines through ClientRenderEvents.
     * 
     * @deprecated Use client-side PlatformLineRenderer.renderBoundary() instead
     */
    @Deprecated
    private void spawnSearchRadiusParticles(ServerLevel level, BlockPos startPos, BlockPos endPos, int radius) {
        // Method body removed - platform boundaries are now rendered client-side as solid lines
        // See: ClientRenderEvents.renderPlatformsForTown() and PlatformLineRenderer.renderBoundary()
    }
    
    /**
     * Gets the number of players currently with extended indicators
     * @return The count of players with extended indicators
     */
    public int getExtendedIndicatorPlayerCount() {
        return extendedIndicatorPlayers.size();
    }
    
    /**
     * Clears all visualization state (useful for cleanup)
     */
    public void clearAll() {
        platformIndicatorSpawnTimes.clear();
        extendedIndicatorPlayers.clear();
    }
} 