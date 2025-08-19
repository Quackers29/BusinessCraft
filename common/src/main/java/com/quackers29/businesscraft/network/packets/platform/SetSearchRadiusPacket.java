package com.quackers29.businesscraft.network.packets.platform;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic packet for setting the search radius for platform detection.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class SetSearchRadiusPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(SetSearchRadiusPacket.class);
    private final int radius;

    /**
     * Create packet for sending.
     */
    public SetSearchRadiusPacket(int x, int y, int z, int radius) {
        super(x, y, z);
        this.radius = radius;
    }

    /**
     * Create packet from network buffer (decode constructor).
     */
    public SetSearchRadiusPacket(Object buffer) {
        super(buffer);
        this.radius = PlatformServices.getNetworkHelper().readInt(buffer);
    }

    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Writes BlockPos
        PlatformServices.getNetworkHelper().writeInt(buffer, radius);
    }

    /**
     * Handle the packet on the server side.
     * This method contains the core business logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Processing SetSearchRadiusPacket for position ({}, {}, {}) with radius: {}", x, y, z, radius);
        
        // Enhanced MultiLoader: Use platform services for cross-platform compatibility
        Object blockEntity = getBlockEntity(player);
        if (blockEntity != null) {
            Object townDataProvider = getTownDataProvider(blockEntity);
            if (townDataProvider != null) {
                // Core business logic - update search radius through platform services
                int oldRadius = PlatformServices.getBlockEntityHelper().getSearchRadius(townDataProvider);
                
                // Use platform services for cross-platform compatibility
                PlatformServices.getBlockEntityHelper().setSearchRadius(townDataProvider, radius);
                markTownDataDirty(townDataProvider);
                
                LOGGER.debug("Search radius updated successfully from {} to {}", oldRadius, radius);
                
                // Platform services can handle menu refreshing if needed
                PlatformServices.getMenuHelper().refreshActiveMenu(player, "search_radius");
            } else {
                LOGGER.warn("No town data provider found at position ({}, {}, {}) for search radius change", x, y, z);
            }
        } else {
            LOGGER.warn("No block entity found at position ({}, {}, {}) for search radius change", x, y, z);
        }
    }
    
    /**
     * Get the search radius for this packet.
     */
    public int getRadius() {
        return radius;
    }
}