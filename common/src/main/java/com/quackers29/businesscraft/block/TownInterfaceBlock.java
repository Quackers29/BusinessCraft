package com.quackers29.businesscraft.block;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.UUID;

/**
 * UNIFIED ARCHITECTURE: Town Interface Block
 * Works on both Forge and Fabric using platform services for the few platform differences.
 */
public class TownInterfaceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceBlock");
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);

    public TownInterfaceBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
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
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Setting up town interface block at position: {}", pos);
            
            TownManager townManager = TownManager.get(serverLevel);
            
            // UNIFIED ARCHITECTURE: Proper town placement validation
            if (!townManager.canPlaceTownAt(pos.getX(), pos.getY(), pos.getZ())) {
                // Town can't be placed here, remove block and notify player
                level.removeBlock(pos, false);
                if (placer instanceof net.minecraft.server.level.ServerPlayer player) {
                    String errorMessage = townManager.getTownPlacementError(pos.getX(), pos.getY(), pos.getZ());
                    if (errorMessage == null) {
                        errorMessage = "Town cannot be placed here due to boundary conflicts";
                    }
                    
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Town cannot be placed here - " + errorMessage), false);
                    
                    // Return the block to the player's inventory  
                    if (!player.isCreative()) {
                        player.getInventory().add(stack.copy());
                    }
                }
                return;
            }
            
            // Generate a random town name
            String townName = generateRandomTownName();
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Generated town name: {}", townName);
            
            // Create a new town at this position using correct API
            Town newTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), townName);
            if (newTown == null) {
                LOGGER.error("Failed to create town at position: {}", pos);
                // This shouldn't happen since we validated placement above
                level.removeBlock(pos, false);
                return;
            }
            
            UUID townId = newTown.getId();
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Created new town with ID: {}", townId);
            
            // CRITICAL FIX: Associate the town ID with the block entity
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity != null) {
                // Use reflection to call setTownId on the platform-specific entity
                try {
                    entity.getClass().getMethod("setTownId", UUID.class).invoke(entity, townId);
                    entity.setChanged(); // Mark as dirty for saving
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Associated town ID {} with entity at {}", townId, pos);
                } catch (Exception e) {
                    LOGGER.error("Failed to associate town ID with entity", e);
                }
            }
            
            // Town is successfully created and managed by TownManager - unified architecture success!
        }
        
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            // UNIFIED ARCHITECTURE: Use platform services for menu opening
            boolean success = PlatformServices.getMenuHelper().openTownInterfaceMenu(player, 
                new int[]{pos.getX(), pos.getY(), pos.getZ()}, "Town Interface");
                
            if (!success) {
                LOGGER.error("Failed to open Town Interface menu at position: {}", pos);
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        // PLATFORM CONDITIONAL: Use platform services for block entity creation
        String platformName = PlatformServices.getPlatformHelper().getPlatformName();
        
        if ("forge".equals(platformName)) {
            // Forge: Use Forge block entity
            try {
                // Use reflection to create Forge TownInterfaceEntity
                Class<?> entityClass = Class.forName("com.quackers29.businesscraft.block.entity.TownInterfaceEntity");
                return (BlockEntity) entityClass.getConstructor(BlockPos.class, BlockState.class)
                    .newInstance(pos, state);
            } catch (Exception e) {
                LOGGER.error("Failed to create Forge TownInterfaceEntity", e);
            }
        } else if ("fabric".equals(platformName)) {
            // Fabric: Use Fabric block entity (when implemented)
            // TODO: Create Fabric TownInterfaceEntity equivalent
            LOGGER.debug("Fabric TownInterfaceEntity not yet implemented");
        }
        
        return null; // Fallback
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        // Platform services will handle ticking through the block entity
        return null;
    }

    /**
     * Generate a random town name for variety
     */
    private String generateRandomTownName() {
        String[] prefixes = {"Stone", "Oak", "Pine", "River", "Hill", "Green", "Blue", "Golden", "Silver", "Iron"};
        String[] suffixes = {"bridge", "town", "ville", "burg", "haven", "ford", "field", "wood", "valley", "ridge"};
        
        Random random = new Random();
        String prefix = prefixes[random.nextInt(prefixes.length)];
        String suffix = suffixes[random.nextInt(suffixes.length)];
        
        return prefix + suffix;
    }
}