package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.init.ModMenuTypes;
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

    /**
     * Constructor for server-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.TOWN_INTERFACE.get(), windowId);
        this.pos = pos;
        this.level = inv.player.level();
        
        // Initialize container data with 4 slots
        this.data = new SimpleContainerData(4);
        addDataSlots(this.data);
        
        // Get town from TownManager
        if (level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            
            // Find the town at this position by iterating through all towns
            Map<UUID, Town> allTowns = townManager.getAllTowns();
            for (Town t : allTowns.values()) {
                if (t.getPosition().equals(pos)) {
                    this.town = t;
                    this.townId = t.getId();
                    LOGGER.debug("Found town with ID {} at position {}", this.townId, pos);
                    break;
                }
            }
            
            // If no town found, try to get from block entity
            if (this.town == null) {
                LOGGER.debug("No town found exactly at position {}, checking block entity", pos);
                if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null) {
                        this.town = townManager.getTown(entityTownId);
                        this.townId = entityTownId;
                        LOGGER.debug("Found town with ID {} from block entity", this.townId);
                    }
                }
            }
            
            if (this.town == null) {
                LOGGER.debug("No town found at or associated with position {}", pos);
            } else {
                // Initialize data values from town
                updateDataSlots();
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
    
    // Getters for UI data
    
    public String getTownName() {
        if (town != null) {
            return town.getName();
        }
        
        // Try to get name from town entity
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
        if (touristsFromData >= 0) {  // Tourist count can be 0
            return touristsFromData;
        }
        
        // If data isn't available yet or we're on server side
        if (town != null) {
            return town.getTouristCount();
        }
        
        // Try to get tourist count from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
        // First try to get from our ContainerData which is synced between client and server
        int radiusFromData = data.get(DATA_SEARCH_RADIUS);
        if (radiusFromData > 0) {
            // Update our client cache for immediate UI feedback
            this.clientSearchRadius = radiusFromData;
            return radiusFromData;
        }
        
        // If data isn't available yet, try to get from town object if available
        if (town != null) {
            return town.getSearchRadius();
        }
        
        // Try to get search radius from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                // Get the value directly from the entity
                return townEntity.getSearchRadius();
            }
        }
        
        // Last resort - return our client-side cached value
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
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
            return townEntity.getPlatforms();
        }
        return new ArrayList<>();
    }
    
    /**
     * Adds a new platform to the town block entity
     * @return true if the platform was added successfully
     */
    public boolean addPlatform() {
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
            return townEntity.getPlatform(platformId);
        }
        return null;
    }
    
    /**
     * Checks if more platforms can be added
     * @return true if more platforms can be added
     */
    public boolean canAddMorePlatforms() {
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
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
            data.set(DATA_POPULATION, town.getPopulation());
            data.set(DATA_TOURIST_COUNT, town.getTouristCount());
            data.set(DATA_MAX_TOURISTS, town.getMaxTourists());
            data.set(DATA_SEARCH_RADIUS, town.getSearchRadius());
        } else if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
            // Try to get values from TownBlockEntity if town is not available
            data.set(DATA_POPULATION, townEntity.getPopulation());
            data.set(DATA_TOURIST_COUNT, 0); // Default to 0 tourists
            data.set(DATA_MAX_TOURISTS, 5);  // Default max tourists
            data.set(DATA_SEARCH_RADIUS, townEntity.getSearchRadius());
        }
    }
    
    /**
     * Refreshes the ContainerData slots with current town data.
     * This method should be called when town data changes to ensure
     * the UI displays up-to-date population and tourist values.
     */
    public void refreshDataSlots() {
        // Re-fetch the town data if needed
        if (town == null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            
            // Try to get town from block entity
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null) {
                    this.town = townManager.getTown(entityTownId);
                    this.townId = entityTownId;
                }
            }
        }
        
        // Update the data slots with current values
        updateDataSlots();
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
            if (blockEntity instanceof TownBlockEntity townEntity) {
                return townEntity.getTownDataProvider();
            }
        }
        
        return null;
    }
    
    /**
     * Get the TownBlockEntity associated with this menu
     * 
     * @return The TownBlockEntity, or null if not available
     */
    public TownBlockEntity getTownBlockEntity() {
        if (level != null) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof TownBlockEntity townEntity) {
                return townEntity;
            }
        }
        return null;
    }
} 