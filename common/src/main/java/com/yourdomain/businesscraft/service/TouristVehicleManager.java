package com.yourdomain.businesscraft.service;

import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.town.utils.TouristUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;

/**
 * Manages the mounting of tourists to different vehicle types
 * Extracted from TownBlockEntity to create a more modular and maintainable component
 */
public class TouristVehicleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TouristVehicleManager.class);
    private static final double VISITOR_POSITION_CHANGE_THRESHOLD = ConfigLoader.INSTANCE.minecartStopThreshold;
    private static final ConfigLoader CONFIG = ConfigLoader.INSTANCE;
    
    // Maps to track vehicle positions for movement detection
    private final Map<UUID, Vec3> lastVehiclePositions = new HashMap<>();
    
    /**
     * Attempts to mount available tourists to nearby vehicles
     *
     * @param level The current level
     * @param pathStart The start point of the tourist path
     * @param pathEnd The end point of the tourist path
     * @param searchRadius The radius to search for vehicles and tourists
     * @param townId The ID of the town
     * @return The number of tourists successfully mounted
     */
    public int mountTouristsToVehicles(Level level, BlockPos pathStart, BlockPos pathEnd, 
                                      int searchRadius, UUID townId) {
        if (level == null || level.isClientSide || pathStart == null || pathEnd == null) {
            return 0;
        }
        
        int mountedCount = 0;
        
        // Create bounds for entity searching
        AABB searchBounds = createSearchBounds(pathStart, pathEnd, searchRadius);
        
        // Get tourists that can be mounted
        List<Villager> tourists = findAvailableTourists(level, searchBounds, townId);
        if (tourists.isEmpty()) {
            return 0;
        }
        
        // Handle mounting to Create mod trains if enabled
        if (CONFIG.enableCreateTrains && level instanceof ServerLevel serverLevel) {
            mountedCount += mountTouristsToCreateTrains(serverLevel, searchBounds, tourists);
        }
        
        // Handle mounting to vanilla minecarts if enabled
        if (CONFIG.enableMinecarts) {
            mountedCount += mountTouristsToMinecarts(level, searchBounds, tourists);
        }
        
        return mountedCount;
    }
    
    /**
     * Creates an appropriate search bound box around the path
     */
    private AABB createSearchBounds(BlockPos start, BlockPos end, int radius) {
        return new AABB(
            Math.min(start.getX(), end.getX()) - radius,
            Math.min(start.getY(), end.getY()) - 2,
            Math.min(start.getZ(), end.getZ()) - radius,
            Math.max(start.getX(), end.getX()) + radius,
            Math.max(start.getY(), end.getY()) + 4,
            Math.max(start.getZ(), end.getZ()) + radius
        );
    }
    
    /**
     * Finds tourists that are available for mounting
     */
    private List<Villager> findAvailableTourists(Level level, AABB bounds, UUID townId) {
        return level.getEntitiesOfClass(Villager.class, bounds,
            villager -> villager.onGround() && 
                       !villager.isPassenger() &&
                       TouristUtils.isTourist(villager) &&
                       townId.toString().equals(TouristUtils.getOriginTownId(villager))
        );
    }
    
    /**
     * Mounts tourists to Create mod carriages
     */
    private int mountTouristsToCreateTrains(ServerLevel level, AABB bounds, List<Villager> tourists) {
        int mountedCount = 0;
        
        // Find stopped carriages
        List<Entity> carriages = level.getEntitiesOfClass(Entity.class, bounds, 
            entity -> isStoppedCreateCarriage(entity, level));
        
        for (Entity carriage : carriages) {
            try {
                // Get total seats
                String seatsCommand = String.format("data get entity %s Contraption.Seats", 
                    carriage.getStringUUID());
                int seatCount = level.getServer().getCommands().getDispatcher()
                    .execute(seatsCommand, level.getServer().createCommandSourceStack());
                
                // Find free seats
                List<Integer> freeSeats = findFreeSeats(level, carriage, seatCount);
                
                // Shuffle seats for random assignment
                Collections.shuffle(freeSeats);
                
                // Mount tourists to free seats
                for (Integer seatIndex : freeSeats) {
                    if (mountTouristToCreateSeat(level, tourists, carriage, seatIndex)) {
                        mountedCount++;
                    }
                }
            } catch (Exception e) {
                DebugConfig.debug(LOGGER, DebugConfig.ENTITY_SYSTEM, "Failed to mount tourists to Create train: {}", e.getMessage());
            }
        }
        
        return mountedCount;
    }
    
    /**
     * Checks if an entity is a stopped Create carriage
     */
    private boolean isStoppedCreateCarriage(Entity entity, ServerLevel level) {
        String entityId = entity.getType().builtInRegistryHolder().key().toString();
        boolean isCarriage = entityId.contains("create:carriage_contraption");
        
        if (!isCarriage) return false;
        
        // Check if carriage is stopped using position change
        Vec3 currentPos = entity.position();
        Vec3 lastPos = lastVehiclePositions.get(entity.getUUID());
        
        boolean isStopped = false;
        if (lastPos != null) {
            double positionChange = currentPos.distanceTo(lastPos);
            isStopped = positionChange < VISITOR_POSITION_CHANGE_THRESHOLD;
        }
        
        lastVehiclePositions.put(entity.getUUID(), currentPos);
        return isStopped;
    }
    
    /**
     * Finds which seats in a Create carriage are available
     */
    private List<Integer> findFreeSeats(ServerLevel level, Entity carriage, int seatCount) {
        List<Integer> freeSeats = new ArrayList<>();
        for (int i = 0; i < seatCount; i++) {
            try {
                String checkSeatCommand = String.format("data get entity %s Contraption.Passengers[{Seat:%d}]", 
                    carriage.getStringUUID(), i);
                level.getServer().getCommands().getDispatcher()
                    .execute(checkSeatCommand, level.getServer().createCommandSourceStack());
            } catch (Exception e) {
                // If command fails, seat is free
                freeSeats.add(i);
            }
        }
        return freeSeats;
    }
    
    /**
     * Mounts a specific tourist to a Create carriage seat
     */
    private boolean mountTouristToCreateSeat(ServerLevel level, List<Villager> tourists, 
                                           Entity carriage, int seatIndex) {
        Optional<Villager> touristOpt = tourists.stream()
            .filter(tourist -> !tourist.isPassenger())
            .findFirst();
            
        if (touristOpt.isPresent()) {
            Villager tourist = touristOpt.get();
            try {
                String command = String.format("create passenger %s %s %d",
                    tourist.getStringUUID(),
                    carriage.getStringUUID(),
                    seatIndex
                );
                level.getServer().getCommands().getDispatcher()
                    .execute(command, level.getServer().createCommandSourceStack());
                DebugConfig.debug(LOGGER, DebugConfig.ENTITY_SYSTEM, "Mounted tourist {} to create carriage {}, seat {}",
                    tourist.getStringUUID().toString().substring(0, 8),
                    carriage.getStringUUID().toString().substring(0, 8),
                    seatIndex);
                return true;
            } catch (Exception e) {
                DebugConfig.debug(LOGGER, DebugConfig.ENTITY_SYSTEM, "Failed to mount tourist to create seat: {}", e.getMessage());
                return false;
            }
        }
        return false;
    }
    
    /**
     * Mounts tourists to vanilla minecarts
     */
    private int mountTouristsToMinecarts(Level level, AABB bounds, List<Villager> tourists) {
        int mountedCount = 0;
        List<AbstractMinecart> minecarts = level.getEntitiesOfClass(AbstractMinecart.class, bounds);
        
        for (AbstractMinecart minecart : minecarts) {
            // Skip if minecart already has a passenger
            if (minecart.hasPassenger(passenger -> true)) {
                continue;
            }
            
            Vec3 currentPos = minecart.position();
            Vec3 lastPos = lastVehiclePositions.get(minecart.getUUID());
            
            // Store current position for next check
            lastVehiclePositions.put(minecart.getUUID(), currentPos);
            
            // Skip if this is the first time we've seen this minecart
            if (lastPos == null) {
                DebugConfig.debug(LOGGER, DebugConfig.ENTITY_SYSTEM, "First time seeing minecart {}", 
                    minecart.getUUID().toString().substring(0, 8));
                continue;
            }
            
            // Calculate position change
            double positionChange = currentPos.distanceTo(lastPos);
            DebugConfig.debug(LOGGER, DebugConfig.ENTITY_SYSTEM, "Minecart {} position change: {} (Threshold: {})", 
                minecart.getUUID().toString().substring(0, 8), 
                String.format("%.6f", positionChange), 
                String.format("%.6f", VISITOR_POSITION_CHANGE_THRESHOLD));
            
            // Only board if position change is LESS than threshold (minecart is stopped)
            if (positionChange < VISITOR_POSITION_CHANGE_THRESHOLD) {
                Optional<Villager> touristOpt = tourists.stream()
                    .filter(tourist -> !tourist.isPassenger())
                    .findFirst();
                    
                if (touristOpt.isPresent()) {
                    Villager tourist = touristOpt.get();
                    DebugConfig.debug(LOGGER, DebugConfig.ENTITY_SYSTEM, "Mounting tourist {} to minecart {}", 
                        tourist.getStringUUID().toString().substring(0, 8),
                        minecart.getUUID().toString().substring(0, 8));
                    tourist.startRiding(minecart);
                    mountedCount++;
                }
            }
        }
        
        return mountedCount;
    }
    
    /**
     * Utility method to format Vec3 for logging
     */
    private String formatVec3(Vec3 vec) {
        return String.format("[%.2f, %.2f, %.2f]", vec.x, vec.y, vec.z);
    }
    
    /**
     * Clears the tracked vehicle positions
     * Should be called when a world is unloaded or server stops
     */
    public void clearTrackedVehicles() {
        lastVehiclePositions.clear();
    }
} 