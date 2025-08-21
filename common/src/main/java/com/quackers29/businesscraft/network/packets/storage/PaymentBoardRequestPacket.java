package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.debug.DebugConfig;
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
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received payment board data request from player for town block at [{}, {}, {}]", getX(), getY(), getZ());
        
        try {
            // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
            com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
            
            if (townData != null) {
                // Basic validation through unified architecture
                if (!townData.isTownRegistered()) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town not registered at position ({}, {}, {}) for payment board request", x, y, z);
                    return;
                }
                
                // Complex payment board operations still use platform services (reward data handling)
                Object blockEntity = getBlockEntity(player);
                List<Object> unclaimedRewards = PlatformServices.getBlockEntityHelper().getUnclaimedRewards(blockEntity);
                
                if (unclaimedRewards != null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Sending {} rewards to player for town block at ({}, {}, {})", 
                               unclaimedRewards.size(), x, y, z);
                    
                    // Send the rewards to the client through platform services
                    PlatformServices.getNetworkHelper().sendPaymentBoardResponsePacket(player, unclaimedRewards);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "No unclaimed rewards found for town block at ({}, {}, {})", x, y, z);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "No TownInterfaceData found at position ({}, {}, {}) for payment board request", x, y, z);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling payment board data request at ({}, {}, {})", x, y, z, e);
        }
    }
    
    // Getters for testing
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
}