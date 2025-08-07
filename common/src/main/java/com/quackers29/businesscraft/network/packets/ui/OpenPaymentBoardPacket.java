package com.quackers29.businesscraft.network.packets.ui;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
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
        LOGGER.debug("Opening Payment Board for player at position [{}, {}, {}]", getX(), getY(), getZ());
        
        // Get the town interface block entity through platform services
        Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, getX(), getY(), getZ());
        if (blockEntity == null) {
            LOGGER.warn("Block entity not found at position [{}, {}, {}] for Payment Board UI", getX(), getY(), getZ());
            return;
        }
        
        // Open the Payment Board UI through platform services
        boolean success = PlatformServices.getBlockEntityHelper().openPaymentBoardUI(blockEntity, player);
        
        if (success) {
            LOGGER.debug("Successfully opened Payment Board for player at [{}, {}, {}]", getX(), getY(), getZ());
        } else {
            LOGGER.warn("Failed to open Payment Board for player at [{}, {}, {}]", getX(), getY(), getZ());
        }
    }
}