package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.service.TouristVehicleManager;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.platform.TownInterfaceEntityService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * COMMON MODULE: Cross-platform TownInterfaceEntity implementation
 *
 * This class contains all the business logic that can be shared across platforms.
 * Platform-specific operations are delegated to PlatformServices.
 */
public abstract class TownInterfaceEntity extends TownInterfaceEntityCommon implements MenuProvider, BlockEntityTicker<TownInterfaceEntity> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(TownInterfaceEntity.class);

    // Platform-agnostic fields (moved from Forge-specific version)
    protected final TouristVehicleManager touristVehicleManager = new TouristVehicleManager();
    protected Map<String, Integer> visitingPopulation = new HashMap<>();

    // Constructor - platform implementations will call super with their specific BlockEntityType
    protected TownInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.literal(getTownName());
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TownInterfaceMenu(id, inventory, this.worldPosition);
    }

    // BlockEntityTicker implementation - delegates to platform-specific service
    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownInterfaceEntity blockEntity) {
        if (level.isClientSide) {
            return; // Client-side ticking handled by platform services
        }

        // Delegate platform-specific operations to the service
        TownInterfaceEntityService service = PlatformServices.getTownInterfaceEntityService();

        if (service != null) {
            // Process tourist spawning
            service.processTouristSpawning(level, this, getTownId() != null ? getTownId().toString() : null,
                    isTouristSpawningEnabled(), getPlatformCount(), level.getGameTime());

            // Process visitor interactions
            service.processVisitorInteractions(level, this, pos,
                    getTownId() != null ? getTownId().toString() : null, getSearchRadius(), getName(), level.getGameTime());

            // Process tourist vehicles
            service.processTouristVehicles(level, this, getTownId() != null ? getTownId().toString() : null,
                    isTouristSpawningEnabled(), getSearchRadius(), level.getGameTime());

            // Update scoreboard
            service.updateScoreboard(level);

            // Process resources in slots
            service.processResourcesInSlot(this);

            // Update from town provider
            service.updateFromTownProvider(this);

            // Handle buffer management
            service.handleBufferManagement(this);

            // Handle client synchronization
            service.handleClientSynchronization(this);
        }
    }

    // Platform-specific methods that must be implemented by platform-specific subclasses
    public abstract int getPlatformCount();
    public abstract Object getPlatformManager();
    public abstract Object getVisitBuffer();
    public abstract Object getTouristSpawningHelper();
    public abstract Object getVisitorProcessingHelper();

    // Helper methods for data synchronization
    private int getBreadCountFromTown() {
        Town town = getTown();
        if (town != null) {
            return town.getBreadCount();
        }
        return 0;
    }

    private int getPopulationFromTown() {
        Town town = getTown();
        if (town != null) {
            return town.getPopulation();
        }
        return 0;
    }

    private int getTouristSpawningEnabledAsInt() {
        return isTouristSpawningEnabled() ? 1 : 0;
    }

    private void setTouristSpawningEnabledFromInt(int value) {
        setTouristSpawningEnabled(value != 0);
    }

    private int getCanSpawnTouristsAsInt() {
        Town town = getTown();
        if (town != null) {
            return town.canSpawnTourists() ? 1 : 0;
        }
        return 0;
    }

    private int getTouristCountFromTown() {
        Town town = getTown();
        if (town != null) {
            return town.getTouristCount();
        }
        return 0;
    }

    private int getMaxTouristsFromTown() {
        Town town = getTown();
        if (town != null) {
            return town.getMaxTourists();
        }
        return 5; // Default value
    }

    // Container data for menu synchronization
    public ContainerData getContainerData() {
        return new SimpleContainerData(4) {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getBreadCountFromTown();
                    case 1 -> getPopulationFromTown();
                    case 2 -> getTouristSpawningEnabledAsInt();
                    case 3 -> getSearchRadius();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 2 -> setTouristSpawningEnabledFromInt(value);
                    case 3 -> setSearchRadius(value);
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    // Visiting population management
    public Map<String, Integer> getVisitingPopulation() {
        return visitingPopulation;
    }

    public int getVisitingPopulationFrom(String townName) {
        return visitingPopulation.getOrDefault(townName, 0);
    }

    // Abstract methods that platform implementations must provide
    public abstract void onLoad();
    public abstract void invalidateCaps();
    public abstract Object getCapability(Object cap, @Nullable Direction side);
    public abstract void saveAdditional(CompoundTag tag);
    public abstract void load(CompoundTag tag);
    public abstract CompoundTag getUpdateTag();
    public abstract void handleUpdateTag(CompoundTag tag);
}