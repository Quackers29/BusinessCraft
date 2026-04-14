package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
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
import com.quackers29.businesscraft.debug.DebugConfig;
// PHASE 3.2: Removed ResourceSyncPacket import - replaced by ResourceViewModelSyncPacket (Phase 1.1)

/**
 * Menu container for the Town Interface block.
 * This class manages the data displayed in the Town Interface UI.
 */
public class TownInterfaceMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceMenu");
    private final BlockPos pos;
    private final Level level;
    private final Player player;
    private Town town;
    private UUID townId;
    private boolean needsImmediateSync = true;

    // Add ContainerData field for syncing values between server and client
    private static final int DATA_SEARCH_RADIUS = 0;
    private static final int DATA_TOURIST_COUNT = 1;
    private static final int DATA_MAX_TOURISTS = 2;
    private static final int DATA_POPULATION = 3;
    private static final int DATA_WORK_UNITS = 4;
    private static final int DATA_WORK_UNIT_CAP = 5;
    private SimpleContainerData data;

    // Town properties for UI display (fallbacks)
    private String townName = "New Town";
    private int townLevel = 1;
    private int townPopulation = 5; // Default population to 5 like TownBlock
    private int townReputation = 50;

    // Tourism data (fallbacks)
    private int currentTourists = 0;
    private int maxTourists = 5;

    // WU data (fallbacks)
    private int workUnits = 0;
    private int workUnitCap = 0;

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
        super((net.minecraft.world.inventory.MenuType<TownInterfaceMenu>) PlatformAccess.getMenuTypes()
                .getTownInterfaceMenuType(), windowId);
        this.pos = pos;
        this.level = inv.player.level();
        this.player = inv.player;

        // Initialize container data with 6 slots and sensible defaults
        // Initialize container data - NOW UNUSED/EMPTY as we use ViewModel syncing
        this.data = new SimpleContainerData(0);
        addDataSlots(this.data);

        // Client side: initialize with default values if needed, but ViewModel should
        // take over
        if (level.isClientSide()) {
            // No-op
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
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU,
                                "Found town with ID {} at position {}", this.townId, pos);
                        break;
                    }
                }

                // If no town found, try to get from block entity
                if (this.town == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU,
                            "No town found exactly at position {}, checking block entity", pos);
                    if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                        UUID entityTownId = townEntity.getTownId();
                        if (entityTownId != null) {
                            this.town = townManager.getTown(entityTownId);
                            this.townId = entityTownId;
                            DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU,
                                    "Found town with ID {} from block entity", this.townId);
                        }
                    }
                }

                if (this.town == null) {
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU,
                            "No town found at or associated with position {}", pos);
                } else {
                    // Initialize data values from town
                    updateDataSlots();

                    // Standard NBT sync handles resource data now
                    // Force a sync to ensure client has latest data when opening menu
                    if (level != null && !level.isClientSide()) {
                        BlockEntity be = level.getBlockEntity(pos);
                        if (be instanceof TownInterfaceEntity entity) {
                            entity.setChanged(); // Triggers sendBlockUpdated -> getUpdateTag -> ClientSyncHelper
                        }

                        // --- REPLACED BY TownInterfaceViewModelSyncPacket (Phase 2.3) ---
                        // Town stats are now synced via updateTownInterfaceViewModel in
                        // TownInterfaceEntity
                        // which is triggered by ticks or events, minimizing packet spam.
                        // Force a sync of the view model to this player right now
                        if (be instanceof TownInterfaceEntity entity
                                && inv.player instanceof net.minecraft.server.level.ServerPlayer sp) {
                            entity.updateInterfaceVM();
                            entity.syncAllDirtyTownVMsToPlayers();
                        }

                        // --- FIX: Sync Contract/Market Data for GPI Display ---
                        com.quackers29.businesscraft.contract.ContractBoard board = com.quackers29.businesscraft.contract.ContractBoard
                                .get((ServerLevel) level);
                        if (board != null) {
                            PlatformAccess.getNetworkMessages().sendToPlayer(
                                    new com.quackers29.businesscraft.network.packets.ui.ContractSyncPacket(
                                            board.getContracts(),
                                            board.getAllMarketPrices()),
                                    (net.minecraft.server.level.ServerPlayer) inv.player);
                            DebugConfig.debug(LOGGER, DebugConfig.SMART_GPI_DEBUG,
                                    "TownInterfaceMenu: Synced market prices to {}", inv.player.getName().getString());
                        }
                        // --------------------------------------------------------

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

    public BlockEntity getBlockEntity() {
        return level.getBlockEntity(pos);
    }

    @Override
    public void broadcastChanges() {
        // Force immediate data sync on first broadcast to eliminate initial delay
        if (needsImmediateSync && level != null && !level.isClientSide()) {
            updateDataSlots();

            // Send Town Overview Sync Packet (moved from constructor to avoid race
            // condition)
            float happiness = town.getHappiness();
            String biome = town.getBiome();
            boolean biomeUnknown = biome == null || "Unknown".equals(biome);

            // If biome is unknown, calculate it and update the town
            if (biomeUnknown && level instanceof ServerLevel sLevel && town.getPosition() != null) {
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biomeHolder = sLevel
                        .getBiome(town.getPosition());
                biome = biomeHolder.unwrapKey().map(key -> key.location().toString()).orElse("Unknown");

                // Update town if we found a valid biome
                if (!"Unknown".equals(biome)) {
                    town.setBiome(biome);
                }
            }

            String currentResearch = town.getUpgrades().getCurrentResearchNode();
            float researchProgress = town.getUpgrades().getResearchProgress();
            int dailyTickInterval = com.quackers29.businesscraft.config.ConfigLoader.dailyTickInterval;

            // Get active productions
            Map<String, Float> activeProductions = town.getProduction().getActiveRecipes();

            String biomeVariant = town.getBiomeVariant();

            com.quackers29.businesscraft.network.packets.ui.TownOverviewSyncPacket syncPacket = new com.quackers29.businesscraft.network.packets.ui.TownOverviewSyncPacket(
                    happiness, biome, biomeVariant, currentResearch, researchProgress, dailyTickInterval,
                    activeProductions, town.getUpgrades().getUpgradeLevels(),
                    town.getUpgrades().getModifier("pop_cap"),
                    town.getTotalTouristsArrived(), town.getTotalTouristDistance(),
                    town.getBoundaryRadius(),
                    town.getUpgrades().getAiScores());

            if (this.player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PlatformAccess.getNetworkMessages().sendToPlayer(syncPacket, serverPlayer);
            }
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
        // CLIENT SIDE: Use cached ViewModel if available
        if (level != null && level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                var vm = townEntity.getVmCache().get(com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.class);
                if (vm != null) {
                    try {
                        String disp = vm.getPopulationDisplay(); // e.g. "5 / 10"
                        if (disp.contains("/")) {
                            return Integer.parseInt(disp.split("/")[0].trim());
                        }
                        return Integer.parseInt(disp.trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0; // Fallback if no VM yet
        }

        // SERVER SIDE: Direct access
        if (town != null) {
            return (int) town.getPopulation();
        }

        return 0;
    }

    /**
     * Gets the current number of tourists in this town
     */
    /**
     * Gets the current number of tourists in this town
     */
    public int getCurrentTourists() {
        // CLIENT SIDE: Use cached ViewModel if available
        if (level != null && level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                var vm = townEntity.getVmCache().get(com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.class);
                if (vm != null) {
                    try {
                        String disp = vm.getTouristsDisplay(); // e.g. "2 / 5"
                        if (disp.contains("/")) {
                            return Integer.parseInt(disp.split("/")[0].trim());
                        }
                        return Integer.parseInt(disp.trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0;
        }

        // SERVER SIDE
        if (town != null) {
            return (int) town.getTouristCount();
        }

        return 0;
    }

    /**
     * Gets the maximum number of tourists this town can support
     */
    public int getMaxTourists() {
        // First try to get from our ContainerData which is synced between server and
        // client
        int maxTouristsFromData = data.get(DATA_MAX_TOURISTS);
        if (maxTouristsFromData > 0) {
            return maxTouristsFromData;
        }

        // If data isn't available yet or we're on server side
        if (town != null) {
            return (int) town.getMaxTourists();
        }

        // Try to get max tourists from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the max tourists
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return (int) townFromEntity.getMaxTourists();
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
     * 
     * @return the search radius value
     */
    /**
     * Gets the search radius for this town
     * 
     * @return the search radius value
     */
    public int getSearchRadius() {
        // CLIENT SIDE
        if (level != null && level.isClientSide()) {
            // Priority 1: Check block entity for latest ViewModel
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                var vm = townEntity.getVmCache().get(com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.class);
                if (vm != null) {
                    return vm.getSearchRadius();
                }
            }
            // Priority 2: Use client cache as fallback
            return clientSearchRadius;
        }

        // SERVER SIDE
        if (town != null) {
            return town.getSearchRadius();
        }

        // Fallback
        return 10;
    }

    /**
     * Updates the client-side search radius value
     * This provides immediate visual feedback until the server syncs
     * 
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
     * 
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
     * 
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
     * 
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
     * 
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
     * 
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
     * 
     * @return A map of items to their quantities.
     */
    public Map<Item, Long> getAllResources() {
        if (town != null) {
            return town.getAllResources();
        }

        // Try to get resources from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                if (level.isClientSide()) {
                    return townEntity.getClientResources();
                } else {
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

        return Collections.emptyMap();
    }

    /**
     * Get all items in the town's communal storage
     * 
     * @return Map of items and their quantities in communal storage
     */
    public Map<Item, Long> getAllCommunalStorageItems() {
        if (town != null) {
            return town.getAllCommunalStorageItems();
        }

        // Try to get communal storage from town entity
        if (level != null) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                // On client-side, we need to get cached communal storage from the entity
                // directly
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

    public int getWorkUnits() {
        // CLIENT SIDE
        if (level != null && level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                var vm = townEntity.getVmCache().get(com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.class);
                if (vm != null) {
                    try {
                        String disp = vm.getWorkUnitsDisplay(); // e.g. "10 / 20"
                        if (disp.contains("/")) {
                            return Integer.parseInt(disp.split("/")[0].trim());
                        }
                        return Integer.parseInt(disp.trim());
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0;
        }

        // SERVER SIDE
        if (town != null)
            return (int) town.getWorkUnits();

        return workUnits;
    }

    public int getWorkUnitCap() {
        // CLIENT SIDE
        if (level != null && level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townEntity) {
                var vm = townEntity.getVmCache().get(com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.class);
                if (vm != null) {
                    try {
                        String disp = vm.getWorkUnitsDisplay(); // e.g. "10 / 20"
                        if (disp.contains("/")) {
                            return Integer.parseInt(disp.split("/")[1].trim());
                        }
                        return 0;
                    } catch (NumberFormatException e) {
                        return 0;
                    }
                }
            }
            return 0;
        }

        // SERVER SIDE
        if (town != null)
            return (int) town.getWorkUnitCap();

        return workUnitCap;
    }

    private void updateDataSlots() {
        // No-op: Data now synced via TownInterfaceViewModelSyncPacket
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
        // No-op: handled by ViewModel sync
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
