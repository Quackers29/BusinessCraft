package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Client-bound packet that sends the current state of a player's personal storage to the client.
 */
public class PersonalStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalStorageResponsePacket.class);
    private final Map<Item, Integer> storageItems;

    public PersonalStorageResponsePacket(Map<Item, Integer> storageItems) {
        this.storageItems = new HashMap<>(storageItems);
    }

    public PersonalStorageResponsePacket(FriendlyByteBuf buf) {
        this.storageItems = new HashMap<>();
        int size = buf.readInt();
        for (int i = 0; i < size; i++) {
            String itemId = buf.readUtf();
            int count = buf.readInt();
            try {
                ResourceLocation resourceLocation = new ResourceLocation(itemId);
                Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                if (item != null) {
                    storageItems.put(item, count);
                }
            } catch (Exception e) {
                LOGGER.error("Error decoding item: {}", itemId, e);
            }
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(storageItems.size());
        storageItems.forEach((item, count) -> {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item);
            if (itemId != null) {
                buf.writeUtf(itemId.toString());
                buf.writeInt(count);
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
            "Received personal storage update with {} items", storageItems.size());
        
        // Get the current screen
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        
        client.execute(() -> {
            // If the current screen is StorageScreen, update its inventory
            if (client.screen instanceof com.quackers29.businesscraft.ui.screens.town.StorageScreen storageScreen) {
                storageScreen.updatePersonalStorageItems(storageItems);
            }
            // Also check for our modal inventory screen
            else if (client.screen instanceof com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen<?> modalScreen) {
                // Check if the container is a StorageMenu
                if (modalScreen.getMenu() instanceof com.quackers29.businesscraft.menu.StorageMenu storageMenu) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Updating personal storage in modal screen");
                    // Update the storage menu's inventory with the personal storage items
                    storageMenu.updatePersonalStorageItems(storageItems);
                }
            }
        });
    }
    
    /**
     * Returns the storage items
     */
    public Map<Item, Integer> getStorageItems() {
        return storageItems;
    }
} 
