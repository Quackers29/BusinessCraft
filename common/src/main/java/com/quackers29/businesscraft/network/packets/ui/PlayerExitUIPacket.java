package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to notify when a player exits the Town Interface UI.
 * This ensures proper cleanup of UI-related resources and visualization systems.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class PlayerExitUIPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerExitUIPacket.class);
    private final int x, y, z;
    
    /**
     * Create packet for sending.
     */
    public PlayerExitUIPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static PlayerExitUIPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        return new PlayerExitUIPacket(pos[0], pos[1], pos[2]);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
    }
    
    /**
     * Handle the packet on the server side.
     * This method registers player UI exit with the town interface entity for proper cleanup.
     */
    public void handle(Object player) {
        LOGGER.debug("Player exiting UI at position [{}, {}, {}]", x, y, z);
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
        if (blockEntity == null) {
            LOGGER.error("Failed to get TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Register player UI exit through platform services
        boolean success = PlatformServices.getBlockEntityHelper().registerPlayerExitUI(blockEntity, player);
        
        if (success) {
            LOGGER.debug("Successfully registered player UI exit at [{}, {}, {}]", x, y, z);
        } else {
            LOGGER.warn("Failed to register player UI exit at [{}, {}, {}]", x, y, z);
        }
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}