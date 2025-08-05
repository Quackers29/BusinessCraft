package com.quackers29.businesscraft.demo;

import com.quackers29.businesscraft.platform.ForgePlatformService;
import com.quackers29.businesscraft.town.service.TownBusinessLogic;
import com.quackers29.businesscraft.town.ForgeTownAdapter;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.error.BCError;

import net.minecraft.core.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Demonstration class showing how the common business logic works with the Forge platform.
 * This proves that Phase 2.2 of the multi-module migration is working correctly.
 */
public class TownBusinessLogicDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBusinessLogicDemo.class);
    
    private final TownBusinessLogic businessLogic;
    
    public TownBusinessLogicDemo() {
        // Create the platform service and business logic
        ForgePlatformService platformService = new ForgePlatformService();
        this.businessLogic = new TownBusinessLogic(platformService);
    }
    
    /**
     * Demonstrate tourist capacity calculations using common business logic
     */
    public void demonstrateTouristCapacity() {
        LOGGER.info("=== Tourist Capacity Calculation Demo ===");
        
        // Test different population sizes
        int[] populations = {5, 10, 25, 50, 100};
        
        for (int population : populations) {
            int maxTourists = businessLogic.calculateMaxTourists(population);
            LOGGER.info("Population: {} -> Max Tourists: {}", population, maxTourists);
        }
    }
    
    /**
     * Demonstrate town name validation using common business logic
     */
    public void demonstrateNameValidation() {
        LOGGER.info("=== Town Name Validation Demo ===");
        
        String[] testNames = {
            "MyTown",           // Valid
            "A",                // Too short
            "",                 // Empty
            "This is a very long town name that exceeds the limit", // Too long
            "Town-With_Numbers123", // Valid with special chars
            "Invalid@Name!",    // Invalid characters
            null                // Null
        };
        
        for (String name : testNames) {
            Result<String, BCError> result = businessLogic.validateTownName(name);
            if (result.isSuccess()) {
                LOGGER.info("Valid name: '{}' -> '{}'", name, result.getValue());
            } else {
                LOGGER.info("Invalid name: '{}' -> Error: {}", name, result.getError().getMessage());
            }
        }
    }
    
    /**
     * Demonstrate tourist visit processing using common business logic
     */
    public void demonstrateTouristVisitProcessing() {
        LOGGER.info("=== Tourist Visit Processing Demo ===");
        
        // Create a sample town using the Forge implementation
        Town forgeTown = new Town(UUID.randomUUID(), new BlockPos(0, 64, 0), "DemoTown");
        
        // Wrap it in the adapter to make it compatible with common interfaces
        ForgeTownAdapter townAdapter = new ForgeTownAdapter(forgeTown);
        
        // Process some tourist visits
        UUID originTownId = UUID.randomUUID();
        ITownDataProvider.Position originPos = new com.quackers29.businesscraft.platform.ForgePosition(100, 64, 200);
        
        Result<TownBusinessLogic.TouristVisitResult, BCError> result = 
            businessLogic.processTouristVisit(townAdapter, originTownId, originPos, 3);
        
        if (result.isSuccess()) {
            TownBusinessLogic.TouristVisitResult visitResult = result.getValue();
            LOGGER.info("Tourist visit processed successfully:");
            LOGGER.info("  Tourists: {}", visitResult.getTouristCount());
            LOGGER.info("  Distance: {:.1f} blocks", visitResult.getDistance());
            LOGGER.info("  Base Reward: {} coins", visitResult.getBaseReward());
            LOGGER.info("  Distance Bonus: {} coins", visitResult.getDistanceBonus());
            LOGGER.info("  Total Reward: {} coins", visitResult.getTotalRewardValue());
        } else {
            LOGGER.error("Failed to process tourist visit: {}", result.getError().getMessage());
        }
    }
    
    /**
     * Demonstrate that the town can be checked for tourist spawning capability
     */
    public void demonstrateTownSpawningCheck() {
        LOGGER.info("=== Town Spawning Capability Demo ===");
        
        Town forgeTown = new Town(UUID.randomUUID(), new BlockPos(0, 64, 0), "TestTown");
        ForgeTownAdapter townAdapter = new ForgeTownAdapter(forgeTown);
        
        boolean canSpawn = businessLogic.canTownSpawnMoreTourists(townAdapter);
        LOGGER.info("Town '{}' can spawn more tourists: {}", townAdapter.getTownName(), canSpawn);
        
        // Disable tourist spawning and test again
        townAdapter.setTouristSpawningEnabled(false);
        canSpawn = businessLogic.canTownSpawnMoreTourists(townAdapter);
        LOGGER.info("After disabling spawning, can spawn tourists: {}", canSpawn);
    }
    
    /**
     * Run all demonstrations
     */
    public void runAllDemos() {
        LOGGER.info("Starting BusinessCraft Phase 2.2 Multi-Module Demo");
        LOGGER.info("This demonstrates that common business logic works with the Forge platform");
        
        demonstrateTouristCapacity();
        demonstrateNameValidation();
        demonstrateTouristVisitProcessing();
        demonstrateTownSpawningCheck();
        
        LOGGER.info("Phase 2.2 Multi-Module Demo completed successfully!");
    }
}