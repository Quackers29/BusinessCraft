package com.quackers29.businesscraft.fabric.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Fabric implementation of BufferStorageResponsePacket using Fabric networking APIs.
 * This packet is sent from server to client to update buffer storage data.
 */
public class BufferStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferStorageResponsePacket.class);
    private final Map<Item, Integer> bufferItems;

    public BufferStorageResponsePacket(Map<Item, Integer> bufferItems) {
        this.bufferItems = new HashMap<>(bufferItems);
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(bufferItems.size());
        for (Map.Entry<Item, Integer> entry : bufferItems.entrySet()) {
            buf.writeResourceLocation(new ResourceLocation(entry.getKey().toString()));
            buf.writeInt(entry.getValue());
        }
    }

    public static BufferStorageResponsePacket fromBytes(FriendlyByteBuf buf) {
        Map<Item, Integer> bufferItems = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            ResourceLocation itemId = buf.readResourceLocation();
            int count = buf.readInt();
            // TODO: Convert ResourceLocation back to Item
            // For now, this is a placeholder
        }
        return new BufferStorageResponsePacket(bufferItems);
    }

    public void handle() {
        // Handle buffer storage update on the client
        LOGGER.info("BufferStorageResponsePacket received with {} items",
                   bufferItems.size());

        // TODO: Implement the actual client-side buffer storage update
        // This would typically involve:
        // 1. Updating client's local buffer storage data
        // 2. Refreshing any UI that displays buffer contents
        // 3. Notifying the player of storage changes
    }

    // Getters
    public Map<Item, Integer> getBufferItems() {
        return new HashMap<>(bufferItems);
    }
}
