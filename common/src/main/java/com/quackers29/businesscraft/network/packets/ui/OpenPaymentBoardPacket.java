package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet to request opening the Payment Board UI.
 * Extends BaseBlockEntityPacket to handle block entity operations.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class OpenPaymentBoardPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenPaymentBoardPacket.class);

    /**
     * Create packet for sending.
     */
    public OpenPaymentBoardPacket(int x, int y, int z) {
        super(x, y, z);
    }

    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static OpenPaymentBoardPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        return new OpenPaymentBoardPacket(pos[0], pos[1], pos[2]);
    }

    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        super.encode(buffer);
    }

    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Opening Payment Board for player at position [{}, {}, {}]", getX(), getY(), getZ());
        
        try {
            // Get block entity using platform services
            Object blockEntity = getBlockEntity(player);
            if (blockEntity == null) {
                LOGGER.warn("No block entity found at position [{}, {}, {}] for Payment Board UI", getX(), getY(), getZ());
                return;
            }
            
            Object townDataProvider = getTownDataProvider(blockEntity);
            if (townDataProvider == null) {
                LOGGER.warn("No TownInterfaceEntity found at position [{}, {}, {}] for Payment Board UI", getX(), getY(), getZ());
                return;
            }
            
            // Use platform service to open Payment Board UI
            boolean success = PlatformServices.getBlockEntityHelper().openPaymentBoardUI(townDataProvider, player);
            
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully opened Payment Board for player at [{}, {}, {}]", getX(), getY(), getZ());
            } else {
                LOGGER.warn("Failed to open Payment Board for player at [{}, {}, {}]", getX(), getY(), getZ());
            }
        } catch (Exception e) {
            LOGGER.error("Error handling open Payment Board request at [{}, {}, {}]", getX(), getY(), getZ(), e);
        }
    }
}