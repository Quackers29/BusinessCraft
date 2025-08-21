package com.quackers29.businesscraft.block;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.fabric.init.FabricModBlocks;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import com.quackers29.businesscraft.block.entity.FabricTownInterfaceEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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

import java.util.Random;
import java.util.UUID;
import java.util.List;

/**
 * Fabric implementation of TownInterfaceBlock.
 * Delegates business logic to common modules while handling Fabric-specific concerns.
 * 
 * Bridge Pattern: This class acts as a bridge between Fabric platform specifics
 * and the common business logic in TownManager and related classes.
 */
public class TownInterfaceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/FabricTownInterfaceBlock");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // Block shape - a slightly smaller than full block (same as Forge implementation)
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public TownInterfaceBlock(Properties properties) {
        super(properties);
        // Set default facing direction
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
            // Get the block entity to ensure all town data is accessible
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof FabricTownInterfaceEntity townInterface) {
                // For now, provide basic feedback that the block works
                // TODO: Implement Fabric menu opening once menu system is ready
                player.displayClientMessage(Component.literal(
                    "Fabric Town Interface - Town: " + townInterface.getTownName() + 
                    " (ID: " + townInterface.getTownId() + ")"
                ), false);
                
                LOGGER.info("Fabric TownInterfaceBlock used by player {} at position {}", 
                    player.getName().getString(), pos);
            } else {
                LOGGER.error("Failed to get FabricTownInterfaceEntity at position: {}", pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FabricTownInterfaceEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        // Use the same ticker pattern as Forge but with Fabric block entity
        return createTickerHelper(type, FabricModBlockEntities.TOWN_INTERFACE_ENTITY,
                (lvl, pos, blockState, blockEntity) -> 
                    ((FabricTownInterfaceEntity) blockEntity).tick(lvl, pos, blockState));
    }

    private String getRandomTownName() {
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        
        if (!level.isClientSide()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Setting up Fabric town interface block at position: {}", pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof FabricTownInterfaceEntity townInterface) {
                if (level instanceof ServerLevel serverLevel) {
                    // Delegate town creation to common TownManager
                    TownManager townManager = TownManager.get(serverLevel);
                    
                    // Check if town can be placed here (using common business logic)
                    if (!townManager.canPlaceTownAt(pos.getX(), pos.getY(), pos.getZ())) {
                        // Town can't be placed here, delete the block and notify player
                        level.removeBlock(pos, false);
                        if (placer instanceof ServerPlayer player) {
                            String errorMessage = townManager.getTownPlacementError(pos.getX(), pos.getY(), pos.getZ());
                            if (errorMessage == null) {
                                errorMessage = "Town cannot be placed here due to boundary conflicts";
                            }
                            
                            player.displayClientMessage(Component.literal(
                                "Town cannot be placed here - " + errorMessage), false);
                            // Return the block to the player's inventory
                            if (!player.isCreative()) {
                                player.getInventory().add(stack.copy());
                            }
                        }
                        return;
                    }
                    
                    // Use common business logic for town creation
                    String newTownName = getRandomTownName();
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Generated town name: {}", newTownName);
                    Town createdTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), newTownName);
                    UUID townId = createdTown != null ? createdTown.getId() : null;
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Created new town with ID: {}", townId);
                    
                    // Set town ID in the block entity
                    townInterface.setTownId(townId);
                    townInterface.setTownName(newTownName);
                    
                    // Create default platform layout (delegate to block entity)
                    townInterface.createDefaultPlatform(pos, state, placer);
                    
                    townInterface.setChanged();
                    serverLevel.sendBlockUpdated(pos, state, state, 3);
                }
            } else {
                LOGGER.error("Failed to get FabricTownInterfaceEntity at position: {}", pos);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof FabricTownInterfaceEntity townInterface) {
                UUID townId = townInterface.getTownId();
                if (townId != null && !level.isClientSide()) {
                    // Delegate town removal to common TownManager
                    TownManager.get((ServerLevel) level).removeTown(townId);
                }
            }
            // Call super after our logic
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}