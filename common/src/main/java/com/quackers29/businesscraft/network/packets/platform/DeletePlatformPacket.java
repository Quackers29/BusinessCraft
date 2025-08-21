package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet for deleting a platform from a town.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class DeletePlatformPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePlatformPacket.class);
    private final String platformId;
    
    /**
     * Create packet for sending.
     */
    public DeletePlatformPacket(int x, int y, int z, String platformId) {
        super(x, y, z);
        this.platformId = platformId;
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public DeletePlatformPacket(Object buffer) {
        super(buffer);
        this.platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
    }

    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Writes BlockPos
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
    }

    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing DeletePlatformPacket for platform {} at ({}, {}, {})", platformId, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            boolean success = townData.removePlatformById(platformId);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully deleted platform {} from town at ({}, {}, {})", platformId, x, y, z);
                
                // Platform-specific operations still use platform services
                PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
                
                // Send refresh packet to all tracking clients
                PlatformServices.getNetworkHelper().sendToAllClients(
                    new RefreshPlatformsPacket(x, y, z)
                );
            } else {
                LOGGER.warn("Failed to delete platform {} from town at ({}, {}, {}) - platform not found", platformId, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for platform deletion", x, y, z);
        }
    }
    
    /**
     * Get the platform ID for this packet.
     */
    public String getPlatformId() {
        return platformId;
    }
}