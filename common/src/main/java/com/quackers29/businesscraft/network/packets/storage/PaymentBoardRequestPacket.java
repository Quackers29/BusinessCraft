package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
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
public class PaymentBoardRequestPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardRequestPacket.class);
    
    /**
     * Create packet for sending.
     */
    public PaymentBoardRequestPacket(int x, int y, int z) {
        super(x, y, z);
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
    @Override
    public void encode(Object buffer) {
        super.encode(buffer);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    @Override
    public void handle(Object player) {
        LOGGER.debug("Received payment board data request from player for town block at [{}, {}, {}]", getX(), getY(), getZ());
        
        try {
            // Enhanced MultiLoader: Use platform services for cross-platform compatibility
            Object blockEntity = getBlockEntity(player);
            if (blockEntity == null) {
                LOGGER.debug("No block entity found at [{}, {}, {}]", getX(), getY(), getZ());
                return;
            }
            
            Object townDataProvider = getTownDataProvider(blockEntity);
            if (townDataProvider == null) {
                LOGGER.debug("No TownInterfaceEntity found at [{}, {}, {}]", getX(), getY(), getZ());
                return;
            }
            
            // Get unclaimed rewards through platform services
            List<Object> unclaimedRewards = PlatformServices.getBlockEntityHelper().getUnclaimedRewards(townDataProvider);
            
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