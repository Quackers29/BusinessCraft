package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
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
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing boundary sync request (enable: {}, distance: {}) at position [{}, {}, {}]", 
                    enableVisualization, renderDistance, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Basic validation through unified architecture
            if (!townData.isTownRegistered()) {
                LOGGER.warn("Town not registered at position ({}, {}, {}) for boundary sync", x, y, z);
                return;
            }
            
            // Boundary visualization is platform-specific (client rendering)
            // Still use platform services for complex visualization operations
            Object townDataProvider = getTownDataProvider(getBlockEntity(player));
            boolean success = PlatformServices.getBlockEntityHelper().processBoundarySyncRequest(
                townDataProvider, player, enableVisualization, renderDistance);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully processed boundary sync request (enable: {}, distance: {}) at ({}, {}, {})", 
                            enableVisualization, renderDistance, x, y, z);
            } else {
                LOGGER.warn("Failed to process boundary sync request (enable: {}, distance: {}) at ({}, {}, {})", 
                           enableVisualization, renderDistance, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for boundary sync", x, y, z);
        }
    }
    
    // Getters for testing
    public boolean isEnableVisualization() { return enableVisualization; }
    public int getRenderDistance() { return renderDistance; }
}