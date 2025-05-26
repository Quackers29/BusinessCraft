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
     */
    private void spawnPathParticles(ServerLevel level, BlockPos startPos, BlockPos endPos) {
        // Calculate the direction vector
        double dx = endPos.getX() - startPos.getX();
        double dy = endPos.getY() - startPos.getY();
        double dz = endPos.getZ() - startPos.getZ();
        
        // Calculate the distance
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        
        // Normalize the direction vector
        if (distance > 0) {
            dx /= distance;
            dy /= distance;
            dz /= distance;
            
            // Spawn particles every 2 blocks along the path
            int particleCount = Math.max(1, (int) (distance / 2));
            for (int i = 1; i < particleCount; i++) {
                double t = (double) i / particleCount;
                double x = startPos.getX() + dx * distance * t;
                double y = startPos.getY() + dy * distance * t + 1.0; // Slightly above ground
                double z = startPos.getZ() + dz * distance * t;
                
                level.sendParticles(ParticleTypes.END_ROD, 
                    x + 0.5, y, z + 0.5, 
                    1, 0.1, 0.1, 0.1, 0.01);
            }
        }
    }
    
    /**
     * Spawns particles to show the search radius around platform area
     * Creates a rectangular perimeter showing the visitor capture area
     */
    private void spawnSearchRadiusParticles(ServerLevel level, BlockPos startPos, BlockPos endPos, int radius) {
        // Calculate the bounding box the same way it's used for entity search
        int minX = Math.min(startPos.getX(), endPos.getX()) - radius;
        int minZ = Math.min(startPos.getZ(), endPos.getZ()) - radius;
        int maxX = Math.max(startPos.getX(), endPos.getX()) + radius;
        int maxZ = Math.max(startPos.getZ(), endPos.getZ()) + radius;
        
        // Use a fixed Y for visualization
        double particleY = Math.min(startPos.getY(), endPos.getY()) + 1.0;
        
        // Calculate perimeter length to determine number of particles
        int perimeterLength = 2 * (maxX - minX + maxZ - minZ);
        int totalPoints = Math.min(200, Math.max(32, perimeterLength / 2));
        
        // Distribute points evenly across the 4 sides of the perimeter
        int pointsPerSide = totalPoints / 4;
        
        // Generate particles along the perimeter
        
        // Bottom edge (minX to maxX at minZ)
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double x = minX + t * (maxX - minX);
            level.sendParticles(ParticleTypes.FLAME,
                x, particleY, minZ,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Right edge (maxX, minZ to maxZ)
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double z = minZ + t * (maxZ - minZ);
            level.sendParticles(ParticleTypes.FLAME,
                maxX, particleY, z,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Top edge (maxX to minX at maxZ)
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double x = maxX - t * (maxX - minX);
            level.sendParticles(ParticleTypes.FLAME,
                x, particleY, maxZ,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        
        // Left edge (minX, maxZ to minZ)
        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double z = maxZ - t * (maxZ - minZ);
            level.sendParticles(ParticleTypes.FLAME,
                minX, particleY, z,
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