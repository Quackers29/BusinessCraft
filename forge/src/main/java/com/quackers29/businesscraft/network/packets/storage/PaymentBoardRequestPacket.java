package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-bound packet requesting payment board data sync
 */
public class PaymentBoardRequestPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardRequestPacket.class);
    
    private final BlockPos townBlockPos;

    public PaymentBoardRequestPacket(BlockPos townBlockPos) {
        this.townBlockPos = townBlockPos;
    }

    public PaymentBoardRequestPacket(FriendlyByteBuf buf) {
        this.townBlockPos = buf.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(townBlockPos);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PaymentBoardRequestPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PaymentBoardRequestPacket decode(FriendlyByteBuf buf) {
        return new PaymentBoardRequestPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Server-side handling
            ServerPlayer player = context.getSender();
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "PaymentBoardRequestPacket.handle() - received packet from player: {}, level: {}", 
                player != null ? player.getName().getString() : "null", 
                player != null ? player.level().getClass().getSimpleName() : "null");
                
            if (player != null && player.level() instanceof ServerLevel serverLevel) {
                handleServerSide(player, serverLevel);
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "PaymentBoardRequestPacket.handle() - Invalid player or level state");
            }
        });
        context.setPacketHandled(true);
    }

    private void handleServerSide(ServerPlayer player, ServerLevel serverLevel) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
            "Received payment board data request from player {} for town block at {}", 
            player.getName().getString(), townBlockPos);
        
        try {
            // Get the town block entity
            var blockEntity = serverLevel.getBlockEntity(townBlockPos);
            if (blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterfaceEntity) {
                // Get the town
                var townId = townInterfaceEntity.getTownId();
                if (townId != null) {
                    Town town = TownManager.get(serverLevel).getTown(townId);
                    if (town != null) {
                        // Get unclaimed rewards from the payment board
                        var rewards = town.getPaymentBoard().getUnclaimedRewards();
                        
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "Sending {} rewards to player {} for town {}", 
                            rewards.size(), player.getName().getString(), town.getName());
                        
                        // Send the rewards to the client
                        ModMessages.sendToPlayer(new PaymentBoardResponsePacket(rewards), player);
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "No town found for ID: {}", townId);
                    }
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Town block at {} has no town ID", townBlockPos);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Block at {} is not a TownInterfaceEntity", townBlockPos);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling payment board data request from player {}", 
                player.getName().getString(), e);
        }
    }
}