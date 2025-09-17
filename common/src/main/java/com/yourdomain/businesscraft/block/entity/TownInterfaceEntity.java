package com.yourdomain.businesscraft.block.entity;

// import com.yourdomain.businesscraft.blocks.ModBlocks;
// import com.yourdomain.businesscraft.capability.ItemHandlerCapability;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.service.TouristVehicleManager;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
// import com.yourdomain.businesscraft.api.IEconomyDataProvider;
import com.yourdomain.businesscraft.api.ITownDataProvider;
// import com.yourdomain.businesscraft.data.VisitHistoryRecord;
import com.yourdomain.businesscraft.scoreboard.TownScoreboardManager;
import net.minecraft.core.particles.ParticleTypes;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.entity.npc.VillagerProfession;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.scoreboard.TownScoreboardManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.Iterator;
import org.slf4j.LoggerFactory;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.yourdomain.businesscraft.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import com.yourdomain.businesscraft.town.utils.TouristUtils;
import com.yourdomain.businesscraft.town.utils.TouristUtils.TouristInfo;
import com.yourdomain.businesscraft.town.utils.TouristAllocationTracker;
import com.yourdomain.businesscraft.entity.TouristEntity;
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.town.utils.TownNotificationUtils;
import com.yourdomain.businesscraft.town.data.VisitBuffer;
import com.yourdomain.businesscraft.town.data.TouristSpawningHelper;
import com.yourdomain.businesscraft.town.data.PlatformManager;
import com.yourdomain.businesscraft.town.data.VisitorProcessingHelper;
import com.yourdomain.businesscraft.town.data.ClientSyncHelper;
import com.yourdomain.businesscraft.town.data.NBTDataHelper;
import com.yourdomain.businesscraft.town.data.ContainerDataHelper;
import com.yourdomain.businesscraft.town.data.TownBufferManager;
import com.yourdomain.businesscraft.debug.DebugConfig;

