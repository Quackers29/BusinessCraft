package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to request town platform positioning data.
 * This is used by the sophisticated town map modal to display platform connections,
 * destinations, and transportation network visualization.
 * 
 * This packet was temporarily removed during Enhanced MultiLoader Template migration
 * and is now restored in the common module for cross-platform compatibility.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class RequestTownPlatformDataPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTownPlatformDataPacket.class);
    
    private final boolean includePlatformConnections;
    private final boolean includeDestinationTowns;
    private final int maxRadius; // Maximum search radius for connected towns
    
    /**
     * Create packet for requesting comprehensive platform data.
     * 
     * @param x Town X coordinate
     * @param y Town Y coordinate
     * @param z Town Z coordinate
     * @param includePlatformConnections Include platform layout and connections
     * @param includeDestinationTowns Include destination town information
     * @param maxRadius Maximum radius to search for connected towns
     */
    public RequestTownPlatformDataPacket(int x, int y, int z, 
                                       boolean includePlatformConnections, 
                                       boolean includeDestinationTowns, 
                                       int maxRadius) {
        super(x, y, z);
        this.includePlatformConnections = includePlatformConnections;
        this.includeDestinationTowns = includeDestinationTowns;
        this.maxRadius = maxRadius;
    }
    
    /**
     * Create packet with default parameters for standard map view.
     */
    public RequestTownPlatformDataPacket(int x, int y, int z) {
        this(x, y, z, true, true, 5000); // Default 5000 block radius
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static RequestTownPlatformDataPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean includePlatformConnections = PlatformServices.getNetworkHelper().readBoolean(buffer);
        boolean includeDestinationTowns = PlatformServices.getNetworkHelper().readBoolean(buffer);
        int maxRadius = PlatformServices.getNetworkHelper().readInt(buffer);
        
        return new RequestTownPlatformDataPacket(pos[0], pos[1], pos[2], 
                                               includePlatformConnections, 
                                               includeDestinationTowns, 
                                               maxRadius);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, includePlatformConnections);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, includeDestinationTowns);
        PlatformServices.getNetworkHelper().writeInt(buffer, maxRadius);
    }
    
    /**
     * Handle the packet on the server side.
     * This method processes the platform data request and sends back comprehensive
     * platform and destination information for map visualization.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Processing platform data request at position [{}, {}, {}] with radius {}", 
                    x, y, z, maxRadius);
        
        try {
            // Use platform services to handle the platform data request
            boolean success = PlatformServices.getBlockEntityHelper().processPlatformDataRequest(
                player, x, y, z, includePlatformConnections, includeDestinationTowns, maxRadius);
            
            if (success) {
                LOGGER.debug("Successfully processed platform data request at [{}, {}, {}]", x, y, z);
            } else {
                LOGGER.warn("Failed to process platform data request at [{}, {}, {}]", x, y, z);
                
                // Send empty response to prevent client hanging
                TownPlatformDataResponsePacket errorResponse = 
                    new TownPlatformDataResponsePacket(x, y, z, false);
                PlatformServices.getNetworkHelper().sendToClient(errorResponse, player);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while processing platform data request at [{}, {}, {}]: {}", 
                        x, y, z, e.getMessage());
            
            // Send error response
            TownPlatformDataResponsePacket errorResponse = 
                new TownPlatformDataResponsePacket(x, y, z, false);
            PlatformServices.getNetworkHelper().sendToClient(errorResponse, player);
        }
    }
    
    // Getters for server-side processing
    public boolean includePlatformConnections() { return includePlatformConnections; }
    public boolean includeDestinationTowns() { return includeDestinationTowns; }
    public int getMaxRadius() { return maxRadius; }
}