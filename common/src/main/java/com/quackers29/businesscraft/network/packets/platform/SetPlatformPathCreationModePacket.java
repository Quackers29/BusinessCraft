package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for setting platform path creation mode.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class SetPlatformPathCreationModePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformPathCreationModePacket.class);
    private final int x, y, z;
    private final String platformId;
    private final boolean mode;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformPathCreationModePacket(int x, int y, int z, String platformId, boolean mode) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.platformId = platformId;
        this.mode = mode;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static SetPlatformPathCreationModePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        boolean mode = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new SetPlatformPathCreationModePacket(pos[0], pos[1], pos[2], platformId, mode);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, mode);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        System.out.println("SET PLATFORM PATH CREATION MODE PACKET: Player is setting platform " + platformId + " path creation mode to " + mode + " at [" + x + ", " + y + ", " + z + "]");
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        if (blockEntity == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Set the platform path creation mode through platform services
        boolean success = PlatformServices.getBlockEntityHelper().setPlatformCreationMode(
            blockEntity, mode, platformId);
            
        if (!success) {
            LOGGER.warn("Failed to set platform {} path creation mode to {} at [{}, {}, {}]", 
                       platformId, mode, x, y, z);
            return;
        }
        
        // Update the platform path handler state through platform services
        if (mode) {
            PlatformServices.getPlatformHelper().setActivePlatformForPathCreation(x, y, z, platformId);
        } else {
            PlatformServices.getPlatformHelper().clearActivePlatformForPathCreation();
        }
        
        LOGGER.debug("Successfully set platform {} path creation mode to {} at [{}, {}, {}]", 
                    platformId, mode, x, y, z);
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getPlatformId() { return platformId; }
    public boolean getMode() { return mode; }
}