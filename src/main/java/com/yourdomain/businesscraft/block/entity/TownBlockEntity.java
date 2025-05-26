package com.yourdomain.businesscraft.block.entity;

// import com.yourdomain.businesscraft.blocks.ModBlocks;
// import com.yourdomain.businesscraft.capability.ItemHandlerCapability;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.init.ModBlockEntities;
import com.yourdomain.businesscraft.menu.TownBlockMenu;
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
import com.yourdomain.businesscraft.init.ModEntityTypes;
import com.yourdomain.businesscraft.town.utils.TownNotificationUtils;
import com.yourdomain.businesscraft.town.data.VisitBuffer;
import com.yourdomain.businesscraft.town.data.PlatformVisualizationHelper;
import com.yourdomain.businesscraft.town.data.TouristSpawningHelper;
import com.yourdomain.businesscraft.town.data.PlatformManager;
import com.yourdomain.businesscraft.town.data.VisitorProcessingHelper;
import com.yourdomain.businesscraft.town.data.ClientSyncHelper;

public class TownBlockEntity extends BlockEntity implements MenuProvider, BlockEntityTicker<TownBlockEntity> {
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
    private static final int DATA_BREAD = 0;
    private static final int DATA_POPULATION = 1;
    private static final int DATA_SPAWN_ENABLED = 2;
    private static final int DATA_CAN_SPAWN = 3;
    private static final int DATA_SEARCH_RADIUS = 4;
    private static final int DATA_TOURIST_COUNT = 5;
    private static final int DATA_MAX_TOURISTS = 6;
    private final ContainerData data = new SimpleContainerData(7) {
        // Rate limiting for logging
        private long lastLogTime = 0;

        @Override
        public int get(int index) {
            if (level != null && level.isClientSide()) {
                return super.get(index);
            }
            
            if (townId == null) return 0;
            if (level instanceof ServerLevel sLevel) {
                Town town = TownManager.get(sLevel).getTown(townId);
                if (town == null) return 0;
                
                int value = switch (index) {
                    case DATA_BREAD -> town.getBreadCount(); // Legacy support for bread count
                    case DATA_POPULATION -> town.getPopulation();
                    case DATA_SPAWN_ENABLED -> town.isTouristSpawningEnabled() ? 1 : 0;
                    case DATA_CAN_SPAWN -> town.canSpawnTourists() ? 1 : 0;
                    case DATA_SEARCH_RADIUS -> town.getSearchRadius();
                    case DATA_TOURIST_COUNT -> town.getTouristCount();
                    case DATA_MAX_TOURISTS -> town.getMaxTourists();
                    default -> 0;
                };
                
                super.set(index, value);
                return value;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            super.set(index, value);
        }
    };
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlockEntity.class);
    private Map<String, Integer> visitingPopulation = new HashMap<>();
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private boolean isInPathCreationMode = false;
    private static final int MAX_PATH_DISTANCE = 50;
    private final Random random = new Random();
    private boolean touristSpawningEnabled = true;
    private UUID townId;
    private String name;
    private Town town;
    private static final ConfigLoader CONFIG = ConfigLoader.INSTANCE;
    private Map<UUID, Vec3> lastPositions = new HashMap<>();
    private static final int DEFAULT_SEARCH_RADIUS = CONFIG.vehicleSearchRadius;
    private int searchRadius = DEFAULT_SEARCH_RADIUS;
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

    // Platform visualization helper (handles complex particle effects)
    private final PlatformVisualizationHelper platformVisualizationHelper = new PlatformVisualizationHelper();

    // Tourist spawning helper (handles complex spawning logic)
    private final TouristSpawningHelper touristSpawningHelper = new TouristSpawningHelper();

    // Visitor processing helper (handles complex visitor detection and processing)
    private final VisitorProcessingHelper visitorProcessingHelper = new VisitorProcessingHelper();

    // Special UUID for "any town" destination
    private static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    private static final String ANY_TOWN_NAME = "Any Town";

