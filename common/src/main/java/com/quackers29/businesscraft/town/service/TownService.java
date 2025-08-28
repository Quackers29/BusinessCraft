package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for town business logic operations.
 * Unified architecture implementation for cross-platform compatibility.
 * Extracted from Town class to separate business logic from data storage.
 * Provides validated operations with explicit error handling using Result pattern.
 */
public class TownService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownService.class);
    
    private final TownValidationService validationService;
    private final Map<UUID, Boolean> cachedResults = new ConcurrentHashMap<>();
    
    public TownService(TownValidationService validationService) {
        this.validationService = validationService;
    }
    
    /**
     * Creates a new town with validation.
     * 
     * @param request Town creation request containing name, position, etc.
     * @return Result containing the created Town or validation errors
     */
    public Result<Town, BCError.TownError> createTown(CreateTownRequest request) {
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Attempting to create town: {}", request.getName());
        
        // Validate the request
        Result<Void, BCError.ValidationError> validation = validationService.validateTownCreation(request);
        if (validation.isFailure()) {
            return Result.failure(new BCError.TownError("VALIDATION_FAILED", validation.getError().getMessage()));
        }
        
        try {
            BlockPos pos = request.getPosition();
            Town town = new Town(UUID.randomUUID(), pos.getX(), pos.getY(), pos.getZ(), request.getName());
            
            // Apply initial configuration
            if (request.getInitialResources() != null) {
                for (Map.Entry<Item, Integer> entry : request.getInitialResources().entrySet()) {
                    town.addResource(entry.getKey(), entry.getValue());
                }
            }
            
            LOGGER.info("Successfully created town: {} at {}", request.getName(), request.getPosition());
            return Result.success(town);
            
        } catch (Exception e) {
            LOGGER.error("Failed to create town: {}", request.getName(), e);
            return Result.failure(new BCError.TownError("CREATION_FAILED", "Failed to create town: " + e.getMessage()));
        }
    }
    
    /**
     * Calculates whether a town can spawn tourists based on business rules.
     * This method contains the actual business logic moved from Town.canSpawnTourists().
     * 
     * @param town The town to check
     * @return Result containing boolean or error
     */
    public Result<Boolean, BCError.TownError> canSpawnTourists(Town town) {
        try {
            boolean enabled = town.isTouristSpawningEnabled();
            int population = town.getPopulation();
            int minRequired = ConfigLoader.minPopForTourists;
            
            boolean canSpawn = enabled && population >= minRequired;
            
            // Log state changes only when significant (moved from Town class)
            Boolean previousResult = cachedResults.get(town.getId());
            if (previousResult == null || previousResult != canSpawn) {
                // Only log if the result actually changed to avoid spam
                if (previousResult != null) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Tourist spawning eligibility changed for town [{}]: {} -> {} (pop: {}, tourists: {})",
                        town.getId().toString().substring(0, 8), previousResult, canSpawn, population, town.getTouristCount());
                }
                cachedResults.put(town.getId(), canSpawn);
            }
            
            return Result.success(canSpawn);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("SPAWN_CHECK_FAILED", 
                "Failed to check tourist spawning eligibility: " + e.getMessage()));
        }
    }
    
    /**
     * Calculates the maximum number of tourists a town can support.
     * This contains the actual business logic moved from Town.calculateMaxTouristsFromPopulation().
     * 
     * @param town The town to check
     * @return Result containing max tourist count or error
     */
    public Result<Integer, BCError.TownError> calculateMaxTourists(Town town) {
        try {
            int population = town.getPopulation();
            
            // Business logic moved from Town class:
            // Calculate based on population / populationPerTourist ratio
            int populationBasedLimit = population / ConfigLoader.populationPerTourist;
            
            // Cap at the configured maximum
            int popBasedMax = Math.min(populationBasedLimit, ConfigLoader.maxPopBasedTourists);
            
            // Apply absolute maximum limit
            int maxTourists = Math.min(popBasedMax, ConfigLoader.maxTouristsPerTown);
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Max tourists calculation for town {}: pop={}, popBased={}, configMax={}, absMax={}, result={}", 
                        town.getId(), population, populationBasedLimit, ConfigLoader.maxPopBasedTourists, ConfigLoader.maxTouristsPerTown, maxTourists);
            
            return Result.success(maxTourists);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("MAX_CALCULATION_FAILED", 
                "Failed to calculate max tourists: " + e.getMessage()));
        }
    }
    
    /**
     * Attempts to add a tourist to the town with validation.
     * This contains business logic and validation, then delegates to Town for data changes.
     * 
     * @param town The town to add tourist to
     * @return Result indicating success or failure reason
     */
    public Result<Void, BCError.TownError> addTourist(Town town) {
        try {
            // Check if town can spawn tourists
            Result<Boolean, BCError.TownError> canSpawnResult = canSpawnTourists(town);
            if (canSpawnResult.isFailure()) {
                return Result.failure(canSpawnResult.getError());
            }
            
            if (!canSpawnResult.getValue()) {
                return Result.failure(new BCError.TownError("SPAWNING_DISABLED", 
                    "Tourist spawning is not enabled for this town"));
            }
            
            // Check if town can support more tourists
            Result<Integer, BCError.TownError> maxTouristsResult = calculateMaxTourists(town);
            if (maxTouristsResult.isFailure()) {
                return Result.failure(maxTouristsResult.getError());
            }
            
            int currentTourists = town.getTouristCount();
            int maxTourists = maxTouristsResult.getValue();
            
            if (currentTourists >= maxTourists) {
                return Result.failure(new BCError.TownError("TOURIST_LIMIT_REACHED", 
                    String.format("Town has reached tourist limit: %d/%d", currentTourists, maxTourists)));
            }
            
            // Perform the actual tourist addition (business logic moved from Town.addTourist())
            town.setTouristCount(currentTourists + 1);
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Added tourist to town {}: {}/{}", town.getId(), currentTourists + 1, maxTourists);
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("TOURIST_ADD_FAILED", 
                "Failed to add tourist: " + e.getMessage()));
        }
    }
    
    /**
     * Removes a tourist from the town.
     * This contains business logic moved from Town.removeTourist().
     * 
     * @param town The town to remove tourist from
     * @return Result indicating success or failure
     */
    public Result<Void, BCError.TownError> removeTourist(Town town) {
        try {
            int currentTourists = town.getTouristCount();
            
            if (currentTourists <= 0) {
                return Result.failure(new BCError.TownError("NO_TOURISTS", 
                    "No tourists to remove from town"));
            }
            
            // Perform the actual tourist removal (business logic moved from Town.removeTourist())
            town.setTouristCount(currentTourists - 1);
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Removed tourist from town {}: {}/{}", town.getId(), currentTourists - 1, 
                       calculateMaxTourists(town).getOrElse(0));
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("TOURIST_REMOVE_FAILED", 
                "Failed to remove tourist: " + e.getMessage()));
        }
    }
    
    /**
     * Adds resources to a town with validation.
     * 
     * @param town The town to add resources to
     * @param item The item type to add
     * @param amount The amount to add
     * @return Result indicating success or validation error
     */
    public Result<Void, BCError.TownError> addResources(Town town, Item item, int amount) {
        // Validate inputs
        if (item == null) {
            return Result.failure(new BCError.TownError("INVALID_ITEM", "Item cannot be null"));
        }
        
        if (amount <= 0) {
            return Result.failure(new BCError.TownError("INVALID_AMOUNT", 
                "Amount must be positive, got: " + amount));
        }
        
        try {
            town.addResource(item, amount);
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Added {} {} to town {}", amount, item, town.getId());
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("RESOURCE_ADD_FAILED", 
                "Failed to add resources: " + e.getMessage()));
        }
    }
    
    /**
     * Processes a visitor to the town, updating statistics and triggering population growth.
     * 
     * @param town The town being visited
     * @param visitorId The UUID of the visitor
     * @param originTownId The UUID of the visitor's origin town (null for players)
     * @return Result indicating success or failure
     */
    public Result<Void, BCError.TownError> processVisitor(Town town, UUID visitorId, UUID originTownId) {
        try {
            // Update visitor count using existing Town method
            town.addVisitor(originTownId);
            
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Processed visitor {} to town {} from origin {}", 
                        visitorId, town.getId(), originTownId);
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("VISITOR_PROCESSING_FAILED", 
                "Failed to process visitor: " + e.getMessage()));
        }
    }
    
    /**
     * Checks if the town should experience population growth based on visitor count.
     * Note: This functionality is currently handled by Town.addVisitor() method.
     * This method is a placeholder for future extraction.
     * 
     * @param town The town to check
     * @return Result indicating if growth occurred
     */
    private Result<Void, BCError.TownError> checkPopulationGrowth(Town town) {
        try {
            // Population growth is currently handled automatically by Town.addVisitor()
            // This method is prepared for future extraction of that logic
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_SERVICE, "Population growth check completed for town {}", town.getId());
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("GROWTH_CHECK_FAILED", 
                "Failed to check population growth: " + e.getMessage()));
        }
    }
    
    /**
     * Updates town configuration settings.
     * 
     * @param town The town to update
     * @param settings The new settings to apply
     * @return Result indicating success or failure
     */
    public Result<Void, BCError.TownError> updateTownSettings(Town town, TownSettings settings) {
        // Validate settings
        Result<Void, BCError.ValidationError> validation = validationService.validateTownSettings(settings);
        if (validation.isFailure()) {
            return Result.failure(new BCError.TownError("INVALID_SETTINGS", validation.getError().getMessage()));
        }
        
        try {
            if (settings.getName() != null) {
                town.setName(settings.getName());
            }
            
            if (settings.getTouristSpawningEnabled() != null) {
                town.setTouristSpawningEnabled(settings.getTouristSpawningEnabled());
            }
            
            if (settings.getSearchRadius() != null) {
                town.setSearchRadius(settings.getSearchRadius());
            }
            
            LOGGER.info("Updated settings for town {}", town.getId());
            return Result.success(null);
            
        } catch (Exception e) {
            return Result.failure(new BCError.TownError("SETTINGS_UPDATE_FAILED", 
                "Failed to update town settings: " + e.getMessage()));
        }
    }
    
    /**
     * Request object for town creation.
     */
    public static class CreateTownRequest {
        private final String name;
        private final BlockPos position;
        private Map<Item, Integer> initialResources;
        
        public CreateTownRequest(String name, BlockPos position) {
            this.name = name;
            this.position = position;
        }
        
        // Getters and setters
        public String getName() { return name; }
        public BlockPos getPosition() { return position; }
        public Map<Item, Integer> getInitialResources() { return initialResources; }
        public void setInitialResources(Map<Item, Integer> initialResources) { 
            this.initialResources = initialResources; 
        }
    }
    
    /**
     * Settings object for town configuration updates.
     */
    public static class TownSettings {
        private String name;
        private Boolean touristSpawningEnabled;
        private Integer searchRadius;
        
        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Boolean getTouristSpawningEnabled() { return touristSpawningEnabled; }
        public void setTouristSpawningEnabled(Boolean touristSpawningEnabled) { 
            this.touristSpawningEnabled = touristSpawningEnabled; 
        }
        
        public Integer getSearchRadius() { return searchRadius; }
        public void setSearchRadius(Integer searchRadius) { this.searchRadius = searchRadius; }
    }
}