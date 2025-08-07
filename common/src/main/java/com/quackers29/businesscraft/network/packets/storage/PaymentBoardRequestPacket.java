package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * Platform-agnostic client-to-server packet requesting payment board data sync.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class PaymentBoardRequestPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardRequestPacket.class);
    private final int x, y, z;
    
    /**
     * Create packet for sending.
     */
    public PaymentBoardRequestPacket(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static PaymentBoardRequestPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        return new PaymentBoardRequestPacket(pos[0], pos[1], pos[2]);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        LOGGER.debug("Received payment board data request from player for town block at [{}, {}, {}]", x, y, z);
        
        try {
            // Get the town interface block entity through platform services
            Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
            if (blockEntity == null) {
                LOGGER.debug("Block at [{}, {}, {}] is not a TownInterfaceEntity", x, y, z);
                return;
            }
            
            // Get unclaimed rewards from the payment board through platform services
            List<Object> unclaimedRewards = PlatformServices.getBlockEntityHelper().getUnclaimedRewards(blockEntity);
            
            if (unclaimedRewards != null) {
                LOGGER.debug("Sending {} rewards to player for town block at [{}, {}, {}]", 
                           unclaimedRewards.size(), x, y, z);
                
                // Send the rewards to the client through platform services
                PlatformServices.getNetworkHelper().sendPaymentBoardResponsePacket(player, unclaimedRewards);
            } else {
                LOGGER.debug("No unclaimed rewards found for town block at [{}, {}, {}]", x, y, z);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling payment board data request at [{}, {}, {}]", x, y, z, e);
        }
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}