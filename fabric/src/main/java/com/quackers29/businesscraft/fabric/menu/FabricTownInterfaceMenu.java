package com.quackers29.businesscraft.fabric.menu;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.fabric.block.entity.FabricTownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Fabric-specific menu for the Town Interface block.
 * Extends ScreenHandler (Fabric) instead of AbstractContainerMenu (Forge).
 * This class manages the data displayed in the Town Interface UI.
 */
public class FabricTownInterfaceMenu extends ScreenHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/FabricTownInterfaceMenu");
    private final BlockPos pos;
    private final World level;
    private Town town;
    private UUID townId;
    private boolean needsImmediateSync = true;
    
    // PropertyDelegate for syncing values between server and client (Fabric equivalent of ContainerData)
    private static final int DATA_SEARCH_RADIUS = 0;
    private static final int DATA_TOURIST_COUNT = 1;
    private static final int DATA_MAX_TOURISTS = 2;
    private static final int DATA_POPULATION = 3;
    private final PropertyDelegate data;
    
    // Town properties for UI display (fallbacks)
    private String townName = "New Town";
    private int townLevel = 1;
    private int townPopulation = 5; // Default population to 5 like TownBlock
    private int townReputation = 50;
    
    // Tourism data (fallbacks)
    private int currentTourists = 0;
    private int maxTourists = 5;
    
    // Economy data
    private int goldCoins = 0;
    private int silverCoins = 0;
    private int bronzeCoins = 0;
    
    // Settings
    private boolean autoCollectEnabled = false;
    private boolean taxesEnabled = false;
    // Local cache for client-side  
    private int clientSearchRadius = 10;
    private long lastUpdateTick = 0;
    
    /**
     * Helper method to find TownManager.get() method compatible with ServerWorld
     * Avoids loading Forge's ServerLevel class name which triggers dependency loading
     */
    private static java.lang.reflect.Method findTownManagerGetMethod(Class<?> townManagerClass, Object serverWorld) {
        for (java.lang.reflect.Method m : townManagerClass.getMethods()) {
            if ("get".equals(m.getName()) && m.getParameterCount() == 1) {
                // Check if parameter type is compatible with ServerWorld
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isInstance(serverWorld)) {
                    return m;
                }
            }
        }
        return null;
    }

    /**
     * Constructor for server-side menu creation
     */
    public FabricTownInterfaceMenu(int syncId, PlayerInventory inv, BlockPos pos) {
        this(null, syncId, inv, pos);
    }
    
    /**
     * Constructor with ScreenHandlerType (used by registration)
     */
    public FabricTownInterfaceMenu(ScreenHandlerType<?> type, int syncId, PlayerInventory inv, BlockPos pos) {
        super(type, syncId);
        this.pos = pos;
        this.level = inv.player.getWorld();
        
        // Create PropertyDelegate for syncing data (Fabric equivalent of SimpleContainerData)
        this.data = new PropertyDelegate() {
            private final int[] values = new int[4];
            
            @Override
            public int get(int index) {
                if (index >= 0 && index < values.length) {
                    return values[index];
                }
                return 0;
            }
            
            @Override
            public void set(int index, int value) {
                if (index >= 0 && index < values.length) {
                    values[index] = value;
                }
            }
            
            @Override
            public int size() {
                return values.length;
            }
        };
        
        // Set default values
        this.data.set(DATA_SEARCH_RADIUS, 10); // Default search radius
        this.data.set(DATA_POPULATION, 5); // Default population
        this.data.set(DATA_TOURIST_COUNT, 0); // Default tourist count
        this.data.set(DATA_MAX_TOURISTS, 5); // Default max tourists
        
        // Add property delegate to screen handler
        this.addProperties(this.data);
        
        if (level.isClient) {
            for(int i=0; i<4; i++) data.set(i, -1);
        } else {
            // Get town from TownManager using reflection to bridge ServerWorld (Fabric) -> ServerLevel (Forge)
            // Wrap in try-catch to handle ClassNotFoundException/NoClassDefFoundError when loading TownManager
            // (TownManager has Forge dependencies that may not be available at this point)
            if (level instanceof ServerWorld serverWorld) {
                try {
                    // Use reflection to call TownManager.get() with ServerWorld cast to ServerLevel
                    Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                    java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                    if (getMethod == null) {
                        LOGGER.warn("Could not find TownManager.get() method compatible with ServerWorld - skipping town initialization");
                        return; // Menu will work without town data initially
                    }
                    Object townManager = getMethod.invoke(null, serverWorld);
                    
                    // Get all towns
                    java.lang.reflect.Method getAllTownsMethod = townManagerClass.getMethod("getAllTowns");
                    @SuppressWarnings("unchecked")
                    Map<UUID, Town> allTowns = (Map<UUID, Town>) getAllTownsMethod.invoke(townManager);
                    
                    // Find the town at this position by iterating through all towns
                    // Use reflection to get position and compare BlockPos (Forge) with BlockPos (Fabric)
                    java.lang.reflect.Method getPositionMethod = null;
                    try {
                        getPositionMethod = Town.class.getMethod("getPosition");
                    } catch (NoSuchMethodException e) {
                        LOGGER.error("Failed to find getPosition method on Town", e);
                    }
                    
                    for (Town t : allTowns.values()) {
                        try {
                            // Use reflection to call getPosition() to avoid compile-time Forge BlockPos dependency
                            Object townPos = getPositionMethod != null ? getPositionMethod.invoke(t) : null;
                            // Compare using equals - at runtime these should be compatible
                            if (townPos != null && townPos.equals(pos)) {
                                this.town = t;
                                this.townId = t.getId();
                                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town with ID {} at position {}", this.townId, pos);
                                break;
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to get town position: " + e.getMessage(), e);
                        }
                    }
                    
                    // If no town found, try to get from block entity
                    if (this.town == null) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "No town found exactly at position {}, checking block entity", pos);
                        BlockEntity blockEntity = level.getBlockEntity(pos);
                        if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                            UUID entityTownId = townEntity.getTownId();
                            if (entityTownId != null) {
                                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                                this.town = (Town) getTownMethod.invoke(townManager, entityTownId);
                                this.townId = entityTownId;
                                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town with ID {} from block entity", this.townId);
                            }
                        }
                    }
                    
                    if (this.town == null) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "No town found at or associated with position {}", pos);
                    } else {
                        // Initialize data values from town
                        updateDataSlots();
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException e) {
                    // TownManager or its dependencies (like net.minecraft.nbt.Tag) not available
                    // This can happen if Forge classes aren't loaded yet
                    // Menu will work without town data initially - town can be looked up later when needed
                    LOGGER.warn("TownManager not available (Forge dependencies not loaded): " + e.getMessage());
                    LOGGER.warn("Menu will work without initial town data - town will be looked up lazily when needed");
                } catch (Exception e) {
                    LOGGER.error("Failed to get TownManager: " + e.getMessage(), e);
                }
            }
        }
    }
    
    /**
     * Constructor for client-side menu creation
     */
    public FabricTownInterfaceMenu(int syncId, PlayerInventory inv, PacketByteBuf data) {
        this(null, syncId, inv, data.readBlockPos());
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        // No inventory slots to manage
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
    
    // Getters for UI data
    
    public String getTownName() {
        if (town != null) {
            return town.getName();
        }
        
        // Try to get name from town entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.getTownName();
            }
        }
        
        return "Unknown Town";
    }
    
    public int getTownPopulation() {
        // First try to get from our PropertyDelegate which is synced between server and client
        int populationFromData = data.get(DATA_POPULATION);
        if (populationFromData > 0) {
            return populationFromData;
        }
        
        // If data isn't available yet or we're on server side
        if (town != null) {
            return town.getPopulation();
        }
        
        // Try to get population from town entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                // Get population directly from the entity
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerWorld serverWorld) {
                    try {
                        Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                        java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                        if (getMethod == null) {
                            LOGGER.error("Could not find TownManager.get() method compatible with ServerWorld");
                            return townEntity.getPopulation();
                        }
                        Object townManager = getMethod.invoke(null, serverWorld);
                        java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                        Town townFromEntity = (Town) getTownMethod.invoke(townManager, entityTownId);
                        if (townFromEntity != null) {
                            return townFromEntity.getPopulation();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to get town from TownManager: " + e.getMessage(), e);
                    }
                }
                return townEntity.getPopulation();
            }
        }
        
        return townPopulation; // Default fallback is 5
    }
    
    /**
     * Gets the current number of tourists in this town
     */
    public int getCurrentTourists() {
        // First try to get from our PropertyDelegate which is synced between server and client
        int touristsFromData = data.get(DATA_TOURIST_COUNT);
        if(touristsFromData < 0) return -1;
        if (touristsFromData >= 0) {  // Tourist count can be 0
            return touristsFromData;
        }
        
        // If data isn't available yet or we're on server side
        if (town != null) {
            return town.getTouristCount();
        }
        
        // Try to get tourist count from town entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the tourist count
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerWorld serverWorld) {
                    try {
                        Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                        java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                        if (getMethod == null) {
                            LOGGER.error("Could not find TownManager.get() method compatible with ServerWorld");
                            return townEntity.getPopulation();
                        }
                        Object townManager = getMethod.invoke(null, serverWorld);
                        java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                        Town townFromEntity = (Town) getTownMethod.invoke(townManager, entityTownId);
                        if (townFromEntity != null) {
                            return townFromEntity.getTouristCount();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to get town from TownManager: " + e.getMessage(), e);
                    }
                }
            }
        }
        
        return currentTourists; // Default fallback
    }
    
    /**
     * Gets the maximum number of tourists this town can support
     */
    public int getMaxTourists() {
        // First try to get from our PropertyDelegate which is synced between server and client
        int maxTouristsFromData = data.get(DATA_MAX_TOURISTS);
        if (maxTouristsFromData > 0) {
            return maxTouristsFromData;
        }
        
        // If data isn't available yet or we're on server side
        if (town != null) {
            return town.getMaxTourists();
        }
        
        // Try to get max tourists from town entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the max tourists
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerWorld serverWorld) {
                    try {
                        Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                        java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                        if (getMethod == null) {
                            LOGGER.error("Could not find TownManager.get() method compatible with ServerWorld");
                            return townEntity.getPopulation();
                        }
                        Object townManager = getMethod.invoke(null, serverWorld);
                        java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                        Town townFromEntity = (Town) getTownMethod.invoke(townManager, entityTownId);
                        if (townFromEntity != null) {
                            return townFromEntity.getMaxTourists();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to get town from TownManager: " + e.getMessage(), e);
                    }
                }
            }
        }
        
        return maxTourists; // Default fallback
    }
    
    public int getTownLevel() {
        return townLevel;
    }
    
    public int getTownReputation() {
        return townReputation;
    }
    
    public int getGoldCoins() {
        return goldCoins;
    }
    
    public int getSilverCoins() {
        return silverCoins;
    }
    
    public int getBronzeCoins() {
        return bronzeCoins;
    }
    
    public boolean isAutoCollectEnabled() {
        return autoCollectEnabled;
    }
    
    public void setAutoCollectEnabled(boolean enabled) {
        this.autoCollectEnabled = enabled;
        // In a real implementation, this would trigger a server-side update
    }
    
    public boolean isTaxesEnabled() {
        return taxesEnabled;
    }
    
    public void setTaxesEnabled(boolean enabled) {
        this.taxesEnabled = enabled;
        // In a real implementation, this would trigger a server-side update
    }
    
    /**
     * Gets the search radius for this town
     * @return the search radius value
     */
    public int getSearchRadius() {
        // Try to get search radius from town entity first (most up-to-date)
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                int entityRadius = townEntity.getSearchRadius();
                // Update cache for consistency
                this.clientSearchRadius = entityRadius;
                data.set(DATA_SEARCH_RADIUS, entityRadius);
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getSearchRadius() returning entity value: {}", entityRadius);
                return entityRadius;
            }
        }
        
        // If block entity isn't available, try town object
        if (town != null) {
            int townRadius = town.getSearchRadius();
            // Update cache for consistency
            this.clientSearchRadius = townRadius;
            data.set(DATA_SEARCH_RADIUS, townRadius);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getSearchRadius() returning town value: {}", townRadius);
            return townRadius;
        }
        
        // Then try our PropertyDelegate as fallback
        int radiusFromData = data.get(DATA_SEARCH_RADIUS);
        if (radiusFromData > 0 && radiusFromData <= 100) {
            this.clientSearchRadius = radiusFromData;
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getSearchRadius() returning data value: {}", radiusFromData);
            return radiusFromData;
        }
        
        // Last resort - return cached value or default
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getSearchRadius() returning cached value: {}", clientSearchRadius);
        return clientSearchRadius;
    }
    
    /**
     * Updates the client-side search radius value
     * This provides immediate visual feedback until the server syncs
     * @param radius The new radius value
     */
    public void setClientSearchRadius(int radius) {
        // Store the new radius in our client cache
        this.clientSearchRadius = radius;
        
        // Also update in the data if we're on the client (for immediate feedback)
        if (level != null && level.isClient) {
            data.set(DATA_SEARCH_RADIUS, radius);
        }
    }
    
    /**
     * Gets the BlockPos of this town interface
     */
    public BlockPos getBlockPos() {
        return pos;
    }
    
    /**
     * Gets the list of platforms from the town block entity
     * @return List of platforms or an empty list if none found
     */
    public List<Platform> getPlatforms() {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.getPlatforms();
            }
        }
        return new ArrayList<>();
    }
    
    /**
     * Adds a new platform to the town block entity
     * @return true if the platform was added successfully
     */
    public boolean addPlatform() {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.addPlatform();
            }
        }
        return false;
    }
    
    /**
     * Removes a platform by ID
     * @param platformId The UUID of the platform to remove
     * @return true if the platform was removed successfully
     */
    public boolean removePlatform(UUID platformId) {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.removePlatform(platformId);
            }
        }
        return false;
    }
    
    /**
     * Gets a specific platform by ID
     * @param platformId The UUID of the platform to get
     * @return The platform or null if not found
     */
    public Platform getPlatform(UUID platformId) {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.getPlatform(platformId);
            }
        }
        return null;
    }
    
    /**
     * Checks if more platforms can be added
     * @return true if more platforms can be added
     */
    public boolean canAddMorePlatforms() {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.canAddMorePlatforms();
            }
        }
        return false;
    }
    
    /**
     * Gets all resources in the town.
     * @return A map of items to their quantities.
     */
    @SuppressWarnings("unchecked")
    public Map<Item, Integer> getAllResources() {
        if (town != null) {
            // Town returns Map<net.minecraft.world.item.Item, Integer> (Forge), need to cast to Fabric Item
            Map<?, Integer> resources = town.getAllResources();
            // At runtime, these should be compatible - use unchecked cast
            return (Map<Item, Integer>) resources;
        }
        
        // Try to get resources from town entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                // On client-side, we need to get cached resources from the entity directly
                if (level.isClient) {
                    return townEntity.getClientResources();
                }
                // On server-side, get from TownManager
                else {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null && level instanceof ServerWorld serverWorld) {
                        try {
                            Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                            java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                            if (getMethod == null) {
                                LOGGER.error("Could not find TownManager.get() method compatible with ServerWorld");
                                return Collections.emptyMap();
                            }
                            Object townManager = getMethod.invoke(null, serverWorld);
                            java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                            Town townFromEntity = (Town) getTownMethod.invoke(townManager, entityTownId);
                            if (townFromEntity != null) {
                                Map<?, Integer> resources = townFromEntity.getAllResources();
                                return (Map<Item, Integer>) resources;
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to get town from TownManager: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
        
        // Return empty map if no resources found
        return Collections.emptyMap();
    }
    
    /**
     * Get all items in the town's communal storage
     * 
     * @return Map of items and their quantities in communal storage
     */
    @SuppressWarnings("unchecked")
    public Map<Item, Integer> getAllCommunalStorageItems() {
        if (town != null) {
            // Town returns Map<net.minecraft.world.item.Item, Integer> (Forge), need to cast to Fabric Item
            Map<?, Integer> storage = town.getAllCommunalStorageItems();
            // At runtime, these should be compatible - use unchecked cast
            return (Map<Item, Integer>) storage;
        }
        
        // Try to get communal storage from town entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                // On client-side, we need to get cached communal storage from the entity directly
                if (level.isClient) {
                    return townEntity.getClientCommunalStorage();
                }
                // On server-side, get from TownManager
                else {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null && level instanceof ServerWorld serverWorld) {
                        try {
                            Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                            java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                            if (getMethod == null) {
                                LOGGER.error("Could not find TownManager.get() method compatible with ServerWorld");
                                return Collections.emptyMap();
                            }
                            Object townManager = getMethod.invoke(null, serverWorld);
                            java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                            Town townFromEntity = (Town) getTownMethod.invoke(townManager, entityTownId);
                            if (townFromEntity != null) {
                                Map<?, Integer> storage = townFromEntity.getAllCommunalStorageItems();
                                return (Map<Item, Integer>) storage;
                            }
                        } catch (Exception e) {
                            LOGGER.error("Failed to get town from TownManager: " + e.getMessage(), e);
                        }
                    }
                }
            }
        }
        
        // Return empty map if no communal storage found
        return Collections.emptyMap();
    }

    private void updateDataSlots() {
        if (town != null) {
            int townSearchRadius = town.getSearchRadius();
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() setting town search radius: {}", townSearchRadius);
            data.set(DATA_POPULATION, town.getPopulation());
            data.set(DATA_TOURIST_COUNT, town.getTouristCount());
            data.set(DATA_MAX_TOURISTS, town.getMaxTourists());
            data.set(DATA_SEARCH_RADIUS, townSearchRadius);
        } else if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                // Try to get values from TownBlockEntity if town is not available
                // First try to get town data through the entity's town provider
                ITownDataProvider provider = townEntity.getTownDataProvider();
                if (provider != null) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() using data provider from entity");
                    data.set(DATA_POPULATION, provider.getPopulation());
                    data.set(DATA_TOURIST_COUNT, provider.getTouristCount());
                    data.set(DATA_MAX_TOURISTS, provider.getMaxTourists());
                    data.set(DATA_SEARCH_RADIUS, provider.getSearchRadius());
                } else {
                    // Fallback to entity methods if no provider available
                    int entitySearchRadius = townEntity.getSearchRadius();
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() using entity fallback, search radius: {}", entitySearchRadius);
                    data.set(DATA_POPULATION, townEntity.getPopulation());
                    data.set(DATA_TOURIST_COUNT, 0);
                    data.set(DATA_MAX_TOURISTS, 5);
                    data.set(DATA_SEARCH_RADIUS, entitySearchRadius);
                }
            }
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() no data source available");
        }
    }

    /**
     * Refreshes the PropertyDelegate slots with current town data.
     * This method should be called when town data changes to ensure
     * the UI displays up-to-date population and tourist values.
     */
    public void refreshDataSlots() {
        // Force re-fetch the town data
        if (level instanceof ServerWorld serverWorld && townId != null) {
            try {
                Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                if (getMethod == null) {
                    LOGGER.error("Could not find TownManager.get() method compatible with ServerWorld");
                    return;
                }
                Object townManager = getMethod.invoke(null, serverWorld);
                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                this.town = (Town) getTownMethod.invoke(townManager, townId);
            } catch (Exception e) {
                LOGGER.error("Failed to refresh town data: " + e.getMessage(), e);
            }
        }
        
        // Update the data slots with current values
        updateDataSlots();
    }
    
    /**
     * Forces a refresh of the search radius data specifically.
     * This should be called when the search radius is updated on the server.
     */
    public void refreshSearchRadius() {
        if (town != null) {
            data.set(DATA_SEARCH_RADIUS, town.getSearchRadius());
        } else if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                data.set(DATA_SEARCH_RADIUS, townEntity.getSearchRadius());
            }
        }
    }

    /**
     * Get the town data provider for this menu
     * This allows access to town data through a standardized interface
     * 
     * @return The town data provider, or null if not available
     */
    public ITownDataProvider getTownDataProvider() {
        // If we have a town reference, it implements ITownDataProvider
        if (town != null) {
            return town;
        }
        
        // Otherwise try to get from block entity
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity.getTownDataProvider();
            }
        }
        
        return null;
    }
    
    /**
     * Get the FabricTownInterfaceEntity associated with this menu
     * 
     * @return The FabricTownInterfaceEntity, or null if not available
     */
    public FabricTownInterfaceEntity getTownInterfaceEntity() {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof FabricTownInterfaceEntity townEntity) {
                return townEntity;
            }
        }
        return null;
    }
}

