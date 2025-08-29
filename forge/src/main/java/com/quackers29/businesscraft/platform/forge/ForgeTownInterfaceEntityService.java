package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.TownInterfaceEntityService;
import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.data.TownBufferManager;
import com.quackers29.businesscraft.town.data.VisitBuffer;
import com.quackers29.businesscraft.scoreboard.TownScoreboardManager;
import com.quackers29.businesscraft.service.TouristVehicleManager;
import com.quackers29.businesscraft.town.data.TouristSpawningHelper;
import com.quackers29.businesscraft.town.data.VisitorProcessingHelper;
import com.quackers29.businesscraft.town.data.PlatformManager;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge implementation of TownInterfaceEntityService.
 * This handles all Forge-specific operations for the TownInterfaceEntity.
 */
public class ForgeTownInterfaceEntityService implements TownInterfaceEntityService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeTownInterfaceEntityService.class);

    // Helper instances
    private final TouristSpawningHelper touristSpawningHelper = new TouristSpawningHelper();
    private final VisitorProcessingHelper visitorProcessingHelper = new VisitorProcessingHelper();
    private final TouristVehicleManager touristVehicleManager = new TouristVehicleManager();

    @Override
    public void processTouristSpawning(Level level, BlockEntity entity, String townId,
                                     boolean touristSpawningEnabled, int platformCount, long gameTime) {
        if (!level.isClientSide && townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(java.util.UUID.fromString(townId));
            if (town != null && touristSpawningEnabled && town.canSpawnTourists() &&
                platformCount > 0 && gameTime % 200 == 0) {

                // Get platforms from the entity - we need to access platformManager
                try {
                    if (entity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity forgeEntity) {
                        var platformManagerObj = forgeEntity.getPlatformManager();
                        if (platformManagerObj != null) {
                            com.quackers29.businesscraft.town.data.PlatformManager platformManager =
                                (com.quackers29.businesscraft.town.data.PlatformManager) platformManagerObj;
                            var enabledPlatforms = platformManager.getEnabledPlatforms();
                            for (var platform : enabledPlatforms) {
                                touristSpawningHelper.spawnTouristOnPlatform(level, town, platform,
                                    java.util.UUID.fromString(townId));
                            }
                        }
                    }
                } catch (Exception e) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                        "Error accessing platform manager for tourist spawning: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void processVisitorInteractions(Level level, BlockEntity entity, BlockPos pos,
                                        String townId, int searchRadius, String townName, long gameTime) {
        if (!level.isClientSide && townId != null && level instanceof ServerLevel sLevel &&
            gameTime % 40 == 0) {

            Town town = TownManager.get(sLevel).getTown(java.util.UUID.fromString(townId));
            if (town != null) {
                try {
                    if (entity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity forgeEntity) {
                        var visitorHelper = forgeEntity.getVisitorProcessingHelper();
                        if (visitorHelper != null) {
                            VisitorProcessingHelper vph = (VisitorProcessingHelper) visitorHelper;
                            // Use visitor processing helper with proper parameters
                            Object visitBufferObj = forgeEntity.getVisitBuffer();
                            com.quackers29.businesscraft.town.data.VisitBuffer visitBuffer =
                                visitBufferObj instanceof com.quackers29.businesscraft.town.data.VisitBuffer ?
                                (com.quackers29.businesscraft.town.data.VisitBuffer) visitBufferObj :
                                new com.quackers29.businesscraft.town.data.VisitBuffer();

                            vph.processVisitors(
                                level,
                                pos,
                                java.util.UUID.fromString(townId),
                                forgeEntity.getPlatformManager(),
                                visitBuffer,
                                searchRadius,
                                townName,
                                entity::setChanged
                            );
                            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                                "Successfully processed visitor interactions for town {} at {}", townId, pos);
                        }
                    }
                } catch (Exception e) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                        "Error processing visitor interactions: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void processTouristVehicles(Level level, BlockEntity entity, String townId,
                                     boolean touristSpawningEnabled, int searchRadius, long gameTime) {
        if (!level.isClientSide && townId != null && level instanceof ServerLevel sLevel &&
            gameTime % 20 == 0 && touristSpawningEnabled) {

            Town town = TownManager.get(sLevel).getTown(java.util.UUID.fromString(townId));
            if (town != null && town.canSpawnTourists()) {
                try {
                    if (entity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity forgeEntity) {
                        var platformManagerObj = forgeEntity.getPlatformManager();
                        if (platformManagerObj != null) {
                            com.quackers29.businesscraft.town.data.PlatformManager platformManager =
                                (com.quackers29.businesscraft.town.data.PlatformManager) platformManagerObj;
                            var enabledPlatforms = platformManager.getEnabledPlatforms();
                            for (var platform : enabledPlatforms) {
                                // Use the proper TouristVehicleManager from the entity or the service
                                var vehicleManager = BusinessCraft.TOURIST_VEHICLE_MANAGER;
                                if (vehicleManager != null) {
                                    vehicleManager.mountTouristsToVehicles(
                                        level,
                                        platform.getStartPos(),
                                        platform.getEndPos(),
                                        searchRadius,
                                        java.util.UUID.fromString(townId)
                                    );
                                }
                            }
                            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                                "Successfully processed tourist vehicles for {} platforms in town {} at {}",
                                enabledPlatforms.size(), townId, entity.getBlockPos());
                        }
                    }
                } catch (Exception e) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                        "Error processing tourist vehicles: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void updateScoreboard(Level level) {
        if (level instanceof ServerLevel sLevel) {
            TownScoreboardManager.updateScoreboard(sLevel);
        }
    }

    @Override
    public void processResourcesInSlot(BlockEntity entity) {
        // Platform-specific resource processing logic
        // This would handle inventory slot operations specific to Forge
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Processing resources in slots for entity at {}", entity.getBlockPos());
    }

    @Override
    public void updateFromTownProvider(BlockEntity entity) {
        // Platform-specific town data synchronization
        // This would sync entity data with town provider
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Updating from town provider for entity at {}", entity.getBlockPos());
    }

    @Override
    public void handleBufferManagement(BlockEntity entity) {
        // Handle buffer management operations
        // This would manage TownBufferManager operations
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Handling buffer management for entity at {}", entity.getBlockPos());
    }

    @Override
    public void handleClientSynchronization(BlockEntity entity) {
        // Handle client synchronization operations
        // This would manage ClientSyncHelper operations
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Handling client synchronization for entity at {}", entity.getBlockPos());
    }
}
