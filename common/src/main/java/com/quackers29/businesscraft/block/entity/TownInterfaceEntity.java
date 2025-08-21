package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.api.ITownDataProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * UNIFIED ARCHITECTURE STUB: Minimal TownInterfaceEntity for compilation.
 * 
 * This is a temporary bridge class that allows the common module TownInterfaceBlock to compile
 * while the full TownInterfaceEntity implementations remain in platform modules.
 * 
 * The actual functionality is provided by platform-specific implementations that extend this class.
 * This approach allows immediate Fabric support without requiring full entity migration.
 */
public class TownInterfaceEntity extends BlockEntity implements ITownDataProvider {
    
    protected UUID townId;
    protected String townName;
    protected int searchRadius = 100;
    protected boolean touristSpawningEnabled = true;
    
    public TownInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    // Constructor for common module usage - platform implementations will override this
    public TownInterfaceEntity(BlockPos pos, BlockState state) {
        this(null, pos, state); // null will be replaced by platform implementations
    }
    
    // Basic tick method for platform-specific overrides
    public void tick(Level level, BlockPos pos, BlockState state, TownInterfaceEntity entity) {
        // Platform-specific implementations will override this
    }
    
    // === ITownDataProvider Implementation ===
    
    @Override
    public @Nullable UUID getTownId() {
        return townId;
    }
    
    public void setTownId(@Nullable UUID townId) {
        this.townId = townId;
        setChanged();
    }
    
    @Override
    public @Nullable String getTownName() {
        return townName;
    }
    
    @Override
    public void setTownName(@Nullable String townName) {
        this.townName = townName;
        setChanged();
    }
    
    @Override
    public int getSearchRadius() {
        return searchRadius;
    }
    
    @Override
    public void setSearchRadius(int radius) {
        this.searchRadius = radius;
        setChanged();
    }
    
    @Override
    public boolean isTouristSpawningEnabled() {
        return touristSpawningEnabled;
    }
    
    @Override
    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
        setChanged();
    }
    
    @Override
    public void markDirty() {
        setChanged();
    }
    
    @Override
    public java.util.List<ITownDataProvider.VisitHistoryRecord> getVisitHistory() {
        // Stub implementation - platform-specific implementations will override
        return java.util.List.of();
    }
    
    @Override
    public void recordVisit(UUID originTownId, int count, ITownDataProvider.Position originPos) {
        // Stub implementation - platform-specific implementations will override
    }
    
    @Override
    public int getTotalVisitors() {
        // Stub implementation - platform-specific implementations will override
        return 0;
    }
    
    @Override
    public void addVisitor(UUID fromTownId) {
        // Stub implementation - platform-specific implementations will override
    }
    
    @Override
    public int getBreadCount() {
        return 0;
    }
    
    @Override
    public void addResource(Object item, int count) {
        // Stub implementation
    }
    
    @Override
    public int getResourceCount(Object item) {
        return 0;
    }
    
    @Override
    public java.util.Map<Object, Integer> getAllResources() {
        return java.util.Map.of();
    }
    
    @Override
    public boolean addToCommunalStorage(Object item, int count) {
        return false;
    }
    
    @Override
    public int getCommunalStorageCount(Object item) {
        return 0;
    }
    
    @Override
    public java.util.Map<Object, Integer> getAllCommunalStorageItems() {
        return java.util.Map.of();
    }
    
    @Override
    public int getPopulation() {
        return 0;
    }
    
    @Override
    public int getTouristCount() {
        return 0;
    }
    
    @Override
    public int getMaxTourists() {
        return 0;
    }
    
    @Override
    public boolean canAddMoreTourists() {
        return false;
    }
    
    @Override
    public ITownDataProvider.Position getPathStart() {
        return null;
    }
    
    @Override
    public void setPathStart(ITownDataProvider.Position pos) {
        // Stub implementation
    }
    
    @Override
    public ITownDataProvider.Position getPathEnd() {
        return null;
    }
    
    @Override
    public void setPathEnd(ITownDataProvider.Position pos) {
        // Stub implementation
    }
    
    @Override
    public boolean canSpawnTourists() {
        return touristSpawningEnabled;
    }
    
    @Override
    public ITownDataProvider.Position getPosition() {
        return new ITownDataProvider.Position() {
            @Override
            public int getX() { return getBlockPos().getX(); }
            @Override
            public int getY() { return getBlockPos().getY(); }
            @Override
            public int getZ() { return getBlockPos().getZ(); }
        };
    }
    
    // === Platform Management (Stub Methods) ===
    
    public List<Platform> getPlatforms() {
        // Platform-specific implementations will override this
        return List.of();
    }
    
    public boolean addPlatform() {
        // Platform-specific implementations will override this
        return false;
    }
    
    public void registerPlayerExitUI(UUID playerId) {
        // Platform-specific implementations will override this
    }
    
    // === NBT Serialization ===
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (townId != null) {
            tag.putUUID("townId", townId);
        }
        if (townName != null) {
            tag.putString("townName", townName);
        }
        tag.putInt("searchRadius", searchRadius);
        tag.putBoolean("touristSpawningEnabled", touristSpawningEnabled);
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("townId")) {
            townId = tag.getUUID("townId");
        }
        if (tag.contains("townName")) {
            townName = tag.getString("townName");
        }
        searchRadius = tag.getInt("searchRadius");
        touristSpawningEnabled = tag.getBoolean("touristSpawningEnabled");
    }
}