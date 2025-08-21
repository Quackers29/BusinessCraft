package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.client.cache.ClientTownMapCache;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Platform-agnostic server-to-client packet for town platform data response.
 * This sends platform connections, destinations, and transportation network data
 * to update the sophisticated client-side town map modal.
 * 
 * This packet was temporarily removed during Enhanced MultiLoader Template migration
 * and is now restored in the common module for cross-platform compatibility.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class TownPlatformDataResponsePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownPlatformDataResponsePacket.class);
    
    private final UUID townId;
    private final Map<UUID, PlatformInfo> platforms = new HashMap<>();
    private TownInfo townInfo;
    private final boolean isSuccess;
    private final int searchRadius;
    
    /**
     * Create packet for sending successful platform data response.
     * 
     * @param x Town X coordinate
     * @param y Town Y coordinate  
     * @param z Town Z coordinate
     * @param townId Town UUID
     * @param searchRadius Radius used for the search
     */
    public TownPlatformDataResponsePacket(int x, int y, int z, 
                                         UUID townId, 
                                         int searchRadius) {
        super(x, y, z);
        this.townId = townId;
        this.isSuccess = true;
        this.searchRadius = searchRadius;
    }
    
    /**
     * Create packet for sending error response.
     */
    public TownPlatformDataResponsePacket(int x, int y, int z, boolean isSuccess) {
        super(x, y, z);
        this.townId = null;
        this.isSuccess = isSuccess;
        this.searchRadius = 0;
    }
    
    /**
     * Add platform data to the packet
     */
    public void addPlatform(UUID platformId, String name, boolean enabled, 
                           int[] startPos, int[] endPos, Set<UUID> enabledDestinations) {
        platforms.put(platformId, new PlatformInfo(platformId, name, enabled, startPos, endPos, enabledDestinations));
    }
    
    /**
     * Set town information
     */
    public void setTownInfo(String name, int population, int touristCount, int boundaryRadius) {
        this.townInfo = new TownInfo(name, population, touristCount, boundaryRadius);
    }
    
    public void setTownInfo(String name, int population, int touristCount, int boundaryRadius, int centerX, int centerY, int centerZ) {
        this.townInfo = new TownInfo(name, population, touristCount, boundaryRadius, centerX, centerY, centerZ);
    }
    
    /**
     * Get the town ID this data is for
     */
    public UUID getTownId() {
        return townId;
    }
    
    /**
     * Get all platform data
     */
    public Map<UUID, PlatformInfo> getPlatforms() {
        return platforms;
    }
    
    /**
     * Get town information
     */
    public TownInfo getTownInfo() {
        return townInfo;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static TownPlatformDataResponsePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean isSuccess = PlatformServices.getNetworkHelper().readBoolean(buffer);
        
        if (!isSuccess) {
            return new TownPlatformDataResponsePacket(pos[0], pos[1], pos[2], false);
        }
        
        UUID townId = UUID.fromString(PlatformServices.getNetworkHelper().readUUID(buffer));
        int searchRadius = PlatformServices.getNetworkHelper().readInt(buffer);
        
        TownPlatformDataResponsePacket packet = new TownPlatformDataResponsePacket(pos[0], pos[1], pos[2], townId, searchRadius);
        
        // Read town info
        boolean hasTownInfo = PlatformServices.getNetworkHelper().readBoolean(buffer);
        if (hasTownInfo) {
            String townName = PlatformServices.getNetworkHelper().readString(buffer);
            int population = PlatformServices.getNetworkHelper().readInt(buffer);
            int touristCount = PlatformServices.getNetworkHelper().readInt(buffer);
            int boundaryRadius = PlatformServices.getNetworkHelper().readInt(buffer);
            // Read center coordinates for boundary rendering
            int centerX = PlatformServices.getNetworkHelper().readInt(buffer);
            int centerY = PlatformServices.getNetworkHelper().readInt(buffer);
            int centerZ = PlatformServices.getNetworkHelper().readInt(buffer);
            packet.setTownInfo(townName, population, touristCount, boundaryRadius, centerX, centerY, centerZ);
        }
        
        // Read platforms
        int platformCount = PlatformServices.getNetworkHelper().readInt(buffer);
        for (int i = 0; i < platformCount; i++) {
            UUID platformId = UUID.fromString(PlatformServices.getNetworkHelper().readUUID(buffer));
            String name = PlatformServices.getNetworkHelper().readString(buffer);
            boolean enabled = PlatformServices.getNetworkHelper().readBoolean(buffer);
            int[] startPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
            int[] endPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
            
            // Read enabled destinations
            int destCount = PlatformServices.getNetworkHelper().readInt(buffer);
            Set<UUID> enabledDestinations = new HashSet<>();
            for (int j = 0; j < destCount; j++) {
                enabledDestinations.add(UUID.fromString(PlatformServices.getNetworkHelper().readUUID(buffer)));
            }
            
            packet.addPlatform(platformId, name, enabled, startPos, endPos, enabledDestinations);
        }
        
        return packet;
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, isSuccess);
        
        if (!isSuccess) {
            return;
        }
        
        PlatformServices.getNetworkHelper().writeUUID(buffer, townId.toString());
        PlatformServices.getNetworkHelper().writeInt(buffer, searchRadius);
        
        // Write town info
        PlatformServices.getNetworkHelper().writeBoolean(buffer, townInfo != null);
        if (townInfo != null) {
            PlatformServices.getNetworkHelper().writeString(buffer, townInfo.name);
            PlatformServices.getNetworkHelper().writeInt(buffer, townInfo.population);
            PlatformServices.getNetworkHelper().writeInt(buffer, townInfo.touristCount);
            PlatformServices.getNetworkHelper().writeInt(buffer, townInfo.boundaryRadius);
            // Write center coordinates for boundary rendering
            PlatformServices.getNetworkHelper().writeInt(buffer, townInfo.centerX);
            PlatformServices.getNetworkHelper().writeInt(buffer, townInfo.centerY);
            PlatformServices.getNetworkHelper().writeInt(buffer, townInfo.centerZ);
        }
        
        // Write platforms
        PlatformServices.getNetworkHelper().writeInt(buffer, platforms.size());
        for (PlatformInfo platform : platforms.values()) {
            PlatformServices.getNetworkHelper().writeUUID(buffer, platform.id.toString());
            PlatformServices.getNetworkHelper().writeString(buffer, platform.name);
            PlatformServices.getNetworkHelper().writeBoolean(buffer, platform.enabled);
            PlatformServices.getNetworkHelper().writeBlockPos(buffer, platform.startPos[0], platform.startPos[1], platform.startPos[2]);
            PlatformServices.getNetworkHelper().writeBlockPos(buffer, platform.endPos[0], platform.endPos[1], platform.endPos[2]);
            
            // Write enabled destinations
            PlatformServices.getNetworkHelper().writeInt(buffer, platform.enabledDestinations.size());
            for (UUID destId : platform.enabledDestinations) {
                PlatformServices.getNetworkHelper().writeUUID(buffer, destId.toString());
            }
        }
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the client-side town map cache and modal with the received
     * platform and destination data for sophisticated map visualization.
     */
    @Override
    public void handle(Object player) {
        LOGGER.warn("CLIENT PACKET HANDLER: Processing platform data response (success: {}) for town {} at position [{}, {}, {}]", 
                    isSuccess, townId, x, y, z);
        
        if (!isSuccess) {
            LOGGER.warn("CLIENT PACKET HANDLER: Received failed platform data response at [{}, {}, {}]", x, y, z);
            return;
        }
        
        try {
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Received structured platform data - {} platforms, townInfo: {}", 
                       platforms.size(), townInfo != null ? townInfo.name : "null");
            
            // Debug each platform
            if (!platforms.isEmpty()) {
                for (PlatformInfo platform : platforms.values()) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Platform {} - ID: {}, enabled: {}, startPos: [{},{},{}], endPos: [{},{},{}]",
                               platform.name, platform.id, platform.enabled, 
                               platform.startPos[0], platform.startPos[1], platform.startPos[2],
                               platform.endPos[0], platform.endPos[1], platform.endPos[2]);
                }
            }
            
            // Update ClientTownMapCache with the received structured data
            ClientTownMapCache cache = ClientTownMapCache.getInstance();
            
            // Store town data if available
            if (townInfo != null) {
                // Add town to cache with structured data
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Caching town data for {}: {} platforms, boundary radius {}", 
                           townInfo.name, platforms.size(), townInfo.boundaryRadius);
            }
            
            // Store platform data
            if (!platforms.isEmpty()) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Caching {} platforms for town {}", platforms.size(), townId);
                cache.updateTownPlatforms(townId, platforms);
            }
            
            // FIXED: Client-side packet handler should directly update the map modal
            // No need to lookup block entity on client - just update the open map modal
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Attempting to update platform UI through BlockEntityHelper...");
            boolean success = PlatformServices.getBlockEntityHelper().updateTownPlatformUIStructured(
                player, x, y, z, this);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Successfully updated platform UI with structured data at [{}, {}, {}]", x, y, z);
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "CLIENT PACKET HANDLER: Failed to update platform UI at [{}, {}, {}] - TownMapModal not open?", x, y, z);
            }
        } catch (Exception e) {
            LOGGER.error("CLIENT PACKET HANDLER: Exception while processing platform data response at [{}, {}, {}]: {}", 
                        x, y, z, e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Data class for platform information with Enhanced MultiLoader compatibility.
     * Uses int arrays for coordinates to avoid platform-specific BlockPos dependencies.
     */
    public static class PlatformInfo {
        public final UUID id;
        public final String name;
        public final boolean enabled;
        public final int[] startPos; // [x, y, z]
        public final int[] endPos;   // [x, y, z]
        public final Set<UUID> enabledDestinations;
        
        // Additional fields expected by TownMapModal for compatibility
        public final UUID platformId; // TownMapModal expects 'platformId' not 'id'
        public final boolean isEnabled; // TownMapModal expects 'isEnabled' not 'enabled'
        public final int x; // Platform start X coordinate
        public final int y; // Platform start Y coordinate
        public final int z; // Platform start Z coordinate  
        public final UUID destinationTownId; // Primary destination for path drawing
        public final String destinationName; // Name of destination for display
        public final int[] pathPoints; // Path coordinates for drawing
        
        public PlatformInfo(UUID id, String name, boolean enabled, int[] startPos, int[] endPos, Set<UUID> enabledDestinations) {
            this.id = id;
            this.name = name;
            this.enabled = enabled;
            this.startPos = startPos != null ? startPos.clone() : new int[]{0, 0, 0};
            this.endPos = endPos != null ? endPos.clone() : new int[]{0, 0, 0};
            this.enabledDestinations = new HashSet<>(enabledDestinations);
            
            // Set compatibility fields
            this.platformId = id;
            this.isEnabled = enabled;
            this.x = startPos != null ? startPos[0] : 0;
            this.y = startPos != null ? startPos[1] : 64;
            this.z = startPos != null ? startPos[2] : 0;
            this.destinationTownId = enabledDestinations.isEmpty() ? null : enabledDestinations.iterator().next();
            this.destinationName = name; // Use platform name as destination name for now
            this.pathPoints = (startPos != null && endPos != null) ? new int[]{
                startPos[0], startPos[2], // Start X,Z
                endPos[0], endPos[2]      // End X,Z  
            } : new int[]{0, 0, 0, 0};
        }
    }
    
    /**
     * Data class for town information including boundary data.
     */
    public static class TownInfo {
        public final String name;
        public final int population;
        public final int touristCount;
        public final int boundaryRadius;
        
        // Additional fields expected by TownMapModal for compatibility
        public final int centerX; // Town center X coordinate
        public final int centerY; // Town center Y coordinate  
        public final int centerZ; // Town center Z coordinate
        public final int detectionRadius; // Detection radius for map features
        
        public TownInfo(String name, int population, int touristCount, int boundaryRadius) {
            this.name = name;
            this.population = population;
            this.touristCount = touristCount;
            this.boundaryRadius = boundaryRadius;
            
            // Set compatibility fields with default values
            // TODO: Get actual town center coordinates from town data
            this.centerX = 0;
            this.centerY = 64;
            this.centerZ = 0;
            this.detectionRadius = boundaryRadius;
        }
        
        // Alternative constructor with center coordinates
        public TownInfo(String name, int population, int touristCount, int boundaryRadius, int centerX, int centerY, int centerZ) {
            this.name = name;
            this.population = population;
            this.touristCount = touristCount;
            this.boundaryRadius = boundaryRadius;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.detectionRadius = boundaryRadius;
        }
    }
}