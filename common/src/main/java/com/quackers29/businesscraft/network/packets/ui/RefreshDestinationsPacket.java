package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
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
public class RefreshDestinationsPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshDestinationsPacket.class);
    private final String platformId;
    private final String destinationData; // JSON or serialized destination data
    
    /**
     * Create packet for sending.
     */
    public RefreshDestinationsPacket(int x, int y, int z, String platformId, String destinationData) {
        super(x, y, z);
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
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeString(buffer, platformId);
        PlatformServices.getNetworkHelper().writeString(buffer, destinationData);
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the client-side destination data cache.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Refreshing destinations for platform '{}' at position [{}, {}, {}]", platformId, x, y, z);
        
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
        
        // Update client-side destination data through platform services
        // NOTE: Platform service still uses old signature - keeping for compatibility
        boolean success = PlatformServices.getBlockEntityHelper().refreshDestinationData(player, x, y, z, platformId, destinationData);
        
        if (success) {
            LOGGER.debug("Successfully refreshed destinations for platform '{}' at [{}, {}, {}]", platformId, x, y, z);
        } else {
            LOGGER.warn("Failed to refresh destinations for platform '{}' at [{}, {}, {}]", platformId, x, y, z);
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
    public String getDestinationData() { return destinationData; }
}