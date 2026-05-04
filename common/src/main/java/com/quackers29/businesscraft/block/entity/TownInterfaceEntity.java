package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.service.TouristVehicleManager;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.scoreboard.TownScoreboardManager;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import java.util.Map;
import java.util.UUID;
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
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ExperienceOrb;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.entity.npc.VillagerProfession;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.scoreboard.TownScoreboardManager;
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
import com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.quackers29.businesscraft.platform.Platform;
import net.minecraft.resources.ResourceLocation;
import com.quackers29.businesscraft.api.ITouristHelper;
import com.quackers29.businesscraft.api.ITouristHelper.TouristInfo;
import com.quackers29.businesscraft.town.utils.TouristAllocationTracker;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.utils.TownNotificationUtils;
import com.quackers29.businesscraft.town.data.VisitBuffer;
import com.quackers29.businesscraft.town.data.TouristSpawningHelper;
import com.quackers29.businesscraft.town.data.PlatformManager;
import com.quackers29.businesscraft.town.viewmodel.TradingViewModel;
import com.quackers29.businesscraft.town.viewmodel.TradingViewModelBuilder;
import com.quackers29.businesscraft.network.packets.TradingViewModelSyncPacket;
import com.quackers29.businesscraft.town.data.VisitorProcessingHelper;
import com.quackers29.businesscraft.town.data.ClientSyncHelper;
import com.quackers29.businesscraft.town.viewmodel.ViewModelCache;
import com.quackers29.businesscraft.town.viewmodel.TownResourceViewModel;
import com.quackers29.businesscraft.town.viewmodel.TownResourceViewModelBuilder;
import com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModel;
import com.quackers29.businesscraft.town.viewmodel.ProductionStatusViewModelBuilder;
import com.quackers29.businesscraft.network.packets.ResourceViewModelSyncPacket;
import com.quackers29.businesscraft.network.packets.ProductionViewModelSyncPacket;
import com.quackers29.businesscraft.town.viewmodel.TradingViewModel;
import com.quackers29.businesscraft.town.viewmodel.TradingViewModelBuilder;
import com.quackers29.businesscraft.network.packets.TradingViewModelSyncPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.quackers29.businesscraft.town.data.NBTDataHelper;
import com.quackers29.businesscraft.town.data.ContainerDataHelper;
import com.quackers29.businesscraft.town.data.TownBufferManager;
import com.quackers29.businesscraft.debug.DebugConfig;

