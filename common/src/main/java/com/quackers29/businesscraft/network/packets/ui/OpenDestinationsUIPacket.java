package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to request opening the Platform Destinations UI.
 * This manages platform destination selection and configuration.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class OpenDestinationsUIPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenDestinationsUIPacket.class);
    private final String platformId;
    
    /**
     * Create packet for sending.
     */
    public OpenDestinationsUIPacket(int x, int y, int z, String platformId) {
        super(x, y, z);
        this.platformId = platformId != null ? platformId : "";
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static OpenDestinationsUIPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readString(buffer);
        return new OpenDestinationsUIPacket(pos[0], pos[1], pos[2], platformId);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeString(buffer, platformId);
    }
    
    /**
     * Handle the packet on the server side.
     * This method opens the platform destinations UI for the specified platform.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Opening Destinations UI for platform '{}' at position [{}, {}, {}]", platformId, x, y, z);
        
        // Get the town interface entity using unified architecture pattern
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
        
        // Open the destinations UI through platform services
        boolean success = PlatformServices.getBlockEntityHelper().openDestinationsUI(townDataProvider, player, platformId);
        
        if (success) {
            LOGGER.debug("Successfully opened Destinations UI for platform '{}' at [{}, {}, {}]", platformId, x, y, z);
        } else {
            LOGGER.warn("Failed to open Destinations UI for platform '{}' at [{}, {}, {}]", platformId, x, y, z);
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
}