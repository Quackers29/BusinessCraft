package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.debug.DebugConfig;
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
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing SetPlatformEnabledPacket for platform {} enabled={} at ({}, {}, {})", 
                    platformId, enabled, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            boolean success = townData.setPlatformEnabledById(platformId, enabled);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully set platform {} enabled state to {} at ({}, {}, {})", 
                            platformId, enabled, x, y, z);
                
                // Platform-specific operations still use platform services
                PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
                
                // Notify clients tracking this chunk of the platform state change
                PlatformServices.getNetworkHelper().sendRefreshPlatformsPacketToChunk(player, x, y, z);
            } else {
                LOGGER.warn("Failed to set platform {} enabled state to {} at ({}, {}, {}) - platform not found", 
                           platformId, enabled, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for platform enabled state change", x, y, z);
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
    public boolean isEnabled() { return enabled; }
}