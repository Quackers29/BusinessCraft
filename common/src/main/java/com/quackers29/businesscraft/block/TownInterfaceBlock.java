package com.quackers29.businesscraft.block;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import java.util.Random;
import java.util.UUID;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.particles.ParticleTypes;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class TownInterfaceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceBlock");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private static final long INDICATOR_SPAWN_INTERVAL = 20;
    private static final long EXTENDED_INDICATOR_DURATION = 600;
    private final Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    private final Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public TownInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TownInterfaceEntity townInterface) {
                if (player instanceof ServerPlayer serverPlayer) {
                    PlatformAccess.getNetwork().openScreen(serverPlayer, new MenuProvider() {
                        @Override
                        public Component getDisplayName() {
                            return Component.translatable("block.businesscraft.town_interface");
                        }

                        @Override
                        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
                            return new TownInterfaceMenu(windowId, inventory, pos);
                        }
                    }, pos);
                }
            } else {
                LOGGER.error("Failed to get TownInterfaceEntity at position: {}", pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TownInterfaceEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        BlockEntityTicker<T> baseTicker = createTickerHelper(type,
                (net.minecraft.world.level.block.entity.BlockEntityType<TownInterfaceEntity>) PlatformAccess
                        .getBlockEntities().getTownInterfaceEntityType(),
                (lvl, pos, blockState, blockEntity) -> ((TownInterfaceEntity) blockEntity).tick(lvl, pos, blockState,
                        (TownInterfaceEntity) blockEntity));

        return (lvl, pos, blockState, blockEntity) -> {
            if (baseTicker != null) {
                baseTicker.tick(lvl, pos, blockState, blockEntity);
            }

            if (!lvl.isClientSide() && lvl.getGameTime() % 20 == 0) {
                cleanupPlatformIndicators(pos, lvl);
                spawnPlatformIndicators(lvl, pos);
            }
        };
    }

    private String getRandomTownName() {
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown";
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (!level.isClientSide()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Setting up town interface block at position: {}", pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity townInterface) {
                if (level instanceof ServerLevel serverLevel) {
                    TownManager townManager = TownManager.get(serverLevel);

                    if (!townManager.canPlaceTownAt(pos)) {
                        level.removeBlock(pos, false);
                        if (placer instanceof ServerPlayer player) {
                            String errorMessage = townManager.getTownPlacementError(pos);
                            if (errorMessage == null) {
                                errorMessage = "Town cannot be placed here due to boundary conflicts";
                            }

                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                    "Town cannot be placed here - " + errorMessage), false);
                            if (!player.isCreative()) {
                                player.getInventory().add(stack.copy());
                            }
                        }
                        return;
                    }

                    String newTownName = getRandomTownName();
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Generated town name: {}", newTownName);
                    UUID townId = townManager.registerTown(pos, newTownName);
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Registered new town with ID: {}", townId);
                    townInterface.setTownId(townId);

                    createDefaultPlatform(townInterface, pos, placer);

                    townInterface.setChanged();
                    serverLevel.sendBlockUpdated(pos, state, state, 3);
                }
            } else {
                LOGGER.error("Failed to get TownInterfaceEntity at position: {}", pos);
            }
        }
    }

    private void createDefaultPlatform(TownInterfaceEntity townInterface, BlockPos townPos,
            @Nullable LivingEntity placer) {
        boolean platformAdded = townInterface.addPlatform();

        if (!platformAdded) {
            LOGGER.error("Failed to add default platform for town at {}", townPos);
            return;
        }

        List<Platform> platforms = townInterface.getPlatforms();
        if (platforms.isEmpty()) {
            LOGGER.error("No platforms found after adding default platform for town at {}", townPos);
            return;
        }

        Platform platform = platforms.get(0);

        Direction direction = Direction.NORTH;

        if (placer != null) {
            direction = Direction.getNearest(
                    (float) placer.getLookAngle().x,
                    (float) placer.getLookAngle().y,
                    (float) placer.getLookAngle().z);
        }

        BlockPos platformStart = null;
        BlockPos platformEnd = null;

        switch (direction) {
            case NORTH -> {
                platformStart = townPos.north(3).above(-1);
                platformEnd = townPos.north(5).above(-1);
            }
            case SOUTH -> {
                platformStart = townPos.south(3).above(-1);
                platformEnd = townPos.south(5).above(-1);
            }
            case WEST -> {
                platformStart = townPos.west(3).above(-1);
                platformEnd = townPos.west(5).above(-1);
            }
            case EAST -> {
                platformStart = townPos.east(3).above(-1);
                platformEnd = townPos.east(5).above(-1);
            }
            default -> {
                platformStart = townPos.north(3).above(-1);
                platformEnd = townPos.north(5).above(-1);
            }
        }

        platform.setStartPos(platformStart);
        platform.setEndPos(platformEnd);
        platform.setName("Platform 1");
        platform.setEnabled(true);

        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM,
                "Created default platform for town at {} with start {} and end {}",
                townPos, platformStart, platformEnd);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townInterface) {
                UUID townId = townInterface.getTownId();
                if (townId != null && !level.isClientSide()) {
                    TownManager.get((ServerLevel) level).removeTown(townId);
                }
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    private void spawnPlatformIndicators(Level level, BlockPos pos) {
        if (level.isClientSide)
            return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TownInterfaceEntity townInterface))
            return;

        List<Platform> platforms = townInterface.getPlatforms();
        if (platforms.isEmpty())
            return;

        long gameTime = level.getGameTime();

        boolean showIndicators = false;
        Iterator<Map.Entry<UUID, Long>> iterator = extendedIndicatorPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > EXTENDED_INDICATOR_DURATION) {
                iterator.remove();
                LOGGER.debug("Removed expired indicator for player {}", entry.getKey());
            } else {
                showIndicators = true;
            }
        }

        if (!showIndicators) {
            return;
        }

        for (Platform platform : platforms) {
            if (!platform.isEnabled() || !platform.isComplete())
                continue;

            UUID platformId = platform.getId();

            boolean shouldSpawnIndicators = !platformIndicatorSpawnTimes.containsKey(platformId) ||
                    gameTime - platformIndicatorSpawnTimes.get(platformId) >= INDICATOR_SPAWN_INTERVAL;

            if (shouldSpawnIndicators) {
                platformIndicatorSpawnTimes.put(platformId, gameTime);

                BlockPos startPos = platform.getStartPos();
                BlockPos endPos = platform.getEndPos();

                int startX = startPos.getX();
                int startY = startPos.getY();
                int startZ = startPos.getZ();
                int endX = endPos.getX();
                int endY = endPos.getY();
                int endZ = endPos.getZ();

                int dx = endX - startX;
                int dy = endY - startY;
                int dz = endZ - startZ;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                int steps = (int) Math.max(1, Math.ceil(length));

                for (int i = 0; i <= steps; i++) {
                    double t = (steps == 0) ? 0 : (double) i / steps;
                    double x = startX + dx * t;
                    double y = startY + dy * t + 1.0;
                    double z = startZ + dz * t;

                    ((ServerLevel) level).sendParticles(
                            ParticleTypes.HAPPY_VILLAGER,
                            x + 0.5,
                            y,
                            z + 0.5,
                            5,
                            0.2, 0.2, 0.2,
                            0.0
                    );
                }

                spawnSearchRadiusParticles(
                        (ServerLevel) level,
                        startPos,
                        endPos,
                        townInterface.getSearchRadius());
            }
        }
    }

    /** After a player closes the town UI, show platform path particles for a short time. */
    public void registerPlayerExitUI(UUID playerId, Level level, BlockPos pos) {
        extendedIndicatorPlayers.put(playerId, level.getGameTime());
        spawnPlatformIndicators(level, pos);
    }

    private void cleanupPlatformIndicators(BlockPos blockPos, Level level) {
        BlockEntity be = level.getBlockEntity(blockPos);
        if (!(be instanceof TownInterfaceEntity townInterface))
            return;

        List<Platform> platforms = townInterface.getPlatforms();

        platformIndicatorSpawnTimes.keySet()
                .removeIf(platformId -> platforms.stream().noneMatch(p -> p.getId().equals(platformId)));
    }

    private void spawnSearchRadiusParticles(ServerLevel level, BlockPos startPos, BlockPos endPos, int radius) {
        int startX = startPos.getX();
        int startY = startPos.getY();
        int startZ = startPos.getZ();
        int endX = endPos.getX();
        int endY = endPos.getY();
        int endZ = endPos.getZ();

        int minX = Math.min(startX, endX) - radius;
        int minZ = Math.min(startZ, endZ) - radius;
        int maxX = Math.max(startX, endX) + radius;
        int maxZ = Math.max(startZ, endZ) + radius;

        double particleY = Math.min(startY, endY) + 1.0;

        int perimeterLength = 2 * (maxX - minX + maxZ - minZ);
        int totalPoints = Math.min(200, Math.max(32, perimeterLength / 2));

        int pointsPerSide = totalPoints / 4;

        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double x = minX + t * (maxX - minX);
            level.sendParticles(
                    ParticleTypes.FLAME,
                    x,
                    particleY,
                    minZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }

        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double z = minZ + t * (maxZ - minZ);
            level.sendParticles(
                    ParticleTypes.FLAME,
                    maxX,
                    particleY,
                    z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }

        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double x = maxX - t * (maxX - minX);
            level.sendParticles(
                    ParticleTypes.FLAME,
                    x,
                    particleY,
                    maxZ,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }

        for (int i = 0; i < pointsPerSide; i++) {
            double t = (double) i / (pointsPerSide - 1);
            double z = maxZ - t * (maxZ - minZ);
            level.sendParticles(
                    ParticleTypes.FLAME,
                    minX,
                    particleY,
                    z,
                    1,
                    0.0, 0.0, 0.0,
                    0.0);
        }
    }
}
