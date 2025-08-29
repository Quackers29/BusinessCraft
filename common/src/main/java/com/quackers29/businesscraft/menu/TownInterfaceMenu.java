package com.quackers29.businesscraft.menu;

/**
 * COMMON MODULE VERSION: This is the platform-independent base implementation
 * of TownInterfaceMenu that can be used by both Forge and Fabric platforms.
 */

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.MenuHelper;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.util.ItemConverter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Menu container for the Town Interface block.
 * This class manages the data displayed in the Town Interface UI.
 *
 * PLATFORM-INDEPENDENT VERSION: This menu uses platform abstractions and can work
 * on both Forge and Fabric without direct platform-specific dependencies.
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
     * Get the MenuType for this menu using platform abstraction
     */
    private static MenuType<?> getMenuType() {
        try {
            // Use PlatformServices to get access to the registered MenuType
            // This should return the MenuType registered with name "town_interface"
            var menuTypeObj = PlatformServices.getMenuHelper().getMenuType("town_interface");
            if (menuTypeObj instanceof MenuType<?> menuType) {
                return menuType;
            } else if (menuTypeObj != null) {
                LOGGER.warn("Retrieved object is not a MenuType: {}", menuTypeObj.getClass());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to get MenuType from PlatformServices", e);
        }

        // Fallback to null if menu type not available
        LOGGER.warn("MenuType not available, screen registration may fail");
        return null;
    }

    /**
     * Helper method to get town from position using TownManager (platform-independent)
     */
    private Town getTownFromPosition() {
        if (level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            for (Town t : townManager.getAllTowns()) {
                if (t.getX() == pos.getX() && t.getY() == pos.getY() && t.getZ() == pos.getZ()) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Constructor for server-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, BlockPos pos) {
        super(getMenuType(), windowId);  // Use platform abstraction for MenuType
        this.pos = pos;
        this.level = inv.player.level();
        
        // Initialize container data with 36 slots (4 original + 32 for town name sync)
        this.data = new SimpleContainerData(36);
        // Set default values to avoid returning 0 when data isn't synced yet
        this.data.set(DATA_SEARCH_RADIUS, 10); // Default search radius
        this.data.set(DATA_POPULATION, 5); // Default population
        this.data.set(DATA_TOURIST_COUNT, 0); // Default tourist count
        this.data.set(DATA_MAX_TOURISTS, 5); // Default max tourists
        addDataSlots(this.data);

        if (level.isClientSide()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Constructor called for position {} on CLIENT side - town data will be synced from server", pos);
            // On client side, we don't have access to TownManager
            // The town name will be provided through other means or we'll use a default
            for(int i=0; i<4; i++) data.set(i, -1);
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Constructor called for position {} on server side", pos);
            // Get town from TownManager
            if (level instanceof ServerLevel serverLevel) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Level is ServerLevel, proceeding with town lookup");
                TownManager townManager = TownManager.get(serverLevel);
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "TownManager retrieved, looking for town at position {} in {} towns", pos, townManager.getAllTowns().size());

                // Find the town at this position by iterating through all towns
                for (Town t : townManager.getAllTowns()) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Checking town '{}' at position [{}, {}, {}]", t.getName(), t.getX(), t.getY(), t.getZ());
                                    if (t.getX() == pos.getX() && t.getY() == pos.getY() && t.getZ() == pos.getZ()) {
                    this.town = t;
                    this.townId = t.getId();
                    this.townName = t.getName(); // Cache town name for client-side access
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town with ID {} and name '{}' at position {}", this.townId, this.town.getName(), pos);

                    // Sync town name to client via data slots
                    syncTownNameToClient(t.getName());
                    break;
                }
                }

                // If no town found, try to get from block entity (using TownManager instead of direct entity access)
                if (this.town == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "No town found exactly at position {}, checking for town at this position via TownManager", pos);
                    // Use TownManager to find town by position instead of direct entity access
                    // This abstracts the entity dependency and works across platforms
                    for (Town t : townManager.getAllTowns()) {
                                            if (t.getX() == pos.getX() && t.getY() == pos.getY() && t.getZ() == pos.getZ()) {
                        this.town = t;
                        this.townId = t.getId();
                        this.townName = t.getName(); // Cache town name for client-side access
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town with ID {} at position via TownManager", this.townId);
                        break;
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

    /**
     * Sync town name to client via data slots
     */
    private void syncTownNameToClient(String townName) {
        // Store town name in data slots for client synchronization
        // Pack 4 bytes into each integer slot for efficient storage
        byte[] nameBytes = townName.getBytes(java.nio.charset.StandardCharsets.UTF_8);

        // Store length in data slot 3
        data.set(3, nameBytes.length);

        // Pack bytes into integer slots (4 bytes per slot)
        int slotIndex = 4; // Start after the 4 original slots
        for (int byteIndex = 0; byteIndex < nameBytes.length && slotIndex < 36; byteIndex += 4) {
            int packedInt = 0;
            // Pack up to 4 bytes into a single integer
            for (int i = 0; i < 4 && byteIndex + i < nameBytes.length; i++) {
                packedInt |= (nameBytes[byteIndex + i] & 0xFF) << (i * 8);
            }
            data.set(slotIndex++, packedInt);
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Synced town name '{}' to client ({} bytes, {} slots)", townName, nameBytes.length, slotIndex - 4);
    }

    public String getTownName() {
        // First priority: Try to read from synced data slots (client-side)
        if (level != null && level.isClientSide()) {
            int nameLength = data.get(3);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Client-side getTownName: nameLength={}, data slots 0-5: [{}, {}, {}, {}, {}, {}]",
                nameLength, data.get(0), data.get(1), data.get(2), data.get(3), data.get(4), data.get(5));

            if (nameLength > 0 && nameLength <= 128) { // Support up to 128 characters
                byte[] nameBytes = new byte[nameLength];
                int byteIndex = 0;
                int slotIndex = 4; // Start after the 4 original slots

                // Unpack bytes from integer slots (4 bytes per slot)
                while (byteIndex < nameLength && slotIndex < 36) {
                    int packedInt = data.get(slotIndex++);
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Unpacking from slot {}: packedInt={}", slotIndex-1, packedInt);
                    // Unpack up to 4 bytes from this integer
                    for (int i = 0; i < 4 && byteIndex < nameLength; i++) {
                        nameBytes[byteIndex++] = (byte) ((packedInt >> (i * 8)) & 0xFF);
                    }
                }

                String syncedName = new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8);
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Unpacked name: '{}' (length: {})", syncedName, syncedName.length());
                if (!syncedName.isEmpty()) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Returning synced town name from data slots: '{}'", syncedName);
                    return syncedName;
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Invalid name length: {} (must be 1-128)", nameLength);
            }
        }
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getTownName() called - townName='{}', town={}, townId={}, level={}", townName, town, townId, level);

        // If we're on client side and don't have a valid town name, try to force a data sync
        if (level != null && level.isClientSide() && (townName == null || townName.equals("New Town"))) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Client-side: No valid town name, attempting to force data sync");
            // Force update data slots to trigger town name sync
            updateDataSlots();
            // Try reading again after sync attempt
            if (level.isClientSide()) {
                int nameLength = data.get(3);
                if (nameLength > 0 && nameLength <= 128) {
                    byte[] nameBytes = new byte[nameLength];
                    int byteIndex = 0;
                    int slotIndex = 4;

                    while (byteIndex < nameLength && slotIndex < 36) {
                        int packedInt = data.get(slotIndex++);
                        for (int i = 0; i < 4 && byteIndex < nameLength; i++) {
                            nameBytes[byteIndex++] = (byte) ((packedInt >> (i * 8)) & 0xFF);
                        }
                    }

                    String syncedName = new String(nameBytes, java.nio.charset.StandardCharsets.UTF_8);
                    if (!syncedName.isEmpty()) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "After forced sync, returning town name: '{}'", syncedName);
                        return syncedName;
                    }
                }
            }
        }

        // First priority: Use cached town name from server initialization (works on both client and server)
        if (townName != null && !townName.equals("New Town")) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Returning cached town name: '{}'", townName);
            return townName;
        }

        // Second priority: Get from town object if available (server-side only)
        if (town != null) {
            this.townName = town.getName(); // Cache it for future use
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Returning town name from town object: '{}'", townName);
            return townName;
        }

        // Third priority: Try TownManager lookup (server-side only)
        if (level != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Searching {} towns for position {}", townManager.getAllTowns().size(), pos);
            for (Town t : townManager.getAllTowns()) {
                if (t.getX() == pos.getX() && t.getY() == pos.getY() && t.getZ() == pos.getZ()) {
                    this.townName = t.getName(); // Cache it for future use
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town '{}' via TownManager lookup", townName);
                    return townName;
                }
            }
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "No town found at position {} in TownManager", pos);
        } else if (level != null && level.isClientSide()) {
            // Client-side: We can't access TownManager, but we can provide a better message
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Client-side town lookup - using fallback message");
            return "Town Interface"; // Better than "Unknown Town" for client-side
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Cannot lookup town - level is null");
        }

        // Final fallback: Use cached default
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Using default town name: '{}'", townName);
        return townName;
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
        
        // Try to get population from town using platform-independent TownManager
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            return townFromPosition.getPopulation();
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
        
        // Try to get tourist count from town using platform-independent TownManager
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            return townFromPosition.getTouristCount();
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
        
        // Try to get max tourists from town using platform-independent TownManager
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            return townFromPosition.getMaxTourists();
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
        // Try to get search radius from town using platform-independent approach
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            int townRadius = townFromPosition.getSearchRadius();
            // Update cache for consistency
            this.clientSearchRadius = townRadius;
            data.set(DATA_SEARCH_RADIUS, townRadius);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getSearchRadius() returning town value: {}", townRadius);
            return townRadius;
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
        // TODO: Implement platform retrieval when TownInterfaceEntity is migrated to common
        // For now, return empty list as platforms are managed by TownInterfaceEntity
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getPlatforms() not yet implemented - requires TownInterfaceEntity migration");
        return new ArrayList<>();
    }
    
    /**
     * Adds a new platform to the town block entity
     * @return true if the platform was added successfully
     */
    public boolean addPlatform() {
        // TODO: Implement platform addition when TownInterfaceEntity is migrated to common
        // For now, return false as platform management requires TownInterfaceEntity
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "addPlatform() not yet implemented - requires TownInterfaceEntity migration");
        return false;
    }
    
    /**
     * Removes a platform by ID
     * @param platformId The UUID of the platform to remove
     * @return true if the platform was removed successfully
     */
    public boolean removePlatform(UUID platformId) {
        // TODO: Implement platform removal when TownInterfaceEntity is migrated to common
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "removePlatform() not yet implemented - requires TownInterfaceEntity migration");
        return false;
    }
    
    /**
     * Gets a specific platform by ID
     * @param platformId The UUID of the platform to get
     * @return The platform or null if not found
     */
    public Platform getPlatform(UUID platformId) {
        // TODO: Implement platform retrieval when TownInterfaceEntity is migrated to common
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getPlatform() not yet implemented - requires TownInterfaceEntity migration");
        return null;
    }
    
    /**
     * Checks if more platforms can be added
     * @return true if more platforms can be added
     */
    public boolean canAddMorePlatforms() {
        // TODO: Implement platform capacity check when TownInterfaceEntity is migrated to common
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "canAddMorePlatforms() not yet implemented - requires TownInterfaceEntity migration");
        return false;
    }
    
    /**
     * Gets all resources in the town.
     * @return A map of items to their quantities.
     */
    public Map<Item, Integer> getAllResources() {
        if (town != null) {
            return ItemConverter.toItemMap(town.getAllResources());
        }

        // Try to get resources from town using platform-independent TownManager
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            return ItemConverter.toItemMap(townFromPosition.getAllResources());
        }

        // TODO: Client-side resource retrieval from entity requires TownInterfaceEntity migration
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getAllResources() client-side retrieval not implemented - requires TownInterfaceEntity migration");

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
            return ItemConverter.toItemMap(town.getAllCommunalStorageItems());
        }

        // Try to get communal storage from town using platform-independent TownManager
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            return ItemConverter.toItemMap(townFromPosition.getAllCommunalStorageItems());
        }

        // TODO: Client-side communal storage retrieval from entity requires TownInterfaceEntity migration
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getAllCommunalStorageItems() client-side retrieval not implemented - requires TownInterfaceEntity migration");

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

            // Sync town name to client when data is updated
            syncTownNameToClient(town.getName());
        } else {
            // Try to get town from position if town object is not available
            Town townFromPosition = getTownFromPosition();
            if (townFromPosition != null) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() using town from position");
                data.set(DATA_POPULATION, townFromPosition.getPopulation());
                data.set(DATA_TOURIST_COUNT, townFromPosition.getTouristCount());
                data.set(DATA_MAX_TOURISTS, townFromPosition.getMaxTourists());
                data.set(DATA_SEARCH_RADIUS, townFromPosition.getSearchRadius());

                // Sync town name to client when data is updated
                syncTownNameToClient(townFromPosition.getName());
            } else {
                // Final fallback to default values
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "updateDataSlots() using default fallback values");
                data.set(DATA_POPULATION, townPopulation);
                data.set(DATA_TOURIST_COUNT, currentTourists);
                data.set(DATA_MAX_TOURISTS, maxTourists);
                data.set(DATA_SEARCH_RADIUS, clientSearchRadius);
            }
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
        } else {
            // Try to get search radius from town using platform-independent TownManager
            Town townFromPosition = getTownFromPosition();
            if (townFromPosition != null) {
                data.set(DATA_SEARCH_RADIUS, townFromPosition.getSearchRadius());
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

        // Try to get town from position using platform-independent TownManager
        Town townFromPosition = getTownFromPosition();
        if (townFromPosition != null) {
            return townFromPosition;
        }

        // TODO: Entity-based provider access requires TownInterfaceEntity migration
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "getTownDataProvider() entity-based access not implemented - requires TownInterfaceEntity migration");

        return null;
    }
    
    /**
     * Check if the town interface entity is available at this menu's position
     * This is a platform-independent way to check entity availability without direct access
     *
     * @return true if the entity exists and is accessible, false otherwise
     */
    public boolean isTownInterfaceEntityAvailable() {
        // Check if we can get town data from position - indicates entity functionality is available
        Town townFromPosition = getTownFromPosition();
        return townFromPosition != null;
    }
    
    /**
     * Enhanced MultiLoader Template factory method approach to bypass field access
     */
    @Override
    public MenuType<?> getType() {
        // Use platform abstraction instead of direct field access
        // This bypasses the Enhanced MultiLoader Template classloader boundary issue
        MenuType<?> menuType = getMenuType();
        if (menuType != null) {
            return menuType;
        }
        // Fallback to null if menu type not available (shouldn't happen in production)
        return null;
    }
} 