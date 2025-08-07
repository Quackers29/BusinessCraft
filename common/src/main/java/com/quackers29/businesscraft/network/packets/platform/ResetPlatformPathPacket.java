package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for resetting a platform's path coordinates.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class ResetPlatformPathPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPlatformPathPacket.class);
    private final int x, y, z;
    private final String platformId;
    
    /**
     * Create packet for sending.
     */
    public ResetPlatformPathPacket(int x, int y, int z, String platformId) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.platformId = platformId;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static ResetPlatformPathPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        return new ResetPlatformPathPacket(pos[0], pos[1], pos[2], platformId);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        LOGGER.debug("Player is resetting platform {} path at [{}, {}, {}]", platformId, x, y, z);
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        if (blockEntity == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Reset the platform path through platform services
        boolean success = PlatformServices.getBlockEntityHelper().resetPlatformPath(blockEntity, platformId);
            
        if (!success) {
            LOGGER.warn("Failed to reset platform {} path at [{}, {}, {}]", platformId, x, y, z);
            return;
        }
        
        // Mark the block entity as changed and sync to client
        PlatformServices.getBlockEntityHelper().markBlockEntityChanged(blockEntity);
        PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
        
        LOGGER.debug("Successfully reset platform {} path at [{}, {}, {}]", platformId, x, y, z);
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getPlatformId() { return platformId; }
}