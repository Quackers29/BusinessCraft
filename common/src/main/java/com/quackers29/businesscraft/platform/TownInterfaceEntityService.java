package com.quackers29.businesscraft.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Platform service for TownInterfaceEntity operations.
 * This abstracts all platform-specific operations for the TownInterfaceEntity
 * including tourist spawning, visitor processing, vehicle management, and scoreboard updates.
 */
public interface TownInterfaceEntityService {

    /**
     * Process tourist spawning for the given town interface entity.
     * @param level The level
     * @param entity The town interface entity
     * @param townId The town ID
     * @param touristSpawningEnabled Whether tourist spawning is enabled
     * @param platformCount The number of platforms
     * @param gameTime The current game time
     */
    void processTouristSpawning(Level level, BlockEntity entity, String townId,
                               boolean touristSpawningEnabled, int platformCount, long gameTime);

    /**
     * Process visitor interactions for the given town interface entity.
     * @param level The level
     * @param entity The town interface entity
     * @param pos The block position
     * @param townId The town ID
     * @param searchRadius The search radius
     * @param townName The town name
     * @param gameTime The current game time
     */
    void processVisitorInteractions(Level level, BlockEntity entity, BlockPos pos,
                                  String townId, int searchRadius, String townName, long gameTime);

    /**
     * Process tourist vehicles for the given town interface entity.
     * @param level The level
     * @param entity The town interface entity
     * @param townId The town ID
     * @param touristSpawningEnabled Whether tourist spawning is enabled
     * @param searchRadius The search radius
     * @param gameTime The current game time
     */
    void processTouristVehicles(Level level, BlockEntity entity, String townId,
                               boolean touristSpawningEnabled, int searchRadius, long gameTime);

    /**
     * Update the scoreboard for the given level.
     * @param level The level to update scoreboard for
     */
    void updateScoreboard(Level level);

    /**
     * Process resources in the entity's inventory slots.
     * @param entity The town interface entity
     */
    void processResourcesInSlot(BlockEntity entity);

    /**
     * Update entity data from town provider.
     * @param entity The town interface entity
     */
    void updateFromTownProvider(BlockEntity entity);

    /**
     * Handle buffer management for the entity.
     * @param entity The town interface entity
     */
    void handleBufferManagement(BlockEntity entity);

    /**
     * Handle client synchronization for the entity.
     * @param entity The town interface entity
     */
    void handleClientSynchronization(BlockEntity entity);
}
