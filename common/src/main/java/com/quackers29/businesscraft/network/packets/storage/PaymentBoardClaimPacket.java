package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import com.quackers29.businesscraft.debug.DebugConfig;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for claiming rewards from the payment board.
 * 
 * Unified Architecture approach: Direct access to TownInterfaceEntity methods
 * instead of platform service wrapper calls.
 */
public class PaymentBoardClaimPacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardClaimPacket.class);
    private final String rewardId;
    private final boolean toBuffer;
    
    /**
     * Create packet for sending.
     */
    public PaymentBoardClaimPacket(int x, int y, int z, String rewardId, boolean toBuffer) {
        super(x, y, z);
        this.rewardId = rewardId;
        this.toBuffer = toBuffer;
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static PaymentBoardClaimPacket decode(Object buffer) {
        int[] pos = PlatformServices.getNetworkHelper().readBlockPos(buffer);
        String rewardId = PlatformServices.getNetworkHelper().readUUID(buffer);
        boolean toBuffer = PlatformServices.getNetworkHelper().readBoolean(buffer);
        return new PaymentBoardClaimPacket(pos[0], pos[1], pos[2], rewardId, toBuffer);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    @Override
    public void encode(Object buffer) {
        super.encode(buffer); // Write block position
        PlatformServices.getNetworkHelper().writeUUID(buffer, rewardId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, toBuffer);
    }
    
    /**
     * Handle the packet on the server side.
     * Unified Architecture approach: Direct access to TownInterfaceEntity methods.
     */
    @Override
    public void handle(Object player) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Processing claim request from player for reward {} at [{}, {}, {}], toBuffer: {}", 
                    rewardId, x, y, z, toBuffer);
        
        try {
            // Unified Architecture: Direct access to TownInterfaceData (no BlockEntityHelper abstraction)
            com.quackers29.businesscraft.town.TownInterfaceData townData = getTownInterfaceData(player);
            
            if (townData != null) {
                // Basic validation through unified architecture
                if (!townData.isTownRegistered()) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Town not registered at position ({}, {}, {}) for reward claim", x, y, z);
                    PlatformServices.getPlatformHelper().sendPlayerMessage(player, "Error: Town not registered", "RED");
                    return;
                }
                
                // Complex reward claiming still uses platform services (inventory, item handling)
                Object blockEntity = getBlockEntity(player);
                Object claimResult = PlatformServices.getBlockEntityHelper().claimPaymentBoardReward(
                    blockEntity, player, rewardId, toBuffer);
                
                if (claimResult != null) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Successfully processed claim for reward {} at ({}, {}, {})", 
                               rewardId, x, y, z);
                    
                    // Get updated payment board data through platform services
                    java.util.List<Object> unclaimedRewards = PlatformServices.getBlockEntityHelper().getUnclaimedRewards(blockEntity);
                    PlatformServices.getNetworkHelper().sendPaymentBoardResponsePacket(player, unclaimedRewards);
                    
                    // Note: Platform implementations handle BufferSlotStorageResponsePacket and inventory management
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Failed to claim reward {} at ({}, {}, {})", rewardId, x, y, z);
                    PlatformServices.getPlatformHelper().sendPlayerMessage(player, "Failed to process claim request", "RED");
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "No TownInterfaceData found at position ({}, {}, {}) for reward claim", x, y, z);
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, "Error: Invalid town block", "RED");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling claim request at ({}, {}, {})", x, y, z, e);
            PlatformServices.getPlatformHelper().sendPlayerMessage(player, "Error: Failed to process claim request", "RED");
        }
    }
    
    // Getters for testing
    public String getRewardId() { return rewardId; }
    public boolean isToBuffer() { return toBuffer; }
}