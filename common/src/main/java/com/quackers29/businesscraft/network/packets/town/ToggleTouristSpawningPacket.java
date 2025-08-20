package com.quackers29.businesscraft.network.packets.town;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
// TownInterfaceEntity access through BlockEntityHelper platform services
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet for toggling tourist spawning in a town.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class ToggleTouristSpawningPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ToggleTouristSpawningPacket.class);

    /**
     * Create packet for sending.
     */
    public ToggleTouristSpawningPacket(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public ToggleTouristSpawningPacket(Object buffer) {
        super(buffer);
    }

    /**
     * Handle the packet on the server side.
     * This method contains the core business logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        // Enhanced MultiLoader: Use platform services for cross-platform compatibility
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        if (blockEntity != null) {
            Object townDataProvider = PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
            if (townDataProvider != null) {
                // Platform-agnostic business logic through platform services
                boolean currentState = PlatformServices.getBlockEntityHelper().isTouristSpawningEnabled(townDataProvider);
                boolean newState = !currentState;
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Toggling tourist spawning to {} at position ({}, {}, {})", newState, x, y, z);
                
                // Use platform services for cross-platform compatibility
                PlatformServices.getBlockEntityHelper().setTouristSpawningEnabled(townDataProvider, newState);
                PlatformServices.getBlockEntityHelper().markTownDataDirty(townDataProvider);
            } else {
                LOGGER.warn("No town data provider found at position ({}, {}, {})", x, y, z);
            }
        } else {
            LOGGER.warn("No block entity found at position ({}, {}, {}) for tourist spawning toggle", x, y, z);
        }
    }
    
    /**
     * Encode packet data for network transmission.
     * Uses base implementation which handles BlockPos serialization.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Writes BlockPos
        // No additional data for this packet
    }
}