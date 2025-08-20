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
     * This method registers player UI exit with the town interface entity for proper cleanup.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Player exiting UI at position [{}, {}, {}]", x, y, z);
        
        // Get the town interface entity using platform services
        Object blockEntity = getBlockEntity(player);
        if (blockEntity == null) {
            LOGGER.error("No block entity found at position: [{}, {}, {}]", x, y, z);
            return;
        }

        Object townDataProvider = getTownDataProvider(blockEntity);
        if (townDataProvider == null) {
            LOGGER.error("Failed to get TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Register player UI exit through platform services
        boolean success = PlatformServices.getBlockEntityHelper().registerPlayerExitUI(townDataProvider, player);
        
        if (success) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully registered player UI exit at [{}, {}, {}]", x, y, z);
        } else {
            LOGGER.warn("Failed to register player UI exit at [{}, {}, {}]", x, y, z);
        }
    }
    
}