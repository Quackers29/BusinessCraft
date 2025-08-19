package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
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
            // Unified Architecture: Direct access to TownInterfaceEntity (replaces 2 BlockEntityHelper calls)
            TownInterfaceEntity townInterface = null;
            if (player instanceof ServerPlayer serverPlayer) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockEntity blockEntity = serverPlayer.serverLevel().getBlockEntity(pos);
                if (blockEntity instanceof TownInterfaceEntity) {
                    townInterface = (TownInterfaceEntity) blockEntity;
                }
            }
            
            if (townInterface == null) {
                LOGGER.debug("No TownInterfaceEntity found at [{}, {}, {}]", x, y, z);
                return;
            }
            
            // Direct unified access - no platform service bridge needed!
            List<Object> unclaimedRewards = townInterface.getUnclaimedRewards();
            
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