package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for setting a platform's path coordinates.
 * 
 * Unified Architecture approach: Direct access to TownInterfaceEntity methods
 * instead of platform service wrapper calls.
 */
public class SetPlatformPathPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetPlatformPathPacket.class);
    private final String platformId;
    private final int startX, startY, startZ;
    private final int endX, endY, endZ;
    
    /**
     * Create packet for sending.
     */
    public SetPlatformPathPacket(int blockX, int blockY, int blockZ, String platformId,
                               int startX, int startY, int startZ,
                               int endX, int endY, int endZ) {
        super(blockX, blockY, blockZ);
        this.platformId = platformId;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.endX = endX;
        this.endY = endY;
        this.endZ = endZ;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static SetPlatformPathPacket decode(Object buffer) {
        int[] blockPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        int[] startPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        int[] endPos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        
        return new SetPlatformPathPacket(
            blockPos[0], blockPos[1], blockPos[2], platformId,
            startPos[0], startPos[1], startPos[2],
            endPos[0], endPos[1], endPos[2]
        );
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, startX, startY, startZ);
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, endX, endY, endZ);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Player is setting platform {} path from [{}, {}, {}] to [{}, {}, {}] at [{}, {}, {}]", 
                    platformId, startX, startY, startZ, endX, endY, endZ, x, y, z);
        
        // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
        com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
        
        if (townData != null) {
            // Direct business logic access - no abstraction layer needed
            boolean success = townData.setPlatformPath(platformId, startX, startY, startZ, endX, endY, endZ);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully set platform {} path from ({}, {}, {}) to ({}, {}, {}) at ({}, {}, {})", 
                            platformId, startX, startY, startZ, endX, endY, endZ, x, y, z);
                
                // Platform-specific operations still use platform services
                PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
            } else {
                LOGGER.warn("Failed to set platform {} path at ({}, {}, {}) - platform not found", platformId, x, y, z);
            }
        } else {
            LOGGER.warn("No TownInterfaceData found at position ({}, {}, {}) for platform path setting", x, y, z);
        }
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getStartZ() { return startZ; }
    public int getEndX() { return endX; }
    public int getEndY() { return endY; }
    public int getEndZ() { return endZ; }
}