    public TownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWN_BLOCK_ENTITY.get(), pos, state);
        
        // Set up platform manager callback
        platformManager.setChangeCallback(this::setChanged);
        
        LOGGER.debug("TownBlockEntity created at position: {}", pos);
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
        return new TownBlockMenu(id, inventory, buffer);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        
        // Update from provider when loaded
        if (!level.isClientSide()) {
            updateFromTownProvider();
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("inventory", itemHandler.serializeNBT());
        
        // Save local data to tag
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        
        tag.putString("name", name != null ? name : "");
        
        // Save legacy path data for backward compatibility
        if (pathStart != null) {
            CompoundTag startPos = new CompoundTag();
            startPos.putInt("x", pathStart.getX());
            startPos.putInt("y", pathStart.getY());
            startPos.putInt("z", pathStart.getZ());
            tag.put("PathStart", startPos);
        }
        
        if (pathEnd != null) {
            CompoundTag endPos = new CompoundTag();
            endPos.putInt("x", pathEnd.getX());
            endPos.putInt("y", pathEnd.getY());
            endPos.putInt("z", pathEnd.getZ());
            tag.put("PathEnd", endPos);
        }
        
        // Save platforms using platform manager
        platformManager.saveToNBT(tag);
        
        tag.putInt("searchRadius", searchRadius);
        
        // Visit history is now saved in the Town object
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        // First load paths from the tag itself as a fallback
        if (tag.contains("PathStart")) {
            CompoundTag startPos = tag.getCompound("PathStart");
            pathStart = new BlockPos(
                startPos.getInt("x"),
                startPos.getInt("y"),
                startPos.getInt("z")
            );
        }
        
        if (tag.contains("PathEnd")) {
            CompoundTag endPos = tag.getCompound("PathEnd");
            pathEnd = new BlockPos(
                endPos.getInt("x"),
                endPos.getInt("y"),
                endPos.getInt("z")
            );
        }
        
        if (tag.contains("searchRadius")) {
            searchRadius = tag.getInt("searchRadius");
        }
        
        // Then try to get the town data
        if (tag.contains("TownId")) {
            townId = tag.getUUID("TownId");
            if (level instanceof ServerLevel sLevel1) {
                town = TownManager.get(sLevel1).getTown(townId);
                
                // Migrate any legacy visit history from block entity to Town
                if (town != null && tag.contains("visitHistory")) {
                    ITownDataProvider provider = town;
                    ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
                    LOGGER.info("Migrating {} visit history records to Town {}", historyTag.size(), town.getName());
                    
                    for (int i = 0; i < historyTag.size(); i++) {
                        CompoundTag visitTag = historyTag.getCompound(i);
                        
                        long timestamp = visitTag.getLong("timestamp");
                        int count = visitTag.getInt("count");
                        
                        // Get the town ID or generate one from the name
                        UUID originTownId = null;
                        if (visitTag.contains("townId")) {
                            originTownId = visitTag.getUUID("townId");
                        } else if (visitTag.contains("town")) {
                            // Legacy format - generate a UUID from the name
                            String townName = visitTag.getString("town");
                            originTownId = UUID.nameUUIDFromBytes(townName.getBytes());
                            LOGGER.info("Converted legacy town name '{}' to UUID: {}", townName, originTownId);
                        } else {
                            continue; // Skip if no town identifier
                        }
                        
                        BlockPos originPos = BlockPos.ZERO;
                        if (visitTag.contains("pos")) {
                            CompoundTag posTag = visitTag.getCompound("pos");
                            originPos = new BlockPos(
                                posTag.getInt("x"),
                                posTag.getInt("y"),
                                posTag.getInt("z")
                            );
                        }
                        
                        // Add the record to the Town using the provider
                        try {
                            // Use reflection to access the private list directly (to preserve exact timestamps)
                            java.lang.reflect.Field historyField = Town.class.getDeclaredField("visitHistory");
                            historyField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            List<VisitHistoryRecord> visitHistory = (List<VisitHistoryRecord>) historyField.get(town);
                            visitHistory.add(new VisitHistoryRecord(timestamp, originTownId, count, originPos));
                            
                            // Ensure list is sorted by timestamp (newest first)
                            visitHistory.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                            
                            // Trim if needed
                            while (visitHistory.size() > 50) { // MAX_HISTORY_SIZE
                                visitHistory.remove(visitHistory.size() - 1);
                            }
                            
                            // Mark town as dirty
                            provider.markDirty();
                        } catch (Exception e) {
                            // Fallback to the standard method if reflection fails
                            LOGGER.error("Error migrating visit history: {}", e.getMessage());
                            provider.recordVisit(originTownId, count, originPos);
                        }
                    }
                }
                
                if (town != null) {
                    // Update local values from the Town object, but prefer the ones we already loaded
                    touristSpawningEnabled = town.isTouristSpawningEnabled();
                    
                    // Only use town paths if we don't have any
                    if (pathStart == null && town.getPathStart() != null) {
                        pathStart = town.getPathStart();
                    }
                    
                    if (pathEnd == null && town.getPathEnd() != null) {
                        pathEnd = town.getPathEnd();
                    }
                    
                    // Use town search radius if already set
                    if (searchRadius <= 0 && town.getSearchRadius() > 0) {
                        searchRadius = town.getSearchRadius();
                    }
                }
            }
        }
        
        if (tag.contains("name")) {
            name = tag.getString("name");
        }

        // Load platforms using platform manager
        platformManager.loadFromNBT(tag);
        
        // Create legacy platform if needed
        if (pathStart != null && pathEnd != null) {
            platformManager.createLegacyPlatform(pathStart, pathEnd);
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownBlockEntity blockEntity) {
        // Process resources every tick (not just once per second)
        processResourcesInSlot();
        
        // Sync town data from the provider
        if (level.getGameTime() % 60 == 0) { // Every 3 seconds
            updateFromTownProvider();
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
            
            // Clean up platform indicators if needed
            platformVisualizationHelper.cleanupPlatformIndicators(platformManager.getPlatforms(false));
        }
        
        // Spawn platform indicators using helper
        platformVisualizationHelper.spawnPlatformIndicators(level, platformManager.getPlatforms(false), searchRadius);
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
                        LOGGER.debug("[DEBUG] Updating cached name from {} to {}", name, town.getName());
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
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
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
        
        // Add name for client display
        tag.putString("name", name != null ? name : "");
        
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
                LOGGER.debug("Updating town name from {} to {}", name, newName);
                name = newName;
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
        return data;
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

    public boolean isValidPathDistance(BlockPos pos) {
        return pos.distManhattan(this.getBlockPos()) <= MAX_PATH_DISTANCE;
    }

    public UUID getTownId() {
        return townId;
    }

    public int getBreadCount() {
        return data.get(DATA_BREAD);
    }

    public int getPopulation() {
        return data.get(DATA_POPULATION);
    }

    public void syncTownData() {
        if (level != null && !level.isClientSide()) {
            // Get latest name from provider if needed
            if (townId != null) {
                ITownDataProvider provider = getTownDataProvider();
                if (provider != null) {
                    String latestName = provider.getTownName();
                    if (!latestName.equals(name)) {
                        LOGGER.debug("Updating town name from '{}' to '{}'", name, latestName);
                        name = latestName;
                    }
                    
                    // Explicitly update client resources when syncing town data
                    if (level instanceof ServerLevel serverLevel) {
                        Town town = TownManager.get(serverLevel).getTown(townId);
                        if (town != null) {
                            clientSyncHelper.updateClientResourcesFromTown(town);
                        }
                    }
                }
            }
            
            // Update container data
            data.set(DATA_BREAD, data.get(DATA_BREAD));
            data.set(DATA_POPULATION, data.get(DATA_POPULATION));
            data.set(DATA_SPAWN_ENABLED, data.get(DATA_SPAWN_ENABLED));
            data.set(DATA_CAN_SPAWN, data.get(DATA_CAN_SPAWN));
            data.set(DATA_SEARCH_RADIUS, data.get(DATA_SEARCH_RADIUS));
            
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
            syncTownData();
        }
    }

    // Replace the old mountTouristsToVehicles method with a simplified version that delegates to the manager
    private void mountTouristsToVehicles() {
        if (level == null || level.isClientSide || town == null) return;
        if (pathStart == null || pathEnd == null) return;

        int mounted = touristVehicleManager.mountTouristsToVehicles(level, pathStart, pathEnd, searchRadius, townId);
        if (mounted > 0) {
            LOGGER.debug("Mounted {} tourists to vehicles for town {}", mounted, name);
        }
    }

    private String formatVec3(Vec3 vec) {
        return String.format("[%.6f, %.6f, %.6f]", vec.x, vec.y, vec.z);
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int radius) {
        this.searchRadius = Math.max(1, Math.min(radius, 100)); // Limit between 1-100 blocks
        
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
            data.set(DATA_SEARCH_RADIUS, this.searchRadius);
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
            data.set(DATA_SPAWN_ENABLED, enabled ? 1 : 0);
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
        LOGGER.debug("Cleared visitor position tracking, client caches, and platform indicators on block removal");
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

    /**
     * Client-side only: Updates the cached town name for immediate UI feedback
     * This is only meant to be used on the client side for visual updates
     * @param newName The new town name to display
     */
    public void setClientTownName(String newName) {
        if (level != null && level.isClientSide) {
            this.name = newName;
        }
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
            platformVisualizationHelper.registerPlayerExitUI(playerId, level.getGameTime());
        }
    }
    



}