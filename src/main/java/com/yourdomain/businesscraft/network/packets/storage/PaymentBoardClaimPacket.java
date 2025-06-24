package com.yourdomain.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.function.Supplier;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.town.data.TownPaymentBoard;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-bound packet for claiming rewards from the payment board
 */
public class PaymentBoardClaimPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardClaimPacket.class);
    
    private final BlockPos townBlockPos;
    private final UUID rewardId;
    private final boolean toBuffer;

    public PaymentBoardClaimPacket(BlockPos townBlockPos, UUID rewardId, boolean toBuffer) {
        this.townBlockPos = townBlockPos;
        this.rewardId = rewardId;
        this.toBuffer = toBuffer;
    }

    public PaymentBoardClaimPacket(FriendlyByteBuf buf) {
        this.townBlockPos = buf.readBlockPos();
        this.rewardId = buf.readUUID();
        this.toBuffer = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(townBlockPos);
        buf.writeUUID(rewardId);
        buf.writeBoolean(toBuffer);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PaymentBoardClaimPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PaymentBoardClaimPacket decode(FriendlyByteBuf buf) {
        return new PaymentBoardClaimPacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Server-side handling
            ServerPlayer player = context.getSender();
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "PaymentBoardClaimPacket.handle() - received claim request from player: {}, rewardId: {}, toBuffer: {}", 
                player != null ? player.getName().getString() : "null", rewardId, toBuffer);
                
            if (player != null && player.level() instanceof ServerLevel serverLevel) {
                handleServerSide(player, serverLevel);
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "PaymentBoardClaimPacket.handle() - Invalid player or level state");
            }
        });
        context.setPacketHandled(true);
    }

    private void handleServerSide(ServerPlayer player, ServerLevel serverLevel) {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
            "Processing claim request from player {} for reward {} at {}", 
            player.getName().getString(), rewardId, townBlockPos);
        
        try {
            // Get the town block entity
            var blockEntity = serverLevel.getBlockEntity(townBlockPos);
            if (blockEntity instanceof com.yourdomain.businesscraft.block.entity.TownBlockEntity townBlockEntity) {
                // Get the town
                var townId = townBlockEntity.getTownId();
                if (townId != null) {
                    Town town = TownManager.get(serverLevel).getTown(townId);
                    if (town != null) {
                        // Attempt to claim the reward
                        TownPaymentBoard.ClaimResult result = town.getPaymentBoard()
                            .claimReward(rewardId, "ALL", toBuffer);
                        
                        if (result.isSuccess()) {
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                                "Successfully claimed reward {} for player {} - {}", 
                                rewardId, player.getName().getString(), result.getMessage());
                            
                            // If not claiming to buffer, try to add items to player inventory
                            if (!toBuffer && !result.getClaimedItems().isEmpty()) {
                                boolean inventoryFull = false;
                                for (ItemStack stack : result.getClaimedItems()) {
                                    if (!player.getInventory().add(stack)) {
                                        // Inventory full - try to add remaining items to buffer
                                        town.getPaymentBoard().addToBuffer(stack.getItem(), stack.getCount());
                                        inventoryFull = true;
                                    }
                                }
                                
                                if (inventoryFull) {
                                    // Notify town block entity that buffer has changed due to overflow
                                    townBlockEntity.onTownBufferChanged();
                                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                        "Some items were sent to buffer storage due to full inventory"));
                                }
                            }
                            
                            // Mark town as dirty for saving
                            town.markDirty();
                            
                            // Notify town block entity that buffer has changed
                            townBlockEntity.onTownBufferChanged();
                            
                            // Send updated payment board data to client
                            var rewards = town.getPaymentBoard().getUnclaimedRewards();
                            ModMessages.sendToPlayer(new PaymentBoardResponsePacket(rewards), player);
                            
                            // Send updated buffer storage data to client using new slot-based packet
                            var bufferSlots = town.getPaymentBoard().getBufferStorageSlots();
                            ModMessages.sendToPlayer(new com.yourdomain.businesscraft.network.packets.storage.BufferSlotStorageResponsePacket(bufferSlots), player);
                            
                            // Send success message to player
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§a" + result.getMessage()));
                            
                        } else {
                            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                                "Failed to claim reward {} for player {} - {}", 
                                rewardId, player.getName().getString(), result.getMessage());
                            
                            // Send failure message to player
                            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§c" + result.getMessage()));
                        }
                    } else {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "No town found for ID: {}", townId);
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "§cError: Town not found"));
                    }
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Town block at {} has no town ID", townBlockPos);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cError: Town block not properly configured"));
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Block at {} is not a TownBlockEntity", townBlockPos);
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "§cError: Invalid town block"));
            }
        } catch (Exception e) {
            LOGGER.error("Error handling claim request from player {}", 
                player.getName().getString(), e);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cError: Failed to process claim request"));
        }
    }
}