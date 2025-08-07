package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic server-to-client packet for town map data response.
 * This sends town boundary and structure data to update the client-side town map modal.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class TownMapDataResponsePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownMapDataResponsePacket.class);
    
    private final String mapData; // JSON or serialized map data
    private final int zoomLevel;
    private final boolean isSuccess;
    
    /**
     * Create packet for sending successful response.
     */
    public TownMapDataResponsePacket(int x, int y, int z, String mapData, int zoomLevel) {
        super(x, y, z);
        this.mapData = mapData != null ? mapData : "{}";
        this.zoomLevel = zoomLevel;
        this.isSuccess = true;
    }
    
    /**
     * Create packet for sending error response.
     */
    public TownMapDataResponsePacket(int x, int y, int z, boolean isSuccess) {
        super(x, y, z);
        this.mapData = "{}";
        this.zoomLevel = 1;
        this.isSuccess = isSuccess;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static TownMapDataResponsePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean isSuccess = PlatformServices.getNetworkHelper().readBoolean(buffer);
        String mapData = PlatformServices.getNetworkHelper().readString(buffer);
        int zoomLevel = PlatformServices.getNetworkHelper().readInt(buffer);
        
        if (isSuccess) {
            return new TownMapDataResponsePacket(pos[0], pos[1], pos[2], mapData, zoomLevel);
        } else {
            return new TownMapDataResponsePacket(pos[0], pos[1], pos[2], false);
        }
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, isSuccess);
        PlatformServices.getNetworkHelper().writeString(buffer, mapData);
        PlatformServices.getNetworkHelper().writeInt(buffer, zoomLevel);
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the client-side town map modal with the received data.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Processing town map data response (zoom: {}, success: {}) at position [{}, {}, {}]", 
                    zoomLevel, isSuccess, x, y, z);
        
        if (!isSuccess) {
            LOGGER.warn("Received failed town map data response at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Update client-side town map modal through platform services
        boolean success = PlatformServices.getBlockEntityHelper().updateTownMapUI(player, x, y, z, mapData, zoomLevel);
        
        if (success) {
            LOGGER.debug("Successfully updated town map UI (zoom: {}) at [{}, {}, {}]", 
                        zoomLevel, x, y, z);
        } else {
            LOGGER.warn("Failed to update town map UI at [{}, {}, {}]", x, y, z);
        }
    }
    
    // Getters for testing
    public String getMapData() { return mapData; }
    public int getZoomLevel() { return zoomLevel; }
    public boolean isSuccess() { return isSuccess; }
}