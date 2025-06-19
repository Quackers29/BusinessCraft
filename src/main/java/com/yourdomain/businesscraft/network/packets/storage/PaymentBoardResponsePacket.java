package com.yourdomain.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.town.data.RewardEntry;
import com.yourdomain.businesscraft.town.data.RewardSource;
import com.yourdomain.businesscraft.town.data.ClaimStatus;

/**
 * Client-bound packet that sends the current state of payment board rewards to the client.
 */
public class PaymentBoardResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardResponsePacket.class);
    private final List<RewardEntry> rewards;

    public PaymentBoardResponsePacket(List<RewardEntry> rewards) {
        this.rewards = new ArrayList<>(rewards);
    }

    public PaymentBoardResponsePacket(FriendlyByteBuf buf) {
        this.rewards = new ArrayList<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            try {
                // Read RewardEntry data
                UUID id = buf.readUUID();
                long timestamp = buf.readLong();
                long expirationTime = buf.readLong();
                RewardSource source = RewardSource.valueOf(buf.readUtf());
                ClaimStatus status = ClaimStatus.valueOf(buf.readUtf());
                String eligibility = buf.readUtf();
                
                // Read rewards list
                int rewardCount = buf.readInt();
                List<ItemStack> rewardItems = new ArrayList<>();
                for (int j = 0; j < rewardCount; j++) {
                    ItemStack stack = buf.readItem();
                    rewardItems.add(stack);
                }
                
                // Read metadata map
                java.util.Map<String, String> metadata = new java.util.HashMap<>();
                int metadataCount = buf.readInt();
                for (int j = 0; j < metadataCount; j++) {
                    String key = buf.readUtf();
                    String value = buf.readUtf();
                    metadata.put(key, value);
                }
                
                // Create RewardEntry using the network factory method with metadata
                RewardEntry entry = RewardEntry.fromNetworkWithMetadata(id, timestamp, expirationTime, 
                                                          source, rewardItems, status, eligibility, metadata);
                
                rewards.add(entry);
            } catch (Exception e) {
                LOGGER.error("Error decoding reward entry", e);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(rewards.size());
        for (RewardEntry entry : rewards) {
            try {
                // Write RewardEntry data
                buf.writeUUID(entry.getId());
                buf.writeLong(entry.getTimestamp());
                buf.writeLong(entry.getExpirationTime());
                buf.writeUtf(entry.getSource().name());
                buf.writeUtf(entry.getStatus().name());
                buf.writeUtf(entry.getEligibility());
                
                // Write rewards list
                List<ItemStack> rewardItems = entry.getRewards();
                buf.writeInt(rewardItems.size());
                for (ItemStack stack : rewardItems) {
                    buf.writeItem(stack);
                }
                
                // Write metadata map
                java.util.Map<String, String> metadata = entry.getMetadata();
                buf.writeInt(metadata.size());
                for (java.util.Map.Entry<String, String> metaEntry : metadata.entrySet()) {
                    buf.writeUtf(metaEntry.getKey());
                    buf.writeUtf(metaEntry.getValue());
                }
            } catch (Exception e) {
                LOGGER.error("Error encoding reward entry: {}", entry.getId(), e);
            }
        }
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PaymentBoardResponsePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PaymentBoardResponsePacket decode(FriendlyByteBuf buf) {
        return new PaymentBoardResponsePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client-side handling
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClientSide();
            }
        });
        context.setPacketHandled(true);
    }

    private void handleClientSide() {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
            "Received payment board update with {} rewards", rewards.size());
        
        // Get the current screen
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        
        client.execute(() -> {
            try {
                // If the current screen is PaymentBoardScreen, update its data
                if (client.screen instanceof com.yourdomain.businesscraft.ui.screens.town.PaymentBoardScreen paymentScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Updating PaymentBoardScreen with reward data");
                    paymentScreen.updateRewardData(rewards);
                    
                    // Also update the menu's cached data
                    com.yourdomain.businesscraft.menu.PaymentBoardMenu paymentMenu = 
                        (com.yourdomain.businesscraft.menu.PaymentBoardMenu) paymentScreen.getMenu();
                    paymentMenu.updateCachedRewards(rewards);
                }
            } catch (Exception e) {
                LOGGER.error("Error handling payment board update", e);
            }
        });
    }
    
    /**
     * Returns the rewards
     */
    public List<RewardEntry> getRewards() {
        return rewards;
    }
}