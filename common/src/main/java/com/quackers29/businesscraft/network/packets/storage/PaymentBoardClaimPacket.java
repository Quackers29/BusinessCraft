package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
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
        LOGGER.debug("Processing claim request from player for reward {} at [{}, {}, {}], toBuffer: {}", 
                    rewardId, x, y, z, toBuffer);
        
        try {
            // Get block entity using platform services
            Object blockEntity = getBlockEntity(player);
            if (blockEntity == null) {
                LOGGER.debug("No block entity at [{}, {}, {}]", x, y, z);
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, 
                    "Error: Invalid town block", "RED");
                return;
            }
            
            Object townDataProvider = getTownDataProvider(blockEntity);
            if (townDataProvider == null) {
                LOGGER.debug("Block at [{}, {}, {}] is not a TownInterfaceEntity", x, y, z);
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, 
                    "Error: Invalid town block", "RED");
                return;
            }
            
            // Attempt to claim the reward through platform services
            Object claimResult = PlatformServices.getBlockEntityHelper().claimPaymentBoardReward(
                townDataProvider, player, rewardId, toBuffer);
            
            if (claimResult != null) {
                LOGGER.debug("Successfully processed claim for reward {} at [{}, {}, {}]", 
                           rewardId, x, y, z);
                
                // Get updated payment board data through platform services
                java.util.List<Object> unclaimedRewards = PlatformServices.getBlockEntityHelper().getUnclaimedRewards(townDataProvider);
                PlatformServices.getNetworkHelper().sendPaymentBoardResponsePacket(player, unclaimedRewards);
                
                // Mark changed and sync through platform services
                markTownDataDirty(townDataProvider);
                
                // Note: Platform implementations will handle sending BufferSlotStorageResponsePacket
                // and player inventory management as part of claimPaymentBoardReward
            } else {
                LOGGER.debug("Failed to claim reward {} at [{}, {}, {}]", rewardId, x, y, z);
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, 
                    "Failed to process claim request", "RED");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling claim request at [{}, {}, {}]", x, y, z, e);
            PlatformServices.getPlatformHelper().sendPlayerMessage(player, 
                "Error: Failed to process claim request", "RED");
        }
    }
    
    // Getters for testing
    public String getRewardId() { return rewardId; }
    public boolean isToBuffer() { return toBuffer; }
}