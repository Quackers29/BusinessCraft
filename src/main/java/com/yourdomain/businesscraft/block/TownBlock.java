package com.yourdomain.businesscraft.block;

import com.yourdomain.businesscraft.block.entity.ModBlockEntities;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Random;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;
import java.util.List;
import net.minecraft.core.Direction;
import com.yourdomain.businesscraft.platform.Platform;

public class TownBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownBlock");

    public TownBlock() {
        super(BlockBehaviour.Properties.copy(Blocks.STONE)
                .strength(3.5F)
                .requiresCorrectToolForDrops());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof TownBlockEntity townBlock) {
                NetworkHooks.openScreen((ServerPlayer) player, townBlock, buf -> {
                    buf.writeBlockPos(pos);
                });
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
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
            LOGGER.info("Setting up town block at position: {}", pos);
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
            );  // Removed .getOpposite() to fix orientation
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