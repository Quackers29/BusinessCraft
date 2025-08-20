package com.quackers29.businesscraft.platform;

import org.jetbrains.annotations.Nullable;

/**
 * Platform-agnostic interface for block entity operations.
 * This interface provides a common API for block entity access across mod loaders.
 * 
 * Enhanced MultiLoader approach: Common module defines the interface,
 * platform modules implement using their specific APIs (Forge/Fabric).
 */
public interface BlockEntityHelper {
    
    /**
     * Get a block entity at the specified position in the player's world.
     * Platform implementations convert coordinates and player context to block entity access.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate  
     * @param z Block Z coordinate
     * @return Platform-specific block entity object, or null if not found
     */
    @Nullable
    Object getBlockEntity(Object player, int x, int y, int z);
    
    /**
     * Get the town data provider from a town interface block entity.
     * Platform implementations access the block entity's town data provider.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return Platform-specific town data provider, or null if not found
     */
    @Nullable
    Object getTownDataProvider(Object blockEntity);
    
    /**
     * Check if tourist spawning is enabled for a town data provider.
     * Platform implementations access the provider's tourist spawning state.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return True if tourist spawning is enabled
     */
    boolean isTouristSpawningEnabled(Object townDataProvider);
    
    /**
     * Set the tourist spawning enabled state for a town data provider.
     * Platform implementations update the provider's tourist spawning state.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param enabled New enabled state
     */
    void setTouristSpawningEnabled(Object townDataProvider, boolean enabled);
    
    /**
     * Mark the town data provider as dirty for persistence.
     * Platform implementations trigger the provider's dirty marking.
     * 
     * @param townDataProvider Platform-specific town data provider
     */
    void markTownDataDirty(Object townDataProvider);
    
    /**
     * Sync town data between client and server.
     * Platform implementations trigger data synchronization.
     * 
     * @param blockEntity Platform-specific town interface block entity
     */
    void syncTownData(Object blockEntity);
    
    /**
     * Get the town name from a town data provider.
     * Platform implementations access the provider's town name.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return Town name, or null if not set
     */
    @Nullable
    String getTownName(Object townDataProvider);
    
    /**
     * Set the town name for a town data provider.
     * Platform implementations update the provider's town name.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param townName New town name
     */
    void setTownName(Object townDataProvider, String townName);
    
    /**
     * Get the town ID from a town data provider.
     * Platform implementations access the provider's town UUID.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return Town UUID string, or null if not set
     */
    @Nullable
    String getTownId(Object townDataProvider);
    
    /**
     * Check if the town data provider has been initialized.
     * Platform implementations check the provider's initialization state.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return True if the provider is initialized
     */
    boolean isTownDataInitialized(Object townDataProvider);
    
    /**
     * Get the search radius for platforms from a town data provider.
     * Platform implementations access the provider's search radius setting.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @return Search radius in blocks
     */
    int getSearchRadius(Object townDataProvider);
    
    /**
     * Set the search radius for platforms for a town data provider.
     * Platform implementations update the provider's search radius setting.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param radius New search radius in blocks
     */
    void setSearchRadius(Object townDataProvider, int radius);
    
    /**
     * Check if more platforms can be added to the town interface.
     * Platform implementations check the platform capacity limits.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return True if more platforms can be added
     */
    boolean canAddMorePlatforms(Object blockEntity);
    
    /**
     * Add a new platform to the town interface.
     * Platform implementations handle platform creation and setup.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return True if platform was successfully added
     */
    boolean addPlatform(Object blockEntity);
    
    /**
     * Mark a block entity as changed for persistence and client sync.
     * Platform implementations trigger the block entity's change notification.
     * 
     * @param blockEntity Platform-specific block entity
     */
    void markBlockEntityChanged(Object blockEntity);
    
    /**
     * Delete a platform from the town interface.
     * Platform implementations handle platform removal.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformIndex Index of the platform to delete
     * @return True if platform was successfully deleted
     */
    boolean deletePlatform(Object blockEntity, int platformIndex);
    
