package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet sent from server to client to refresh platform data.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle client-side operations through PlatformServices.
 */
public class RefreshPlatformsPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshPlatformsPacket.class);
    
    /**
     * Create packet for sending.
     */
    public RefreshPlatformsPacket(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public RefreshPlatformsPacket(Object buffer) {
        super(buffer);
    }

    /**
     * Handle the packet on the client side.
     * This method contains the core client-side logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        // This is a client-side packet, player parameter is not used
        LOGGER.debug("Received platform refresh packet for block at ({}, {}, {})", x, y, z);
        
        // Check if we're on client side
        if (!PlatformServices.getPlatformHelper().isClientSide()) {
            LOGGER.warn("RefreshPlatformsPacket received on server side - ignoring");
            return;
        }
        
        // Note: This is a client-side packet, so we still use platform services for client-side operations
        // which involve complex UI and cache management not yet unified
        Object blockEntity = PlatformServices.getBlockEntityHelper().getClientBlockEntity(x, y, z);
        
        if (blockEntity != null) {
            // Get town ID for cache clearing
            Object townDataProvider = PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
            if (townDataProvider != null) {
                String townId = PlatformServices.getBlockEntityHelper().getTownId(townDataProvider);
                if (townId != null) {
                    // Clear platform cache through platform services
                    PlatformServices.getPlatformHelper().clearTownPlatformCache(townId);
                    LOGGER.debug("Cleared platform cache for town {}", townId);
                }
            }
            
            // Refresh any open platform management screens
            PlatformServices.getPlatformHelper().refreshPlatformManagementScreen();
        } else {
            LOGGER.debug("No block entity found at position ({}, {}, {}) for platform refresh", x, y, z);
        }
    }
}