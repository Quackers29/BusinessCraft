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
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing ToggleTouristSpawningPacket at position ({}, {}, {})", x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            boolean currentState = townData.isTouristSpawningEnabled();
            boolean newState = !currentState;
            
            townData.setTouristSpawningEnabled(newState);
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Tourist spawning toggled from {} to {} at position ({}, {}, {})", 
                        currentState, newState, x, y, z);
            
            // Platform-specific operations still use platform services
            PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for tourist spawning toggle", x, y, z);
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