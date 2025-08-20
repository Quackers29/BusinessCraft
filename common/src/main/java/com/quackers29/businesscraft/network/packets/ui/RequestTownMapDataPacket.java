package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to request town map data.
 * This is used by the town map modal to display current town boundaries and structures.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class RequestTownMapDataPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownMapDataPacket.class);
    
    private final int zoomLevel;
    private final boolean includeStructures;
    
    /**
     * Create packet for sending.
     */
    public RequestTownMapDataPacket(int x, int y, int z, int zoomLevel, boolean includeStructures) {
        super(x, y, z);
        this.zoomLevel = zoomLevel;
        this.includeStructures = includeStructures;
    }
    
    /**
     * Create packet with default parameters.
     */
    public RequestTownMapDataPacket(int x, int y, int z) {
        this(x, y, z, 1, true);
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static RequestTownMapDataPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        int zoomLevel = PlatformServices.getNetworkHelper().readInt(buffer);
        boolean includeStructures = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new RequestTownMapDataPacket(pos[0], pos[1], pos[2], zoomLevel, includeStructures);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeInt(buffer, zoomLevel);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, includeStructures);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing town map data request (zoom: {}, structures: {}) at position ({}, {}, {})", 
                    zoomLevel, includeStructures, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Basic validation through unified architecture
            if (!townData.isTownRegistered()) {
                LOGGER.warn("Town not registered at position ({}, {}, {}) for map data request", x, y, z);
                return;
            }
            
            // Complex map data operations still use platform services (world data, structure analysis)
            Object townDataProvider = getTownDataProvider(getBlockEntity(player));
            boolean success = PlatformServices.getBlockEntityHelper().processTownMapDataRequest(
                townDataProvider, player, zoomLevel, includeStructures);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully processed town map data request (zoom: {}, structures: {}) at ({}, {}, {})", 
                            zoomLevel, includeStructures, x, y, z);
            } else {
                LOGGER.warn("Failed to process town map data request (zoom: {}, structures: {}) at ({}, {}, {})", 
                           zoomLevel, includeStructures, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for map data request", x, y, z);
        }
    }
    
    // Getters for testing
    public int getZoomLevel() { return zoomLevel; }
    public boolean isIncludeStructures() { return includeStructures; }
}