public class TownInterfaceEntity extends BlockEntity
        implements MenuProvider, BlockEntityTicker<TownInterfaceEntity>, WorldlyContainer {
    private final Object itemHandler = PlatformAccess.getItemHandlers().createItemStackHandler(1);
    private Object lazyItemHandler = PlatformAccess.getItemHandlers().getEmptyLazyOptional();

    private TownBufferManager bufferManager;
    private Object lazyBufferHandler = PlatformAccess.getItemHandlers().getEmptyLazyOptional();

    private final ContainerDataHelper containerData = ContainerDataHelper.builder("TownBlock")

            .addReadOnlyField("population", this::getPopulationFromTown, "Current town population")
            .addField("spawn_enabled", this::getTouristSpawningEnabledAsInt, this::setTouristSpawningEnabledFromInt,
                    "Tourist spawning enabled flag")
            .addReadOnlyField("can_spawn", this::getCanSpawnTouristsAsInt, "Whether town can currently spawn tourists")
            .addField("search_radius", this::getSearchRadius, this::setSearchRadius,
                    "Search radius for tourist detection")
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

    private long lastMarkDirtyTime = 0;
    private static final long MARK_DIRTY_COOLDOWN_MS = 2000; // 2 seconds between calls
    private long lastSearchRadiusLogTime = 0; // For rate-limiting debug logs

    private final TouristVehicleManager touristVehicleManager = new TouristVehicleManager();

    private final VisitBuffer visitBuffer = new VisitBuffer();

    private final ClientSyncHelper clientSyncHelper = new ClientSyncHelper();

    private final ViewModelCache vmCache = new ViewModelCache();

    private void updateResourceVM() {
        Town town = getTown();
        if (town != null) {
            TownResourceViewModel vm = TownResourceViewModelBuilder.buildResourceViewModel(town);
            vmCache.update(TownResourceViewModel.class, vm);
        }
    }

    private void updateProductionVM() {
        Town town = getTown();
        if (town != null) {
            ProductionStatusViewModel vm = ProductionStatusViewModelBuilder.buildProductionViewModel(town);
            vmCache.update(ProductionStatusViewModel.class, vm);
        }
    }

    private void updateUpgradeVM() {
        Town town = getTown();
        if (town != null) {
            com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel vm = com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModelBuilder.buildUpgradeViewModel(town);
            vmCache.update(com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel.class, vm);
        }
    }

    public void updateInterfaceVM() {
        Town town = getTown();
        if (town != null) {
            com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel vm = com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModelBuilder.build(town, this);
            vmCache.update(com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel.class, vm);
        }
    }

    private void updateAllTownVMs() {
        updateResourceVM();
        updateProductionVM();
        updateUpgradeVM();
        updateInterfaceVM();
    }

    public void syncAllDirtyTownVMsToPlayers() {
        if (level == null || level.isClientSide()) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        BlockPos pos = getBlockPos();

        vmCache.syncAllDirty(vm -> {
            if (vm instanceof TownResourceViewModel rvm) {
                ResourceViewModelSyncPacket packet = new ResourceViewModelSyncPacket(pos, rvm);
                serverLevel.players().forEach(player -> PlatformAccess.getNetworkMessages().sendToPlayer(packet, player));
            } else if (vm instanceof ProductionStatusViewModel pvm) {
                ProductionViewModelSyncPacket packet = new ProductionViewModelSyncPacket(pos, pvm);
                serverLevel.players().forEach(player -> PlatformAccess.getNetworkMessages().sendToPlayer(packet, player));
            } else if (vm instanceof com.quackers29.businesscraft.town.viewmodel.UpgradeStatusViewModel uvm) {
                com.quackers29.businesscraft.network.packets.UpgradeViewModelSyncPacket packet = new com.quackers29.businesscraft.network.packets.UpgradeViewModelSyncPacket(pos, uvm);
                serverLevel.players().forEach(player -> PlatformAccess.getNetworkMessages().sendToPlayer(packet, player));
            } else if (vm instanceof com.quackers29.businesscraft.town.viewmodel.TownInterfaceViewModel ivm) {
                com.quackers29.businesscraft.network.packets.TownInterfaceViewModelSyncPacket packet = new com.quackers29.businesscraft.network.packets.TownInterfaceViewModelSyncPacket(pos, ivm);
                serverLevel.players().forEach(player -> PlatformAccess.getNetworkMessages().sendToPlayer(packet, player));
            }
        });
    }

public ViewModelCache getVmCache() {
    return vmCache;
}

    public ClientSyncHelper getClientSyncHelper() {
        return clientSyncHelper;
    }

    /**
     * Syncs the trading view-model to nearby players.
     * This ensures the client has the latest stock levels and prices before opening
     * the UI.
     */
    public void syncTradingViewModelToNearbyPlayers() {
        if (level == null || level.isClientSide())
            return;
        if (!(level instanceof ServerLevel serverLevel))
            return;
        if (townId == null)
            return;

        Town town = TownManager.get(serverLevel).getTown(townId);
        if (town == null)
            return;

        TradingViewModel viewModel = TradingViewModelBuilder.build(town);

        TradingViewModelSyncPacket packet = new TradingViewModelSyncPacket(viewModel);

        int syncRadius = 64;
        double syncRadiusSqr = syncRadius * syncRadius;
        BlockPos pos = getBlockPos();

        for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < syncRadiusSqr) {
                PlatformAccess.getNetworkMessages().sendToPlayer(packet, player);
            }
        }
    }

    /**
     * SERVER-SIDE: Sends updated trading view-model to a specific player.
     * This eliminates client-side stock calculations and logic.
     */
    public void syncTradingViewModelToPlayer(ServerPlayer player) {
        if (level == null || level.isClientSide())
            return;

        Town town = this.getTown();
        if (town == null)
            return;

        TradingViewModel viewModel = TradingViewModelBuilder.build(town);

        TradingViewModelSyncPacket packet = new TradingViewModelSyncPacket(viewModel);
        PlatformAccess.getNetworkMessages().sendToPlayer(packet, player);

        DebugConfig.debug(LOGGER, DebugConfig.SYNC_HELPERS,
                "Sent trading view-model to player {} - Resources: {}",
                player.getName().getString(), viewModel.getResourceInfo().size());
    }

    private final PlatformManager platformManager = new PlatformManager();

    private Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    private static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks

    private Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();
    private static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks

    private final TouristSpawningHelper touristSpawningHelper = new TouristSpawningHelper();

    private final VisitorProcessingHelper visitorProcessingHelper = new VisitorProcessingHelper();

    private final NBTDataHelper nbtDataHelper = new NBTDataHelper();

    private static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    private static final String ANY_TOWN_NAME = "Any Town";

    private int getPopulationFromTown() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? (int) town.getPopulation() : 0;
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
            return town != null ? (int) town.getTouristCount() : 0;
        }
        return 0;
    }

    private int getMaxTouristsFromTown() {
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            return town != null ? (int) town.getMaxTourists() : 0;
        }
        return 0;
    }

    public TownInterfaceEntity(BlockPos pos, BlockState state) {
        super((net.minecraft.world.level.block.entity.BlockEntityType<TownInterfaceEntity>) PlatformAccess
                .getBlockEntities().getTownInterfaceEntityType(), pos, state);

        this.bufferManager = new TownBufferManager(this, null);

        platformManager.setChangeCallback(this::setChanged);

        this.lazyItemHandler = PlatformAccess.getItemHandlers().createLazyOptional(itemHandler);
        this.lazyBufferHandler = PlatformAccess.getItemHandlers().createLazyOptional(bufferManager.getBufferHandler());

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

    public @NotNull <T> Object getCapabilityCommon(@NotNull Object cap, @Nullable Direction side) {
        if (PlatformAccess.getItemHandlers().isItemHandlerCapability(cap)) {
            if (side == Direction.DOWN && bufferManager != null) {
                return PlatformAccess.getItemHandlers().castLazyOptional(lazyBufferHandler, cap);
            }
            return PlatformAccess.getItemHandlers().castLazyOptional(lazyItemHandler, cap);
        }
        return PlatformAccess.getItemHandlers().getEmptyLazyOptional();
    }

    /**
     * Gets the container size for hopper interactions
     * Top/Front: Input handler (1 slot), Bottom: Buffer handler (18 slots)
     */
    public int getContainerSize() {
        return 19; // 1 input slot + 18 buffer slots
    }

    /**
     * Checks if the container is empty
     */
    public boolean isEmpty() {
        Object inputStack = PlatformAccess.getItemHandlers().getStackInSlot(itemHandler, 0);
        if (inputStack instanceof ItemStack stack && !stack.isEmpty()) {
            return false;
        }

        if (bufferManager != null && bufferManager.getBufferHandler() instanceof Container bufferContainer) {
            for (int i = 0; i < bufferContainer.getContainerSize(); i++) {
                if (!bufferContainer.getItem(i).isEmpty()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Gets an item from the specified slot index
     */
    public ItemStack getItem(int slot) {
        if (slot == 0) {
            Object stack = PlatformAccess.getItemHandlers().getStackInSlot(itemHandler, 0);
            return stack instanceof ItemStack ? (ItemStack) stack : ItemStack.EMPTY;
        } else if (slot >= 1 && slot <= 18) {
            if (bufferManager != null && bufferManager.getBufferHandler() instanceof Container bufferContainer) {
                int bufferSlot = slot - 1;
                if (bufferSlot < bufferContainer.getContainerSize()) {
                    return bufferContainer.getItem(bufferSlot);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Sets an item in the specified slot index
     */
    public void setItem(int slot, ItemStack stack) {
        if (slot == 0) {
            PlatformAccess.getItemHandlers().setStackInSlot(itemHandler, 0, stack);
        } else if (slot >= 1 && slot <= 18) {
            if (bufferManager != null && bufferManager.getBufferHandler() instanceof Container bufferContainer) {
                int bufferSlot = slot - 1;
                if (bufferSlot < bufferContainer.getContainerSize()) {
                    bufferContainer.setItem(bufferSlot, stack);
                }
            }
        }
    }

    /**
     * Removes and returns an item from the specified slot
     */
    public ItemStack removeItem(int slot, int amount) {
        ItemStack existing = getItem(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toRemove = Math.min(amount, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(toRemove);

        existing.shrink(toRemove);
        setItem(slot, existing);

        return result;
    }

    /**
     * Removes and returns the entire item from the specified slot
     */
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack existing = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return existing;
    }

    /**
     * Gets the maximum stack size for the specified slot
     */
    public int getMaxStackSize() {
        return 64; // Standard stack size
    }

    /**
     * Checks if the specified item can be placed in the slot
     */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the specified item can be taken from the slot
     */
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (direction == Direction.DOWN && slot >= 1 && slot <= 18) {
            return true;
        }
        return false; // Input slot cannot be extracted
    }

    /**
     * Gets the slots accessible from the specified face
     */
    public int[] getSlotsForFace(Direction direction) {
        if (direction == Direction.DOWN) {
            return new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18 };
        } else {
            return new int[] { 0 };
        }
    }

    /**
     * Checks if automation can insert into the specified slot from the specified
     * face
     */
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
        if (direction != Direction.DOWN && slot == 0) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the player can still interact with this container
     * Required by Container interface
     */
    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) < 64;
    }

    /**
     * Clears all contents from this container
     * Required by Clearable interface
     */
    @Override
    public void clearContent() {
        PlatformAccess.getItemHandlers().setStackInSlot(itemHandler, 0, ItemStack.EMPTY);

        if (bufferManager != null && bufferManager.getBufferHandler() instanceof Container bufferContainer) {
            for (int i = 0; i < bufferContainer.getContainerSize(); i++) {
                bufferContainer.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * Gets the container for hopper interactions
     * This is needed for Fabric's hopper logic
     */
    public Container getContainer() {
        return new SimpleContainer(getContainerSize()) {
            @Override
            public ItemStack getItem(int slot) {
                return TownInterfaceEntity.this.getItem(slot);
            }

            @Override
            public void setItem(int slot, ItemStack stack) {
                TownInterfaceEntity.this.setItem(slot, stack);
            }

            @Override
            public ItemStack removeItem(int slot, int amount) {
                return TownInterfaceEntity.this.removeItem(slot, amount);
            }

            @Override
            public ItemStack removeItemNoUpdate(int slot) {
                return TownInterfaceEntity.this.removeItemNoUpdate(slot);
            }

            @Override
            public boolean canPlaceItem(int slot, ItemStack stack) {
                return TownInterfaceEntity.this.canPlaceItem(slot, stack);
            }

            @Override
            public int getMaxStackSize() {
                return TownInterfaceEntity.this.getMaxStackSize();
            }

            @Override
            public boolean isEmpty() {
                return TownInterfaceEntity.this.isEmpty();
            }
        };
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (bufferManager != null) {
            bufferManager.setLevel(level);
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!level.isClientSide()) {
            updateFromTownProvider();
        }
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Saving TownInterfaceEntity with searchRadius: {}",
                searchRadius);
        nbtDataHelper.saveToNBT(tag, itemHandler, townId, name, pathStart, pathEnd, platformManager, searchRadius);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        NBTDataHelper.LoadResult result = nbtDataHelper.loadFromNBT(tag, itemHandler, level, platformManager);

        this.townId = result.townId;
        this.name = result.name;
        this.town = result.town;
        this.pathStart = result.pathStart;
        this.pathEnd = result.pathEnd;
        this.touristSpawningEnabled = result.touristSpawningEnabled;

        if (bufferManager != null && townId != null) {
            bufferManager.setTownId(townId);
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Before NBT load: searchRadius={}", this.searchRadius);
        if (result.hasSearchRadius()) {
            this.searchRadius = result.searchRadius;
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Loaded searchRadius from NBT: {} -> {}",
                    result.searchRadius, this.searchRadius);
        } else {
            this.searchRadius = DEFAULT_SEARCH_RADIUS;
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Using default searchRadius: {} -> {}",
                    DEFAULT_SEARCH_RADIUS, this.searchRadius);
        }

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Loaded NBT data: {}", result.getSummary());

        // CRITICAL FOR FABRIC SYNC:
        // On Fabric, block entity updates via packets also call load(), not just
        // handleUpdateTag() or onDataPacket()
        // We must ensure client sync data is processed here if present
        if (level != null && level.isClientSide()) {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM,
                    "[PLATFORM] load() called on CLIENT - checking for sync data");

            if (tag.contains("clientResources")) {
                loadResourcesFromTag(tag);
            }

            if (tag.contains("visitHistory")) {
                clientSyncHelper.loadVisitHistoryFromTag(tag);
            }

            if (tag.contains("clientWantedResources")) {
                clientSyncHelper.loadWantedResourcesFromTag(tag);
            }

            if (tag.contains("platforms")) {
                platformManager.updateClientPlatforms(tag);
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownInterfaceEntity blockEntity) {
        processResourcesInSlot();

        if (level.getGameTime() % 10 == 0) { // Every 10 ticks (0.5 seconds) for snappier UI
            updateFromTownProvider();
            setChanged(); // Force sync of resources to client

            if (!level.isClientSide && level instanceof ServerLevel) {
                updateAllTownVMs();
                syncAllDirtyTownVMsToPlayers();
                syncTradingViewModelToNearbyPlayers();
            }

            if (bufferManager != null) {
                bufferManager.setLevel(level);
                bufferManager.tick();
            }
        }

        if (!level.isClientSide && townId != null) {
            if (level instanceof ServerLevel sLevel1) {
                Town town = TownManager.get(sLevel1).getTown(townId);
                if (town != null) {
                    if (!ConfigLoader.touristSystemEnabled) return; // Phase 11 global toggle
                    if (touristSpawningEnabled && town.canSpawnTourists() &&
                            platformManager.getPlatformCount() > 0 &&
                            town.getPendingTouristSpawns() > 0 &&
                            level.getGameTime() % 20 == 0) { // Check every second

                        List<Platform> platforms = platformManager.getEnabledPlatforms();
                        if (!platforms.isEmpty()) {
                            Platform platform = platforms.get(random.nextInt(platforms.size()));
                            if (touristSpawningHelper.spawnTouristOnPlatform(level, town, platform, townId)) {
                                town.addPendingTouristSpawns(-1);
                            }
                        }
                    }

                    if (level.getGameTime() % 40 == 0) {
                        visitorProcessingHelper.processVisitors(
                                level,
                                pos,
                                townId,
                                platformManager,
                                visitBuffer,
                                searchRadius,
                                name,
                                this::setChanged);
                    }

                    if (level instanceof ServerLevel sLevel2) {
                        TownScoreboardManager.updateScoreboard(sLevel2);
                    }
                }
            }
        }

        if (level.getGameTime() % 20 == 0) { // Every 1 second
            if (!ConfigLoader.touristSystemEnabled) return; // Phase 11 global toggle
            if (touristSpawningEnabled && townId != null) {
                Town currentTown = null;
                if (level instanceof ServerLevel sLevel) {
                    currentTown = TownManager.get(sLevel).getTown(townId);
                }

                if (currentTown != null && currentTown.canSpawnTourists()) {
                    for (Platform platform : platformManager.getEnabledPlatforms()) {
                        touristVehicleManager.mountTouristsToVehicles(
                                level,
                                platform.getStartPos(),
                                platform.getEndPos(),
                                searchRadius,
                                townId);
                    }
                }
            }

        }

    }

    public String getTownName() {
        if (townId != null) {
            if (level.isClientSide && name != null) {
                return name;
            }
            if (level instanceof ServerLevel sLevel1) {
                Town town = TownManager.get(sLevel1).getTown(townId);
                if (town != null) {
                    if (!town.getName().equals(name)) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Updating cached name from {} to {}",
                                name, town.getName());
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
            return "DefaultTown";
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "[PLATFORM] setChanged called on SERVER");
            if (townId != null && level instanceof ServerLevel serverLevel) {
                Town town = TownManager.get(serverLevel).getTown(townId);
                if (town != null) {
                    clientSyncHelper.updateClientResourcesFromTown(town);
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM,
                            "[PLATFORM] Updated client resources from town, resource count: {}",
                            town.getAllResources().size());
                }
            }

            refreshOpenMenus();

            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "[PLATFORM] sendBlockUpdated called");
        }
    }

    /**
     * Refreshes ContainerData for any open TownInterfaceMenu instances
     * This ensures population and tourist values are updated in real-time
     */
    private void refreshOpenMenus() {
        if (level instanceof ServerLevel serverLevel) {
            for (net.minecraft.server.level.ServerPlayer player : serverLevel.players()) {
                if (player.containerMenu instanceof com.quackers29.businesscraft.menu.TownInterfaceMenu menu) {
                    if (getBlockPos().equals(menu.getBlockPos())) {
                        menu.refreshDataSlots();
                    }
                }
            }
        }
    }

    /**
     * Creates a standardized update tag with all necessary data for client
     * rendering
     */
    @Override
    public CompoundTag getUpdateTag() {
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "[PLATFORM] getUpdateTag called on SERVER");
        CompoundTag tag = super.getUpdateTag();
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }

        String freshTownName = getTownName();
        tag.putString("name", freshTownName != null ? freshTownName : "");

        tag.putInt("searchRadius", getSearchRadius());

        syncResourcesForClient(tag);

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

            if (provider instanceof Town town) {
                clientSyncHelper.syncWantedResourcesForClient(tag, town);
                clientSyncHelper.syncEscrowedResourcesForClient(tag, town);
            }

            clientSyncHelper.syncVisitHistoryForClient(tag, provider, level);
        }
    }

    /**
     * Loads resources from the provided tag into the client-side cache
     * This centralizes our resource deserialization logic in one place
     */
    private void loadResourcesFromTag(CompoundTag tag) {
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "[PLATFORM] loadResourcesFromTag called");
        clientSyncHelper.loadResourcesFromTag(tag);
        clientSyncHelper.loadEscrowedResourcesFromTag(tag);
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

        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setPathStart(pos);
            markDirtyWithRateLimit(provider);
        }

        setChanged();
    }

    public void setPathEnd(BlockPos pos) {
        this.pathEnd = pos;

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
     * 
     * @param pos The position to validate
     * @return true if valid, false if outside boundary
     */
    public boolean isValidPathDistance(BlockPos pos) {
        if (townId == null || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        Town town = TownManager.get(serverLevel).getTown(townId);
        if (town == null) {
            return false;
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
            updateFromTownProvider();

            if (townId != null && level instanceof ServerLevel serverLevel) {
                Town town = TownManager.get(serverLevel).getTown(townId);
                if (town != null) {
                    clientSyncHelper.updateClientResourcesFromTown(town);
                    clientSyncHelper.updateClientResourcesFromTown(town);
                }
            }

            containerData.markAllDirty();

            if (level instanceof ServerLevel) {
                syncTradingViewModelToNearbyPlayers();
            }

            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            setChanged();
        }
    }

    public void setTownId(UUID id) {
        this.townId = id;
        if (level instanceof ServerLevel sLevel1) {
            Town town = TownManager.get(sLevel1).getTown(id);
            this.name = town != null ? town.getName() : "Unnamed";

            bufferManager.setTownId(id);

            syncTownData();
        }
    }

    private void mountTouristsToVehicles() {
        if (level == null || level.isClientSide || town == null)
            return;
        if (pathStart == null || pathEnd == null)
            return;

        int mounted = touristVehicleManager.mountTouristsToVehicles(level, pathStart, pathEnd, searchRadius, townId);
        if (mounted > 0) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "Mounted {} tourists to vehicles for town {}",
                    mounted, name);
        }
    }

    private String formatVec3(Vec3 vec) {
        return String.format("[%.6f, %.6f, %.6f]", vec.x, vec.y, vec.z);
    }

    public int getSearchRadius() {
        int result = searchRadius > 0 ? searchRadius : DEFAULT_SEARCH_RADIUS;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSearchRadiusLogTime > 1000) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "getSearchRadius() field={}, result={}",
                    searchRadius, result);
            lastSearchRadiusLogTime = currentTime;
        }

        return result;
    }

    public void setSearchRadius(int radius) {
        int oldValue = this.searchRadius;
        this.searchRadius = Math.max(1, Math.min(radius, 100)); // Limit between 1-100 blocks
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY, "setSearchRadius() {} -> {}", oldValue,
                this.searchRadius);

        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                town.setSearchRadius(this.searchRadius);
                TownManager.get(sLevel).markDirty();
            }
        }

        if (level != null && !level.isClientSide()) {
            containerData.markDirty("search_radius");
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }

        setChanged();
    }

    public Town getTown() {
        if (townId != null && level instanceof ServerLevel serverLevel) {
            return TownManager.get(serverLevel).getTown(townId);
        }
        return null;
    }

    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;

        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setTouristSpawningEnabled(enabled);
            markDirtyWithRateLimit(provider);
        }

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

        TownManager townManager = TownManager.get(serverLevel);
        Map<UUID, Town> allTowns = townManager.getAllTowns();

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
            this.touristSpawningEnabled = provider.isTouristSpawningEnabled();
            this.pathStart = provider.getPathStart();
            this.pathEnd = provider.getPathEnd();
            this.searchRadius = provider.getSearchRadius();

            if (level != null && !level.isClientSide() && this.townId != null) {
                markDirtyWithRateLimit(provider);
            }
        }
    }

    /**
     * Marks the provider as dirty with rate limiting to reduce excessive updates
     * 
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
        if (level == null || level.isClientSide())
            return;

        Object stackObj = PlatformAccess.getItemHandlers().getStackInSlot(itemHandler, 0);
        if (stackObj instanceof net.minecraft.world.item.ItemStack stack) {
            if (!stack.isEmpty() && townId != null) {
                if (level instanceof ServerLevel sLevel) {
                    if (com.quackers29.businesscraft.util.ContractItemHelper.isContractItem(stack)) {
                        UUID contractId = com.quackers29.businesscraft.util.ContractItemHelper.getContractId(stack);
                        CompoundTag contractData = com.quackers29.businesscraft.util.ContractItemHelper
                                .getContractData(stack);

                        if (contractId != null && contractData != null) {
                            UUID destTownId = contractData.getUUID("destinationTownId");

                            if (townId.equals(destTownId)) {
                                stack.shrink(1);
                                ContractBoard.get(sLevel).processCourierDelivery(contractId, 1L);
                                setChanged();
                                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(),
                                        Block.UPDATE_ALL);
                                return;
                            }
                        }
                    }

                    Town town = TownManager.get(sLevel).getTown(townId);
                    if (town != null) {
                        Item item = stack.getItem();
                        stack.shrink(1);
                        town.addResource(item, 1);

                        syncTownData();
                    }
                }
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        touristVehicleManager.clearTrackedVehicles();
        visitorProcessingHelper.clearAll();
        clientSyncHelper.clearAll();
        platformIndicatorSpawnTimes.clear();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_BLOCK_ENTITY,
                "Cleared visitor position tracking, client caches, and platform indicators on block removal");
    }

    /**
     * Gets the client-side cached resources
     * 
     * @return Map of resources
     */
    public Map<Item, Long> getClientResources() {
        return clientSyncHelper.getClientResources();
    }

    /**
     * Gets the client-side cached communal storage items
     * 
     * @return Map of communal storage items
     */
    public Map<Item, Long> getClientCommunalStorage() {
        return clientSyncHelper.getClientCommunalStorage();
    }

    /**
     * Gets the client-side cached personal storage items for a specific player
     * 
     * @param playerId UUID of the player
     * @return Map of personal storage items for that player
     */
    public Map<Item, Long> getClientPersonalStorage(UUID playerId) {
        return clientSyncHelper.getClientPersonalStorage(playerId);
    }

    /**
     * Updates the client-side personal storage cache for a player
     * 
     * @param playerId UUID of the player
     * @param items    Map of items in the player's personal storage
     */
    public void updateClientPersonalStorage(UUID playerId, Map<Item, Long> items) {
        clientSyncHelper.updateClientPersonalStorage(playerId, items);
    }

    /**
     * Creates the update packet for sending to clients
     * 
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "[PLATFORM] getUpdatePacket called, creating packet");
        Packet<ClientGamePacketListener> packet = ClientboundBlockEntityDataPacket.create(this);
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "[PLATFORM] getUpdatePacket created packet: {}",
                packet != null);
        return packet;
    }

    /**
     * Gets the visit history for client-side display
     */
    public List<VisitHistoryRecord> getVisitHistory() {
        return clientSyncHelper.getVisitHistory(level, getTownDataProvider());
    }

    public String getTownNameFromId(UUID townId) {
        return clientSyncHelper.getTownNameFromId(townId, level);
    }

    /**
     * Gets the list of all platforms
     */
    public List<Platform> getPlatforms() {
        return platformManager.getPlatforms(level != null && level.isClientSide());
    }

    /**
     * Updates client-side platform data from a packet
     * 
     * This is called when receiving platform data from the server
     */
    public void updateClientPlatformsFromPacket(net.minecraft.nbt.CompoundTag tag) {
        if (level != null && level.isClientSide()) {
            platformManager.updateClientPlatforms(tag);
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM,
                    "[PLATFORM] TownInterfaceEntity updated client platforms from packet");
        }
    }

    /**
     * Adds a new platform
     * 
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
     * 
     * @param playerId The UUID of the player who exited the UI
     */
    public void registerPlayerExitUI(UUID playerId) {
        if (level != null) {
            extendedIndicatorPlayers.put(playerId, level.getGameTime());
        }
    }

    /**
     * Called when items are added to town buffer storage externally (e.g., from
     * claim system)
     * Forces a buffer sync to ensure ItemStackHandler reflects the new items
     */
    public void onTownBufferChanged() {
        if (bufferManager != null) {
            bufferManager.onTownBufferChanged();
        }
    }

    /**
     * Get the buffer handler for direct access (used by PaymentBoardMenu)
     * 
     * @return The buffer handler or null if not initialized
     */
    public Object getBufferHandler() {
        return bufferManager != null ? bufferManager.getBufferHandler() : null;
    }

    /**
     * Creates a Payment Board menu provider for proper server-client
     * synchronization
     */
    public MenuProvider createPaymentBoardMenuProvider() {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("Payment Board");
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
                net.minecraft.network.FriendlyByteBuf friendlyBuf = new net.minecraft.network.FriendlyByteBuf(buf);
                friendlyBuf.writeBlockPos(TownInterfaceEntity.this.getBlockPos());

                return new com.quackers29.businesscraft.menu.PaymentBoardMenu(containerId, playerInventory,
                        friendlyBuf);
            }

        };
    }

}
