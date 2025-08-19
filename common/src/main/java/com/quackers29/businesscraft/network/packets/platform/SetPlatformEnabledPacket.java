package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for toggling platform enabled state.
 * 
 * Unified Architecture approach: Direct access to TownInterfaceEntity methods
 * instead of platform service wrapper calls.
 */
public class SetPlatformEnabledPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformEnabledPacket.class);
    private final String platformId;
    private final boolean enabled;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformEnabledPacket(int x, int y, int z, String platformId, boolean enabled) {
        super(x, y, z);
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
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, enabled);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Player is setting platform {} enabled state to {} at [{}, {}, {}]", 
                    platformId, enabled, x, y, z);
        
        // Get the town interface block entity using unified architecture
        com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterface = getTownInterfaceEntity(player);
        if (townInterface == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Set the platform enabled state through platform services (complex platform management)
        boolean success = PlatformServices.getBlockEntityHelper().setPlatformEnabledById(
            townInterface, platformId, enabled);
            
        if (!success) {
            LOGGER.warn("Failed to set platform {} enabled state to {} at [{}, {}, {}] - platform not found", 
                       platformId, enabled, x, y, z);
            return;
        }
        
        // Mark changed and sync using unified architecture
        markChangedAndSync(townInterface);
        PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
        
        // Notify clients tracking this chunk of the platform state change
        PlatformServices.getNetworkHelper().sendRefreshPlatformsPacketToChunk(player, x, y, z);
        
        LOGGER.debug("Successfully set platform {} enabled state to {} at [{}, {}, {}]", 
                    platformId, enabled, x, y, z);
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
    public boolean isEnabled() { return enabled; }
}