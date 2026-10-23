package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-bound packet that sends the current state of buffer storage to the client.
 */
public class BufferStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferStorageResponsePacket.class);
    private final Map<Item, Long> bufferItems;

    public BufferStorageResponsePacket(Map<Item, Long> bufferItems) {
        this.bufferItems = new HashMap<>(bufferItems);
    }

    public BufferStorageResponsePacket(FriendlyByteBuf buf) {
        this.bufferItems = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String itemName = buf.readUtf();
            long count = buf.readLong();
            
            // Get the item from the registry
            try {
                Object itemObj = PlatformAccess.getRegistry().getItem(new net.minecraft.resources.ResourceLocation(itemName));
                if (itemObj instanceof net.minecraft.world.item.Item item) {
                    if (item != null) {
                        bufferItems.put(item, count);
                    } else {
                        LOGGER.warn("Failed to find item {} in registry", itemName);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error decoding item: {}", itemName, e);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(bufferItems.size());
        for (Map.Entry<Item, Long> entry : bufferItems.entrySet()) {
            Object itemIdObj = PlatformAccess.getRegistry().getItemKey(entry.getKey());
            if (itemIdObj instanceof net.minecraft.resources.ResourceLocation itemId) {
                buf.writeUtf(itemId.toString());
            }
            buf.writeLong(entry.getValue());
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

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () ->
                com.quackers29.businesscraft.network.ClientPacketDispatcher.dispatch(this));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
    
    /**
     * Returns the buffer items
     */
    public Map<Item, Long> getBufferItems() {
        return bufferItems;
    }
}
