package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for resetting a platform's path coordinates.
 * 
 * Unified Architecture approach: Direct access to TownInterfaceEntity methods
 * instead of platform service wrapper calls.
 */
public class ResetPlatformPathPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPlatformPathPacket.class);
    private final String platformId;
    
    /**
     * Create packet for sending.
     */
    public ResetPlatformPathPacket(int x, int y, int z, String platformId) {
        super(x, y, z);
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
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Player is resetting platform {} path at [{}, {}, {}]", platformId, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            boolean success = townData.resetPlatformPath(platformId);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully reset platform {} path at ({}, {}, {})", platformId, x, y, z);
                
                // Platform-specific operations still use platform services
                PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
            } else {
                LOGGER.warn("Failed to reset platform {} path at ({}, {}, {}) - platform not found", platformId, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for platform path reset", x, y, z);
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
}