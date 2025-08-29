package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.TownInterfaceEntityService;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.data.VisitBuffer;
import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Random;
import java.lang.Math;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * COMMON MODULE: Platform-independent TownInterfaceEntity base implementation
 *
 * This class contains all the business logic that can be shared across platforms.
 * Platform-specific operations are delegated to PlatformServices.
 */
public abstract class TownInterfaceEntityCommon extends BlockEntity implements ITownDataProvider {
    protected static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceEntityCommon.class);

    // Core data fields
    protected UUID townId;
    protected String name;
    protected Town town;
    protected boolean touristSpawningEnabled = true;
    protected int searchRadius = -1; // Will be set from NBT or default
    protected final Random random = new Random();

    // Position tracking
    protected Map<UUID, Vec3> lastPositions = new HashMap<>();
    protected BlockPos pathStart;
    protected BlockPos pathEnd;
    protected boolean isInPathCreationMode = false;

    // Platform visualization
    protected Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    protected Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();
    protected static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks
    protected static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks

    // Helper classes for modular functionality - to be implemented by platform-specific subclasses
    protected ClientSyncHelper clientSyncHelper;
    protected PlatformManager platformManager;
    protected TouristSpawningHelper touristSpawningHelper;
    protected VisitorProcessingHelper visitorProcessingHelper;
    protected VisitBuffer visitBuffer;

    // Simple placeholder classes - will be replaced by platform-specific implementations
    protected static class ClientSyncHelper {
        // Platform-specific client synchronization logic
    }

    protected static class PlatformManager {
        public int getPlatformCount() { return 0; }
        public boolean addPlatform() { return false; }
        // Platform-specific platform management logic
    }

    protected static class TouristSpawningHelper {
        // Platform-specific tourist spawning logic
    }

    protected static class VisitorProcessingHelper {
        // Platform-specific visitor processing logic
    }

    // Buffer management - will be initialized by platform-specific implementations
    protected TownBufferManager bufferManager;

    protected static class TownBufferManager {
        public void tick() {
            // Platform-specific buffer management logic
        }
    }

    // Rate limiting for markDirty calls
    protected long lastMarkDirtyTime = 0;
    protected static final long MARK_DIRTY_COOLDOWN_MS = 2000; // 2 seconds between calls

    // Special UUID for "any town" destination
    protected static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    protected static final String ANY_TOWN_NAME = "Any Town";

    protected TownInterfaceEntityCommon(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);

        // Initialize helper classes
        this.clientSyncHelper = new ClientSyncHelper();
        this.platformManager = new PlatformManager();
        this.touristSpawningHelper = new TouristSpawningHelper();
        this.visitorProcessingHelper = new VisitorProcessingHelper();
        this.visitBuffer = new VisitBuffer();
        this.bufferManager = new TownBufferManager();

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "TownInterfaceEntityCommon created at position: {}", pos);
    }

    // ===== ABSTRACT METHODS FOR PLATFORM-SPECIFIC IMPLEMENTATIONS =====

    /**
     * Platform-specific inventory capability handling
     */
    protected abstract void initializeInventoryCapabilities();

    /**
     * Platform-specific menu creation
     */
    public abstract Component getDisplayName();

    /**
     * Platform-specific container menu creation
     */
    public abstract Object createMenu(int id, Object inventory, Object player);

    /**
     * Platform-specific capability provider
     */
    public abstract Object getCapability(Object cap, @Nullable Direction side);

    // ===== COMMON BUSINESS LOGIC =====

    /**
     * Main tick method - contains platform-independent business logic
     */
    public void commonTick(Level level, BlockPos pos, BlockState state) {
        // Process resources every tick (not just once per second)
        processResourcesInSlot();

        // Sync town data from the provider every 3 seconds
        if (level.getGameTime() % 60 == 0) {
            updateFromTownProvider();
            // Delegate buffer synchronization to manager
            if (bufferManager != null) {
                bufferManager.tick();
            }
        }

        if (!level.isClientSide && townId != null) {
            if (level instanceof ServerLevel sLevel) {
                Town town = TownManager.get(sLevel).getTown(townId);
                if (town != null) {
                    // Platform-based tourist spawning
                    if (touristSpawningEnabled && town.canSpawnTourists() &&
                        platformManager.getPlatformCount() > 0 &&
                        level.getGameTime() % 200 == 0) {

                        // Platform-specific tourist spawning will be implemented by subclasses
                    // This delegates to platform-specific implementations
                    }

                                        // Platform-specific visitor processing will be implemented by subclasses
                    // This delegates to platform-specific implementations
                }
            }
        }
    }

    /**
     * Process resources in inventory slots
     */
    protected void processResourcesInSlot() {
        // Platform-specific inventory handling will be implemented by subclasses
        // This is where resources would be processed and converted to town benefits
    }

    /**
     * Update entity state from town provider
     */
    protected void updateFromTownProvider() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                this.town = town;
                this.name = town.getName();

                // Sync search radius from town if not set
                if (searchRadius == -1) {
                    searchRadius = town.getSearchRadius();
                }
            }
        }
    }

    // ===== GETTERS AND SETTERS =====

    public UUID getTownId() {
        return townId;
    }

    public void setTownId(UUID townId) {
        this.townId = townId;
        setChanged();
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

    public void setTownName(String name) {
        // Get town directly from TownManager instead of relying on cached field
        Town town = null;
        if (townId != null && level instanceof ServerLevel sLevel) {
            town = TownManager.get(sLevel).getTown(townId);
        }

        if (town != null) {
            town.setName(name);

            // Update our local cached name too
            this.name = name;

            setChanged();

            // Force block update to sync to clients immediately
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    protected String getRandomTownName() {
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    // ===== PATH MANAGEMENT BUSINESS LOGIC =====

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

    /**
     * Sets path creation mode and updates town data provider
     */
    public void setPathCreationMode(boolean mode) {
        this.isInPathCreationMode = mode;

        // Update town data provider if available
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            // Path creation mode is tracked locally, but we could sync it if needed
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "Path creation mode set to {} for town {}", mode, townId);
        }

        setChanged();
    }

    /**
     * Gets the town data provider for this entity
     */
    protected ITownDataProvider getTownDataProvider() {
        if (townId != null && level instanceof ServerLevel serverLevel) {
            Town town = TownManager.get(serverLevel).getTown(townId);
            return town;
        }
        return null;
    }

    // ===== COMMON TICK BUSINESS LOGIC =====

    /**
     * Common tick logic that can be shared across platforms
     * Platform-specific implementations should call this method
     */
    public void commonTickBusinessLogic(Level level, BlockPos pos, BlockState state) {
        // Use platform services for all operations
        TownInterfaceEntityService service = PlatformServices.getTownInterfaceEntityService();
        if (service != null) {
            // Process resources in inventory slots
            service.processResourcesInSlot(this);

            // Update entity data from town provider
            service.updateFromTownProvider(this);

            // Handle buffer management
            service.handleBufferManagement(this);

            // Process tourist spawning
            if (!level.isClientSide && townId != null) {
                // Get platform count from platform manager (would need to be abstracted)
                int platformCount = 1; // Placeholder - should be abstracted
                service.processTouristSpawning(level, this, townId.toString(),
                    touristSpawningEnabled, platformCount, level.getGameTime());

                // Process visitor interactions
                service.processVisitorInteractions(level, this, pos, townId.toString(),
                    searchRadius, name, level.getGameTime());

                // Update scoreboard
                service.updateScoreboard(level);
            }

            // Process tourist vehicles
            service.processTouristVehicles(level, this, townId != null ? townId.toString() : null,
                touristSpawningEnabled, searchRadius, level.getGameTime());

            // Handle client synchronization
            service.handleClientSynchronization(this);

            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "Common tick: Processing town {} at {}", townId, pos);
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "TownInterfaceEntityService not available, using fallback logic");

            // Fallback logic if service is not available
            if (level.getGameTime() % 60 == 0) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                    "Common tick: Fallback processing for town {} at {}", townId, pos);
            }
        }
    }

    public boolean isTouristSpawningEnabled() {
        return touristSpawningEnabled;
    }

    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
        setChanged();
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
        setChanged();
    }

    @Override
    public Position getPathStart() {
        return pathStart != null ? (Position) pathStart : (Position) new BlockPos(0, 0, 0);
    }

    @Override
    public void setPathStart(Position pos) {
        this.pathStart = (BlockPos) pos;
        setChanged();
    }

    @Override
    public Position getPathEnd() {
        return pathEnd != null ? (Position) pathEnd : (Position) new BlockPos(0, 0, 0);
    }

    @Override
    public void setPathEnd(Position pos) {
        this.pathEnd = (BlockPos) pos;
        setChanged();
    }

    public boolean isInPathCreationMode() {
        return isInPathCreationMode;
    }

    public void setInPathCreationMode(boolean inPathCreationMode) {
        this.isInPathCreationMode = inPathCreationMode;
    }

    // ===== PLATFORM MANAGER DELEGATION =====

    public PlatformManager getPlatformManager() {
        return platformManager;
    }

    public int getPlatformCount() {
        return platformManager.getPlatformCount();
    }

    public boolean addPlatform() {
        boolean result = platformManager.addPlatform();
        if (result) {
            setChanged();
        }
        return result;
    }

    // Platform-specific methods to be implemented by subclasses
    public abstract List<Object> getPlatforms();
    public abstract List<Object> getEnabledPlatforms();

    // ===== VISIT BUFFER DELEGATION =====

    public VisitBuffer getVisitBuffer() {
        return visitBuffer;
    }

    // ===== CLIENT SYNC HELPER DELEGATION =====

    public ClientSyncHelper getClientSyncHelper() {
        return clientSyncHelper;
    }

    // ===== PLATFORM VISUALIZATION =====

    public void registerPlayerExitUI(UUID playerId, Level level) {
        extendedIndicatorPlayers.put(playerId, level.getGameTime());

        // Spawn immediate indicators
        spawnPlatformIndicators(level);
    }

    protected void spawnPlatformIndicators(Level level) {
        // Platform-specific particle spawning will be implemented by subclasses
    }

    protected void cleanupPlatformIndicators(BlockPos blockPos, Level level) {
        // Clean up expired indicator timers
        extendedIndicatorPlayers.entrySet().removeIf(entry ->
            level.getGameTime() - entry.getValue() > EXTENDED_INDICATOR_DURATION);

        // Platform-specific platform cleanup will be implemented by subclasses
        // This delegates to platform-specific implementations
    }

    // ===== NBT DATA MANAGEMENT =====

    public void load(CompoundTag tag) {
        super.load(tag);

        // Load core data
        if (tag.contains("TownId")) {
            townId = tag.getUUID("TownId");
        }
        if (tag.contains("Name")) {
            name = tag.getString("Name");
        }
        if (tag.contains("TouristSpawningEnabled")) {
            touristSpawningEnabled = tag.getBoolean("TouristSpawningEnabled");
        }
        if (tag.contains("SearchRadius")) {
            searchRadius = tag.getInt("SearchRadius");
        }

        // Load path data
        if (tag.contains("PathStart")) {
            pathStart = BlockPos.of(tag.getLong("PathStart"));
        }
        if (tag.contains("PathEnd")) {
            pathEnd = BlockPos.of(tag.getLong("PathEnd"));
        }

        // Load positions
        if (tag.contains("LastPositions")) {
            CompoundTag positionsTag = tag.getCompound("LastPositions");
            lastPositions.clear();
            for (String key : positionsTag.getAllKeys()) {
                UUID uuid = UUID.fromString(key);
                long packedPos = positionsTag.getLong(key);
                Vec3 pos = BlockPos.of(packedPos).getCenter();
                lastPositions.put(uuid, pos);
            }
        }

        // Platform-specific data loading will be implemented by subclasses
        // This delegates to platform-specific implementations

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Loaded TownInterfaceEntityCommon data: townId={}, name={}", townId, name);
    }

    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        // Save core data
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        if (name != null) {
            tag.putString("Name", name);
        }
        tag.putBoolean("TouristSpawningEnabled", touristSpawningEnabled);
        tag.putInt("SearchRadius", searchRadius);

        // Save path data
        if (pathStart != null) {
            tag.putLong("PathStart", pathStart.asLong());
        }
        if (pathEnd != null) {
            tag.putLong("PathEnd", pathEnd.asLong());
        }

        // Save positions
        if (!lastPositions.isEmpty()) {
            CompoundTag positionsTag = new CompoundTag();
            for (Map.Entry<UUID, Vec3> entry : lastPositions.entrySet()) {
                BlockPos pos = BlockPos.containing(entry.getValue());
                positionsTag.putLong(entry.getKey().toString(), pos.asLong());
            }
            tag.put("LastPositions", positionsTag);
        }

        // Platform-specific data saving will be implemented by subclasses
        // This delegates to platform-specific implementations

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
            "Saved TownInterfaceEntityCommon data: townId={}, name={}", townId, name);
    }

    // ===== ITownDataProvider IMPLEMENTATION =====

    public UUID getTownUUID() {
        return townId;
    }

    public String getTownNameForProvider() {
        return name;
    }

    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getVisitHistory() : Collections.emptyList();
        }
        return Collections.emptyList();
    }

    @Override
    public int getPopulation() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getPopulation() : 0;
        }
        return 0;
    }

    @Override
    public int getBreadCount() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getBreadCount() : 0;
        }
        return 0;
    }

    @Override
    public int getTouristCount() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getTouristCount() : 0;
        }
        return 0;
    }

    @Override
    public int getMaxTourists() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.getMaxTourists() : 0;
        }
        return 0;
    }

    @Override
    public boolean canSpawnTourists() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? town.canSpawnTourists() : false;
        }
        return false;
    }
}