    /**
     * Remove a platform from the town interface by UUID.
     * Platform implementations handle platform removal by UUID.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @return True if platform was successfully removed
     */
    boolean removePlatform(Object blockEntity, String platformId);
    
    /**
     * Get the number of platforms in the town interface.
     * Platform implementations return the current platform count.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return Number of platforms
     */
    int getPlatformCount(Object blockEntity);
    
    /**
     * Enable or disable a platform in the town interface.
     * Platform implementations update the platform's enabled state.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformIndex Index of the platform
     * @param enabled New enabled state
     */
    void setPlatformEnabled(Object blockEntity, int platformIndex, boolean enabled);
    
    /**
     * Check if a platform is enabled.
     * Platform implementations return the platform's enabled state.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformIndex Index of the platform
     * @return True if the platform is enabled
     */
    boolean isPlatformEnabled(Object blockEntity, int platformIndex);
    
    /**
     * Get a block entity on the client side at the specified coordinates.
     * Platform implementations access client world block entities.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @return Platform-specific block entity object, or null if not found
     */
    @Nullable
    Object getClientBlockEntity(int x, int y, int z);
    
    /**
     * Set destination enabled state for a platform.
     * Platform implementations update the platform's destination routing.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @param townId Target town UUID as string
     * @param enabled New enabled state for this destination
     * @return True if destination state was successfully updated
     */
    boolean setPlatformDestinationEnabled(Object blockEntity, String platformId, String townId, boolean enabled);
    
    /**
     * Get all destination towns for platform management.
     * Platform implementations access the town interface's destination data.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return Map of town UUID strings to town names, or empty map if none found
     */
    java.util.Map<String, String> getAllTownsForDestination(Object blockEntity);
    
    /**
     * Get destination enabled states for a platform.
     * Platform implementations access the platform's destination routing data.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @return Map of town UUID strings to enabled states, or empty map if platform not found
     */
    java.util.Map<String, Boolean> getPlatformDestinations(Object blockEntity, String platformId);
    
    /**
     * Get the origin town for the town interface.
     * Platform implementations access the block entity's associated town.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return Platform-specific town object, or null if not found
     */
    @Nullable
    Object getOriginTown(Object blockEntity);
    
    /**
     * Get town position as coordinate array.
     * Platform implementations convert town position to coordinate array.
     * 
     * @param town Platform-specific town object
     * @return Array [x, y, z] of town position, or null if not set
     */
    @Nullable
    int[] getTownPosition(Object town);
    
    /**
     * Get town by UUID from the server level.
     * Platform implementations access the town manager for the level.
     * 
     * @param player Platform-specific player object (for level access)
     * @param townId Town UUID as string
     * @return Platform-specific town object, or null if not found
     */
    @Nullable
    Object getTownById(Object player, String townId);
    
    /**
     * Set platform path coordinates (start and end positions).
     * Platform implementations update the platform's path configuration.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @param startX Start position X coordinate
     * @param startY Start position Y coordinate
     * @param startZ Start position Z coordinate
     * @param endX End position X coordinate
     * @param endY End position Y coordinate
     * @param endZ End position Z coordinate
     * @return True if platform path was successfully updated
     */
    boolean setPlatformPath(Object blockEntity, String platformId, 
                          int startX, int startY, int startZ,
                          int endX, int endY, int endZ);
    
    /**
     * Reset platform path coordinates (clear start and end positions).
     * Platform implementations clear the platform's path configuration.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @return True if platform path was successfully reset
     */
    boolean resetPlatformPath(Object blockEntity, String platformId);
    
    /**
     * Set platform enabled state by UUID.
     * Platform implementations update the platform's enabled state.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param platformId Platform UUID as string
     * @param enabled New enabled state
     * @return True if platform enabled state was successfully updated
     */
    boolean setPlatformEnabledById(Object blockEntity, String platformId, boolean enabled);
    
    /**
     * Set platform path creation mode.
     * Platform implementations update the interface's path creation mode.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param mode Path creation mode enabled state
     * @param platformId Platform UUID as string for the active platform
     * @return True if path creation mode was successfully set
     */
    boolean setPlatformCreationMode(Object blockEntity, boolean mode, String platformId);
    
