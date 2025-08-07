package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;

/**
 * Platform-agnostic client-to-server packet to set a destination enabled state for a platform.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class SetPlatformDestinationPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformDestinationPacket.class);
    private final int x, y, z;
    private final String platformId;
    private final String townId;
    private final boolean enabled;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformDestinationPacket(int x, int y, int z, String platformId, String townId, boolean enabled) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.platformId = platformId;
        this.townId = townId;
        this.enabled = enabled;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static SetPlatformDestinationPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        String townId = PlatformServices.getNetworkHelper().readUUID(buffer);
        boolean enabled = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new SetPlatformDestinationPacket(pos[0], pos[1], pos[2], platformId, townId, enabled);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeUUID(buffer, townId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, enabled);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        LOGGER.debug("Player is setting destination {} to {} for platform {} at [{}, {}, {}]", 
                    townId, enabled, platformId, x, y, z);
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        if (blockEntity == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Set the destination enabled state through platform services
        boolean success = PlatformServices.getBlockEntityHelper().setPlatformDestinationEnabled(
            blockEntity, platformId, townId, enabled);
            
        if (!success) {
            LOGGER.warn("Failed to set destination {} to {} for platform {} at [{}, {}, {}]", 
                       townId, enabled, platformId, x, y, z);
            return;
        }
        
        // Mark the block entity as changed
        PlatformServices.getBlockEntityHelper().markBlockEntityChanged(blockEntity);
        
        // Send refresh destinations packet back to client
        sendRefreshDestinationsPacket(player, blockEntity);
        
        LOGGER.debug("Successfully set destination {} to {} for platform {} at [{}, {}, {}]", 
                    townId, enabled, platformId, x, y, z);
    }
    
    /**
     * Send refresh destinations packet with updated data to the client.
     */
    private void sendRefreshDestinationsPacket(Object player, Object blockEntity) {
        try {
            // Get destination states for this platform
            Map<String, Boolean> townDestinations = PlatformServices.getBlockEntityHelper()
                .getPlatformDestinations(blockEntity, platformId);
            
            // Get all available destination towns
            Map<String, String> townNames = PlatformServices.getBlockEntityHelper()
                .getAllTownsForDestination(blockEntity);
            
            // Get the origin town for distance calculations
            Object originTown = PlatformServices.getBlockEntityHelper().getOriginTown(blockEntity);
            
            // Use block position if town or town position is null
            int[] originPos = (originTown != null) 
                ? PlatformServices.getBlockEntityHelper().getTownPosition(originTown)
                : new int[]{x, y, z}; // Use block entity position as fallback
            
            if (originPos == null) {
                originPos = new int[]{x, y, z}; // Final fallback to block position
            }
            
            Map<String, Integer> distances = new HashMap<>();
            Map<String, String> directions = new HashMap<>();
            
            // Calculate distances and directions for each town
            for (String destTownId : townNames.keySet()) {
                // Skip the current town (platforms shouldn't route to themselves)
                if (originTown != null) {
                    String originTownId = PlatformServices.getBlockEntityHelper().getTownId(
                        PlatformServices.getBlockEntityHelper().getTownDataProvider(originTown));
                    if (destTownId.equals(originTownId)) {
                        continue; // Skip this town
                    }
                }
                
                // Get destination town to calculate distance
                Object destTown = PlatformServices.getBlockEntityHelper().getTownById(player, destTownId);
                
                int distance = 100; // Default fallback distance
                String direction = ""; // Direction (N, NE, E, etc.)
                
                if (destTown != null) {
                    int[] destPos = PlatformServices.getBlockEntityHelper().getTownPosition(destTown);
                    if (destPos != null) {
                        // Calculate Euclidean distance in blocks
                        double dx = destPos[0] - originPos[0];
                        double dy = destPos[1] - originPos[1];
                        double dz = destPos[2] - originPos[2];
                        distance = (int)Math.sqrt(dx*dx + dy*dy + dz*dz);
                        
                        // Skip towns with distance <= 1 (likely same location = same town)
                        if (distance <= 1) {
                            continue;
                        }
                        
                        // Calculate cardinal direction (ignore Y-axis)
                        direction = calculateDirection(dx, dz);
                    }
                }
                
                distances.put(destTownId, distance);
                directions.put(destTownId, direction);
            }
            
            // Send the refresh packet through platform services
            PlatformServices.getNetworkHelper().sendRefreshDestinationsPacket(
                player, x, y, z, platformId, townDestinations, townNames, distances, directions);
                
        } catch (Exception e) {
            LOGGER.error("Failed to send refresh destinations packet", e);
        }
    }
    
    /**
     * Calculate cardinal direction based on x and z coordinates
     * @param dx X distance
     * @param dz Z distance
     * @return Cardinal direction (N, NE, E, SE, S, SW, W, NW)
     */
    private String calculateDirection(double dx, double dz) {
        if (dx == 0 && dz == 0) return "";
        
        double angle = Math.toDegrees(Math.atan2(dz, dx));
        // Convert angle to 0-360 range
        if (angle < 0) angle += 360;
        
        // Determine direction based on angle
        if (angle >= 337.5 || angle < 22.5) return "E";
        if (angle >= 22.5 && angle < 67.5) return "SE";
        if (angle >= 67.5 && angle < 112.5) return "S";
        if (angle >= 112.5 && angle < 157.5) return "SW";
        if (angle >= 157.5 && angle < 202.5) return "W";
        if (angle >= 202.5 && angle < 247.5) return "NW";
        if (angle >= 247.5 && angle < 292.5) return "N";
        return "NE"; // 292.5-337.5
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getPlatformId() { return platformId; }
    public String getTownId() { return townId; }
    public boolean isEnabled() { return enabled; }
}