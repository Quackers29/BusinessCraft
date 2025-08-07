package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for toggling platform enabled state.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class SetPlatformEnabledPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformEnabledPacket.class);
    private final int x, y, z;
    private final String platformId;
    private final boolean enabled;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformEnabledPacket(int x, int y, int z, String platformId, boolean enabled) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.platformId = platformId;
        this.enabled = enabled;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static SetPlatformEnabledPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        boolean enabled = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new SetPlatformEnabledPacket(pos[0], pos[1], pos[2], platformId, enabled);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, enabled);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        LOGGER.debug("Player is setting platform {} enabled state to {} at [{}, {}, {}]", 
                    platformId, enabled, x, y, z);
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        if (blockEntity == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Set the platform enabled state through platform services
        boolean success = PlatformServices.getBlockEntityHelper().setPlatformEnabledById(
            blockEntity, platformId, enabled);
            
        if (!success) {
            LOGGER.warn("Failed to set platform {} enabled state to {} at [{}, {}, {}] - platform not found", 
                       platformId, enabled, x, y, z);
            return;
        }
        
        // Mark the block entity as changed and sync to client
        PlatformServices.getBlockEntityHelper().markBlockEntityChanged(blockEntity);
        PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
        
        // Notify clients tracking this chunk of the platform state change
        PlatformServices.getNetworkHelper().sendRefreshPlatformsPacketToChunk(player, x, y, z);
        
        LOGGER.debug("Successfully set platform {} enabled state to {} at [{}, {}, {}]", 
                    platformId, enabled, x, y, z);
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getPlatformId() { return platformId; }
    public boolean isEnabled() { return enabled; }
}