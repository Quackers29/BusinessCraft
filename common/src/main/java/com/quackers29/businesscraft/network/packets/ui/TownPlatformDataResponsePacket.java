package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.client.cache.ClientTownMapCache;
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
    
    private final String platformData; // JSON serialized platform connection data
    private final String destinationData; // JSON serialized destination town data
    private final boolean isSuccess;
    private final int searchRadius;
    
    /**
     * Create packet for sending successful platform data response.
     * 
     * @param x Town X coordinate
     * @param y Town Y coordinate  
     * @param z Town Z coordinate
     * @param platformData JSON string with platform layout and connections
     * @param destinationData JSON string with destination towns and distances
     * @param searchRadius Radius used for the search
     */
    public TownPlatformDataResponsePacket(int x, int y, int z, 
                                         String platformData, 
                                         String destinationData, 
                                         int searchRadius) {
        super(x, y, z);
        this.platformData = platformData != null ? platformData : "{}";
        this.destinationData = destinationData != null ? destinationData : "{}";
        this.isSuccess = true;
        this.searchRadius = searchRadius;
    }
    
    /**
     * Create packet for sending error response.
     */
    public TownPlatformDataResponsePacket(int x, int y, int z, boolean isSuccess) {
        super(x, y, z);
        this.platformData = "{}";
        this.destinationData = "{}";
        this.isSuccess = isSuccess;
        this.searchRadius = 0;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static TownPlatformDataResponsePacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        boolean isSuccess = PlatformServices.getNetworkHelper().readBoolean(buffer);
        String platformData = PlatformServices.getNetworkHelper().readString(buffer);
        String destinationData = PlatformServices.getNetworkHelper().readString(buffer);
        int searchRadius = PlatformServices.getNetworkHelper().readInt(buffer);
        
        if (isSuccess) {
            return new TownPlatformDataResponsePacket(pos[0], pos[1], pos[2], 
                                                    platformData, destinationData, searchRadius);
        } else {
            return new TownPlatformDataResponsePacket(pos[0], pos[1], pos[2], false);
        }
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeBoolean(buffer, isSuccess);
        PlatformServices.getNetworkHelper().writeString(buffer, platformData);
        PlatformServices.getNetworkHelper().writeString(buffer, destinationData);
        PlatformServices.getNetworkHelper().writeInt(buffer, searchRadius);
    }
    
    /**
     * Handle the packet on the client side.
     * This method updates the client-side town map cache and modal with the received
     * platform and destination data for sophisticated map visualization.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Processing platform data response (success: {}) at position [{}, {}, {}]", 
                    isSuccess, x, y, z);
        
        if (!isSuccess) {
            LOGGER.warn("Received failed platform data response at [{}, {}, {}]", x, y, z);
            return;
        }
        
        try {
            // Update ClientTownMapCache with the received data
            updateTownMapCache();
            
            // Update the map UI through platform services
            boolean success = PlatformServices.getBlockEntityHelper().updateTownPlatformUI(
                player, x, y, z, platformData, destinationData);
            
            if (success) {
                LOGGER.debug("Successfully updated platform UI at [{}, {}, {}]", x, y, z);
            } else {
                LOGGER.warn("Failed to update platform UI at [{}, {}, {}]", x, y, z);
            }
        } catch (Exception e) {
            LOGGER.error("Exception while processing platform data response at [{}, {}, {}]: {}", 
                        x, y, z, e.getMessage());
        }
    }
    
    /**
     * Update the ClientTownMapCache with received platform and destination data.
     * This enables sophisticated map features like town markers, distance calculation,
     * and transportation network visualization.
     */
    private void updateTownMapCache() {
        try {
            ClientTownMapCache cache = ClientTownMapCache.getInstance();
            
            // Parse and cache destination town data
            if (destinationData != null && !destinationData.equals("{}")) {
                Map<String, Object> destinations = parseDestinationData(destinationData);
                
                for (Map.Entry<String, Object> entry : destinations.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> townInfo = (Map<String, Object>) entry.getValue();
                    
                    // Extract town information
                    UUID townId = UUID.fromString((String) townInfo.get("id"));
                    String townName = (String) townInfo.get("name");
                    int townX = ((Number) townInfo.get("x")).intValue();
                    int townY = ((Number) townInfo.get("y")).intValue();
                    int townZ = ((Number) townInfo.get("z")).intValue();
                    
                    // Include platform and connection data as additional data
                    Map<String, Object> additionalData = new HashMap<>();
                    additionalData.put("platforms", platformData);
                    additionalData.put("searchRadius", searchRadius);
                    if (townInfo.containsKey("distance")) {
                        additionalData.put("distance", townInfo.get("distance"));
                    }
                    if (townInfo.containsKey("connections")) {
                        additionalData.put("connections", townInfo.get("connections"));
                    }
                    
                    // Cache the town data
                    cache.cacheTownData(townId, townName, townX, townY, townZ, additionalData);
                }
                
                LOGGER.debug("Updated ClientTownMapCache with {} destination towns", destinations.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update ClientTownMapCache: {}", e.getMessage());
        }
    }
    
    /**
     * Parse destination data JSON string into a map structure.
     * This implements basic JSON parsing for town data without external dependencies.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseDestinationData(String jsonData) {
        Map<String, Object> result = new HashMap<>();
        
        if (jsonData == null || jsonData.trim().equals("{}") || jsonData.trim().isEmpty()) {
            return result;
        }
        
        try {
            // Remove outer braces and whitespace
            String content = jsonData.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            
            content = content.trim();
            if (content.isEmpty()) {
                return result;
            }
            
            // Simple parser for the specific JSON structure we generate
            // Format: "townId":{"id":"uuid","name":"Name","x":123,"y":64,"z":456,"distance":789}
            String[] townEntries = content.split("},");
            
            for (String townEntry : townEntries) {
                try {
                    // Handle last entry that doesn't end with comma
                    if (!townEntry.endsWith("}")) {
                        townEntry += "}";
                    }
                    
                    // Extract town ID (the key)
                    int colonIndex = townEntry.indexOf(":");
                    if (colonIndex == -1) continue;
                    
                    String townIdKey = townEntry.substring(0, colonIndex).trim();
                    townIdKey = townIdKey.replaceAll("\"", ""); // Remove quotes
                    
                    // Parse the town data object
                    String townDataJson = townEntry.substring(colonIndex + 1).trim();
                    if (!townDataJson.startsWith("{") || !townDataJson.endsWith("}")) {
                        continue;
                    }
                    
                    Map<String, Object> townData = parseSimpleJsonObject(townDataJson);
                    if (!townData.isEmpty()) {
                        result.put(townIdKey, townData);
                    }
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse town entry '{}': {}", townEntry, e.getMessage());
                }
            }
            
            LOGGER.debug("Parsed {} towns from destination data", result.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to parse destination data JSON '{}': {}", jsonData, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Parse a simple JSON object like {"id":"uuid","name":"Name","x":123,"y":64,"z":456}
     */
    private Map<String, Object> parseSimpleJsonObject(String jsonObject) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Remove braces
            String content = jsonObject.trim();
            if (content.startsWith("{")) {
                content = content.substring(1);
            }
            if (content.endsWith("}")) {
                content = content.substring(0, content.length() - 1);
            }
            
            // Split by commas, being careful about quotes
            String[] pairs = splitJsonPairs(content);
            
            for (String pair : pairs) {
                int colonIndex = pair.indexOf(":");
                if (colonIndex == -1) continue;
                
                String key = pair.substring(0, colonIndex).trim();
                String value = pair.substring(colonIndex + 1).trim();
                
                // Remove quotes from key
                key = key.replaceAll("\"", "");
                
                // Parse value based on type
                Object parsedValue;
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    // String value
                    parsedValue = value.substring(1, value.length() - 1);
                } else {
                    // Numeric value
                    try {
                        parsedValue = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        try {
                            parsedValue = Double.parseDouble(value);
                        } catch (NumberFormatException e2) {
                            parsedValue = value; // Keep as string
                        }
                    }
                }
                
                result.put(key, parsedValue);
            }
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse JSON object '{}': {}", jsonObject, e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Split JSON key-value pairs, being careful about quoted strings with commas.
     */
    private String[] splitJsonPairs(String content) {
        java.util.List<String> pairs = new ArrayList<>();
        boolean inQuotes = false;
        int start = 0;
        
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                pairs.add(content.substring(start, i).trim());
                start = i + 1;
            }
        }
        
        // Add the last pair
        if (start < content.length()) {
            pairs.add(content.substring(start).trim());
        }
        
        return pairs.toArray(new String[0]);
    }
    
    // Getters for testing and debugging
    public String getPlatformData() { return platformData; }
    public String getDestinationData() { return destinationData; }
    public boolean isSuccess() { return isSuccess; }
    public int getSearchRadius() { return searchRadius; }
    
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
    }
}