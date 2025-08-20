package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet for adding a new platform to a town.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class AddPlatformPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddPlatformPacket.class);
    
    /**
     * Create packet for sending.
     */
    public AddPlatformPacket(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public AddPlatformPacket(Object buffer) {
        super(buffer);
    }

    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing AddPlatformPacket for position ({}, {}, {})", x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            if (townData.canAddMorePlatforms()) {
                boolean added = townData.addPlatform();
                
                if (added) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Platform added successfully at ({}, {}, {})", x, y, z);
                    
                    // Platform-specific operations still use platform services
                    PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
                    
                    // Send refresh packet to all tracking clients
                    PlatformServices.getNetworkHelper().sendToAllClients(
                        new RefreshPlatformsPacket(x, y, z)
                    );
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Failed to add platform at ({}, {}, {}) - internal error", x, y, z);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Cannot add platform at ({}, {}, {}) - already at max capacity", x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for platform addition", x, y, z);
        }
    }
}