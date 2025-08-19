package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for resetting a platform's path coordinates.
 * 
 * Unified Architecture approach: Direct access to TownInterfaceEntity methods
 * instead of platform service wrapper calls.
 */
public class ResetPlatformPathPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResetPlatformPathPacket.class);
    private final String platformId;
    
    /**
     * Create packet for sending.
     */
    public ResetPlatformPathPacket(int x, int y, int z, String platformId) {
        super(x, y, z);
        this.platformId = platformId;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static ResetPlatformPathPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
        return new ResetPlatformPathPacket(pos[0], pos[1], pos[2], platformId);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Player is resetting platform {} path at [{}, {}, {}]", platformId, x, y, z);
        
        // Get the town interface block entity using platform services
        Object blockEntity = getBlockEntity(player);
        if (blockEntity == null) {
            LOGGER.warn("No block entity found at [{}, {}, {}]", x, y, z);
            return;
        }

        Object townDataProvider = getTownDataProvider(blockEntity);
        if (townDataProvider == null) {
            LOGGER.warn("Block entity not found at [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Reset the platform path through platform services (complex path management operations)
        boolean success = PlatformServices.getBlockEntityHelper().resetPlatformPath(townDataProvider, platformId);
            
        if (!success) {
            LOGGER.warn("Failed to reset platform {} path at [{}, {}, {}]", platformId, x, y, z);
            return;
        }
        
        // Mark changed and sync using platform services
        markTownDataDirty(townDataProvider);
        PlatformServices.getPlatformHelper().forceBlockUpdate(player, x, y, z);
        
        LOGGER.debug("Successfully reset platform {} path at [{}, {}, {}]", platformId, x, y, z);
    }
    
    // Getters for testing
    public String getPlatformId() { return platformId; }
}