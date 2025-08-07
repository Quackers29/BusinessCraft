package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-to-server packet for claiming rewards from the payment board.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class PaymentBoardClaimPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardClaimPacket.class);
    private final int x, y, z;
    private final String rewardId;
    private final boolean toBuffer;
    
    /**
     * Create packet for sending.
     */
    public PaymentBoardClaimPacket(int x, int y, int z, String rewardId, boolean toBuffer) {
        this.x = x;
        this.y = y;
        this.z = z;
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
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeBlockPos(buffer, x, y, z);
        PlatformServices.getNetworkHelper().writeUUID(buffer, rewardId);
        PlatformServices.getNetworkHelper().writeBoolean(buffer, toBuffer);
    }
    
    /**
     * Handle the packet on the server side.
     * This method contains the core server-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        LOGGER.debug("Processing claim request from player for reward {} at [{}, {}, {}], toBuffer: {}", 
                    rewardId, x, y, z, toBuffer);
        
        try {
            // Get the town interface block entity through platform services
            Object blockEntity = PlatformServices.getBlockEntityHelper().getBlockEntity(player, x, y, z);
            if (blockEntity == null) {
                LOGGER.debug("Block at [{}, {}, {}] is not a TownInterfaceEntity", x, y, z);
                PlatformServices.getPlatformHelper().sendPlayerMessage(player, 
                    "Error: Invalid town block", "RED");
                return;
            }
            
            // Attempt to claim the reward through platform services
            Object claimResult = PlatformServices.getBlockEntityHelper().claimPaymentBoardReward(
                blockEntity, player, rewardId, toBuffer);
            
            if (claimResult != null) {
                LOGGER.debug("Successfully processed claim for reward {} at [{}, {}, {}]", 
                           rewardId, x, y, z);
                
                // Send updated payment board data to client through platform services
                java.util.List<Object> unclaimedRewards = PlatformServices.getBlockEntityHelper()
                    .getUnclaimedRewards(blockEntity);
                PlatformServices.getNetworkHelper().sendPaymentBoardResponsePacket(player, unclaimedRewards);
                
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
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getRewardId() { return rewardId; }
    public boolean isToBuffer() { return toBuffer; }
}