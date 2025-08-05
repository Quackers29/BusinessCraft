package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.platform.PlatformService;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.error.BCError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Platform-agnostic business logic for town operations.
 * This class contains the core business rules and calculations that should work
 * consistently across different mod platforms.
 */
public class TownBusinessLogic {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBusinessLogic.class);
    
    private final PlatformService platformService;
    
    public TownBusinessLogic(PlatformService platformService) {
        this.platformService = platformService;
    }
    
    /**
     * Calculate the maximum number of tourists a town can handle based on population
     */
    public int calculateMaxTourists(int population) {
        // Business rule: Max tourists = population * 2, minimum of 5
        return Math.max(5, population * 2);
    }
    
    /**
     * Determine if a town can spawn more tourists
     */
    public boolean canTownSpawnMoreTourists(ITownDataProvider town) {
        if (!town.isTouristSpawningEnabled()) {
            return false;
        }
        
        int currentTourists = town.getTouristCount();
        int maxTourists = calculateMaxTourists(town.getPopulation());
        
        return currentTourists < maxTourists;
    }
    
    /**
     * Process a tourist visit and determine rewards
     */
    public Result<TouristVisitResult, BCError> processTouristVisit(
            ITownDataProvider destinationTown, 
            UUID originTownId, 
            ITownDataProvider.Position originPosition,
            int touristCount) {
        
        try {
            // Record the visit in town history
            destinationTown.recordVisit(originTownId, touristCount, originPosition);
            
            // Calculate distance for milestone rewards
            double distance = platformService.getWorldService()
                .calculateDistance(destinationTown.getPosition(), originPosition);
            
            // Calculate rewards based on distance and tourist count
            TouristVisitResult result = calculateVisitRewards(distance, touristCount);
            
            LOGGER.debug("Processed tourist visit: {} tourists, distance: {}, rewards: {}", 
                        touristCount, distance, result.getTotalRewardValue());
            
            return Result.success(result);
            
        } catch (Exception e) {
            LOGGER.error("Failed to process tourist visit", e);
            return Result.failure(BCError.TOWN_OPERATION_FAILED);
        }
    }
    
    /**
     * Calculate visit rewards based on distance and tourist count
     */
    private TouristVisitResult calculateVisitRewards(double distance, int touristCount) {
        // Business rules for reward calculation
        int baseReward = touristCount * 2; // 2 coins per tourist
        
        // Distance bonuses
        int distanceBonus = 0;
        if (distance > 1000) {
            distanceBonus = (int) (distance / 100); // 1 coin per 100 blocks over 1000
        }
        
        int totalReward = baseReward + distanceBonus;
        
        return new TouristVisitResult(touristCount, distance, baseReward, distanceBonus, totalReward);
    }
    
    /**
     * Validate if a town name is acceptable
     */
    public Result<String, BCError> validateTownName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Result.failure(BCError.INVALID_TOWN_NAME);
        }
        
        String trimmedName = name.trim();
        
        if (trimmedName.length() > 32) {
            return Result.failure(BCError.TOWN_NAME_TOO_LONG);
        }
        
        if (trimmedName.length() < 2) {
            return Result.failure(BCError.TOWN_NAME_TOO_SHORT);
        }
        
        // Check for invalid characters
        if (!trimmedName.matches("^[a-zA-Z0-9\\s_-]+$")) {
            return Result.failure(BCError.INVALID_TOWN_NAME_CHARACTERS);
        }
        
        return Result.success(trimmedName);
    }
    
    /**
     * Calculate population growth from tourism
     */
    public int calculatePopulationGrowth(int currentPopulation, int totalVisitors) {
        // Business rule: 1 population growth per 50 total visitors, max growth of 10% of current population
        int growthFromVisitors = totalVisitors / 50;
        int maxGrowth = Math.max(1, currentPopulation / 10);
        
        return Math.min(growthFromVisitors, maxGrowth);
    }
    
    /**
     * Result of processing a tourist visit
     */
    public static class TouristVisitResult {
        private final int touristCount;
        private final double distance;
        private final int baseReward;
        private final int distanceBonus;
        private final int totalRewardValue;
        
        public TouristVisitResult(int touristCount, double distance, int baseReward, 
                                int distanceBonus, int totalRewardValue) {
            this.touristCount = touristCount;
            this.distance = distance;
            this.baseReward = baseReward;
            this.distanceBonus = distanceBonus;
            this.totalRewardValue = totalRewardValue;
        }
        
        public int getTouristCount() { return touristCount; }
        public double getDistance() { return distance; }
        public int getBaseReward() { return baseReward; }
        public int getDistanceBonus() { return distanceBonus; }
        public int getTotalRewardValue() { return totalRewardValue; }
    }
}