    /**
     * Process a resource trade with the town.
     * Platform implementations handle town resource trading and payment calculation.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @param itemStack Platform-specific ItemStack to trade
     * @param slotId Slot ID for the trade
     * @return Platform-specific ItemStack payment result, or null if trade failed
     */
    Object processResourceTrade(Object blockEntity, Object player, Object itemStack, int slotId);
    
    /**
     * Get unclaimed rewards from the town's payment board.
     * Platform implementations access the payment board data from the town.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @return List of unclaimed rewards, or empty list if none found
     */
    java.util.List<Object> getUnclaimedRewards(Object blockEntity);
    
    /**
     * Claim a reward from the town's payment board.
     * Platform implementations handle the reward claiming process.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @param rewardId Reward UUID as string
     * @param toBuffer Whether to claim to buffer storage
     * @return Claim result object, or null if claim failed
     */
    Object claimPaymentBoardReward(Object blockEntity, Object player, String rewardId, boolean toBuffer);
    
    /**
     * Open the Payment Board UI for a player.
     * Platform implementations handle opening the payment board container screen.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @return True if the Payment Board UI was successfully opened
     */
    boolean openPaymentBoardUI(Object blockEntity, Object player);
    
    /**
     * Open the Town Interface UI for a player.
     * Platform implementations handle opening the town interface container screen.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @return True if the Town Interface UI was successfully opened
     */
    boolean openTownInterfaceUI(Object blockEntity, Object player);
    
    /**
     * Register player UI exit for proper cleanup.
     * Platform implementations handle UI exit registration for visualization cleanup.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @return True if player exit was successfully registered
     */
    boolean registerPlayerExitUI(Object blockEntity, Object player);
    
    
    /**
     * Open destinations UI on client side with provided town data.
     * Unified architecture approach - direct UI opening like main branch.
     * 
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param platformId Platform UUID as string
     * @param platformName Platform display name
     * @param townNames Map of town IDs to names
     * @param enabledState Map of town IDs to enabled states
     * @param townDistances Map of town IDs to distances
     * @param townDirections Map of town IDs to directions
     */
    void openDestinationsUI(int x, int y, int z, String platformId, String platformName, 
                           java.util.Map<java.util.UUID, String> townNames,
                           java.util.Map<java.util.UUID, Boolean> enabledState,
                           java.util.Map<java.util.UUID, Integer> townDistances,
                           java.util.Map<java.util.UUID, String> townDirections);
    
    /**
     * Add an item to communal storage.
     * Platform implementations handle adding items to town communal storage.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @param itemStack Platform-specific ItemStack to add
     * @param slotId Target slot ID
     * @return True if item was successfully added to communal storage
     */
    boolean addToCommunalStorage(Object blockEntity, Object player, Object itemStack, int slotId);
    
    /**
     * Remove an item from communal storage.
     * Platform implementations handle removing items from town communal storage.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @param itemStack Platform-specific ItemStack to remove
     * @param slotId Source slot ID
     * @return True if item was successfully removed from communal storage
     */
    boolean removeFromCommunalStorage(Object blockEntity, Object player, Object itemStack, int slotId);
    
    /**
     * Update communal storage UI on client side.
     * Platform implementations handle client-side communal storage UI updates.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param storageItems Map of slot IDs to ItemStacks
     * @return True if communal storage UI was successfully updated
     */
    boolean updateCommunalStorageUI(Object player, int x, int y, int z, java.util.Map<Integer, Object> storageItems);
    
    /**
     * Update buffer storage UI on client side.
     * Platform implementations handle client-side payment board buffer UI updates.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param bufferSlots Map of slot IDs to ItemStacks
     * @return True if buffer storage UI was successfully updated
     */
    boolean updateBufferStorageUI(Object player, int x, int y, int z, java.util.Map<Integer, Object> bufferSlots);
    