public class TownInterfaceEntity extends BlockEntity implements MenuProvider, BlockEntityTicker<TownInterfaceEntity> {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Accept any item as a resource
            return !stack.isEmpty();
        }
    };
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    
    // Buffer management - extracted to separate class for better organization  
    private TownBufferManager bufferManager;
    private LazyOptional<IItemHandler> lazyBufferHandler = LazyOptional.empty();
    
    // Modular ContainerData system - replaces hardcoded indices with named fields
    private final ContainerDataHelper containerData = ContainerDataHelper.builder("TownBlock")
        .addReadOnlyField("bread_count", this::getBreadCountFromTown, "Legacy bread count for compatibility")
        .addReadOnlyField("population", this::getPopulationFromTown, "Current town population")
        .addField("spawn_enabled", this::getTouristSpawningEnabledAsInt, this::setTouristSpawningEnabledFromInt, "Tourist spawning enabled flag")
        .addReadOnlyField("can_spawn", this::getCanSpawnTouristsAsInt, "Whether town can currently spawn tourists")
        .addField("search_radius", this::getSearchRadius, this::setSearchRadius, "Search radius for tourist detection")
        .addReadOnlyField("tourist_count", this::getTouristCountFromTown, "Current number of tourists in town")
        .addReadOnlyField("max_tourists", this::getMaxTouristsFromTown, "Maximum tourists allowed in town")
        .build();
    private static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceEntity.class);
    private Map<String, Integer> visitingPopulation = new HashMap<>();
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private boolean isInPathCreationMode = false;
    private final Random random = new Random();
    private boolean touristSpawningEnabled = true;
    private UUID townId;
    private String name;
    private Town town;
    private static final ConfigLoader CONFIG = ConfigLoader.INSTANCE;
    private Map<UUID, Vec3> lastPositions = new HashMap<>();
    private static final int DEFAULT_SEARCH_RADIUS = CONFIG.vehicleSearchRadius;
    private int searchRadius = -1; // Will be set from NBT or default
    private final AABB searchBounds = new AABB(worldPosition).inflate(15);
    private List<LivingEntity> tourists = new ArrayList<>();
    private ITownDataProvider townDataProvider;
    
    /**
     * Rate limiting parameters for markDirty calls
     * Prevents excessive updates which can flood logs and impact performance
     */
    private long lastMarkDirtyTime = 0;
    private static final long MARK_DIRTY_COOLDOWN_MS = 2000; // 2 seconds between calls
    
    // Add a new TouristVehicleManager instance
    private final TouristVehicleManager touristVehicleManager = new TouristVehicleManager();

    // History buffer storage
    private final VisitBuffer visitBuffer = new VisitBuffer();

    // Client-server synchronization helper (handles all client caching and sync logic)
    private final ClientSyncHelper clientSyncHelper = new ClientSyncHelper();

    // Platform management (handles platform storage and operations)
    private final PlatformManager platformManager = new PlatformManager();

    // Added for platform visualization
    private Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    private static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks

    // Track when players exit town UI for extended indicators
    private Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();
    private static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks

    // Platform visualization is now handled by the modular client-side rendering system
    // See: client.render.world.PlatformVisualizationRenderer

    // Tourist spawning helper (handles complex spawning logic)
    private final TouristSpawningHelper touristSpawningHelper = new TouristSpawningHelper();

    // Visitor processing helper (handles complex visitor detection and processing)
    private final VisitorProcessingHelper visitorProcessingHelper = new VisitorProcessingHelper();

    // NBT data management helper (handles complex save/load operations)
    private final NBTDataHelper nbtDataHelper = new NBTDataHelper();

    // Special UUID for "any town" destination
    private static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    private static final String ANY_TOWN_NAME = "Any Town";
    
    // Helper methods for ContainerData integration
    private int getBreadCountFromTown() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getBreadCount() : 0;
        }
        return 0;
    }
    
    private int getPopulationFromTown() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getPopulation() : 0;
        }
        return 0;
    }
    
    private int getTouristSpawningEnabledAsInt() {
        return touristSpawningEnabled ? 1 : 0;
    }
    
    private void setTouristSpawningEnabledFromInt(int value) {
        setTouristSpawningEnabled(value != 0);
    }
    
    private int getCanSpawnTouristsAsInt() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null && town.canSpawnTourists() ? 1 : 0;
        }
        return 0;
    }
    
    private int getTouristCountFromTown() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getTouristCount() : 0;
        }
        return 0;
    }
    
    private int getMaxTouristsFromTown() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getMaxTourists() : 0;
        }
        return 0;
    }

    public TownInterfaceEntity(BlockPos pos, BlockState state) {
        super(PlatformAccess.getBlockEntities().getTownInterfaceEntityType(), pos, state);
        
        // Set up platform manager callback
        platformManager.setChangeCallback(this::setChanged);
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "TownInterfaceEntity created at position: {}", pos);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.businesscraft.town_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(this.getBlockPos());
        return new TownInterfaceMenu(id, inventory, this.getBlockPos());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            // Return buffer handler for hopper extraction from below
            if (side == Direction.DOWN && bufferManager != null) {
                return lazyBufferHandler.cast();
            }
            // Return regular resource input handler for other sides
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        
        // Initialize buffer manager now that level is available
        if (bufferManager == null) {
            bufferManager = new TownBufferManager(this, level);
            if (townId != null) {
                bufferManager.setTownId(townId);
            }
        }
        
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyBufferHandler = LazyOptional.of(() -> bufferManager.getBufferHandler());
        
        // Update from provider when loaded
        if (!level.isClientSide()) {
            updateFromTownProvider();
            bufferManager.onLoad(); // Delegate buffer initialization to manager
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyBufferHandler.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        // Ensure we have the latest search radius from the town before saving
        if (!level.isClientSide()) {
            updateFromTownProvider();
        }
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Saving TownInterfaceEntity with searchRadius: {}", searchRadius);
        nbtDataHelper.saveToNBT(tag, itemHandler, townId, name, pathStart, pathEnd, platformManager, searchRadius);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        NBTDataHelper.LoadResult result = nbtDataHelper.loadFromNBT(tag, itemHandler, level, platformManager);
        
        // Apply loaded data to instance variables
        this.townId = result.townId;
        this.name = result.name;
        this.town = result.town;
        this.pathStart = result.pathStart;
        this.pathEnd = result.pathEnd;
        this.touristSpawningEnabled = result.touristSpawningEnabled;
        
        // Update buffer manager with townId if it exists
        if (bufferManager != null && townId != null) {
            bufferManager.setTownId(townId);
        }
        
        // Apply search radius if loaded, otherwise use default
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Before NBT load: searchRadius={}", this.searchRadius);
        if (result.hasSearchRadius()) {
            this.searchRadius = result.searchRadius;
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Loaded searchRadius from NBT: {} -> {}", result.searchRadius, this.searchRadius);
        } else {
            this.searchRadius = DEFAULT_SEARCH_RADIUS;
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Using default searchRadius: {} -> {}", DEFAULT_SEARCH_RADIUS, this.searchRadius);
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Loaded NBT data: {}", result.getSummary());
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownInterfaceEntity blockEntity) {
        // Process resources every tick (not just once per second)
        processResourcesInSlot();
        
        // Sync town data from the provider
        if (level.getGameTime() % 60 == 0) { // Every 3 seconds
            updateFromTownProvider();
            // Delegate buffer synchronization to manager
            if (bufferManager != null) {
                bufferManager.tick();
            }
        }
        
        if (!level.isClientSide && townId != null) {
            if (level instanceof ServerLevel sLevel1) {
                Town town = TownManager.get(sLevel1).getTown(townId);
                if (town != null) {
                    // Platform-based villager spawning
                    if (touristSpawningEnabled && town.canSpawnTourists() && 
                        platformManager.getPlatformCount() > 0 && 
                        level.getGameTime() % 200 == 0) {
                        
                        // Try to spawn from each enabled platform
                        for (Platform platform : platformManager.getEnabledPlatforms()) {
                            touristSpawningHelper.spawnTouristOnPlatform(level, town, platform, townId);
                        }
                    }

                    // Check for visitors using helper
                    if (level.getGameTime() % 40 == 0) {
                        visitorProcessingHelper.processVisitors(
                            level, 
                            pos, 
                            townId, 
                            platformManager, 
                            visitBuffer, 
                            searchRadius, 
                            name, 
                            this::setChanged
                        );
                    }

                    // Add scoreboard update
                    if (level instanceof ServerLevel sLevel2) {
                        TownScoreboardManager.updateScoreboard(sLevel2);
                    }
                }
            }
        }
        
        // Handle tourist vehicles for all platforms
        if (level.getGameTime() % 20 == 0) { // Every 1 second
            if (touristSpawningEnabled && townId != null) {
                // Get town object safely
                Town currentTown = null;
                if (level instanceof ServerLevel sLevel) {
                    currentTown = TownManager.get(sLevel).getTown(townId);
                }
                
                if (currentTown != null && currentTown.canSpawnTourists()) {
                    // Process each enabled platform
                    for (Platform platform : platformManager.getEnabledPlatforms()) {
                            touristVehicleManager.mountTouristsToVehicles(
                                level, 
                                platform.getStartPos(), 
                                platform.getEndPos(), 
                                searchRadius, 
                                townId
                            );
                    }
                }
            }
            
            // Platform visualization cleanup is now handled automatically by the modular system
        }
        
        // Platform visualization is now handled entirely client-side through the modular rendering system
        // No server-side platform indicator spawning needed
    }



    public String getTownName() {
        if (townId != null) {
            if (level.isClientSide && name != null) {
                return name; // Use client-cached name
            }
            if (level instanceof ServerLevel sLevel1) {
                Town town = TownManager.get(sLevel1).getTown(townId);
                if (town != null) {
                    // Always update our local cached name with the latest town name
                    if (!town.getName().equals(name)) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Updating cached name from {} to {}", name, town.getName());
                        name = town.getName();
                    }
                    return town.getName();
                }
                return "Loading...2";
            }
        }
        return "Initializing...";
    }

    private String getRandomTownName() {
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            // Update client cache from town data before syncing to ensure latest data is sent
            if (townId != null && level instanceof ServerLevel serverLevel) {
                Town town = TownManager.get(serverLevel).getTown(townId);
                if (town != null) {
                    clientSyncHelper.updateClientResourcesFromTown(town);
                }
            }
            
            // Refresh ContainerData for any open TownInterfaceMenu instances
            refreshOpenMenus();
            
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }
    
    /**
     * Refreshes ContainerData for any open TownInterfaceMenu instances
     * This ensures population and tourist values are updated in real-time
     */
    private void refreshOpenMenus() {
        if (level instanceof ServerLevel serverLevel) {
            // Find all players with open menus for this block
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.containerMenu instanceof com.yourdomain.businesscraft.menu.TownInterfaceMenu menu) {
                    // Check if this menu is for our block position
                    if (getBlockPos().equals(menu.getBlockPos())) {
                        menu.refreshDataSlots();
                    }
                }
            }
        }
    }

    /**
     * Creates a standardized update tag with all necessary data for client rendering
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        
        // Add town info
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        
        // Add name for client display - use fresh name from Town object instead of cached field
        String freshTownName = getTownName();
        tag.putString("name", freshTownName != null ? freshTownName : "");
        
        // Add search radius for client sync
        tag.putInt("searchRadius", getSearchRadius());
        
        // Add resource data for client rendering
        syncResourcesForClient(tag);
        
        // Add platforms using platform manager
        platformManager.saveToNBT(tag);
        
        return tag;
    }
    
    /**
     * Adds resource data to the provided tag for client-side rendering
     * This centralizes our resource serialization logic in one place
     */
    private void syncResourcesForClient(CompoundTag tag) {
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            clientSyncHelper.syncResourcesForClient(tag, provider);
        }
        
        // Add visit history data
        clientSyncHelper.syncVisitHistoryForClient(tag, provider, level);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        
        // Handle name updates
        if (tag.contains("name")) {
            String newName = tag.getString("name");
            if (!newName.equals(name)) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Updating town name from {} to {}", name, newName);
                name = newName;
            }
        }
        
        // Handle search radius updates
        if (tag.contains("searchRadius")) {
            int newSearchRadius = tag.getInt("searchRadius");
            if (newSearchRadius != this.searchRadius) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Client updating search radius from {} to {}", this.searchRadius, newSearchRadius);
                this.searchRadius = newSearchRadius;
            }
        }
        
        // Handle platform data updates using platform manager
        platformManager.updateClientPlatforms(tag);
        
        // Load client resources data
        loadResourcesFromTag(tag);
        
        // Load visit history data
        clientSyncHelper.loadVisitHistoryFromTag(tag);
    }
    
    /**
     * Loads resources from the provided tag into the client-side cache
     * This centralizes our resource deserialization logic in one place
     */
    private void loadResourcesFromTag(CompoundTag tag) {
        clientSyncHelper.loadResourcesFromTag(tag);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    public ContainerData getContainerData() {
        return containerData;
    }

    public Map<String, Integer> getVisitingPopulation() {
        return Collections.unmodifiableMap(visitingPopulation);
    }

    public int getVisitingPopulationFrom(String townName) {
        return visitingPopulation.getOrDefault(townName, 0);
    }

    public BlockPos getPathStart() {
        return pathStart;
    }

    public BlockPos getPathEnd() {
        return pathEnd;
    }

    public void setPathStart(BlockPos pos) {
        this.pathStart = pos;
        
        // Update the town through the provider
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setPathStart(pos);
            markDirtyWithRateLimit(provider);
        }
        
        setChanged();
    }

    public void setPathEnd(BlockPos pos) {
        this.pathEnd = pos;
        
        // Update the town through the provider
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setPathEnd(pos);
            markDirtyWithRateLimit(provider);
        }
        
        setChanged();
    }

    public boolean isInPathCreationMode() {
        return isInPathCreationMode;
    }

    public void setPathCreationMode(boolean mode) {
        this.isInPathCreationMode = mode;
    }

    /**
     * Validates if a position is within the town's boundary radius
     * @param pos The position to validate
     * @return true if valid, false if outside boundary
     */
    public boolean isValidPathDistance(BlockPos pos) {
        if (townId == null || !(level instanceof ServerLevel serverLevel)) {
            return false; // Cannot validate without town or on client
        }
        
        Town town = TownManager.get(serverLevel).getTown(townId);
        if (town == null) {
            return false; // No town found
        }
        
        int boundaryRadius = town.getBoundaryRadius();
        double distance = Math.sqrt(pos.distSqr(this.getBlockPos()));
        
        return distance <= boundaryRadius;
    }

    public UUID getTownId() {
        return townId;
    }

    public int getBreadCount() {
        return containerData.getValue("bread_count");
    }

    public int getPopulation() {
        return containerData.getValue("population");
    }

    public void syncTownData() {
        if (level != null && !level.isClientSide()) {
            // Sync all data from town provider
            updateFromTownProvider();
            
            // Explicitly update client resources when syncing town data
            if (townId != null && level instanceof ServerLevel serverLevel) {
                Town town = TownManager.get(serverLevel).getTown(townId);
                if (town != null) {
                    clientSyncHelper.updateClientResourcesFromTown(town);
                }
            }
            
            // Update container data - mark all fields as dirty to refresh values
            containerData.markAllDirty();
            
            // Force a block update to sync the latest data
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            setChanged();
        }
    }

    public void setTownId(UUID id) {
        this.townId = id;
        if (level instanceof ServerLevel sLevel1) {
            Town town = TownManager.get(sLevel1).getTown(id);
            this.name = town != null ? town.getName() : "Unnamed";

            // Initialize buffer manager with the new town ID
            bufferManager.setTownId(id);

            syncTownData();
        }
    }

    // Replace the old mountTouristsToVehicles method with a simplified version that delegates to the manager
    private void mountTouristsToVehicles() {
        if (level == null || level.isClientSide || town == null) return;
        if (pathStart == null || pathEnd == null) return;

        int mounted = touristVehicleManager.mountTouristsToVehicles(level, pathStart, pathEnd, searchRadius, townId);
        if (mounted > 0) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Mounted {} tourists to vehicles for town {}", mounted, name);
        }
    }

    private String formatVec3(Vec3 vec) {
        return String.format("[%.6f, %.6f, %.6f]", vec.x, vec.y, vec.z);
    }

    public int getSearchRadius() {
        // Use default if not yet initialized from NBT
        int result = searchRadius > 0 ? searchRadius : DEFAULT_SEARCH_RADIUS;
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "getSearchRadius() field={}, result={}", searchRadius, result);
        return result;
    }

    public void setSearchRadius(int radius) {
        int oldValue = this.searchRadius;
        this.searchRadius = Math.max(1, Math.min(radius, 100)); // Limit between 1-100 blocks
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "setSearchRadius() {} -> {}", oldValue, this.searchRadius);
        
        // Also update in the Town object
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                town.setSearchRadius(this.searchRadius);
                TownManager.get(sLevel).markDirty();
            }
        }
        
        // Make sure to update the container data
        if (level != null && !level.isClientSide()) {
            containerData.markDirty("search_radius");
            // Force client sync when search radius changes
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
        
        setChanged();
    }

    public Town getTown() {
        return town;
    }

    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
        
        // Update the town through the provider
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setTouristSpawningEnabled(enabled);
            markDirtyWithRateLimit(provider);
        }
        
        // Update container data
        if (level != null && !level.isClientSide()) {
            containerData.markDirty("spawn_enabled");
        }
        
        setChanged();
    }
    
    /**
     * Gets all towns available as destinations
     * 
     * @param serverLevel The server level
     * @return Map of town IDs to town names
     */
    public Map<UUID, String> getAllTownsForDestination(ServerLevel serverLevel) {
        Map<UUID, String> result = new HashMap<>();
        
        // Get all towns from the town manager
        TownManager townManager = TownManager.get(serverLevel);
        Map<UUID, Town> allTowns = townManager.getAllTowns();
        
        // Filter out the current town
        allTowns.forEach((id, town) -> {
            if (!id.equals(townId)) {
                result.put(id, town.getName());
            }
        });
        
        return result;
    }

    /**
     * Gets the town data provider, initializing it if needed
     */
    public ITownDataProvider getTownDataProvider() {
        if (townDataProvider == null && townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                townDataProvider = town;
            }
        }
        return townDataProvider;
    }
    
    /**
     * Updates cached values from the town data provider
     */
    private void updateFromTownProvider() {
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            // Sync data from the provider (the single source of truth)
            this.touristSpawningEnabled = provider.isTouristSpawningEnabled();
            this.pathStart = provider.getPathStart();
            this.pathEnd = provider.getPathEnd();
            this.searchRadius = provider.getSearchRadius();
            
            // If we made any local changes, we need to sync them back
            if (level != null && !level.isClientSide() && this.townId != null) {
                // Mark the provider as dirty to ensure changes are saved, but with rate limiting
                markDirtyWithRateLimit(provider);
            }
        }
    }
    
    /**
     * Marks the provider as dirty with rate limiting to reduce excessive updates
     * @param provider The provider to mark as dirty
     */
    private void markDirtyWithRateLimit(ITownDataProvider provider) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMarkDirtyTime > MARK_DIRTY_COOLDOWN_MS) {
            provider.markDirty();
            lastMarkDirtyTime = currentTime;
        }
    }

    public void processResourcesInSlot() {
        if (level == null || level.isClientSide()) return;
        
            ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty() && townId != null) {
            if (level instanceof ServerLevel sLevel) {
                Town town = TownManager.get(sLevel).getTown(townId);
                if (town != null) {
                    Item item = stack.getItem();
                    // Process just 1 item per tick
                    stack.shrink(1);
                    town.addResource(item, 1);
                    setChanged();
                    
                    // Send update to clients when resources change
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    // Ensure we clean up resources when the block entity is removed
    @Override
    public void setRemoved() {
        super.setRemoved();
        touristVehicleManager.clearTrackedVehicles();
        visitorProcessingHelper.clearAll();
        clientSyncHelper.clearAll();
        // Clear platform indicators
        platformIndicatorSpawnTimes.clear();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Cleared visitor position tracking, client caches, and platform indicators on block removal");
    }

    /**
     * Gets the client-side cached resources
     * @return Map of resources
     */
    public Map<Item, Integer> getClientResources() {
        return clientSyncHelper.getClientResources();
    }
    
    /**
     * Gets the client-side cached communal storage items
     * @return Map of communal storage items
     */
    public Map<Item, Integer> getClientCommunalStorage() {
        return clientSyncHelper.getClientCommunalStorage();
    }
    
    /**
     * Gets the client-side cached personal storage items for a specific player
     * @param playerId UUID of the player
     * @return Map of personal storage items for that player
     */
    public Map<Item, Integer> getClientPersonalStorage(UUID playerId) {
        return clientSyncHelper.getClientPersonalStorage(playerId);
    }
    
    /**
     * Updates the client-side personal storage cache for a player
     * @param playerId UUID of the player
     * @param items Map of items in the player's personal storage
     */
    public void updateClientPersonalStorage(UUID playerId, Map<Item, Integer> items) {
        clientSyncHelper.updateClientPersonalStorage(playerId, items);
    }

    /**
     * Creates the update packet for sending to clients
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Gets the visit history for client-side display
     */
    public List<VisitHistoryRecord> getVisitHistory() {
        return clientSyncHelper.getVisitHistory(level, getTownDataProvider());
    }

    // Helper method to get town name from client cache or resolve from server
    public String getTownNameFromId(UUID townId) {
        return clientSyncHelper.getTownNameFromId(townId, level);
    }




    
    // Platform management methods - delegated to PlatformManager
    
    /**
     * Gets the list of all platforms
     */
    public List<Platform> getPlatforms() {
        return platformManager.getPlatforms(level != null && level.isClientSide());
    }
    
    /**
     * Adds a new platform
     * @return true if added successfully, false if at max capacity
     */
    public boolean addPlatform() {
        return platformManager.addPlatform();
    }
    
    /**
     * Removes a platform by ID
     */
    public boolean removePlatform(UUID platformId) {
        boolean removed = platformManager.removePlatform(platformId);
        if (removed) {
            // Remove the platform indicator spawning data when a platform is removed
            platformIndicatorSpawnTimes.remove(platformId);
        }
        return removed;
    }
    
    /**
     * Gets a platform by ID
     */
    public Platform getPlatform(UUID platformId) {
        return platformManager.getPlatform(platformId);
    }
    
    /**
     * Sets the path start for a specific platform
     */
    public void setPlatformPathStart(UUID platformId, BlockPos pos) {
        platformManager.setPlatformPathStart(platformId, pos);
    }
    
    /**
     * Sets the path end for a specific platform
     */
    public void setPlatformPathEnd(UUID platformId, BlockPos pos) {
        platformManager.setPlatformPathEnd(platformId, pos);
    }
    
    /**
     * Toggles a platform's enabled state
     */
    public void togglePlatformEnabled(UUID platformId) {
        platformManager.togglePlatformEnabled(platformId);
    }
    
    /**
     * Sets whether we're in platform path creation mode
     */
    public void setPlatformCreationMode(boolean mode, UUID platformId) {
        platformManager.setPlatformCreationMode(mode, platformId);
    }
    
    /**
     * Gets whether we're in platform path creation mode
     */
    public boolean isInPlatformCreationMode() {
        return platformManager.isInPlatformCreationMode();
    }
    
    /**
     * Gets the ID of the platform currently being edited
     */
    public UUID getPlatformBeingEdited() {
        return platformManager.getPlatformBeingEdited();
    }
    
    /**
     * Checks if we can add more platforms
     */
    public boolean canAddMorePlatforms() {
        return platformManager.canAddMorePlatforms();
    }


    
    /**
     * Registers a player as having exited the town UI, enabling extended indicators
     * @param playerId The UUID of the player who exited the UI
     */
    public void registerPlayerExitUI(UUID playerId) {
        if (level != null) {
            // Platform visualization is now handled by the modular client-side system
            // The visualization packet will trigger the new PlatformVisualizationRenderer
            extendedIndicatorPlayers.put(playerId, level.getGameTime());
        }
    }
    
    /**
     * Called when items are added to town buffer storage externally (e.g., from claim system)
     * Forces a buffer sync to ensure ItemStackHandler reflects the new items
     */
    public void onTownBufferChanged() {
        if (bufferManager != null) {
            bufferManager.onTownBufferChanged();
        }
    }
    
    /**
     * Get the buffer handler for direct access (used by PaymentBoardMenu)
     * @return The buffer ItemStackHandler or null if not initialized
     */
    public net.minecraftforge.items.ItemStackHandler getBufferHandler() {
        return bufferManager != null ? bufferManager.getBufferHandler() : null;
    }

    /**
     * Creates a Payment Board menu provider for proper server-client synchronization
     */
    public MenuProvider createPaymentBoardMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Payment Board");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                // Create a FriendlyByteBuf to pass the BlockPos
                io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
                net.minecraft.network.FriendlyByteBuf friendlyBuf = new net.minecraft.network.FriendlyByteBuf(buf);
                friendlyBuf.writeBlockPos(TownInterfaceEntity.this.getBlockPos());
                
                return new com.yourdomain.businesscraft.menu.PaymentBoardMenu(containerId, playerInventory, friendlyBuf);
            }
        };
    }

}