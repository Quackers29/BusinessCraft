package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Client-to-server packet to request opening the Platform Destinations UI.
 * This manages platform destination selection and configuration.
 * 
 * Unified architecture approach: Server gathers town data and sends back a RefreshDestinationsPacket
 * which opens the UI directly on the client side.
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
     * Unified Architecture approach: Direct access to TownInterfaceData without BlockEntityHelper abstraction.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing Destinations UI request for platform '{}' at position ({}, {}, {})", platformId, x, y, z);
        
        try {
            // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
            com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
            
            if (townData != null) {
                // Basic validation through unified architecture
                if (!townData.isTownRegistered()) {
                    LOGGER.warn("Town not registered at position ({}, {}, {}) for Destinations UI", x, y, z);
                    return;
                }
                
                // Complex operations (town lookups, distance calculations) still use platform services
                // Send refresh destinations packet with updated data
                PlatformServices.getNetworkHelper().sendRefreshDestinationsPacket(
                    player, x, y, z, platformId, 
                    new java.util.HashMap<>(), // townDestinations - will be populated by server
                    new java.util.HashMap<>(), // townNames - will be populated by server  
                    new java.util.HashMap<>(), // distances - will be populated by server
                    new java.util.HashMap<>()  // directions - will be populated by server
                );
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully sent destinations data for platform '{}' at ({}, {}, {})", 
                    platformId, x, y, z);
            } else {
                LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for Destinations UI", x, y, z);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to process destinations data for platform '{}' at ({}, {}, {}): {}", 
                platformId, x, y, z, e.getMessage());
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
}