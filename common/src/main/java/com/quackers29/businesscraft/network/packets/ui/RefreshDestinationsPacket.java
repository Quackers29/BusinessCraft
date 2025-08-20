package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

/**
 * Server-to-client packet to refresh platform destination data and open destinations UI.
 * Uses unified architecture approach - direct client-side handling like main branch.
 */
public class RefreshDestinationsPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(RefreshDestinationsPacket.class);
    private final String platformId;
    private final String platformName;
    private final Map<UUID, String> townNames = new HashMap<>();
    private final Map<UUID, Boolean> enabledState = new HashMap<>();
    private final Map<UUID, Integer> townDistances = new HashMap<>();
    private final Map<UUID, String> townDirections = new HashMap<>();
    
    /**
     * Create packet for sending with town data.
     */
    public RefreshDestinationsPacket(int x, int y, int z, String platformId, String platformName) {
        super(x, y, z);
        this.platformId = platformId != null ? platformId : "";
        this.platformName = platformName != null ? platformName : "Platform";
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     */
    public static RefreshDestinationsPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readString(buffer);
        String platformName = PlatformServices.getNetworkHelper().readString(buffer);
        
        RefreshDestinationsPacket packet = new RefreshDestinationsPacket(pos[0], pos[1], pos[2], platformId, platformName);
        
        // Read town data
        int size = PlatformServices.getNetworkHelper().readInt(buffer);
        for (int i = 0; i < size; i++) {
            String townIdStr = PlatformServices.getNetworkHelper().readUUID(buffer);
            UUID townId = UUID.fromString(townIdStr);
            String name = PlatformServices.getNetworkHelper().readString(buffer);
            boolean enabled = PlatformServices.getNetworkHelper().readBoolean(buffer);
            int distance = PlatformServices.getNetworkHelper().readInt(buffer);
            String direction = PlatformServices.getNetworkHelper().readString(buffer);
            packet.addTown(townId, name, enabled, distance, direction);
        }
        
        return packet;
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeString(buffer, platformId);
        PlatformServices.getNetworkHelper().writeString(buffer, platformName);
        
        // Write town data
        PlatformServices.getNetworkHelper().writeInt(buffer, townNames.size());
        for (Map.Entry<UUID, String> entry : townNames.entrySet()) {
            UUID townId = entry.getKey();
            PlatformServices.getNetworkHelper().writeUUID(buffer, townId.toString());
            PlatformServices.getNetworkHelper().writeString(buffer, entry.getValue());
            PlatformServices.getNetworkHelper().writeBoolean(buffer, enabledState.getOrDefault(townId, false));
            PlatformServices.getNetworkHelper().writeInt(buffer, townDistances.getOrDefault(townId, 0));
            PlatformServices.getNetworkHelper().writeString(buffer, townDirections.getOrDefault(townId, ""));
        }
    }
    
    /**
     * Add town data to this packet
     */
    public void addTown(UUID townId, String name, boolean enabled, int distance, String direction) {
        townNames.put(townId, name);
        enabledState.put(townId, enabled);
        townDistances.put(townId, distance);
        townDirections.put(townId, direction);
    }
    
    /**
     * Handle the packet on the client side.
     * Opens the destinations UI directly using unified architecture approach.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Refreshing destinations for platform '{}' at position [{}, {}, {}]", platformId, x, y, z);
        LOGGER.info("DEBUG DESTINATIONS CLIENT: Received packet with platform: '{}', town count: {}", platformName, townNames.size());
        
        // Debug log all received town data
        for (java.util.Map.Entry<java.util.UUID, String> entry : townNames.entrySet()) {
            java.util.UUID townId = entry.getKey();
            String townName = entry.getValue();
            boolean enabled = enabledState.getOrDefault(townId, false);
            int distance = townDistances.getOrDefault(townId, 0);
            String direction = townDirections.getOrDefault(townId, "");
            
            LOGGER.info("DEBUG DESTINATIONS CLIENT: Town data - '{}': enabled={}, distance={}m, direction={}", 
                townName, enabled, distance, direction);
        }
        
        // Delegate to platform services for unified handling - but simplified approach
        PlatformServices.getBlockEntityHelper().openDestinationsUI(x, y, z, platformId, platformName, townNames, enabledState, townDistances, townDirections);
    }
    
    // Getters
    public String getPlatformId() { return platformId; }
    public String getPlatformName() { return platformName; }
    public Map<UUID, String> getTownNames() { return townNames; }
    public Map<UUID, Boolean> getEnabledState() { return enabledState; }
    public Map<UUID, Integer> getTownDistances() { return townDistances; }
    public Map<UUID, String> getTownDirections() { return townDirections; }
}