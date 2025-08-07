package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic server-to-client packet to refresh platform destination data.
 * This updates the client-side destination cache for platform management UI.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class RefreshDestinationsPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshDestinationsPacket.class);
    private final int x, y, z;
    private final String platformId;
    private final String destinationData; // JSON or serialized destination data
    
    /**
     * Create packet for sending.
     */
    public RefreshDestinationsPacket(int x, int y, int z, String platformId, String destinationData) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.platformId = platformId != null ? platformId : "";
        this.destinationData = destinationData != null ? destinationData : "{}";
    }
    
    /**
     * Constructor for simple refresh without specific data.
     */
    public RefreshDestinationsPacket(int x, int y, int z) {
        this(x, y, z, "", "{}");
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static RefreshDestinationsPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readString(buffer);
        String destinationData = PlatformServices.getNetworkHelper().readString(buffer);
        return new RefreshDestinationsPacket(pos[0], pos[1], pos[2], platformId, destinationData);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
        PlatformServices.getNetworkHelper().writeString(buffer, platformId);
        PlatformServices.getNetworkHelper().writeString(buffer, destinationData);
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the client-side destination data cache.
     */
    public void handle(Object player) {
        LOGGER.debug("Refreshing destinations for platform '{}' at position [{}, {}, {}]", platformId, x, y, z);
        
        // Update client-side destination data through platform services
        boolean success = PlatformServices.getBlockEntityHelper().refreshDestinationData(player, x, y, z, platformId, destinationData);
        
        if (success) {
            LOGGER.debug("Successfully refreshed destinations for platform '{}' at [{}, {}, {}]", platformId, x, y, z);
        } else {
            LOGGER.warn("Failed to refresh destinations for platform '{}' at [{}, {}, {}]", platformId, x, y, z);
        }
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getPlatformId() { return platformId; }
    public String getDestinationData() { return destinationData; }
}