    /**
     * Process town map data request.
     * Platform implementations handle server-side town map data generation and response.
     * 
     * @param blockEntity Platform-specific town interface block entity
     * @param player Platform-specific player object
     * @param zoomLevel Map zoom level
     * @param includeStructures Whether to include structure data
     * @return True if town map data request was successfully processed
     */
    boolean processTownMapDataRequest(Object blockEntity, Object player, int zoomLevel, boolean includeStructures);
    
    /**
     * Process boundary sync request.
     * Platform implementations handle server-side boundary data synchronization and response.
     * 
     * @param townDataProvider Platform-specific town data provider
     * @param player Platform-specific player object
     * @param enableVisualization Whether to enable boundary visualization
     * @param renderDistance Boundary render distance
     * @return True if boundary sync request was successfully processed
     */
    boolean processBoundarySyncRequest(Object townDataProvider, Object player, boolean enableVisualization, int renderDistance);
    
    /**
     * Update town map UI on client side.
     * Platform implementations handle client-side town map UI updates.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param mapData JSON or serialized map data
     * @param zoomLevel Map zoom level
     * @return True if town map UI was successfully updated
     */
    boolean updateTownMapUI(Object player, int x, int y, int z, String mapData, int zoomLevel);
    
    /**
     * Process platform data request for sophisticated map view.
     * Platform implementations handle server-side platform data collection and response.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param includePlatformConnections Include platform layout and connections
     * @param includeDestinationTowns Include destination town information
     * @param maxRadius Maximum search radius for connected towns
     * @return True if platform data request was successfully processed
     */
    boolean processPlatformDataRequest(Object player, int x, int y, int z, 
                                     boolean includePlatformConnections, 
                                     boolean includeDestinationTowns, 
                                     int maxRadius);
    
    /**
     * Process platform data request with target town ID (UUID-based lookup).
     * Platform implementations handle server-side platform data generation for specific towns.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate (may be placeholder for UUID-based requests)
     * @param y Block Y coordinate (may be placeholder for UUID-based requests)
     * @param z Block Z coordinate (may be placeholder for UUID-based requests)
     * @param includePlatformConnections Include platform layout and connections
     * @param includeDestinationTowns Include destination town information
     * @param maxRadius Maximum search radius for connected towns
     * @param targetTownId Target town UUID as string (null for coordinate-based lookup)
     * @return True if platform data request was successfully processed
     */
    boolean processPlatformDataRequest(Object player, int x, int y, int z, 
                                     boolean includePlatformConnections, 
                                     boolean includeDestinationTowns, 
                                     int maxRadius, String targetTownId);
    
    /**
     * Process platform data request using UUID-based town lookup.
     * Platform implementations handle server-side platform data collection and response for map visualization.
     * 
     * @param player Platform-specific player object
     * @param targetTownId Target town UUID as string
     * @param includePlatformConnections Include platform layout and connections
     * @param includeDestinationTowns Include destination town information
     * @param maxRadius Maximum search radius for connected towns
     * @return True if platform data request was successfully processed
     */
    boolean processPlatformDataRequestByTownId(Object player, String targetTownId,
                                             boolean includePlatformConnections, 
                                             boolean includeDestinationTowns, 
                                             int maxRadius);
    
    /**
     * Update client-side town platform UI with received data.
     * Platform implementations handle client-side sophisticated map modal updates.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param platformData JSON serialized platform data
     * @param destinationData JSON serialized destination data
     * @return True if platform UI was successfully updated
     */
    boolean updateTownPlatformUI(Object player, int x, int y, int z, String platformData, String destinationData);
    
    /**
     * Update client-side town platform UI with structured data packet.
     * Platform implementations handle client-side sophisticated map modal updates with structured PlatformInfo data.
     * 
     * @param player Platform-specific player object
     * @param x Block X coordinate
     * @param y Block Y coordinate
     * @param z Block Z coordinate
     * @param packet Structured platform data response packet
     * @return True if platform UI was successfully updated
     */
    boolean updateTownPlatformUIStructured(Object player, int x, int y, int z, Object packet);
}