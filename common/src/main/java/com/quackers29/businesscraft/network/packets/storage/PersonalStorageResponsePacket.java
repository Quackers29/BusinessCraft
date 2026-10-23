package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import com.quackers29.businesscraft.api.PlatformAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-bound packet that sends the current state of a player's personal storage to the client.
 */
public class PersonalStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalStorageResponsePacket.class);
    private final Map<Item, Long> storageItems;

    public PersonalStorageResponsePacket(Map<Item, Long> storageItems) {
        this.storageItems = new HashMap<>(storageItems);
    }

    public PersonalStorageResponsePacket(FriendlyByteBuf buf) {
        this.storageItems = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String itemId = buf.readUtf();
            long count = buf.readLong();
            try {
                ResourceLocation resourceLocation = new ResourceLocation(itemId);
                Object itemObj = PlatformAccess.getRegistry().getItem(resourceLocation);
                if (itemObj instanceof net.minecraft.world.item.Item item) {
                    if (item != null) {
                        storageItems.put(item, count);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error decoding item: {}", itemId, e);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(storageItems.size());
        storageItems.forEach((item, count) -> {
            Object itemIdObj = PlatformAccess.getRegistry().getItemKey(item);
            if (itemIdObj instanceof net.minecraft.resources.ResourceLocation itemId) {
                if (itemId != null) {
                    buf.writeUtf(itemId.toString());
                    buf.writeLong(count);
                }
            }
        });
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PersonalStorageResponsePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PersonalStorageResponsePacket decode(FriendlyByteBuf buf) {
        return new PersonalStorageResponsePacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () ->
                com.quackers29.businesscraft.network.ClientPacketDispatcher.dispatch(this));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
    
    /**
     * Returns the storage items
     */
    public Map<Item, Long> getStorageItems() {
        return storageItems;
    }
} 
