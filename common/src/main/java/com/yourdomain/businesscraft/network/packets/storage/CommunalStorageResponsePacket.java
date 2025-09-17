package com.yourdomain.businesscraft.network.packets.storage;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import com.yourdomain.businesscraft.debug.DebugConfig;

/**
 * Client-bound packet that sends the current state of communal storage to the client.
 */
public class CommunalStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunalStorageResponsePacket.class);
    private final Map<Item, Integer> storageItems;

    public CommunalStorageResponsePacket(Map<Item, Integer> storageItems) {
        this.storageItems = new HashMap<>(storageItems);
    }

    public CommunalStorageResponsePacket(FriendlyByteBuf buf) {
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
    public static void encode(CommunalStorageResponsePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static CommunalStorageResponsePacket decode(FriendlyByteBuf buf) {
        return new CommunalStorageResponsePacket(buf);
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
            "Received communal storage update with {} items", storageItems.size());
        
        // Get the current screen
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        
        client.execute(() -> {
            try {
                // If the current screen is StorageScreen, update its inventory
                if (client.screen instanceof com.yourdomain.businesscraft.ui.screens.town.StorageScreen storageScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Updating StorageScreen with communal storage data");
                    storageScreen.updateStorageItems(storageItems);
                } else if (client.screen instanceof com.yourdomain.businesscraft.ui.modal.specialized.BCModalInventoryScreen<?> modalScreen) {
                    // Check if the container is a StorageMenu
                    if (modalScreen.getMenu() instanceof com.yourdomain.businesscraft.menu.StorageMenu storageMenu) {
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                            "Updating communal storage in BCModalInventoryScreen");
                        // Update the storage menu's inventory with the communal storage items
                        storageMenu.updateStorageItems(storageItems);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error handling communal storage update", e);
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