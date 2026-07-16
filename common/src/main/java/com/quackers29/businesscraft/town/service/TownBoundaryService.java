package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Town boundary calculations and validation.
 * Radii come from the {@code border} upgrade/kit modifier (plus
 * {@link ConfigLoader#defaultBorderRadius} fallback) — never from population.
 * Optional population-linked border growth belongs in production recipes
 * (e.g. {@code pop:>80%pop_cap} → {@code border:1}), not this service.
 */
public class TownBoundaryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBoundaryService.class);

    /**
     * Boundary radius for a town (delegates to {@link Town#getBoundaryRadius()}).
     */
    public int calculateBoundaryRadius(Town town) {
        if (town == null) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Cannot calculate boundary for null town");
            return ConfigLoader.defaultBorderRadius;
        }

        int radius = town.getBoundaryRadius();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Boundary radius for town {}: {} blocks (border modifier)",
                town.getName(), radius);
        return radius;
    }

    /**
     * Validates new town placement: distance must be &gt;= defaultBorder + existing border.
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

        int newTownBoundary = ConfigLoader.defaultBorderRadius;

        for (Town existingTown : existingTowns) {
            double distance = Math.sqrt(newTownPos.distSqr(existingTown.getPosition()));
            double requiredDistance = newTownBoundary + existingTown.getBoundaryRadius();

            if (distance < requiredDistance) {
                String errorMessage = String.format(
                        "Town too close to existing town '%s' - distance: %.1f, required: %.1f (your boundary: %d + their boundary: %d)",
                        existingTown.getName(), distance, requiredDistance, newTownBoundary,
                        existingTown.getBoundaryRadius());

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
     * Minimum distance required between two towns (sum of border radii).
     * Null sides use {@link ConfigLoader#defaultBorderRadius}.
     */
    public double getMinimumDistanceRequired(Town town1, Town town2) {
        if (town1 == null && town2 == null) {
            return ConfigLoader.defaultBorderRadius * 2.0;
        }

        if (town1 == null) {
            return ConfigLoader.defaultBorderRadius + town2.getBoundaryRadius();
        }

        if (town2 == null) {
            return town1.getBoundaryRadius() + ConfigLoader.defaultBorderRadius;
        }

        double minDistance = town1.getBoundaryRadius() + town2.getBoundaryRadius();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Minimum distance between '{}' and '{}': {} blocks ({}+{})",
                town1.getName(), town2.getName(), minDistance,
                town1.getBoundaryRadius(), town2.getBoundaryRadius());

        return minDistance;
    }

    /**
     * Checks whether growing a town's border to {@code proposedBorderRadius} would
     * overlap another town. Population changes do not call this — production/upgrade
     * paths that change the border modifier may use it when conflict checks are wired.
     */
    public Result<Void, BCError.TownError> checkBorderExpansion(Town expandingTown, int proposedBorderRadius,
            Collection<Town> existingTowns) {
        if (expandingTown == null) {
            return Result.failure(new BCError.TownError("INVALID_TOWN", "Expanding town cannot be null"));
        }

        int currentBoundaryRadius = expandingTown.getBoundaryRadius();
        if (proposedBorderRadius <= currentBoundaryRadius) {
            return Result.success(null); // Not actually expanding
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Checking border expansion for '{}': {} -> {}",
                expandingTown.getName(), currentBoundaryRadius, proposedBorderRadius);

        if (existingTowns != null) {
            for (Town otherTown : existingTowns) {
                if (otherTown.getId().equals(expandingTown.getId())) {
                    continue;
                }

                double distance = Math.sqrt(expandingTown.getPosition().distSqr(otherTown.getPosition()));
                double requiredDistance = proposedBorderRadius + otherTown.getBoundaryRadius();

                if (distance < requiredDistance) {
                    String errorMessage = String.format(
                            "Border expansion would conflict with town '%s' - distance: %.1f, would require: %.1f",
                            otherTown.getName(), distance, requiredDistance);

                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                            "Border expansion blocked: {}", errorMessage);

                    return Result.failure(new BCError.TownError("EXPANSION_CONFLICT", errorMessage));
                }
            }
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Border expansion validated for '{}' - no conflicts detected", expandingTown.getName());

        return Result.success(null);
    }
}
