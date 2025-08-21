package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to notify when a player exits the Town Interface UI.
 * This ensures proper cleanup of UI-related resources and visualization systems.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class PlayerExitUIPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerExitUIPacket.class);
    
    /**
     * Create packet for sending.
     */
    public PlayerExitUIPacket(int x, int y, int z) {
        super(x, y, z);
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static PlayerExitUIPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        return new PlayerExitUIPacket(pos[0], pos[1], pos[2]);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing PlayerExitUIPacket at position ({}, {}, {})", x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // For UI exit, the primary concern is platform-specific cleanup
            // The town data itself doesn't need updates, but we need platform services for UI cleanup
            
            // Platform-specific operations for UI cleanup
            boolean success = PlatformServices.getBlockEntityHelper().registerPlayerExitUI(getBlockEntity(player), player);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully registered player UI exit at ({}, {}, {})", x, y, z);
            } else {
                LOGGER.warn("Failed to register player UI exit at ({}, {}, {}) - platform cleanup failed", x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for player UI exit", x, y, z);
        }
    }
    
}