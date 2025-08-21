package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.util.PositionConverter;

import java.util.Collection;

/**
 * Service for town boundary calculations and validation.
 * Handles population-based boundary system with dynamic distance calculations.
 */
public class TownBoundaryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBoundaryService.class);
    
    /**
     * Calculates the boundary radius for a town based on population (1:1 ratio)
     * 
     * @param town The town to calculate boundary for
     * @return The boundary radius in blocks
     */
    public int calculateBoundaryRadius(Town town) {
        if (town == null) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Cannot calculate boundary for null town");
            return ConfigLoader.defaultStartingPopulation; // Fallback to default
        }
        
        int radius = town.getPopulation();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Calculated boundary radius for town {}: {} blocks (population: {})", 
            town.getName(), radius, town.getPopulation());
        
        return radius;
    }
    
    /**
     * Validates if a new town can be placed at the specified position without boundary conflicts
     * 
     * @param newTownPos Position where new town would be placed
     * @param existingTowns Collection of existing towns to check against
     * @return Result indicating success or failure with detailed error information
     */
    public Result<Void, BCError.TownError> checkTownPlacement(BlockPos newTownPos, Collection<Town> existingTowns) {
        if (newTownPos == null) {
            return Result.failure(new BCError.TownError("INVALID_POSITION", "Town position cannot be null"));
        }
        
        if (existingTowns == null || existingTowns.isEmpty()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                "No existing towns to check placement against at {}", newTownPos);
            return Result.success(null);
        }
        
        // New towns start with default population boundary
        int newTownBoundary = ConfigLoader.defaultStartingPopulation;
        
        for (Town existingTown : existingTowns) {
            double distance = Math.sqrt(newTownPos.distSqr(PositionConverter.toBlockPos(existingTown.getPosition())));
            double requiredDistance = newTownBoundary + existingTown.getBoundaryRadius();
            
            if (distance < requiredDistance) {
                String errorMessage = String.format(
                    "Town too close to existing town '%s' - distance: %.1f, required: %.1f (your boundary: %d + their boundary: %d)",
                    existingTown.getName(), distance, requiredDistance, newTownBoundary, existingTown.getBoundaryRadius()
                );
                
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                    "Town placement blocked: {}", errorMessage);
                
                return Result.failure(new BCError.TownError("BOUNDARY_CONFLICT", errorMessage));
            }
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Town placement validated successfully at {} with {} existing towns checked", 
            newTownPos, existingTowns.size());
        
        return Result.success(null);
    }
    
    /**
     * Calculates the minimum distance required between two towns based on their boundaries
     * 
     * @param town1 First town
     * @param town2 Second town
     * @return The minimum required distance in blocks
     */
    public double getMinimumDistanceRequired(Town town1, Town town2) {
        if (town1 == null && town2 == null) {
            return ConfigLoader.defaultStartingPopulation * 2.0; // Two default boundaries
        }
        
        if (town1 == null) {
            return ConfigLoader.defaultStartingPopulation + town2.getBoundaryRadius();
        }
        
        if (town2 == null) {
            return town1.getBoundaryRadius() + ConfigLoader.defaultStartingPopulation;
        }
        
        double minDistance = town1.getBoundaryRadius() + town2.getBoundaryRadius();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Minimum distance between '{}' and '{}': {} blocks ({}+{})", 
            town1.getName(), town2.getName(), minDistance, 
            town1.getBoundaryRadius(), town2.getBoundaryRadius());
        
        return minDistance;
    }
    
    /**
     * Checks if a town's boundary expansion would cause conflicts with existing towns
     * 
     * @param expandingTown The town that would expand
     * @param newPopulation The proposed new population
     * @param existingTowns Collection of existing towns to check against
     * @return Result indicating if expansion is safe or would cause conflicts
     */
    public Result<Void, BCError.TownError> checkBoundaryExpansion(Town expandingTown, int newPopulation, Collection<Town> existingTowns) {
        if (expandingTown == null) {
            return Result.failure(new BCError.TownError("INVALID_TOWN", "Expanding town cannot be null"));
        }
        
        if (newPopulation <= expandingTown.getPopulation()) {
            return Result.success(null); // Not actually expanding
        }
        
        int newBoundaryRadius = newPopulation; // 1:1 ratio
        int currentBoundaryRadius = expandingTown.getBoundaryRadius();
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Checking boundary expansion for '{}': {} -> {} (radius: {} -> {})", 
            expandingTown.getName(), expandingTown.getPopulation(), newPopulation, 
            currentBoundaryRadius, newBoundaryRadius);
        
        if (existingTowns != null) {
            for (Town otherTown : existingTowns) {
                if (otherTown.getId().equals(expandingTown.getId())) {
                    continue; // Skip self
                }
                
                double distance = Math.sqrt(expandingTown.getPosition().distSqr(otherTown.getPosition()));
                double requiredDistance = newBoundaryRadius + otherTown.getBoundaryRadius();
                
                if (distance < requiredDistance) {
                    String errorMessage = String.format(
                        "Boundary expansion would conflict with town '%s' - distance: %.1f, would require: %.1f",
                        otherTown.getName(), distance, requiredDistance
                    );
                    
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
                        "Boundary expansion blocked: {}", errorMessage);
                    
                    return Result.failure(new BCError.TownError("EXPANSION_CONFLICT", errorMessage));
                }
            }
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, 
            "Boundary expansion validated for '{}' - no conflicts detected", expandingTown.getName());
        
        return Result.success(null);
    }
}