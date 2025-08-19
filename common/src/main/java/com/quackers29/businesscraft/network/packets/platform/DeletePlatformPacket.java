package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet for deleting a platform from a town.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class DeletePlatformPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePlatformPacket.class);
    private final String platformId;
    
    /**
     * Create packet for sending.
     */
    public DeletePlatformPacket(int x, int y, int z, String platformId) {
        super(x, y, z);
        this.platformId = platformId;
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public DeletePlatformPacket(Object buffer) {
        super(buffer);
        this.platformId = PlatformServices.getNetworkHelper().readUUID(buffer);
    }

    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Writes BlockPos
        PlatformServices.getNetworkHelper().writeUUID(buffer, platformId);
    }

    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Deleting platform {} from town at ({}, {}, {})", platformId, x, y, z);
        
        // Get the town interface entity using platform services
        Object blockEntity = getBlockEntity(player);
        if (blockEntity == null) {
            LOGGER.error("No block entity found at position: [{}, {}, {}]", x, y, z);
            return;
        }

        Object townDataProvider = getTownDataProvider(blockEntity);
        if (townDataProvider == null) {
            LOGGER.error("Failed to get TownInterfaceEntity at position: [{}, {}, {}]", x, y, z);
            return;
        }
        
        // Delete platform through platform services
        // NOTE: Platform service handles complex platform operations
        boolean success = PlatformServices.getBlockEntityHelper().removePlatform(townDataProvider, platformId);
        
        if (success) {
            LOGGER.debug("Successfully deleted platform {} from town at [{}, {}, {}]", platformId, x, y, z);
        } else {
            LOGGER.warn("Failed to delete platform {} from town at [{}, {}, {}]", platformId, x, y, z);
        }
    }
    
    /**
     * Get the platform ID for this packet.
     */
    public String getPlatformId() {
        return platformId;
    }
}