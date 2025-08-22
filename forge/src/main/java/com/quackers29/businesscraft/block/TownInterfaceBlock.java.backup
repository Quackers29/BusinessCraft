package com.quackers29.businesscraft.block;

import com.quackers29.businesscraft.init.ModBlockEntities;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
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
import net.minecraftforge.network.NetworkHooks;
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

/**
 * The Town Interface Block provides a modern UI for managing town settings.
 * It showcases the BusinessCraft UI system capabilities.
 */
public class TownInterfaceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceBlock");
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // Add fields for platform visualization
    private static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks
    private static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks
    private final Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    private final Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();
    
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
            if (entity instanceof TownInterfaceEntity townInterface) {
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
        // First use the default ticker for the TownInterfaceEntity
        BlockEntityTicker<T> baseTicker = createTickerHelper(type, ModBlockEntities.TOWN_INTERFACE_ENTITY.get(),
                (lvl, pos, blockState, blockEntity) -> ((TownInterfaceEntity) blockEntity).tick(lvl, pos, blockState,
                        (TownInterfaceEntity) blockEntity));
        
        // Then add our own ticker that also handles platform visualization
        return (lvl, pos, blockState, blockEntity) -> {
            // First call the base ticker
            if (baseTicker != null) {
                baseTicker.tick(lvl, pos, blockState, blockEntity);
            }
            
            // Then handle our own logic for platform visualization
            if (!lvl.isClientSide() && lvl.getGameTime() % 20 == 0) { // Every 1 second
                // Clean up platform indicators
                cleanupPlatformIndicators(pos, lvl);
                
                // Spawn platform indicators
                spawnPlatformIndicators(lvl, pos);
            }
        };
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
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Setting up town interface block at position: {}", pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof TownInterfaceEntity townInterface) {
                if (level instanceof ServerLevel serverLevel) {
                    TownManager townManager = TownManager.get(serverLevel);
                    
                    // Check if town can be placed here (dynamic boundary distance check)
                    if (!townManager.canPlaceTownAt(pos.getX(), pos.getY(), pos.getZ())) {
                        // Town can't be placed here, delete the block and notify player
                        level.removeBlock(pos, false);
                        if (placer instanceof ServerPlayer player) {
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
                    
                    String newTownName = getRandomTownName();
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Generated town name: {}", newTownName);
                    Town createdTown = townManager.createTown(pos.getX(), pos.getY(), pos.getZ(), newTownName);
                    UUID townId = createdTown != null ? createdTown.getId() : null;
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_MANAGER, "Created new town with ID: {}", townId);
                    townInterface.setTownId(townId);
                    
                    // Create default platform layout
                    createDefaultPlatform(townInterface, pos, state.getBlock().defaultBlockState(), placer);
                    
                    townInterface.setChanged();
                    serverLevel.sendBlockUpdated(pos, state, state, 3);
                }
            } else {
                LOGGER.error("Failed to get TownInterfaceEntity at position: {}", pos);
            }
        }
    }

    /**
     * Creates a default platform layout for a new town
     * 
     * @param townInterface The town interface entity
     * @param townPos The position of the town block
     * @param state The block state of the town block
     * @param placer The entity that placed the town block
     */
    private void createDefaultPlatform(TownInterfaceEntity townInterface, BlockPos townPos, BlockState state, @Nullable LivingEntity placer) {
        // Add the default platform
        boolean platformAdded = townInterface.addPlatform();
        
        if (!platformAdded) {
            LOGGER.error("Failed to add default platform for town at {}", townPos);
            return;
        }
        
        // Get the newly added platform (it should be the only one)
        List<Platform> platforms = townInterface.getPlatforms();
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
        
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Created default platform for town at {} with start {} and end {}", 
            townPos, platformStart, platformEnd);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof TownInterfaceEntity townInterface) {
                UUID townId = townInterface.getTownId();
                if (townId != null && !level.isClientSide()) {
                    // Remove town from manager
                    TownManager.get((ServerLevel) level).removeTown(townId);
                }
            }
            // Call super after our logic
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    /**
     * Spawns visual indicators at platform start and end points and along the line
     */
    private void spawnPlatformIndicators(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        
        // Get the block entity
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TownInterfaceEntity townInterface)) return;
        
        List<Platform> platforms = townInterface.getPlatforms();
        if (platforms.isEmpty()) return;
        
        long gameTime = level.getGameTime();
        
        // Check if any players have recently exited the UI
        boolean showIndicators = false;
        Iterator<Map.Entry<UUID, Long>> iterator = extendedIndicatorPlayers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (gameTime - entry.getValue() > EXTENDED_INDICATOR_DURATION) {
                // Remove expired entries
                iterator.remove();
                LOGGER.debug("Removed expired indicator for player {}", entry.getKey());
            } else {
                showIndicators = true;
            }
        }
        
        // Only show particles if players have recently exited the UI
        if (!showIndicators) {
            return; // Exit early if no recent UI exits
        }
        
        // For each platform, spawn particles
        for (Platform platform : platforms) {
            if (!platform.isEnabled() || !platform.isComplete()) continue;
            
            UUID platformId = platform.getId();
            
            // Check if it's time to spawn indicators for this platform
            boolean shouldSpawnIndicators = 
                !platformIndicatorSpawnTimes.containsKey(platformId) || 
                gameTime - platformIndicatorSpawnTimes.get(platformId) >= INDICATOR_SPAWN_INTERVAL;
                
            if (shouldSpawnIndicators) {
                // Update spawn time
                platformIndicatorSpawnTimes.put(platformId, gameTime);
                
                BlockPos startPos = platform.getStartPos();
                BlockPos endPos = platform.getEndPos();
                
                // Calculate the path between start and end
                int startX = startPos.getX();
                int startY = startPos.getY();
                int startZ = startPos.getZ();
                int endX = endPos.getX();
                int endY = endPos.getY();
                int endZ = endPos.getZ();
                
                // Calculate line length for parameter
                int dx = endX - startX;
                int dy = endY - startY;
                int dz = endZ - startZ;
                double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
                int steps = (int) Math.max(1, Math.ceil(length));
                
                // Spawn particles along the line, one per block
                for (int i = 0; i <= steps; i++) {
                    double t = (steps == 0) ? 0 : (double) i / steps;
                    double x = startX + dx * t;
                    double y = startY + dy * t + 1.0; // +1.0 to place above the ground
                    double z = startZ + dz * t;
                    
                    ((ServerLevel)level).sendParticles(
                        ParticleTypes.HAPPY_VILLAGER,
                        x + 0.5,
                        y,
                        z + 0.5,
                        5, // particle count (increased from 1 to 5)
                        0.2, 0.2, 0.2, // spread
                        0.0 // speed
                    );
                }
                
                // Add red particles to show search radius
                spawnSearchRadiusParticles(
                    (ServerLevel)level,
                    startPos,
                    endPos,
                    townInterface.getSearchRadius()
                );
            }
        }
    }
    
    /**
     * Registers a player as having exited the town UI, enabling extended indicators
     * @param playerId The UUID of the player who exited the UI
     * @param level The level containing the block
     * @param pos The position of the block
     */
    public void registerPlayerExitUI(UUID playerId, Level level, BlockPos pos) {
        extendedIndicatorPlayers.put(playerId, level.getGameTime());
        
        // Spawn immediate indicators
        spawnPlatformIndicators(level, pos);
    }
    
    /**
     * Clean up indicator data for removed platforms
     * @param blockPos The position of the block
     * @param level The level containing the block
     */
    private void cleanupPlatformIndicators(BlockPos blockPos, Level level) {
        BlockEntity be = level.getBlockEntity(blockPos);
        if (!(be instanceof TownInterfaceEntity townInterface)) return;
        
        List<Platform> platforms = townInterface.getPlatforms();
        
        // Remove spawn times for platforms that no longer exist
        platformIndicatorSpawnTimes.keySet().removeIf(platformId -> 
            platforms.stream().noneMatch(p -> p.getId().equals(platformId))
        );
    }

    /**
     * Spawns red particles to show search radius around platform line
     * 
     * @param level The server level
     * @param startPos Platform start position
     * @param endPos Platform end position
     * @param radius Search radius
     */
    private void spawnSearchRadiusParticles(ServerLevel level, BlockPos startPos, BlockPos endPos, int radius) {
        // Calculate the path between start and end
        int startX = startPos.getX();
        int startY = startPos.getY();
        int startZ = startPos.getZ();
        int endX = endPos.getX();
        int endY = endPos.getY();
        int endZ = endPos.getZ();
        
        // Calculate the bounding box the same way it's used for entity search
        int minX = Math.min(startX, endX) - radius;
        int minZ = Math.min(startZ, endZ) - radius;
        int maxX = Math.max(startX, endX) + radius;
        int maxZ = Math.max(startZ, endZ) + radius;
        
        // Use a fixed Y for visualization
        double particleY = Math.min(startY, endY) + 1.0;
        
        // Calculate perimeter length to determine number of particles
        int perimeterLength = 2 * (maxX - minX + maxZ - minZ);
        int totalPoints = Math.min(200, Math.max(32, perimeterLength / 2));
        
        // Distribute points evenly across the 4 sides of the perimeter
        int pointsPerSide = totalPoints / 4;
        
        // Generate particles along the perimeter
        
        // Bottom edge (minX to maxX at minZ)
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
                0.0
            );
        }
        
        // Right edge (maxX, minZ to maxZ)
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
                0.0
            );
        }
        
        // Top edge (maxX to minX at maxZ)
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
                0.0
            );
        }
        
        // Left edge (minX, maxZ to minZ)
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
                0.0
            );
        }
    }
} 