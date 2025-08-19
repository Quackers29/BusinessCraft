package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

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
        
        // Get the town interface entity using unified architecture pattern
        TownInterfaceEntity townInterface = getTownInterfaceEntity(player);
        if (townInterface == null) {
            LOGGER.error("Failed to get TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Update client-side town map modal through platform services
        // NOTE: Platform service still uses old signature - keeping for compatibility
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
    
    /**
     * Structured data class for sophisticated town map information.
     * Contains detailed town data for enhanced map rendering capabilities.
     * Uses primitive coordinates for Enhanced MultiLoader compatibility.
     */
    public static class TownMapInfo {
        public final UUID townId;
        public final String name;
        public final int x, y, z;
        public final int population;
        public final int visitCount;
        public final long lastVisited;
        public final boolean isCurrentTown;
        
        public TownMapInfo(UUID townId, String name, int x, int y, int z, int population, int visitCount, long lastVisited, boolean isCurrentTown) {
            this.townId = townId;
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.population = population;
            this.visitCount = visitCount;
            this.lastVisited = lastVisited;
            this.isCurrentTown = isCurrentTown;
        }
        
        // Helper method to get position as BlockPos in platform code
        public Object getPosition() {
            return PlatformServices.getPlatformHelper().createBlockPos(x, y, z);
        }
    }
    
    /**
     * Structured data class for platform information in sophisticated map view.
     * Used for transportation network visualization.
     * Uses primitive coordinates for Enhanced MultiLoader compatibility.
     */
    public static class PlatformInfo {
        public final UUID platformId;
        public final int x, y, z;
        public final String destinationName;
        public final UUID destinationTownId;
        public final boolean isEnabled;
        public final int[] pathPoints; // Flattened array of x,z coordinates for path visualization
        
        public PlatformInfo(UUID platformId, int x, int y, int z, String destinationName, UUID destinationTownId, boolean isEnabled, int[] pathPoints) {
            this.platformId = platformId;
            this.x = x;
            this.y = y;
            this.z = z;
            this.destinationName = destinationName;
            this.destinationTownId = destinationTownId;
            this.isEnabled = isEnabled;
            this.pathPoints = pathPoints != null ? pathPoints : new int[0];
        }
        
        // Helper method to get position as BlockPos in platform code
        public Object getPosition() {
            return PlatformServices.getPlatformHelper().createBlockPos(x, y, z);
        }
    }
    
    /**
     * Structured data class for live town information including boundaries.
     * Used for advanced map features like territory visualization.
     * Uses primitive coordinates for Enhanced MultiLoader compatibility.
     */
    public static class TownInfo {
        public final UUID townId;
        public final String name;
        public final int centerX, centerY, centerZ;
        public final int[] boundaryCorners; // Flattened array of x,z coordinates defining town boundary
        public final int detectionRadius;
        public final boolean hasBoundaryVisualization;
        
        public TownInfo(UUID townId, String name, int centerX, int centerY, int centerZ, int[] boundaryCorners, int detectionRadius, boolean hasBoundaryVisualization) {
            this.townId = townId;
            this.name = name;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.boundaryCorners = boundaryCorners != null ? boundaryCorners : new int[0];
            this.detectionRadius = detectionRadius;
            this.hasBoundaryVisualization = hasBoundaryVisualization;
        }
        
        // Helper method to get center position as BlockPos in platform code
        public Object getCenterPosition() {
            return PlatformServices.getPlatformHelper().createBlockPos(centerX, centerY, centerZ);
        }
    }
}