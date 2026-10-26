package com.quackers29.businesscraft.block;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.PlatformPlacement;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A platform inside an existing town. Placement fails outside a Town Centre boundary, at the platform cap,
 * or too close to another Platform Interface. Breaking it removes that platform from the town.
 */
public class PlatformInterfaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    /** Sign lamp. On while the interface is linked and its platform is enabled. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    /** Cabinet, ticket tray, roof, and sign lamp. North is the unrotated model, front toward -Z. */
    private static final VoxelShape NORTH_SHAPE = Shapes.or(
            Block.box(2, 0, 1, 14, 13.8, 14),
            Block.box(4.5, 13.8, 7, 11.5, 16, 12));
    private static final Map<Direction, VoxelShape> SHAPES = facingShapes(NORTH_SHAPE);

    public PlatformInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.getOrDefault(state.getValue(FACING), NORTH_SHAPE);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static Map<Direction, VoxelShape> facingShapes(VoxelShape north) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, north);
        shapes.put(Direction.EAST, rotateY(north, 1));
        shapes.put(Direction.SOUTH, rotateY(north, 2));
        shapes.put(Direction.WEST, rotateY(north, 3));
        return shapes;
    }

    /** Clockwise quarters around Y, matching the blockstate {@code y} rotation. */
    private static VoxelShape rotateY(VoxelShape shape, int quarters) {
        VoxelShape result = shape;
        for (int i = 0; i < quarters; i++) {
            VoxelShape turned = Shapes.empty();
            for (AABB box : result.toAabbs()) {
                double x1 = box.minX * 16.0;
                double y1 = box.minY * 16.0;
                double z1 = box.minZ * 16.0;
                double x2 = box.maxX * 16.0;
                double y2 = box.maxY * 16.0;
                double z2 = box.maxZ * 16.0;
                double nx1 = 16.0 - z2;
                double nz1 = x1;
                double nx2 = 16.0 - z1;
                double nz2 = x2;
                turned = Shapes.or(turned, Block.box(
                        Math.min(nx1, nx2), y1, Math.min(nz1, nz2),
                        Math.max(nx1, nx2), y2, Math.max(nz1, nz2)));
            }
            result = turned;
        }
        return result;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlatformInterfaceEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, platformInterfaceType(),
                (lvl, pos, blockState, blockEntity) -> blockEntity.tickServer());
    }

    @SuppressWarnings("unchecked")
    private static BlockEntityType<PlatformInterfaceEntity> platformInterfaceType() {
        return (BlockEntityType<PlatformInterfaceEntity>) PlatformAccess.getBlockEntities()
                .getPlatformInterfaceEntityType();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PlatformInterfaceEntity entity)) {
            return;
        }

        PlatformPlacement.Decision decision = PlatformPlacement.evaluate(pos, candidates(serverLevel, pos));
        if (!decision.accepted()) {
            reject(level, pos, placer, stack,
                    decision.reject() == null
                            ? PlatformPlacement.Reject.OUTSIDE.translationKey()
                            : decision.reject().translationKey());
            return;
        }

        level.getChunkAt(decision.townCentre());
        if (!(level.getBlockEntity(decision.townCentre()) instanceof TownInterfaceEntity town)) {
            reject(level, pos, placer, stack, PlatformPlacement.Reject.OUTSIDE.translationKey());
            return;
        }
        UUID platformId = town.addInterfacePlatform(pos);
        if (platformId == null) {
            String key = town.canAddMorePlatforms()
                    ? PlatformPlacement.Reject.TOO_CLOSE.translationKey()
                    : PlatformPlacement.Reject.AT_CAP.translationKey();
            reject(level, pos, placer, stack, key);
            return;
        }
        entity.link(decision.townId(), platformId, decision.townCentre());
        town.syncPlatforms();
    }

    private static List<PlatformPlacement.TownCandidate> candidates(ServerLevel level, BlockPos pos) {
        List<PlatformPlacement.TownCandidate> list = new ArrayList<>();
        for (Town town : TownManager.get(level).getAllTowns().values()) {
            if (town == null || town.getPosition() == null) {
                continue;
            }
            double distance = Math.sqrt(pos.distSqr(town.getPosition()));
            if (distance > town.getBoundaryRadius()) {
                continue;
            }
            level.getChunkAt(town.getPosition());
            int count = 0;
            List<BlockPos> interfaces = List.of();
            if (level.getBlockEntity(town.getPosition()) instanceof TownInterfaceEntity townEntity) {
                count = townEntity.getPlatformCount();
                interfaces = townEntity.getPlatformInterfacePositions();
            }
            list.add(new PlatformPlacement.TownCandidate(
                    town.getId(), town.getPosition(), town.getBoundaryRadius(), count, interfaces));
        }
        return list;
    }

    private static void reject(Level level, BlockPos pos, @Nullable LivingEntity placer, ItemStack stack, String key) {
        level.removeBlock(pos, false);
        if (placer instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable(key), false);
            if (!player.isCreative() && !stack.isEmpty()) {
                // Placement shrinks the hand stack after setPlacedBy returns, so grow it back by one.
                stack.grow(1);
            }
        }
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PlatformInterfaceEntity entity) {
            if (!entity.isActive()) {
                String reason = entity.getInactiveReason() == null
                        ? PlatformInterfaceEntity.REASON_NOT_LINKED
                        : entity.getInactiveReason();
                serverPlayer.displayClientMessage(Component.translatable(
                        "businesscraft.platform_interface.inactive", Component.translatable(reason)), false);
            }
            PlatformAccess.getNetwork().openScreen(serverPlayer, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.businesscraft.platform_interface");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player opener) {
                    return new com.quackers29.businesscraft.menu.PlatformInterfaceMenu(windowId, inventory, pos);
                }
            }, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel
                    && level.getBlockEntity(pos) instanceof PlatformInterfaceEntity entity) {
                entity.onBroken(serverLevel);
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}
