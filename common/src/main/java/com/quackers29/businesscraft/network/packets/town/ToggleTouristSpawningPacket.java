package com.quackers29.businesscraft.network.packets.town;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
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
        // Unified Architecture: Direct access to TownInterfaceEntity (replaces 5 BlockEntityHelper calls)
        TownInterfaceEntity townInterface = getTownInterfaceEntity(player);
        
        if (townInterface != null) {
            // Core business logic - platform-agnostic
            boolean currentState = townInterface.isTouristSpawningEnabled();
            boolean newState = !currentState;
            
            LOGGER.debug("Toggling tourist spawning to {} at position ({}, {}, {})", newState, x, y, z);
            
            // Direct unified access - no platform service bridge needed!
            townInterface.setTouristSpawningEnabled(newState);
            townInterface.setChanged();
            townInterface.syncToClient();
        } else {
            LOGGER.warn("No TownInterfaceEntity found at position ({}, {}, {}) for tourist spawning toggle", x, y, z);
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