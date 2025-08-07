package com.quackers29.businesscraft.network.packets.town;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
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
        // Platform services will provide block entity access
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        
        if (blockEntity != null) {
            // Use platform services to access town interface functionality
            Object townDataProvider = PlatformServices.getBlockEntityHelper().getTownDataProvider(blockEntity);
            
            if (townDataProvider != null) {
                // This is the core business logic - platform-agnostic
                boolean currentState = PlatformServices.getBlockEntityHelper().isTouristSpawningEnabled(townDataProvider);
                boolean newState = !currentState;
                
                LOGGER.debug("Toggling tourist spawning to {} at position ({}, {}, {})", newState, x, y, z);
                
                // Update through platform services
                PlatformServices.getBlockEntityHelper().setTouristSpawningEnabled(townDataProvider, newState);
                PlatformServices.getBlockEntityHelper().markTownDataDirty(townDataProvider);
                PlatformServices.getBlockEntityHelper().syncTownData(blockEntity);
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