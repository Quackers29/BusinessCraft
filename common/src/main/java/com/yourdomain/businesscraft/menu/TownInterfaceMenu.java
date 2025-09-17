package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Menu container for the Town Interface block.
 * This class manages the data displayed in the Town Interface UI.
 */
public class TownInterfaceMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceMenu");
    private final BlockPos pos;
    private final Level level;
    private Town town;
    private UUID townId;
    private boolean needsImmediateSync = true;
    
    // Add ContainerData field for syncing values between server and client
    private static final int DATA_SEARCH_RADIUS = 0;
    private static final int DATA_TOURIST_COUNT = 1;
    private static final int DATA_MAX_TOURISTS = 2;
    private static final int DATA_POPULATION = 3;
    private SimpleContainerData data;
    
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
     * Constructor for server-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, BlockPos pos) {
        super(PlatformAccess.getMenuTypes().getTownInterfaceMenuType(), windowId);
        this.pos = pos;
        this.level = inv.player.level();
        
        // Initialize container data with 4 slots and sensible defaults
        this.data = new SimpleContainerData(4);
        // Set default values to avoid returning 0 when data isn't synced yet
        this.data.set(DATA_SEARCH_RADIUS, 10); // Default search radius
        this.data.set(DATA_POPULATION, 5); // Default population
        this.data.set(DATA_TOURIST_COUNT, 0); // Default tourist count
        this.data.set(DATA_MAX_TOURISTS, 5); // Default max tourists
        addDataSlots(this.data);
        
        if (level.isClientSide()) {
            for(int i=0; i<4; i++) data.set(i, -1);
        } else {
        // Get town from TownManager
        if (level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            
            // Find the town at this position by iterating through all towns
            Map<UUID, Town> allTowns = townManager.getAllTowns();
            for (Town t : allTowns.values()) {
                if (t.getPosition().equals(pos)) {
                    this.town = t;
                    this.townId = t.getId();
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town with ID {} at position {}", this.townId, pos);
                    break;
                }
            }
            
            // If no town found, try to get from block entity
            if (this.town == null) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "No town found exactly at position {}, checking block entity", pos);
                if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null) {
                        this.town = townManager.getTown(entityTownId);
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
                    if (level != null && !level.isClientSide()) {
                        broadcastChanges();
                    }
                }
            }
        }
    }
    
    /**
     * Constructor for client-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf data) {
        this(windowId, inv, data.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // No inventory slots to manage
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
    
    @Override
    public void broadcastChanges() {
        // Force immediate data sync on first broadcast to eliminate initial delay
        if (needsImmediateSync && level != null && !level.isClientSide()) {
            updateDataSlots();
            needsImmediateSync = false;
        }
        if (level != null && !level.isClientSide() && level.getGameTime() - lastUpdateTick >= 20) {
            updateDataSlots();
            lastUpdateTick = level.getGameTime();
        }
        super.broadcastChanges();
    }
    
    // Getters for UI data
    
    public String getTownName() {
        if (town != null) {
            return town.getName();
        }
        
        // Try to get name from town entity
        if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            return townEntity.getTownName();
        }
        
        return "Unknown Town";
    }
    
    public int getTownPopulation() {
        // First try to get from our ContainerData which is synced between server and client
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
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // Get population directly from the entity
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return townFromEntity.getPopulation();
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
        // First try to get from our ContainerData which is synced between server and client
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
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the tourist count
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return townFromEntity.getTouristCount();
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
        // First try to get from our ContainerData which is synced between server and client
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
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the max tourists
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return townFromEntity.getMaxTourists();
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
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
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
        
        // Then try our ContainerData as fallback
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
        if (level != null && level.isClientSide()) {
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
        if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            return townEntity.getPlatforms();
        }
        return new ArrayList<>();
    }
    
    /**
     * Adds a new platform to the town block entity
     * @return true if the platform was added successfully
     */
    public boolean addPlatform() {
        if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            return townEntity.addPlatform();
        }
        return false;
    }
    
    /**
     * Removes a platform by ID
     * @param platformId The UUID of the platform to remove
     * @return true if the platform was removed successfully
     */
    public boolean removePlatform(UUID platformId) {
        if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            return townEntity.removePlatform(platformId);
        }
        return false;
    }
    
    /**
     * Gets a specific platform by ID
     * @param platformId The UUID of the platform to get
     * @return The platform or null if not found
     */
    public Platform getPlatform(UUID platformId) {
        if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            return townEntity.getPlatform(platformId);
        }
        return null;
    }
    
    /**
     * Checks if more platforms can be added
     * @return true if more platforms can be added
     */
    public boolean canAddMorePlatforms() {
        if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            return townEntity.canAddMorePlatforms();
        }
        return false;
    }
    
    /**
     * Gets all resources in the town.
     * @return A map of items to their quantities.
     */
    public Map<Item, Integer> getAllResources() {
        if (town != null) {
            return town.getAllResources();
        }
        
        // Try to get resources from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // On client-side, we need to get cached resources from the entity directly
                if (level.isClientSide()) {
                    return townEntity.getClientResources();
                }
                // On server-side, get from TownManager
                else {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                        Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                        if (townFromEntity != null) {
                            return townFromEntity.getAllResources();
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
    public Map<Item, Integer> getAllCommunalStorageItems() {
        if (town != null) {
            return town.getAllCommunalStorageItems();
        }
        
        // Try to get communal storage from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // On client-side, we need to get cached communal storage from the entity directly
                if (level.isClientSide()) {
                    return townEntity.getClientCommunalStorage();
                }
                // On server-side, get from TownManager
                else {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                        Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                        if (townFromEntity != null) {
                            return townFromEntity.getAllCommunalStorageItems();
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
        } else if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
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
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() no data source available");
        }
    }

    /**
     * Refreshes the ContainerData slots with current town data.
     * This method should be called when town data changes to ensure
     * the UI displays up-to-date population and tourist values.
     */
    public void refreshDataSlots() {
        // Force re-fetch the town data
        if (level instanceof ServerLevel serverLevel && townId != null) {
            TownManager townManager = TownManager.get(serverLevel);
            this.town = townManager.getTown(townId);
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
        } else if (level != null && level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
            data.set(DATA_SEARCH_RADIUS, townEntity.getSearchRadius());
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
            if (blockEntity instanceof TownInterfaceEntity townEntity) {
                return townEntity.getTownDataProvider();
            }
        }
        
        return null;
    }
    
    /**
     * Get the TownInterfaceEntity associated with this menu
     * 
     * @return The TownInterfaceEntity, or null if not available
     */
    public TownInterfaceEntity getTownInterfaceEntity() {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TownInterfaceEntity townEntity) {
                return townEntity;
            }
        }
        return null;
    }
} 