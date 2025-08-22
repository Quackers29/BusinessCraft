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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/**
 * UNIFIED ARCHITECTURE: Town Interface Block
 * Works on both Forge and Fabric using platform services for the few platform differences.
 */
public class TownInterfaceBlock extends BaseEntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceBlock");
    
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    
    // RESTORED: Custom VoxelShape from main branch (not full block)
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);
    
    // RESTORED: Platform visualization fields from main branch
    private static final long INDICATOR_SPAWN_INTERVAL = 20; // 1 second in ticks
    private static final long EXTENDED_INDICATOR_DURATION = 600; // 30 seconds in ticks
    private final Map<UUID, Long> platformIndicatorSpawnTimes = new HashMap<>();
    private final Map<UUID, Long> extendedIndicatorPlayers = new HashMap<>();

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
                    
                    // CRITICAL FIX: Create default platform using reflection (preserves main branch behavior)
                    boolean platformAdded = (Boolean) entity.getClass().getMethod("addPlatform").invoke(entity);
                    if (platformAdded) {
                        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Created default platform for unified town at {}", pos);
                        
                        // CRITICAL FIX: Set platform path coordinates (main branch functionality)
                        setupDefaultPlatformPath(entity, pos, placer);
                        
                    } else {
                        LOGGER.error("Failed to add default platform for unified town at {}", pos);
                    }
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to associate town ID with entity or create platform", e);
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
        // RESTORED: Advanced block ticker from main branch with platform visualization
        return (lvl, pos, blockState, blockEntity) -> {
            // First call the base entity ticker if it exists
            try {
                // Use reflection to call tick method on the block entity
                blockEntity.getClass().getMethod("tick", Level.class, BlockPos.class, BlockState.class, blockEntity.getClass())
                    .invoke(blockEntity, lvl, pos, blockState, blockEntity);
            } catch (Exception e) {
                // Ignore if tick method doesn't exist - not all entities have it
            }
            
            // RESTORED: Platform visualization logic from main branch
            if (!lvl.isClientSide() && lvl.getGameTime() % 20 == 0) { // Every 1 second
                // Clean up platform indicators
                cleanupPlatformIndicators(pos, lvl);
                
                // Spawn platform indicators
                spawnPlatformIndicators(lvl, pos);
            }
        };
    }

    /**
     * RESTORED: Random town name generation from main branch using ConfigLoader
     */
    private String generateRandomTownName() {
        // RESTORED: Use ConfigLoader.townNames like main branch for consistency
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }
    
    /**
     * RESTORED: Sophisticated platform creation logic from main branch
     * Creates a default platform layout for a new town with proper direction-based positioning
     */
    private void setupDefaultPlatformPath(BlockEntity entity, BlockPos townPos, @Nullable LivingEntity placer) {
        try {
            // Get the platforms list using reflection
            @SuppressWarnings("unchecked")
            java.util.List<Object> platforms = (java.util.List<Object>) entity.getClass().getMethod("getPlatforms").invoke(entity);
            
            if (platforms.isEmpty()) {
                LOGGER.error("No platforms found after adding default platform for town at {}", townPos);
                return;
            }
            
            Object platform = platforms.get(0); // Get the first (default) platform
            
            // RESTORED: Sophisticated direction calculation from main branch
            Direction direction = Direction.NORTH; // Default direction
            
            if (placer != null) {
                // Use the exact logic from main branch for direction calculation
                direction = Direction.getNearest(
                    (float) placer.getLookAngle().x,
                    (float) placer.getLookAngle().y,
                    (float) placer.getLookAngle().z
                );
            }
            
            // RESTORED: Exact platform positioning from main branch
            // Create the default platform layout based on the orientation
            // Start 3 blocks in the direction the player is facing
            // with the pattern "X X X X X O O T" where X is closest to the player
            
            BlockPos platformStart = null;
            BlockPos platformEnd = null;
            
            // RESTORED: Calculate platform start and end positions based on the direction
            // Using the exact logic from main branch - above(-1) instead of below(1)
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
            
            // RESTORED: Set platform properties using reflection (exact main branch createDefaultPlatform logic)
            platform.getClass().getMethod("setStartPos", BlockPos.class).invoke(platform, platformStart);
            platform.getClass().getMethod("setEndPos", BlockPos.class).invoke(platform, platformEnd);
            platform.getClass().getMethod("setName", String.class).invoke(platform, "Platform 1");
            platform.getClass().getMethod("setEnabled", boolean.class).invoke(platform, true);
            
            entity.setChanged(); // Mark as dirty for saving
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
                "Created default platform for unified town at {} with start {} and end {}", 
                townPos, platformStart, platformEnd);
                
        } catch (Exception e) {
            LOGGER.error("Failed to setup default platform path for unified town at {}", townPos, e);
        }
    }

    // RESTORED: onRemove functionality from main branch
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null) {
                try {
                    // Get town ID using reflection
                    UUID townId = (UUID) blockEntity.getClass().getMethod("getTownId").invoke(blockEntity);
                    if (townId != null && !level.isClientSide()) {
                        // Remove town from manager using unified architecture
                        TownManager.get((ServerLevel) level).removeTown(townId);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to remove town on block removal", e);
                }
            }
            // Call super after our logic
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    // RESTORED: Platform visualization system from main branch
    /**
     * Spawns visual indicators at platform start and end points and along the line
     */
    private void spawnPlatformIndicators(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        
        // Get the block entity using reflection
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) return;
        
        try {
            @SuppressWarnings("unchecked")
            List<Object> platforms = (List<Object>) blockEntity.getClass().getMethod("getPlatforms").invoke(blockEntity);
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
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Removed expired indicator for player {}", entry.getKey());
                } else {
                    showIndicators = true;
                }
            }
            
            // Only show particles if players have recently exited the UI
            if (!showIndicators) {
                return; // Exit early if no recent UI exits
            }
            
            // For each platform, spawn particles
            for (Object platform : platforms) {
                // Check if platform is enabled and complete using reflection
                boolean enabled = (Boolean) platform.getClass().getMethod("isEnabled").invoke(platform);
                boolean complete = (Boolean) platform.getClass().getMethod("isComplete").invoke(platform);
                
                if (!enabled || !complete) continue;
                
                UUID platformId = (UUID) platform.getClass().getMethod("getId").invoke(platform);
                
                // Check if it's time to spawn indicators for this platform
                boolean shouldSpawnIndicators = 
                    !platformIndicatorSpawnTimes.containsKey(platformId) || 
                    gameTime - platformIndicatorSpawnTimes.get(platformId) >= INDICATOR_SPAWN_INTERVAL;
                    
                if (shouldSpawnIndicators) {
                    // Update spawn time
                    platformIndicatorSpawnTimes.put(platformId, gameTime);
                    
                    BlockPos startPos = (BlockPos) platform.getClass().getMethod("getStartPos").invoke(platform);
                    BlockPos endPos = (BlockPos) platform.getClass().getMethod("getEndPos").invoke(platform);
                    
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
                    int searchRadius = (Integer) blockEntity.getClass().getMethod("getSearchRadius").invoke(blockEntity);
                    spawnSearchRadiusParticles(
                        (ServerLevel)level,
                        startPos,
                        endPos,
                        searchRadius
                    );
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to spawn platform indicators", e);
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
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity == null) return;
        
        try {
            @SuppressWarnings("unchecked")
            List<Object> platforms = (List<Object>) blockEntity.getClass().getMethod("getPlatforms").invoke(blockEntity);
            
            // Remove spawn times for platforms that no longer exist
            platformIndicatorSpawnTimes.keySet().removeIf(platformId -> {
                try {
                    return platforms.stream().noneMatch(p -> {
                        try {
                            UUID id = (UUID) p.getClass().getMethod("getId").invoke(p);
                            return id.equals(platformId);
                        } catch (Exception e) {
                            return false;
                        }
                    });
                } catch (Exception e) {
                    return true; // Remove if we can't check
                }
            });
        } catch (Exception e) {
            LOGGER.error("Failed to cleanup platform indicators", e);
        }
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