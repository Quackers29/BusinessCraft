package com.yourdomain.businesscraft.block;

import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
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
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import java.util.UUID;
import java.util.List;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The Town Interface Block provides a modern UI for managing town settings.
 * It showcases the BusinessCraft UI system capabilities.
 */
public class TownInterfaceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceBlock");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // Block shape - a slightly smaller than full block
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
            if (entity instanceof TownBlockEntity townBlock) {
                // Open the TownInterfaceScreen instead of TownBlockScreen
                NetworkHooks.openScreen((ServerPlayer) player, new MenuProvider() {
                    @Override
                    public Component getDisplayName() {
                        return Component.translatable("block.businesscraft.town_interface");
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
                        // Create the TownInterfaceMenu using the town's position
                        return new TownInterfaceMenu(windowId, inventory, pos);
                    }
                }, pos);
            } else {
                LOGGER.error("Failed to get TownBlockEntity at position: {}", pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TownBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.TOWN_BLOCK_ENTITY.get(),
                (lvl, pos, blockState, blockEntity) -> ((TownBlockEntity) blockEntity).tick(lvl, pos, blockState,
                        (TownBlockEntity) blockEntity));
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
            LOGGER.info("Setting up town interface block at position: {}", pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownBlockEntity townBlock) {
                if (level instanceof ServerLevel serverLevel) {
                    TownManager townManager = TownManager.get(serverLevel);
                    
                    // Check if town can be placed here (minimum distance check)
                    if (!townManager.canPlaceTownAt(pos)) {
                        // Town can't be placed here, delete the block and notify player
                        level.removeBlock(pos, false);
                        if (placer instanceof ServerPlayer player) {
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                                "Town cannot be placed here - too close to another town (min distance: " + 
                                ConfigLoader.minDistanceBetweenTowns + " blocks)"), false);
                            // Return the block to the player's inventory
                            if (!player.isCreative()) {
                                player.getInventory().add(stack.copy());
                            }
                        }
                        return;
                    }
                    
                    String newTownName = getRandomTownName();
                    LOGGER.info("Generated town name: {}", newTownName);
                    UUID townId = townManager.registerTown(pos, newTownName);
                    LOGGER.info("Registered new town with ID: {}", townId);
                    townBlock.setTownId(townId);
                    
                    // Create default platform layout
                    createDefaultPlatform(townBlock, pos, state.getBlock().defaultBlockState(), placer);
                    
                    townBlock.setChanged();
                    serverLevel.sendBlockUpdated(pos, state, state, 3);
                }
            } else {
                LOGGER.error("Failed to get TownBlockEntity at position: {}", pos);
            }
        }
    }

    /**
     * Creates a default platform layout for a new town
     * 
     * @param townBlock The town block entity
     * @param townPos The position of the town block
     * @param state The block state of the town block
     * @param placer The entity that placed the town block
     */
    private void createDefaultPlatform(TownBlockEntity townBlock, BlockPos townPos, BlockState state, @Nullable LivingEntity placer) {
        // Add the default platform
        boolean platformAdded = townBlock.addPlatform();
        
        if (!platformAdded) {
            LOGGER.error("Failed to add default platform for town at {}", townPos);
            return;
        }
        
        // Get the newly added platform (it should be the only one)
        List<Platform> platforms = townBlock.getPlatforms();
        if (platforms.isEmpty()) {
            LOGGER.error("No platforms found after adding default platform for town at {}", townPos);
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
        // Start 3 blocks in the direction the player is facing
        // with the pattern "X X X X X O O T" where X is closest to the player
        
        BlockPos platformStart = null;
        BlockPos platformEnd = null;
        
        // Calculate platform start and end positions based on the direction
        // Adjust Y level to be at the same level as the town block
        switch (direction) {
            case NORTH -> {
                // Platform extends north (negative Z)
                platformStart = townPos.north(3).above(-1);  // Closer to player
                platformEnd = townPos.north(5).above(-1);    // Further from player
            }
            case SOUTH -> {
                // Platform extends south (positive Z)
                platformStart = townPos.south(3).above(-1);
                platformEnd = townPos.south(5).above(-1);
            }
            case WEST -> {
                // Platform extends west (negative X)
                platformStart = townPos.west(3).above(-1);
                platformEnd = townPos.west(5).above(-1);
            }
            case EAST -> {
                // Platform extends east (positive X)
                platformStart = townPos.east(3).above(-1);
                platformEnd = townPos.east(5).above(-1);
            }
            default -> {
                // Use north as fallback
                platformStart = townPos.north(3).above(-1);
                platformEnd = townPos.north(5).above(-1);
            }
        }
        
        // Set the platform start and end points
        platform.setStartPos(platformStart);
        platform.setEndPos(platformEnd);
        platform.setName("Platform 1");
        platform.setEnabled(true);
        
        LOGGER.info("Created default platform for town at {} with start {} and end {}", 
            townPos, platformStart, platformEnd);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townBlock) {
                UUID townId = townBlock.getTownId();
                if (townId != null && !level.isClientSide()) {
                    // Remove town from manager
                    TownManager.get((ServerLevel) level).removeTown(townId);
                }
            }
            // Call super after our logic
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
} 