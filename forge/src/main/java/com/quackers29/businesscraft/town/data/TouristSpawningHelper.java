package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.init.ModEntityTypes;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.utils.TouristAllocationTracker;
import java.util.Collection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.*;

/**
 * Helper class for tourist spawning and destination selection.
 * Extracted from TownBlockEntity to improve code organization.
 * 
 * This class handles the complex tourist spawning logic including
 * destination selection, spawn position calculation, and tourist entity creation.
 */
public class TouristSpawningHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristSpawningHelper.class);
    private static final ConfigLoader CONFIG = ConfigLoader.INSTANCE;
    
    // Special UUID for "any town" destination
    private static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    
    private final Random random = new Random();
    
    /**
     * Attempts to spawn a tourist on the specified platform
     * 
     * @param level The level
     * @param town The origin town
     * @param platform The platform to spawn on
     * @param originTownId The ID of the origin town
     * @return true if a tourist was spawned, false otherwise
     */
    public boolean spawnTouristOnPlatform(Level level, Town town, Platform platform, UUID originTownId) {
        // Skip if town can't support more tourists
        if (!town.canAddMoreTourists()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_SPAWNING, "Cannot spawn tourist - town {} at maximum capacity ({}/{})",
                town.getName(), town.getTouristCount(), town.getMaxTourists());
            return false;
        }
        
        BlockPos startPos = platform.getStartPos();
        BlockPos endPos = platform.getEndPos();
        
        if (startPos == null || endPos == null) return false;
        
        // Count existing tourists in the platform area
        AABB pathBounds = new AABB(
            Math.min(startPos.getX(), endPos.getX()) - 1,
            startPos.getY(),
            Math.min(startPos.getZ(), endPos.getZ()) - 1,
            Math.max(startPos.getX(), endPos.getX()) + 1,
            startPos.getY() + 2,
            Math.max(startPos.getZ(), endPos.getZ()) + 1
        );
        
        List<Villager> existingTourists = level.getEntitiesOfClass(Villager.class, pathBounds);
        
        // Use configurable max tourists per platform
        if (existingTourists.size() < ConfigLoader.maxTouristsPerTown) {
            // Select a destination for the tourist
            UUID destinationTownId = selectTouristDestination(level, platform, originTownId);
            if (destinationTownId == null) {
                return false;
            }
            
            // Try up to 3 times to find a valid spawn location
            for (int attempt = 0; attempt < 3; attempt++) {
                // Calculate a random position along the path
                double progress = random.nextDouble();
                double exactX = startPos.getX() + (endPos.getX() - startPos.getX()) * progress;
                double exactZ = startPos.getZ() + (endPos.getZ() - startPos.getZ()) * progress;
                int x = (int) Math.round(exactX);
                int z = (int) Math.round(exactZ);
                int y = startPos.getY() + 1;
                
                BlockPos spawnPos = new BlockPos(x, y, z);
                
                // Check if the position is already occupied
                boolean isOccupied = existingTourists.stream()
                    .anyMatch(v -> {
                        BlockPos villagerPos = v.blockPosition();
                        return Math.abs(villagerPos.getX() - spawnPos.getX()) < 1 && 
                               Math.abs(villagerPos.getZ() - spawnPos.getZ()) < 1;
                    });
                
                if (!isOccupied) {
                    // Get destination town name for the tourist tag
                    String destinationName = "Destination N/A";
                    if (level instanceof ServerLevel serverLevel) {
                        Town destTown = TownManager.get(serverLevel).getTown(destinationTownId);
                        if (destTown != null) {
                            destinationName = destTown.getName();
                        }
                    }
                    
                    // Spawn the tourist
                    spawnTourist(level, spawnPos, town, platform, destinationTownId, destinationName);
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Selects a destination town for a tourist based on platform configuration and town population
     * 
     * @param level The level
     * @param platform The platform the tourist is spawning from
     * @param originTownId The ID of the origin town
     * @return The selected destination town ID, or null if no valid destination found
     */
    public UUID selectTouristDestination(Level level, Platform platform, UUID originTownId) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        
        // Get all possible destination towns
        Map<UUID, Boolean> platformDestinations = platform.getDestinations();
        
        // If no specific destinations are set or all destinations are disabled,
        // return the special ANY_TOWN_DESTINATION instead of selecting a random town
        if (platformDestinations.isEmpty() || platform.hasNoEnabledDestinations()) {
            return ANY_TOWN_DESTINATION;
        }
        
        // Filter to only enabled destinations
        List<UUID> enabledDestinations = new ArrayList<>();
        for (Map.Entry<UUID, Boolean> entry : platformDestinations.entrySet()) {
            if (entry.getValue()) {
                enabledDestinations.add(entry.getKey());
            }
        }
        
        if (enabledDestinations.isEmpty()) {
            return ANY_TOWN_DESTINATION;
        }
        
        // Select a destination based on population weights and fairness
        return selectFairTownByPopulation(serverLevel, enabledDestinations, originTownId);
    }
    
    /**
     * Selects a town based on population and fair allocation
     * 
     * @param serverLevel The server level
     * @param allowedTowns List of allowed town IDs, or null for all towns except current
     * @param originTownId The ID of the origin town to exclude
     * @return The selected town ID, or null if no valid towns found
     */
    public UUID selectFairTownByPopulation(ServerLevel serverLevel, List<UUID> allowedTowns, UUID originTownId) {
        TownManager townManager = TownManager.get(serverLevel);
        Collection<Town> allTowns = townManager.getAllTowns();
        
        // Remove current town from options
        Map<UUID, Town> possibleTowns = new HashMap<>();
        for (Town town : allTowns) {
            possibleTowns.put(town.getId(), town);
        }
        possibleTowns.remove(originTownId);
        
        // Filter to only allowed towns if specified
        if (allowedTowns != null) {
            possibleTowns.keySet().retainAll(allowedTowns);
        }
        
        if (possibleTowns.isEmpty()) {
            return null;
        }
        
        // Create a map of town ID to population for the tracker
        Map<UUID, Integer> populationMap = new HashMap<>();
        for (Map.Entry<UUID, Town> entry : possibleTowns.entrySet()) {
            populationMap.put(entry.getKey(), entry.getValue().getPopulation());
        }
        
        // Use our allocation tracker to select a fair destination
        return TouristAllocationTracker.selectFairDestination(originTownId, populationMap);
    }
    
    /**
     * Spawns a tourist villager at the specified location
     * 
     * @param level The level
     * @param pos The spawn position
     * @param originTown The origin town
     * @param platform The platform
     * @param destinationTownId The destination town ID
     * @param destinationName The destination town name
     */
    private void spawnTourist(Level level, BlockPos pos, Town originTown, Platform platform, 
                             UUID destinationTownId, String destinationName) {
        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
            // Create our custom TouristEntity instead of a regular Villager
            TouristEntity tourist = new TouristEntity(
                ModEntityTypes.TOURIST.get(),
                level,
                originTown,
                platform,
                destinationTownId,
                destinationName
            );
            
            // Position the tourist
            tourist.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            
            // Set expiry time based on config (convert minutes to ticks)
            int expiryTicks = (int)(ConfigLoader.touristExpiryMinutes * 60 * 20);
            tourist.setExpiryTicks(expiryTicks);
            
            // Spawn the entity into the world
            level.addFreshEntity(tourist);
            
            // Log the tourist spawn
            DebugConfig.debug(LOGGER, DebugConfig.TOURIST_SPAWNING, "Spawned tourist at {} from {} to {}", pos, originTown.getName(), destinationName);
            
            // Update town stats
            originTown.addTourist();
            
            // Only decrement bread if the town has enough (fix for negative bread issue)
            int breadNeeded = CONFIG.breadPerPop;
            if (breadNeeded > 0) {
                int currentBread = originTown.getResourceCount(Items.BREAD);
                if (currentBread >= breadNeeded) {
                    originTown.addResource(Items.BREAD, -breadNeeded);
                    DebugConfig.debug(LOGGER, DebugConfig.TOURIST_SPAWNING, 
                        "Consumed {} bread for tourist spawning. Town {} has {} bread remaining", 
                        breadNeeded, originTown.getName(), currentBread - breadNeeded);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.TOURIST_SPAWNING, 
                        "Town {} has insufficient bread ({}) for tourist spawning (needs {})", 
                        originTown.getName(), currentBread, breadNeeded);
                }
            }
        }
    }
} 