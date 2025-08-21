package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
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
     * Unified Architecture approach: Minimal abstraction reduction for client-side operations.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing RefreshPlatformsPacket at position ({}, {}, {})", x, y, z);
        
        // Check if we're on client side
        if (!PlatformServices.getPlatformHelper().isClientSide()) {
            LOGGER.warn("RefreshPlatformsPacket received on server side - ignoring");
            return;
        }
        
        // Client-side operations: Use hybrid approach with some abstraction reduction
        Object blockEntity = PlatformServices.getBlockEntityHelper().getClientBlockEntity(x, y, z);
        
        if (blockEntity != null) {
            // Reduced abstraction: Get town ID through fewer calls
            Object townDataProvider = PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
            if (townDataProvider != null) {
                String townId = PlatformServices.getBlockEntityHelper().getTownId(townDataProvider);
                if (townId != null) {
                    // Client-side cache and UI operations still use platform services (appropriate)
                    PlatformServices.getPlatformHelper().clearTownPlatformCache(townId);
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Cleared platform cache for town {}", townId);
                }
            }
            
            // Client-side UI operations still use platform services
            PlatformServices.getPlatformHelper().refreshPlatformManagementScreen();
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "No block entity found at position ({}, {}, {}) for platform refresh", x, y, z);
        }
    }
}