package com.quackers29.businesscraft.town.service;

import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Validation service for town-related operations.
 * Provides comprehensive validation logic with descriptive error messages
 * using the Result pattern for type-safe error handling.
 */
public class TownValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownValidationService.class);
    
    private static final int MIN_TOWN_NAME_LENGTH = 1;
    private static final int MAX_TOWN_NAME_LENGTH = 50;
    private static final int MIN_SEARCH_RADIUS = 1;
    private static final int MAX_SEARCH_RADIUS = 100;
    
    /**
     * Validates town creation request.
     * 
     * @param request The town creation request to validate
     * @return Result indicating validation success or failure with detailed error
     */
    public Result<Void, BCError.ValidationError> validateTownCreation(TownService.CreateTownRequest request) {
        if (request == null) {
            return Result.failure(new BCError.ValidationError("INVALID_REQUEST", "Town creation request cannot be null"));
        }
        
        // Validate town name
        Result<Void, BCError.ValidationError> nameValidation = validateTownName(request.getName());
        if (nameValidation.isFailure()) {
            return nameValidation;
        }
        
        // Validate position
        Result<Void, BCError.ValidationError> positionValidation = validatePosition(request.getPosition());
        if (positionValidation.isFailure()) {
            return positionValidation;
        }
        
        // Validate initial resources if provided
        if (request.getInitialResources() != null) {
            Result<Void, BCError.ValidationError> resourcesValidation = validateInitialResources(request);
            if (resourcesValidation.isFailure()) {
                return resourcesValidation;
            }
        }
        
        LOGGER.debug("Town creation request validation passed: {}", request.getName());
        return Result.success(null);
    }
    
    /**
     * Validates town settings update.
     * 
     * @param settings The town settings to validate
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateTownSettings(TownService.TownSettings settings) {
        if (settings == null) {
            return Result.failure(new BCError.ValidationError("INVALID_SETTINGS", "Town settings cannot be null"));
        }
        
        // Validate name if being updated
        if (settings.getName() != null) {
            Result<Void, BCError.ValidationError> nameValidation = validateTownName(settings.getName());
            if (nameValidation.isFailure()) {
                return nameValidation;
            }
        }
        
        // Validate search radius if being updated
        if (settings.getSearchRadius() != null) {
            Result<Void, BCError.ValidationError> radiusValidation = validateSearchRadius(settings.getSearchRadius());
            if (radiusValidation.isFailure()) {
                return radiusValidation;
            }
        }
        
        // Tourist spawning enabled can be any boolean value, no validation needed
        
        LOGGER.debug("Town settings validation passed");
        return Result.success(null);
    }
    
    /**
     * Validates a town name.
     * 
     * @param name The town name to validate
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateTownName(String name) {
        if (name == null) {
            return Result.failure(new BCError.ValidationError("NULL_NAME", "Town name cannot be null"));
        }
        
        if (name.trim().isEmpty()) {
            return Result.failure(new BCError.ValidationError("EMPTY_NAME", "Town name cannot be empty"));
        }
        
        if (name.length() < MIN_TOWN_NAME_LENGTH) {
            return Result.failure(new BCError.ValidationError("NAME_TOO_SHORT", 
                String.format("Town name must be at least %d characters long", MIN_TOWN_NAME_LENGTH)));
        }
        
        if (name.length() > MAX_TOWN_NAME_LENGTH) {
            return Result.failure(new BCError.ValidationError("NAME_TOO_LONG", 
                String.format("Town name cannot exceed %d characters", MAX_TOWN_NAME_LENGTH)));
        }
        
        // Check for invalid characters
        if (!name.matches("^[a-zA-Z0-9\\s\\-_'.]+$")) {
            return Result.failure(new BCError.ValidationError("INVALID_NAME_CHARACTERS", 
                "Town name can only contain letters, numbers, spaces, hyphens, underscores, apostrophes, and periods"));
        }
        
        // Check for profanity or inappropriate content (basic check)
        String lowerName = name.toLowerCase();
        if (containsInappropriateContent(lowerName)) {
            return Result.failure(new BCError.ValidationError("INAPPROPRIATE_NAME", 
                "Town name contains inappropriate content"));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validates a block position.
     * 
     * @param position The position to validate
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validatePosition(BlockPos position) {
        if (position == null) {
            return Result.failure(new BCError.ValidationError("NULL_POSITION", "Position cannot be null"));
        }
        
        // Check Y level bounds (reasonable limits for town placement)
        if (position.getY() < -64) {
            return Result.failure(new BCError.ValidationError("POSITION_TOO_LOW", 
                "Town cannot be placed below Y=-64"));
        }
        
        if (position.getY() > 320) {
            return Result.failure(new BCError.ValidationError("POSITION_TOO_HIGH", 
                "Town cannot be placed above Y=320"));
        }
        
        // Check for reasonable X/Z bounds to prevent overflow issues
        if (Math.abs(position.getX()) > 30000000) {
            return Result.failure(new BCError.ValidationError("POSITION_TOO_FAR", 
                "Town X coordinate is too far from world center"));
        }
        
        if (Math.abs(position.getZ()) > 30000000) {
            return Result.failure(new BCError.ValidationError("POSITION_TOO_FAR", 
                "Town Z coordinate is too far from world center"));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validates search radius setting.
     * 
     * @param radius The search radius to validate
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateSearchRadius(int radius) {
        if (radius < MIN_SEARCH_RADIUS) {
            return Result.failure(new BCError.ValidationError("RADIUS_TOO_SMALL", 
                String.format("Search radius must be at least %d blocks", MIN_SEARCH_RADIUS)));
        }
        
        if (radius > MAX_SEARCH_RADIUS) {
            return Result.failure(new BCError.ValidationError("RADIUS_TOO_LARGE", 
                String.format("Search radius cannot exceed %d blocks", MAX_SEARCH_RADIUS)));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validates initial resources for town creation.
     * 
     * @param request The town creation request containing initial resources
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateInitialResources(TownService.CreateTownRequest request) {
        if (request.getInitialResources().isEmpty()) {
            return Result.success(null); // Empty resources are fine
        }
        
        // Check each item and amount
        for (var entry : request.getInitialResources().entrySet()) {
            Item item = entry.getKey();
            Integer amount = entry.getValue();
            
            if (item == null) {
                return Result.failure(new BCError.ValidationError("NULL_ITEM", 
                    "Initial resource item cannot be null"));
            }
            
            if (amount == null || amount <= 0) {
                return Result.failure(new BCError.ValidationError("INVALID_AMOUNT", 
                    String.format("Initial resource amount must be positive, got %d for item %s", 
                    amount, item.getDescription().getString())));
            }
            
            if (amount > 64 * 9 * 6) { // Reasonable limit (6 rows of double chests)
                return Result.failure(new BCError.ValidationError("AMOUNT_TOO_LARGE", 
                    String.format("Initial resource amount %d is too large for item %s", 
                    amount, item.getDescription().getString())));
            }
        }
        
        // Check total number of different items
        if (request.getInitialResources().size() > 100) {
            return Result.failure(new BCError.ValidationError("TOO_MANY_ITEM_TYPES", 
                "Too many different item types in initial resources (max 100)"));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validates tourist management parameters.
     * 
     * @param currentTourists Current number of tourists
     * @param maxTourists Maximum allowed tourists
     * @param population Town population
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateTouristManagement(int currentTourists, int maxTourists, int population) {
        if (currentTourists < 0) {
            return Result.failure(new BCError.ValidationError("NEGATIVE_TOURISTS", 
                "Current tourist count cannot be negative"));
        }
        
        if (maxTourists < 0) {
            return Result.failure(new BCError.ValidationError("NEGATIVE_MAX_TOURISTS", 
                "Maximum tourist count cannot be negative"));
        }
        
        if (population < 0) {
            return Result.failure(new BCError.ValidationError("NEGATIVE_POPULATION", 
                "Population cannot be negative"));
        }
        
        // Check configuration consistency
        if (population < ConfigLoader.minPopForTourists && maxTourists > 0) {
            return Result.failure(new BCError.ValidationError("INSUFFICIENT_POPULATION", 
                String.format("Population %d is below minimum required for tourists (%d)", 
                population, ConfigLoader.minPopForTourists)));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validates resource management parameters.
     * 
     * @param item The item being managed
     * @param amount The amount being added/removed
     * @param currentAmount The current amount in storage
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateResourceManagement(Item item, int amount, int currentAmount) {
        if (item == null) {
            return Result.failure(new BCError.ValidationError("NULL_ITEM", "Item cannot be null"));
        }
        
        if (amount == 0) {
            return Result.failure(new BCError.ValidationError("ZERO_AMOUNT", "Amount cannot be zero"));
        }
        
        if (currentAmount < 0) {
            return Result.failure(new BCError.ValidationError("NEGATIVE_CURRENT", 
                "Current amount cannot be negative"));
        }
        
        // Check for removal of more items than available
        if (amount < 0 && Math.abs(amount) > currentAmount) {
            return Result.failure(new BCError.ValidationError("INSUFFICIENT_RESOURCES", 
                String.format("Cannot remove %d %s, only %d available", 
                Math.abs(amount), item.getDescription().getString(), currentAmount)));
        }
        
        // Check for overflow
        if (amount > 0 && currentAmount > Integer.MAX_VALUE - amount) {
            return Result.failure(new BCError.ValidationError("AMOUNT_OVERFLOW", 
                "Adding resources would cause numeric overflow"));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validates visitor processing parameters.
     * 
     * @param visitorId The visitor's UUID
     * @param originTownId The origin town's UUID (can be null for players)
     * @return Result indicating validation success or failure
     */
    public Result<Void, BCError.ValidationError> validateVisitorProcessing(UUID visitorId, UUID originTownId) {
        if (visitorId == null) {
            return Result.failure(new BCError.ValidationError("NULL_VISITOR_ID", 
                "Visitor ID cannot be null"));
        }
        
        // Origin town ID can be null for player visitors, so no validation needed
        
        return Result.success(null);
    }
    
    /**
     * Basic check for inappropriate content in town names.
     * This is a simple implementation and could be expanded with a proper word filter.
     * 
     * @param name The name to check (should be lowercase)
     * @return True if the name contains inappropriate content
     */
    private boolean containsInappropriateContent(String name) {
        // Basic list of inappropriate words - in a real implementation,
        // this would be more comprehensive and configurable
        String[] inappropriateWords = {
            "admin", "server", "moderator", "owner", "staff", "op",
            "null", "undefined", "error", "exception", "debug"
        };
        
        for (String word : inappropriateWords) {
            if (name.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
}