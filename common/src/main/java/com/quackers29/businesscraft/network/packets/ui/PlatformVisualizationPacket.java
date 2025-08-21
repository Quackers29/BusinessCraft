package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic server-to-client packet to enable platform and boundary visualization
 * for a specific town block for 30 seconds.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific visualization operations through PlatformServices.
 */
public class PlatformVisualizationPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlatformVisualizationPacket.class);
    
    /**
     * Create packet for sending.
     */
    public PlatformVisualizationPacket(int x, int y, int z) {
        super(x, y, z);
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static PlatformVisualizationPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        return new PlatformVisualizationPacket(pos[0], pos[1], pos[2]);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
    }
    
    /**
     * Handle the packet on the client side.
     * This method enables platform and boundary visualization for the specified town block.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Enabling platform/boundary visualization at position [{}, {}, {}]", x, y, z);
        
        // Use platform services to handle client-side visualization
        boolean success = PlatformServices.getPlatformHelper().enablePlatformVisualization(x, y, z);
        
        if (success) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Successfully enabled platform/boundary visualization at [{}, {}, {}]", x, y, z);
        } else {
            LOGGER.warn("Failed to enable platform/boundary visualization at [{}, {}, {}]", x, y, z);
        }
    }
}