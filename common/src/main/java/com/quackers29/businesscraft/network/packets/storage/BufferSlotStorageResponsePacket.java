package com.quackers29.businesscraft.network.packets.storage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.data.SlotBasedStorage;

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

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () ->
                com.quackers29.businesscraft.network.ClientPacketDispatcher.dispatch(this));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
    
    /**
     * Returns the slot-based storage
     */
    public SlotBasedStorage getSlotStorage() {
        return slotStorage;
    }
}
