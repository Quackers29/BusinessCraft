package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic server-to-client packet to update boundary visualization data.
 * This is sent from server to client with updated boundary radius information.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific visualization updates through PlatformServices.
 */
public class BoundarySyncResponsePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundarySyncResponsePacket.class);
    
    private final int boundaryRadius;
    
    /**
     * Create packet for sending.
     */
    public BoundarySyncResponsePacket(int x, int y, int z, int boundaryRadius) {
        super(x, y, z);
        this.boundaryRadius = Math.max(0, boundaryRadius); // Ensure non-negative
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static BoundarySyncResponsePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        int boundaryRadius = PlatformServices.getNetworkHelper().readInt(buffer);
        return new BoundarySyncResponsePacket(pos[0], pos[1], pos[2], boundaryRadius);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeInt(buffer, boundaryRadius);
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the boundary visualization radius for the specified town.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
            "Boundary sync response for town at [{}, {}, {}]: updating to boundary radius={}", x, y, z, boundaryRadius);
        
        // Update boundary visualization through platform services
        boolean success = PlatformServices.getPlatformHelper().updateBoundaryVisualization(x, y, z, boundaryRadius);
        
        if (success) {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Successfully updated boundary visualization at [{}, {}, {}] to radius={}", x, y, z, boundaryRadius);
        } else {
            LOGGER.warn("Failed to update boundary visualization at [{}, {}, {}] to radius={}", x, y, z, boundaryRadius);
        }
    }
    
    // Getter for testing
    public int getBoundaryRadius() { return boundaryRadius; }
}