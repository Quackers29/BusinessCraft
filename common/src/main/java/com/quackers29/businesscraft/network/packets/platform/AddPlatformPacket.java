package com.quackers29.businesscraft.network.packets.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.platform.RefreshPlatformsPacket;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;

/**
 * Packet for adding a new platform to a town
 */
public class AddPlatformPacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final BlockPos blockPos;
    
    public AddPlatformPacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }
    
    /**
     * Encode the packet data into the buffer
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }
    
    /**
     * Serialize packet data for Fabric networking (toBytes)
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(blockPos);
    }
    
    /**
     * Decode the packet data from the buffer
     */
    public static AddPlatformPacket decode(FriendlyByteBuf buf) {
        return new AddPlatformPacket(buf.readBlockPos());
    }
    
    /**
     * Handle the packet on the receiving side
     */
    public boolean handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            try {
                // Get player and world from context
                Object senderObj = PlatformAccess.getNetwork().getSender(context);
                if (!(senderObj instanceof ServerPlayer player)) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "AddPlatformPacket: No player sender");
                    return;
                }

                player.displayClientMessage(
                        Component.translatable("businesscraft.platform_interface.add_refused"), false);
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                        "Refused AddPlatformPacket from {} — platforms are placed as Platform Interface blocks",
                        player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Error handling AddPlatformPacket", e);
                // Don't crash the server, but log the error
            }
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
        return true;
    }
} 
