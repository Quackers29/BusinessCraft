package com.quackers29.businesscraft.block;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformInterfaceService;
import com.quackers29.businesscraft.platform.PlatformLink;
import com.quackers29.businesscraft.platform.PlatformLinkResolver;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A Town Interface placed beside one tourist platform. It does not create or
 * own a town. Right-click opens that platform's screen.
 * <p>
 * Registry id: {@code businesscraft:platform_interface}. The town-defining
 * block keeps {@code businesscraft:town_interface}.
 */
public class PlatformInterfaceBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    private static final VoxelShape SHAPE = Shapes.block();

    public PlatformInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlatformInterfaceEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type,
                (BlockEntityType<PlatformInterfaceEntity>) PlatformAccess.getBlockEntities()
                        .getPlatformInterfaceEntityType(),
                (lvl, pos, blockState, blockEntity) -> {
                    if (lvl.isClientSide() || lvl.getGameTime() % 40 != 0) {
                        return;
                    }
                    if (lvl instanceof ServerLevel serverLevel && blockEntity instanceof PlatformInterfaceEntity) {
                        PlatformInterfaceService.refreshActiveFlag(serverLevel, pos, false);
                    }
                });
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(level.getBlockEntity(pos) instanceof PlatformInterfaceEntity kiosk)) {
            return;
        }

        PlatformLink.Choice choice = PlatformLinkResolver.choose(serverLevel, pos);
        if (choice.status() != PlatformLink.Status.LINKED) {
            reject(serverLevel, pos, placer, stack, messageFor(choice.status()));
            return;
        }

        Town town = TownManager.get(serverLevel).getTown(choice.townId());
        String townName = town != null ? town.getName() : "";
        String platformName = platformName(serverLevel, choice);
        kiosk.setLink(choice.townId(), choice.platformId(), townName, platformName);
        if (!state.getValue(ACTIVE)) {
            serverLevel.setBlock(pos, state.setValue(ACTIVE, true), 3);
        }
    }

    private static String platformName(ServerLevel level, PlatformLink.Choice choice) {
        Town town = TownManager.get(level).getTown(choice.townId());
        if (town == null || town.getPosition() == null) {
            return "";
        }
        BlockEntity blockEntity = level.getBlockEntity(town.getPosition());
        if (!(blockEntity instanceof TownInterfaceEntity centre)) {
            return "";
        }
        Platform platform = centre.getPlatform(choice.platformId());
        return platform != null ? platform.getName() : "";
    }

    private static void reject(ServerLevel level, BlockPos pos, @Nullable LivingEntity placer, ItemStack stack,
            Component message) {
        level.removeBlock(pos, false);
        if (placer instanceof ServerPlayer player) {
            player.displayClientMessage(message, false);
            if (!player.isCreative()) {
                player.getInventory().add(stack.copy());
            }
        }
    }

    private static Component messageFor(PlatformLink.Status status) {
        if (status == PlatformLink.Status.AMBIGUOUS) {
            return Component.translatable("gui.businesscraft.platform_interface.ambiguous")
                    .withStyle(ChatFormatting.RED);
        }
        return Component.translatable("gui.businesscraft.platform_interface.no_stop", PlatformLink.HORIZONTAL_RADIUS)
                .withStyle(ChatFormatting.RED);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PlatformInterfaceService.open(serverPlayer, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
