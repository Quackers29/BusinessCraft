package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Client-bound packet that sends the current state of buffer storage to the client.
 */
public class BufferStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferStorageResponsePacket.class);
    private final Map<Item, Integer> bufferItems;

    public BufferStorageResponsePacket(Map<Item, Integer> bufferItems) {
        this.bufferItems = new HashMap<>(bufferItems);
    }

    public BufferStorageResponsePacket(FriendlyByteBuf buf) {
        this.bufferItems = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String itemName = buf.readUtf();
            int count = buf.readInt();
            
            // Get the item from the registry
                Item item = PlatformAccess.getRegistry().getItem(new net.minecraft.resources.ResourceLocation(itemName));
            if (item != null) {
                bufferItems.put(item, count);
            } else {
                LOGGER.warn("Failed to find item {} in registry", itemName);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(bufferItems.size());
        for (Map.Entry<Item, Integer> entry : bufferItems.entrySet()) {
            buf.writeUtf(PlatformAccess.getRegistry().getItemKey(entry.getKey()).toString());
            buf.writeInt(entry.getValue());
        }
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(BufferStorageResponsePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static BufferStorageResponsePacket decode(FriendlyByteBuf buf) {
        return new BufferStorageResponsePacket(buf);
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
            "Received buffer storage update with {} items", bufferItems.size());
        
        // Get the current screen
        Minecraft client = Minecraft.getInstance();
        
        client.execute(() -> {
            try {
                // If the current screen is PaymentBoardScreen, update its buffer storage data
                if (client.screen instanceof com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen paymentScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Updating PaymentBoardScreen with buffer storage data");
                    paymentScreen.updateBufferStorageItems(bufferItems);
                }
            } catch (Exception e) {
                LOGGER.error("Error handling buffer storage update", e);
            }
        });
    }
    
    /**
     * Returns the buffer items
     */
    public Map<Item, Integer> getBufferItems() {
        return bufferItems;
    }
}
