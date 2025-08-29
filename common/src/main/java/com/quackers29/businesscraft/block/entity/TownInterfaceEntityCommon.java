package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * COMMON MODULE: Base class for TownInterfaceEntity
 *
 * This class contains the core data fields and basic functionality
 * that is shared across all platform implementations.
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

    // Path management
    protected BlockPos pathStart;
    protected BlockPos pathEnd;
    protected boolean isInPathCreationMode = false;

    // Platform-specific helper instances (to be implemented by subclasses)
    protected Object platformManager;
    protected Object touristSpawningHelper;
    protected Object visitorProcessingHelper;

    // Position tracking
    protected Map<UUID, Vec3> lastPositions = new HashMap<>();
    protected Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    protected Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();
    protected static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks
    protected static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks

    protected Object clientSyncHelper;

    // Platform management collections (to be initialized by subclasses)
    protected List<Object> platforms = new ArrayList<>();

    protected TownInterfaceEntityCommon(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // Core business logic methods
    protected String getRandomTownName() {
        List<String> townNames = ConfigLoader.INSTANCE.townNames;
        if (!townNames.isEmpty()) {
            return townNames.get(random.nextInt(townNames.size()));
        }
        return "New Town"; // Fallback
    }

    public void syncTownData() {
        if (level instanceof ServerLevel serverLevel) {
            var townManager = com.quackers29.businesscraft.town.TownManager.get(serverLevel);
            if (townId != null) {
                Town foundTown = townManager.getTown(townId);
                if (foundTown != null) {
                    town = foundTown;
                    name = foundTown.getName();
                    searchRadius = foundTown.getSearchRadius();
                    touristSpawningEnabled = foundTown.canSpawnTourists();
                } else {
                    town = null;
                    name = null;
                }
            }
        }
    }

    // Abstract methods that platform implementations must provide
    public abstract int getPlatformCount();
    public abstract Object getPlatformManager();
    public abstract Object getVisitBuffer();
    public abstract Object getTouristSpawningHelper();
    public abstract Object getVisitorProcessingHelper();

    // ITownDataProvider implementation
    @Override
    public ITownDataProvider.Position getPathStart() {
        if (pathStart == null) return null;
        return new ITownDataProvider.Position() {
            @Override public int getX() { return pathStart.getX(); }
            @Override public int getY() { return pathStart.getY(); }
            @Override public int getZ() { return pathStart.getZ(); }
        };
    }

    @Override
    public ITownDataProvider.Position getPathEnd() {
        if (pathEnd == null) return null;
        return new ITownDataProvider.Position() {
            @Override public int getX() { return pathEnd.getX(); }
            @Override public int getY() { return pathEnd.getY(); }
            @Override public int getZ() { return pathEnd.getZ(); }
        };
    }

    // Basic BlockEntity methods
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        if (name != null) {
            tag.putString("TownName", name);
        }
        tag.putBoolean("TouristSpawningEnabled", touristSpawningEnabled);
        tag.putInt("SearchRadius", searchRadius);

        if (pathStart != null) {
            tag.putLong("PathStart", pathStart.asLong());
        }
        if (pathEnd != null) {
            tag.putLong("PathEnd", pathEnd.asLong());
        }
        tag.putBoolean("PathCreationMode", isInPathCreationMode);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.hasUUID("TownId")) {
            townId = tag.getUUID("TownId");
        }
        if (tag.contains("TownName")) {
            name = tag.getString("TownName");
        }
        touristSpawningEnabled = tag.getBoolean("TouristSpawningEnabled");
        searchRadius = tag.getInt("SearchRadius");

        if (tag.contains("PathStart")) {
            pathStart = BlockPos.of(tag.getLong("PathStart"));
        }
        if (tag.contains("PathEnd")) {
            pathEnd = BlockPos.of(tag.getLong("PathEnd"));
        }
        isInPathCreationMode = tag.getBoolean("PathCreationMode");
    }

    // Getters and setters for core data
    public UUID getTownId() {
        return townId;
    }

    public void setTownId(UUID townId) {
        this.townId = townId;
        setChanged();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        setChanged();
    }

    public Town getTown() {
        return town;
    }

    public void setTown(Town town) {
        this.town = town;
        if (town != null) {
            this.townId = town.getId();
            this.name = town.getName();
            this.searchRadius = town.getSearchRadius();
            this.touristSpawningEnabled = town.canSpawnTourists();
        }
        setChanged();
    }

    public boolean isTouristSpawningEnabled() {
        return touristSpawningEnabled;
    }

    public void setTouristSpawningEnabled(boolean touristSpawningEnabled) {
        this.touristSpawningEnabled = touristSpawningEnabled;
        setChanged();
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int searchRadius) {
        this.searchRadius = searchRadius;
        setChanged();
    }

    public BlockPos getPathStartBlockPos() {
        return pathStart;
    }

    public BlockPos getPathEndBlockPos() {
        return pathEnd;
    }

    public void setPathStart(BlockPos pos) {
        this.pathStart = pos;
        setChanged();
    }

    public void setPathEnd(BlockPos pos) {
        this.pathEnd = pos;
        setChanged();
    }

    public boolean isInPathCreationMode() {
        return isInPathCreationMode;
    }

    public void setPathCreationMode(boolean mode) {
        this.isInPathCreationMode = mode;
        if (!mode) {
            pathStart = null;
        }
        setChanged();
    }

    public boolean isValidPathDistance(BlockPos pos) {
        if (pathStart == null) return true;
        double distance = Math.sqrt(pathStart.distSqr(pos));
        return distance >= 3 && distance <= 64;
    }

    // Platform management methods
    public java.util.List<Object> getPlatforms() {
        return platforms;
    }

    public Object getPlatform(UUID platformId) {
        // Default implementation - platform subclasses should override
        return null;
    }

    public boolean canAddMorePlatforms() {
        // Default implementation - platform subclasses should override
        return platforms.size() < 4; // Max 4 platforms
    }

    public Object addPlatform() {
        // Default implementation - platform subclasses should override
        return null;
    }

    public boolean removePlatform(UUID platformId) {
        // Default implementation - platform subclasses should override
        return false;
    }

    public void setPlatformCreationMode(boolean mode, UUID platformId) {
        // Default implementation - platform subclasses should override
    }

    public boolean isInPlatformCreationMode() {
        // Default implementation - platform subclasses should override
        return false;
    }

    public void setPlatformPathStart(UUID platformId, BlockPos pos) {
        // Default implementation - platform subclasses should override
    }

    public void setPlatformPathEnd(UUID platformId, BlockPos pos) {
        // Default implementation - platform subclasses should override
    }

    // Data management methods
    public Object getTownInterfaceData() {
        // Default implementation - platform subclasses should override
        return null;
    }

    public Object getBufferHandler() {
        // Default implementation - platform subclasses should override
        return null;
    }

    public void onTownBufferChanged() {
        // Default implementation - platform subclasses should override
        setChanged();
    }

    public Object getTownDataProvider() {
        // Default implementation - platform subclasses should override
        return this;
    }

    public void registerPlayerExitUI(UUID playerId) {
        // Default implementation - platform subclasses should override
    }

    // Menu/UI integration methods
    public Object createPaymentBoardMenuProvider() {
        // Default implementation - platform subclasses should override
        return null;
    }

    public String getTownNameFromId(UUID townId) {
        if (townId != null && townId.equals(this.townId)) {
            return name;
        }
        // Default implementation - platform subclasses should override
        return "Unknown Town";
    }

    // ITownDataProvider implementation
    @Override
    public void recordVisit(UUID originTownId, int count, ITownDataProvider.Position originPos) {
        // Default implementation - platform subclasses should override with actual visit recording
        // This could be implemented by platform-specific visit tracking systems
    }

    @Override
    public java.util.List<ITownDataProvider.VisitHistoryRecord> getVisitHistory() {
        // Default implementation - platform subclasses should override with actual visit history
        return new java.util.ArrayList<>();
    }

    @Override
    public int getTotalVisitors() {
        // Default implementation - return current tourist count
        return getTouristCount();
    }

    @Override
    public void addVisitor(UUID visitorId) {
        // Default implementation - platform subclasses should override with actual visitor tracking
        // This could be implemented by platform-specific visitor tracking systems
    }

    @Override
    public ITownDataProvider.Position getPosition() {
        // Return the block position as a Position
        return new ITownDataProvider.Position() {
            @Override public int getX() { return TownInterfaceEntityCommon.this.worldPosition.getX(); }
            @Override public int getY() { return TownInterfaceEntityCommon.this.worldPosition.getY(); }
            @Override public int getZ() { return TownInterfaceEntityCommon.this.worldPosition.getZ(); }
        };
    }

    @Override
    public void markDirty() {
        // Mark the block entity as dirty to trigger synchronization
        setChanged();
    }

    @Override
    public boolean canSpawnTourists() {
        // Return the tourist spawning enabled flag
        return isTouristSpawningEnabled();
    }

    @Override
    public void setPathStart(ITownDataProvider.Position pos) {
        // Set the path start position
        if (pos != null) {
            setPathStart(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    @Override
    public void setPathEnd(ITownDataProvider.Position pos) {
        // Set the path end position
        if (pos != null) {
            setPathEnd(new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
        }
    }

    @Override
    public String getTownName() {
        // Return the town name
        return getName();
    }

    @Override
    public void setTownName(String name) {
        // Set the town name
        setName(name);
    }

    @Override
    public int getBreadCount() {
        // Return the current bread count
        // Default implementation - can be overridden by platform implementations
        return 0; // Default bread count
    }

    @Override
    public int getPopulation() {
        // Return the current population
        // Default implementation - can be overridden by platform implementations
        return 0; // Default population
    }

    @Override
    public int getTouristCount() {
        // Return the current number of tourists
        // Default implementation - can be overridden by platform implementations
        return 0; // Default tourist count
    }

    @Override
    public int getMaxTourists() {
        // Return the maximum number of tourists allowed
        // Default implementation - can be overridden by platform implementations
        return 10; // Default max tourists
    }

    @Override
    public boolean canAddMoreTourists() {
        // Check if we can add more tourists based on current count and max
        return getTouristCount() < getMaxTourists();
    }

    // Communal storage methods
    @Override
    public boolean addToCommunalStorage(Object item, int count) {
        // Default implementation - platform subclasses should override
        return false;
    }

    @Override
    public int getCommunalStorageCount(Object item) {
        // Default implementation - platform subclasses should override
        return 0;
    }

    @Override
    public Map<Object, Integer> getAllCommunalStorageItems() {
        // Default implementation - platform subclasses should override
        return new HashMap<>();
    }

    // Resource methods
    @Override
    public void addResource(Object item, int count) {
        // Default implementation - platform subclasses should override
        // This could add items to the entity's inventory
    }

    @Override
    public int getResourceCount(Object item) {
        // Default implementation - platform subclasses should override
        return 0;
    }

    @Override
    public Map<Object, Integer> getAllResources() {
        // Default implementation - platform subclasses should override
        return new HashMap<>();
    }


}