package com.yourdomain.businesscraft.block;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * The Town Interface Block provides a modern UI for managing town settings.
 * It showcases the BusinessCraft UI system capabilities.
 */
public class TownInterfaceBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // Block shape - a slightly smaller than full block
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public TownInterfaceBlock(Properties properties) {
        super(properties);
        // Set default facing direction
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
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
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide()) {
            // Open the menu container for this block
            NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.translatable("block.businesscraft.town_interface");
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
                    return new TownInterfaceMenu(windowId, inventory, pos);
                }
            }, pos);
            return InteractionResult.SUCCESS;
        }
        
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
} 