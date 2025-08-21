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
     * FIXED: Use TownInterfaceEntity platformManager instead of TownInterfaceData.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing AddPlatformPacket for position ({}, {}, {})", x, y, z);
        
        // CRITICAL FIX: Use the actual TownInterfaceEntity platform system, not TownInterfaceData
        Object blockEntity = getBlockEntity(player);
        if (blockEntity != null) {
            try {
                // Use simplified reflection - call methods directly on the block entity
                boolean canAdd = (Boolean) blockEntity.getClass().getMethod("canAddMorePlatforms").invoke(blockEntity);
                
                if (canAdd) {
                    boolean added = (Boolean) blockEntity.getClass().getMethod("addPlatform").invoke(blockEntity);
                    
                    if (added) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Platform added successfully via TownInterfaceEntity at ({}, {}, {})", x, y, z);
                        
                        // Mark the entity as changed for persistence and client sync
                        blockEntity.getClass().getMethod("setChanged").invoke(blockEntity);
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Marked TownInterfaceEntity as changed for client sync");
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Failed to add platform via TownInterfaceEntity at ({}, {}, {})", x, y, z);
                    }
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Cannot add platform at ({}, {}, {}) - already at max capacity", x, y, z);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to add platform via TownInterfaceEntity: {}", e.getMessage(), e);
            }
                    
            // Platform-specific operations still use platform services
            PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
            
            // Send refresh packet to all tracking clients
            PlatformServices.getNetworkHelper().sendToAllClients(
                new RefreshPlatformsPacket(x, y, z)
            );
        } else {
            LOGGER.warn("No TownInterfaceEntity found at position ({}, {}, {}) for platform addition", x, y, z);
        }
    }
}