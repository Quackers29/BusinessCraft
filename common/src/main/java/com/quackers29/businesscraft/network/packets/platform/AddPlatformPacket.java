package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
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
     * This method contains the core business logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Player is adding a new platform to town block at ({}, {}, {})", x, y, z);
        
        // Get town interface using unified access
        TownInterfaceEntity townInterface = getTownInterfaceEntity(player);
        
        if (townInterface != null) {
            // Check if we can add more platforms through platform services
            boolean canAdd = PlatformServices.getBlockEntityHelper().canAddMorePlatforms(townInterface);
            
            if (canAdd) {
                // Add platform through platform services
                boolean added = PlatformServices.getBlockEntityHelper().addPlatform(townInterface);
                
                if (added) {
                    LOGGER.debug("Successfully added new platform to town block at ({}, {}, {})", x, y, z);
                    PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
                    
                    // Send refresh packet to all tracking clients
                    PlatformServices.getNetworkHelper().sendToAllClients(
                        new RefreshPlatformsPacket(x, y, z)
                    );
                } else {
                    LOGGER.debug("Failed to add platform to town block at ({}, {}, {}) - internal error", x, y, z);
                }
            } else {
                LOGGER.debug("Failed to add platform to town block at ({}, {}, {}) - already at max capacity", x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceEntity found at position ({}, {}, {}) for platform addition", x, y, z);
        }
    }
}