package com.yourdomain.businesscraft.town.data;

import com.yourdomain.businesscraft.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
                
                // Spawn particles at start and end positions
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                    startPos.getX() + 0.5, startPos.getY() + 1.0, startPos.getZ() + 0.5, 
                    3, 0.3, 0.3, 0.3, 0.1);
                
                serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                    endPos.getX() + 0.5, endPos.getY() + 1.0, endPos.getZ() + 0.5, 
                    3, 0.3, 0.3, 0.3, 0.1);
                
                // Spawn particles along the path
                spawnPathParticles(serverLevel, startPos, endPos);
                
                // Spawn search radius particles
                spawnSearchRadiusParticles(serverLevel, startPos, endPos, searchRadius);
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
     * Spawns particles along the path between start and end positions
     * Creates a precise line showing the platform spawn path
     */
    private void spawnPathParticles(ServerLevel level, BlockPos startPos, BlockPos endPos) {
        // Create a precise line using Bresenham-like algorithm for block positions
        int x0 = startPos.getX();
        int y0 = startPos.getY();
        int z0 = startPos.getZ();
        int x1 = endPos.getX();
        int y1 = endPos.getY();
        int z1 = endPos.getZ();
        
        // Calculate differences
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        
        // Determine step directions
        int stepX = x0 < x1 ? 1 : -1;
        int stepY = y0 < y1 ? 1 : -1;
        int stepZ = z0 < z1 ? 1 : -1;
        
        // Find the maximum difference to determine number of steps
        int maxSteps = Math.max(Math.max(dx, dy), dz);
        
        // Generate particles along the line
        for (int i = 0; i <= maxSteps; i++) {
            double t = maxSteps > 0 ? (double) i / maxSteps : 0;
            
            int x = x0 + (int) Math.round(t * (x1 - x0));
            int y = y0 + (int) Math.round(t * (y1 - y0));
            int z = z0 + (int) Math.round(t * (z1 - z0));
            
            // Spawn green particle at each block position along the path
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, 
                x + 0.5, y + 1.0, z + 0.5, 
                1, 0.0, 0.0, 0.0, 0.0);
        }
    }
    
    /**
     * Spawns particles to show the search radius around platform area
     * Creates a precise rectangular perimeter showing the visitor capture area
     * Uses block-by-block placement for accurate boundary visualization
     */
    private void spawnSearchRadiusParticles(ServerLevel level, BlockPos startPos, BlockPos endPos, int radius) {
        // Calculate the bounding box the same way it's used for entity search
        int minX = Math.min(startPos.getX(), endPos.getX()) - radius;
        int minZ = Math.min(startPos.getZ(), endPos.getZ()) - radius;
        int maxX = Math.max(startPos.getX(), endPos.getX()) + radius;
        int maxZ = Math.max(startPos.getZ(), endPos.getZ()) + radius;
        
        // Use a fixed Y for visualization
        double particleY = Math.min(startPos.getY(), endPos.getY()) + 1.0;
        
        // Create precise boundary by placing particles at exact block positions
        // This ensures symmetric 1-wide radius display on all sides
        
        // Bottom edge (minX to maxX at minZ) - inclusive of corners
        for (int x = minX; x <= maxX; x++) {
            level.sendParticles(ParticleTypes.FLAME,
                x + 0.5, particleY, minZ + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Top edge (minX to maxX at maxZ) - inclusive of corners
        for (int x = minX; x <= maxX; x++) {
            level.sendParticles(ParticleTypes.FLAME,
                x + 0.5, particleY, maxZ + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Left edge (minZ+1 to maxZ-1 at minX) - exclude corners to avoid duplicates
        for (int z = minZ + 1; z < maxZ; z++) {
            level.sendParticles(ParticleTypes.FLAME,
                minX + 0.5, particleY, z + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Right edge (minZ+1 to maxZ-1 at maxX) - exclude corners to avoid duplicates
        for (int z = minZ + 1; z < maxZ; z++) {
            level.sendParticles(ParticleTypes.FLAME,
                maxX + 0.5, particleY, z + 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        }
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