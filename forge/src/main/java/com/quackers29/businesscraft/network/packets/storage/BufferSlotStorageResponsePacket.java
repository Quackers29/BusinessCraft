package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.data.SlotBasedStorage;

import java.util.function.Supplier;

/**
 * Client-bound packet that sends slot-based storage data to preserve exact slot positions.
 * This replaces BufferStorageResponsePacket for slot-aware buffer updates.
 */
public class BufferSlotStorageResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferSlotStorageResponsePacket.class);
    private final SlotBasedStorage slotStorage;

    public BufferSlotStorageResponsePacket(SlotBasedStorage slotStorage) {
        this.slotStorage = slotStorage.copy(); // Create a copy to prevent modification during transmission
    }

    public BufferSlotStorageResponsePacket(FriendlyByteBuf buf) {
        // Read slot count first
        int slotCount = buf.readInt();
        this.slotStorage = new SlotBasedStorage(slotCount);
        
        // Read NBT data and deserialize
        CompoundTag nbt = buf.readNbt();
        if (nbt != null) {
            this.slotStorage.fromNBT(nbt);
        }
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Write slot count for validation
        buf.writeInt(slotStorage.getSlotCount());
        
        // Write NBT data containing all slot information
        buf.writeNbt(slotStorage.toNBT());
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(BufferSlotStorageResponsePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static BufferSlotStorageResponsePacket decode(FriendlyByteBuf buf) {
        return new BufferSlotStorageResponsePacket(buf);
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
            "Received slot-based buffer storage update with {} slots", slotStorage.getSlotCount());
        
        // Get the current screen
        Minecraft client = Minecraft.getInstance();
        
        client.execute(() -> {
            try {
                // Update both PaymentBoardScreen and PaymentBoardMenu
                if (client.screen instanceof com.quackers29.businesscraft.ui.screens.town.PaymentBoardScreen paymentScreen) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Updating PaymentBoardScreen with slot-based buffer storage data");
                    
                    // Update the screen's menu with slot-based data
                    var menu = paymentScreen.getMenu();
                    if (menu instanceof com.quackers29.businesscraft.menu.PaymentBoardMenu) {
                        com.quackers29.businesscraft.menu.PaymentBoardMenu paymentMenu = 
                            (com.quackers29.businesscraft.menu.PaymentBoardMenu) menu;
                        paymentMenu.updateBufferStorageSlots(slotStorage);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Error handling slot-based buffer storage update", e);
            }
        });
    }
    
    /**
     * Returns the slot-based storage
     */
    public SlotBasedStorage getSlotStorage() {
        return slotStorage;
    }
}