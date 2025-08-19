package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to request boundary visualization synchronization.
 * This is used to sync town boundary visualization data between client and server.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class BoundarySyncRequestPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BoundarySyncRequestPacket.class);
    
    private final boolean enableVisualization;
    private final int renderDistance;
    
    /**
     * Create packet for sending.
     */
    public BoundarySyncRequestPacket(int x, int y, int z, boolean enableVisualization, int renderDistance) {
        super(x, y, z);
        this.enableVisualization = enableVisualization;
        this.renderDistance = Math.max(1, Math.min(renderDistance, 32)); // Clamp between 1-32
    }
    
    /**
     * Create packet with default parameters.
     */
    public BoundarySyncRequestPacket(int x, int y, int z, boolean enableVisualization) {
        this(x, y, z, enableVisualization, 16);
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static BoundarySyncRequestPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean enableVisualization = PlatformServices.getNetworkHelper().readBoolean(buffer);
        int renderDistance = PlatformServices.getNetworkHelper().readInt(buffer);
        return new BoundarySyncRequestPacket(pos[0], pos[1], pos[2], enableVisualization, renderDistance);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, enableVisualization);
        PlatformServices.getNetworkHelper().writeInt(buffer, renderDistance);
    }
    
    /**
     * Handle the packet on the server side.
     * This method processes the boundary sync request and sends updated boundary data to the client.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Processing boundary sync request (enable: {}, distance: {}) at position [{}, {}, {}]", 
                    enableVisualization, renderDistance, x, y, z);
        
        // Get the town interface entity using unified architecture pattern
        TownInterfaceEntity townInterface = getTownInterfaceEntity(player);
        if (townInterface == null) {
            LOGGER.error("Failed to get TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Process boundary sync request through platform services
        boolean success = PlatformServices.getBlockEntityHelper().processBoundarySyncRequest(
            townInterface, player, enableVisualization, renderDistance);
        
        if (success) {
            LOGGER.debug("Successfully processed boundary sync request (enable: {}, distance: {}) at [{}, {}, {}]", 
                        enableVisualization, renderDistance, x, y, z);
        } else {
            LOGGER.warn("Failed to process boundary sync request (enable: {}, distance: {}) at [{}, {}, {}]", 
                       enableVisualization, renderDistance, x, y, z);
        }
    }
    
    // Getters for testing
    public boolean isEnableVisualization() { return enableVisualization; }
    public int getRenderDistance() { return renderDistance; }
}