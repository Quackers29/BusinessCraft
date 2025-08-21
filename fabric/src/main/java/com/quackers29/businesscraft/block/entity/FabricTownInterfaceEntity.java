package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Fabric implementation of TownInterfaceEntity.
 * Uses Bridge Pattern to delegate business logic to common modules while handling
 * Fabric-specific platform concerns.
 */
public class FabricTownInterfaceEntity extends BlockEntity implements ITownDataProvider, MenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/FabricTownInterfaceEntity");
    
    // Core town data - delegated to common TownManager
    private UUID townId;
    private String townName;
    private boolean touristSpawningEnabled = true;
    private int searchRadius = ConfigLoader.vehicleSearchRadius;
    
    // Platform management - using common Platform class
    private final List<Platform> platforms = new ArrayList<>();
    private final Random random = new Random();
    
    public FabricTownInterfaceEntity(BlockPos pos, BlockState blockState) {
        super(FabricModBlockEntities.TOWN_INTERFACE_ENTITY, pos, blockState);
    }

    // ============ ITownDataProvider Implementation ============
    // All business logic is delegated to common Town/TownManager classes

    @Override
    public @Nullable UUID getTownId() {
        return townId;
    }

    @Override
    public void setTownId(@Nullable UUID townId) {
        this.townId = townId;
        setChanged();
    }

    @Override
    public @Nullable String getTownName() {
        // Try to get from common Town first, fallback to stored name
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                return town.getName();
            }
        }
        return townName;
    }

    @Override
    public void setTownName(String name) {
        this.townName = name;
        // Also update the common Town object if available
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                town.setName(name);
            }
        }
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
    public int getPopulation() {
        // Delegate to common Town business logic
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                return town.getPopulation();
            }
        }
        return 0;
    }

    @Override
    public boolean canSpawnTourists() {
        // Delegate to common Town business logic
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                return town.canSpawnTourists() && touristSpawningEnabled;
            }
        }
        return false;
    }

    @Override
    public int getTouristCount() {
        // Delegate to common Town business logic
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                return town.getTouristCount();
            }
        }
        return 0;
    }

    @Override
    public int getMaxTourists() {
        // Delegate to common Town business logic
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                return town.getMaxTourists();
            }
        }
        return 0;
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
    public void markDirty() {
        setChanged();
    }

    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        // Delegate to common Town business logic
        if (townId != null && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            Town town = townManager.getTown(townId);
            if (town != null) {
                return town.getVisitHistory();
            }
        }
        return Collections.emptyList();
    }

    // ============ Platform Management ============

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public boolean addPlatform() {
        if (platforms.size() >= 10) { // Same limit as Forge implementation
            return false;
        }
        
        Platform newPlatform = new Platform();
        newPlatform.setName("Platform " + (platforms.size() + 1));
        platforms.add(newPlatform);
        setChanged();
        
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
            "Added platform {} to Fabric town interface at {}", newPlatform.getName(), getBlockPos());
        return true;
    }

    public void createDefaultPlatform(BlockPos townPos, BlockState state, @Nullable LivingEntity placer) {
        // Same logic as Forge implementation but using common Platform class
        boolean platformAdded = addPlatform();
        
        if (!platformAdded) {
            LOGGER.error("Failed to add default platform for Fabric town at {}", townPos);
            return;
        }
        
        if (platforms.isEmpty()) {
            LOGGER.error("No platforms found after adding default platform for Fabric town at {}", townPos);
            return;
        }
        
        Platform platform = platforms.get(0);
        
        // Determine orientation based on placer's facing direction
        Direction direction = Direction.NORTH; // Default direction
        
        if (placer != null) {
            direction = Direction.getNearest(
                (float) placer.getLookAngle().x,
                (float) placer.getLookAngle().y,
                (float) placer.getLookAngle().z
            );
        }
        
        // Create the default platform layout based on the orientation
        BlockPos platformStart = null;
        BlockPos platformEnd = null;
        
        switch (direction) {
            case NORTH -> {
                platformStart = townPos.north(3).below();
                platformEnd = townPos.north(5).below();
            }
            case SOUTH -> {
                platformStart = townPos.south(3).below();
                platformEnd = townPos.south(5).below();
            }
            case WEST -> {
                platformStart = townPos.west(3).below();
                platformEnd = townPos.west(5).below();
            }
            case EAST -> {
                platformStart = townPos.east(3).below();
                platformEnd = townPos.east(5).below();
            }
            default -> {
                platformStart = townPos.north(3).below();
                platformEnd = townPos.north(5).below();
            }
        }
        
        platform.setStartPos(platformStart);
        platform.setEndPos(platformEnd);
        platform.setName("Platform 1");
        platform.setEnabled(true);
        
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
            "Created default platform for Fabric town at {} with start {} and end {}", 
            townPos, platformStart, platformEnd);
    }

    // ============ Block Entity Lifecycle ============

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide() && level.getGameTime() % 20 == 0) { // Every second
            // Basic tick functionality - can be expanded later
            // Delegate any business logic to common Town if needed
            if (townId != null && level instanceof ServerLevel serverLevel) {
                TownManager townManager = TownManager.get(serverLevel);
                Town town = townManager.getTown(townId);
                if (town != null) {
                    // Any periodic town updates would go here
                    // For now, just ensure the town is still valid
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        if (townId != null) {
            tag.putString("townId", townId.toString());
        }
        if (townName != null) {
            tag.putString("townName", townName);
        }
        tag.putBoolean("touristSpawningEnabled", touristSpawningEnabled);
        tag.putInt("searchRadius", searchRadius);
        
        // Save platforms
        ListTag platformsTag = new ListTag();
        for (Platform platform : platforms) {
            CompoundTag platformTag = new CompoundTag();
            platform.saveToNBT(platformTag);
            platformsTag.add(platformTag);
        }
        tag.put("platforms", platformsTag);
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, 
            "Saved Fabric TownInterfaceEntity data: townId={}, platforms={}", townId, platforms.size());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        if (tag.contains("townId")) {
            try {
                townId = UUID.fromString(tag.getString("townId"));
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid townId in NBT: {}", tag.getString("townId"));
                townId = null;
            }
        }
        
        townName = tag.getString("townName");
        touristSpawningEnabled = tag.getBoolean("touristSpawningEnabled");
        searchRadius = tag.getInt("searchRadius");
        if (searchRadius <= 0) {
            searchRadius = ConfigLoader.vehicleSearchRadius;
        }
        
        // Load platforms
        platforms.clear();
        if (tag.contains("platforms")) {
            ListTag platformsTag = tag.getList("platforms", Tag.TAG_COMPOUND);
            for (int i = 0; i < platformsTag.size(); i++) {
                CompoundTag platformTag = platformsTag.getCompound(i);
                Platform platform = new Platform();
                platform.loadFromNBT(platformTag);
                platforms.add(platform);
            }
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, 
            "Loaded Fabric TownInterfaceEntity data: townId={}, platforms={}", townId, platforms.size());
    }

    // ============ MenuProvider Implementation ============
    // For future GUI implementation

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.businesscraft.town_interface");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        // TODO: Implement Fabric menu when ready
        // For now, return null to indicate no GUI
        return null;
    }
}