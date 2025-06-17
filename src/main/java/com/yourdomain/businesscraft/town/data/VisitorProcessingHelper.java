package com.yourdomain.businesscraft.town.data;

import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.town.utils.TouristAllocationTracker;
import com.yourdomain.businesscraft.town.utils.TouristUtils;
import com.yourdomain.businesscraft.town.utils.TownNotificationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class for visitor processing and tourist management.
 * Extracted from TownBlockEntity to improve code organization.
 * 
 * This class handles the complex visitor detection, processing, and reward calculation
 * while maintaining separation from the main block entity.
 */
public class VisitorProcessingHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisitorProcessingHelper.class);
    
    // Constants
    private static final double VISITOR_POSITION_CHANGE_THRESHOLD = 0.001;
    private static final UUID ANY_TOWN_DESTINATION = new UUID(0, 0);
    
    // Position tracking for visitor movement detection
    private final Map<UUID, Vec3> lastVisitorPositions = new HashMap<>();
    
    /**
     * Processes visitors for a town across all enabled platforms
     * 
     * @param level The server level
     * @param townBlockPos The position of the town block
     * @param townId The UUID of the town
     * @param platformManager The platform manager containing platform data
     * @param visitBuffer The visit buffer for grouping arrivals
     * @param searchRadius The search radius for visitor detection
     * @param townName The name of the town for logging
     * @param changeCallback Callback to invoke when changes occur
     */
    public void processVisitors(Level level, BlockPos townBlockPos, UUID townId, 
                               PlatformManager platformManager, VisitBuffer visitBuffer, 
                               int searchRadius, String townName, Runnable changeCallback) {
        
        if (townId == null || !(level instanceof ServerLevel serverLevel)) return;
        
        Town thisTown = TownManager.get(serverLevel).getTown(townId);
        if (thisTown == null) return;
        
        // Get the town data provider
        ITownDataProvider provider = thisTown;
        if (provider == null) return;

        // If no platforms, can't check for visitors
        if (platformManager.getPlatformCount() == 0) return;
        
        // Create a list to collect all nearby villagers across all platforms
        List<Villager> allNearbyVillagers = new ArrayList<>();
        
        // Check each platform for visitors
        for (Platform platform : platformManager.getEnabledPlatforms()) {
            BlockPos startPos = platform.getStartPos();
            BlockPos endPos = platform.getEndPos();
            
            // Create capsule-shaped AABB around the platform path
            AABB platformBounds = new AABB(
                Math.min(startPos.getX(), endPos.getX()) - searchRadius,
                Math.min(startPos.getY(), endPos.getY()) - 2,
                Math.min(startPos.getZ(), endPos.getZ()) - searchRadius,
                Math.max(startPos.getX(), endPos.getX()) + searchRadius,
                Math.max(startPos.getY(), endPos.getY()) + 4,
                Math.max(startPos.getZ(), endPos.getZ()) + searchRadius
            );

            List<Villager> platformVillagers = level.getEntitiesOfClass(Villager.class, platformBounds);
            allNearbyVillagers.addAll(platformVillagers);
        }

        // Clean up positions of villagers that are no longer present
        cleanupVisitorPositions(allNearbyVillagers);

        // Process visitors that are stationary
        for (Villager villager : allNearbyVillagers) {
            if (processIndividualVisitor(villager, townBlockPos, townId, visitBuffer, serverLevel)) {
                if (changeCallback != null) {
                    changeCallback.run();
                }
            }
        }
        
        // Process the visit buffer if it's ready
        if (visitBuffer.shouldProcess()) {
            processVisitBuffer(visitBuffer, provider, thisTown, serverLevel, townBlockPos, townName, changeCallback);
        }
    }
    
    /**
     * Processes an individual visitor villager
     * 
     * @param villager The villager to process
     * @param townBlockPos The position of the town block
     * @param townId The UUID of the town
     * @param visitBuffer The visit buffer for grouping arrivals
     * @param serverLevel The server level
     * @return true if the villager was processed (removed), false otherwise
     */
    private boolean processIndividualVisitor(Villager villager, BlockPos townBlockPos, UUID townId, 
                                           VisitBuffer visitBuffer, ServerLevel serverLevel) {
        
        Vec3 currentPos = villager.position();
        Vec3 lastPos = lastVisitorPositions.get(villager.getUUID());
        
        // Store current position for next check
        lastVisitorPositions.put(villager.getUUID(), currentPos);
        
        // Skip if this is the first time we've seen this villager
        if (lastPos == null) return false;
        
        // Calculate position change
        double positionChange = currentPos.distanceTo(lastPos);
        
        // Skip if villager has moved too much (likely on transport)
        if (positionChange > VISITOR_POSITION_CHANGE_THRESHOLD) {
            return false;
        }

        if (TouristUtils.isTourist(villager)) {
            TouristUtils.TouristInfo touristInfo = TouristUtils.extractTouristInfo(villager);
            
            // Process tourists if:
            // 1. This is their specific destination town, OR
            // 2. They have the ANY_TOWN_DESTINATION and this isn't their origin town
            if (touristInfo != null && 
                !touristInfo.originTownId.equals(townId.toString()) && 
                (
                    // Specific destination that matches this town
                    (touristInfo.destinationTownId != null && 
                     touristInfo.destinationTownId.equals(townId.toString()))
                    ||
                    // "Any town" destination (UUID 0-0)
                    (touristInfo.destinationTownId != null && 
                     touristInfo.destinationTownId.equals(ANY_TOWN_DESTINATION.toString()))
                )) {
                
                // Look up the origin town to get its actual current position
                UUID originTownUuid = UUID.fromString(touristInfo.originTownId);
                Town lookupOriginTown = TownManager.get(serverLevel).getTown(originTownUuid);
                BlockPos originPos;
                
                if (lookupOriginTown != null) {
                    // Use the actual town's position
                    originPos = lookupOriginTown.getPosition();
                } else {
                    // Fallback to stored coordinates if town is not found
                    originPos = new BlockPos(touristInfo.originX, touristInfo.originY, touristInfo.originZ);
                    LOGGER.warn("Origin town {} not found, using stored coordinates: {}", originTownUuid, originPos);
                }
                
                visitBuffer.addVisitor(originTownUuid, originPos);

                // Calculate distance from origin town to destination town (not tourist's current position)
                double distance = Math.sqrt(originPos.distSqr(townBlockPos));
                
                // Add detailed distance logging
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "TOURIST DISTANCE - Town: {}, From: {}, Origin town pos: {}, Destination town pos: {}, Calculated distance: {}", 
                    townId, touristInfo.originTownName, 
                    originPos, townBlockPos, distance);
                
                // Store the distance in the visitor info for later payment calculation
                visitBuffer.updateVisitorDistance(UUID.fromString(touristInfo.originTownId), distance);
                
                // Verify the distance was stored
                double storedDistance = visitBuffer.getAverageDistance(UUID.fromString(touristInfo.originTownId));
                DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "TOURIST DISTANCE - After storing: townId: {}, storedDistance: {}", 
                    touristInfo.originTownId, storedDistance);

                // Find the origin town and decrement its tourist count
                Town originTown = TownManager.get(serverLevel).getTown(UUID.fromString(touristInfo.originTownId));
                if (originTown != null) {
                    originTown.removeTourist();
                    
                    // Record tourist removal in the allocation tracker
                    UUID destId = touristInfo.destinationTownId.equals(ANY_TOWN_DESTINATION.toString()) 
                        ? townId  // Use actual town ID for stats when ANY_TOWN
                        : UUID.fromString(touristInfo.destinationTownId);
                    
                    TouristAllocationTracker.recordTouristRemoval(
                        UUID.fromString(touristInfo.originTownId),
                        destId
                    );
                    
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Removed tourist from town {}, count now {}/{}", 
                        originTown.getName(), originTown.getTouristCount(), originTown.getMaxTourists());
                }

                villager.remove(Entity.RemovalReason.DISCARDED);
                return true; // Villager was processed and removed
            }
        }
        
        return false; // Villager was not processed
    }
    
    /**
     * Processes the visit buffer when ready, handling payments and notifications
     */
    private void processVisitBuffer(VisitBuffer visitBuffer, ITownDataProvider provider, Town thisTown, 
                                   ServerLevel serverLevel, BlockPos townBlockPos, String townName, Runnable changeCallback) {
        
        List<ITownDataProvider.VisitHistoryRecord> newVisits = visitBuffer.processVisits();
        if (newVisits.isEmpty()) return;
        
        // Add to the town data provider (single source of truth)
        for (ITownDataProvider.VisitHistoryRecord record : newVisits) {
            try {
                // Record the visit in the Town
                provider.recordVisit(record.getOriginTownId(), record.getCount(), record.getOriginPos());
                
                // Each visitor should be processed by adding them to the town's tally
                for (int i = 0; i < record.getCount(); i++) {
                    thisTown.addVisitor(record.getOriginTownId());
                }
                
                // Get distance before it gets cleared by payment calculation
                double averageDistance = visitBuffer.getAverageDistance(record.getOriginTownId());
                
                // Calculate payment based on travel distance
                int payment = calculatePayment(visitBuffer, record);
                
                // Check for milestone achievements and deliver rewards (using distance from before payment clearing)
                DistanceMilestoneHelper.MilestoneResult milestoneResult = 
                    DistanceMilestoneHelper.checkMilestones(averageDistance, record.getCount());
                
                if (milestoneResult.hasRewards()) {
                    DistanceMilestoneHelper.deliverRewards(thisTown, milestoneResult);
                    // Trigger callback to update client sync after milestone rewards are added
                    if (changeCallback != null) {
                        changeCallback.run();
                    }
                }
                
                // Add emeralds to communal storage if payment > 0
                if (payment > 0) {
                    addPaymentToTown(thisTown, payment);
                    // Trigger callback to update client sync after emeralds are added
                    if (changeCallback != null) {
                        changeCallback.run();
                    }
                }
                
                // Send grouped notification with payment and milestone information
                if (ConfigLoader.notifyOnTouristDeparture) {
                    String originTownName = resolveTownName(serverLevel, record.getOriginTownId());
                    TownNotificationUtils.notifyTouristArrivals(
                        serverLevel,
                        townBlockPos,
                        originTownName,
                        townName,
                        record.getCount(),
                        payment,
                        averageDistance,
                        milestoneResult
                    );
                }
            } catch (Exception e) {
                LOGGER.error("Error processing visitor batch: {}", e.getMessage());
            }
        }
        
        // Ensure data is saved
        TownManager.get(serverLevel).markDirty();
    }
    
    /**
     * Calculates payment for a visit record based on travel distance
     */
    private int calculatePayment(VisitBuffer visitBuffer, ITownDataProvider.VisitHistoryRecord record) {
        double averageDistance = visitBuffer.getAverageDistance(record.getOriginTownId());
        
        // Add detailed distance debugging
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "PAYMENT CALCULATION - VisitBuffer state: {}", visitBuffer.toString());
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "PAYMENT CALCULATION - Processing visitor from townId: {}, Visitors stored in buffer: {}, Average distance: {}", 
            record.getOriginTownId(), visitBuffer.getVisitorCount(), averageDistance);
            
        if (averageDistance > 0) {
            // Calculate emeralds based on distance traveled and meters per emerald rate
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Tourist payment calculation: averageDistance={}, metersPerEmerald={}, touristCount={}", 
                averageDistance, ConfigLoader.metersPerEmerald, record.getCount());
            
            int payment = (int) Math.max(1, (averageDistance / ConfigLoader.metersPerEmerald) * record.getCount());
            DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "Calculated payment: {} emeralds (distance={}, rate=1 per {} meters)", 
                payment, averageDistance, ConfigLoader.metersPerEmerald);
            
            // Clear the saved distance after using it
            visitBuffer.clearSavedDistance(record.getOriginTownId());
            
            return payment;
        } else {
            LOGGER.warn("No distance recorded for tourists from {}, skipping payment", record.getOriginTownId());
            return 0;
        }
    }
    
    /**
     * Adds payment to the town's communal storage
     */
    private void addPaymentToTown(Town town, int payment) {
        // Add more detailed logging
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "TOURIST PAYMENT - Attempting to add {} emeralds to {} communal storage",
            payment, town.getName());
            
        // Print current storage before update
        Map<Item, Integer> beforeItems = town.getAllCommunalStorageItems();
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "BEFORE payment - Communal storage for {}: {}", town.getName(), 
            beforeItems.entrySet().stream()
                .map(e -> e.getKey().getDescription().getString() + ": " + e.getValue())
                .collect(Collectors.joining(", ")));
        
        boolean success = town.addToCommunalStorage(Items.EMERALD, payment);
        
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "TOURIST PAYMENT - Added {} emeralds to {} communal storage - success: {}", 
            payment, town.getName(), success);
            
        // Print the current communal storage content
        Map<Item, Integer> communalItems = town.getAllCommunalStorageItems();
        DebugConfig.debug(LOGGER, DebugConfig.VISITOR_PROCESSING, "AFTER payment - Communal storage for {}: {}", town.getName(), 
            communalItems.entrySet().stream()
                .map(e -> e.getKey().getDescription().getString() + ": " + e.getValue())
                .collect(Collectors.joining(", ")));
    }
    
    /**
     * Resolves a town name from its UUID
     */
    private String resolveTownName(ServerLevel serverLevel, UUID townId) {
        if (townId == null) return "Unknown";
        
        Town town = TownManager.get(serverLevel).getTown(townId);
        if (town != null) {
            return town.getName();
        }
        
        return "Unknown Town";
    }
    
    /**
     * Cleans up the lastVisitorPositions map by removing entries for villagers 
     * that no longer exist in the current list of nearby villagers.
     * This prevents memory leaks from accumulating over time.
     */
    private void cleanupVisitorPositions(List<Villager> currentVillagers) {
        // Skip if empty to avoid unnecessary work
        if (lastVisitorPositions.isEmpty()) return;
        
        // Create a set of current villager UUIDs for efficient lookups
        Set<UUID> currentVillagerIds = currentVillagers.stream()
            .map(Entity::getUUID)
            .collect(Collectors.toSet());
            
        // Keep track of how many entries we're removing for logging
        int removedCount = 0;
        
        // Remove entries that don't correspond to current villagers
        Iterator<UUID> iterator = lastVisitorPositions.keySet().iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            if (!currentVillagerIds.contains(id)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        // Log only if we actually removed something and at debug level only
        if (removedCount > 0 && LOGGER.isDebugEnabled()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Cleaned up {} stale visitor position entries", removedCount);
        }
    }
    
    /**
     * Clears all visitor tracking state (useful for cleanup)
     */
    public void clearAll() {
        lastVisitorPositions.clear();
    }
    
    /**
     * Gets the number of tracked visitor positions
     * @return The count of tracked visitors
     */
    public int getTrackedVisitorCount() {
        return lastVisitorPositions.size();
    }
} 