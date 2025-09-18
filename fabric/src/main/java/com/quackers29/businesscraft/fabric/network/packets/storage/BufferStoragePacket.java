package com.quackers29.businesscraft.fabric.network.packets.storage;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of BufferStoragePacket using Fabric networking APIs.
 */
public class BufferStoragePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferStoragePacket.class);

    private final BlockPos pos;
    private final ItemStack itemStack;
    private final int slotId;
    private final boolean isAddOperation;

    public BufferStoragePacket(BlockPos pos, ItemStack itemStack, int slotId, boolean isAddOperation) {
        this.pos = pos;
        this.itemStack = itemStack;
        this.slotId = slotId;
        this.isAddOperation = isAddOperation;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeItem(itemStack);
        buf.writeInt(slotId);
        buf.writeBoolean(isAddOperation);
    }

    public static BufferStoragePacket fromBytes(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ItemStack itemStack = buf.readItem();
        int slotId = buf.readInt();
        boolean isAddOperation = buf.readBoolean();
        return new BufferStoragePacket(pos, itemStack, slotId, isAddOperation);
    }

    public void handle(ServerPlayer player) {
        // Handle buffer storage operation on the server
        LOGGER.info("BufferStoragePacket handled - slot: {}, add: {}, item: {}",
                   slotId, isAddOperation, itemStack.getHoverName().getString());

        // TODO: Implement the actual buffer storage logic
        // This would typically involve:
        // 1. Getting the town at the position
        // 2. Updating the town's buffer storage
        // 3. Sending a response packet back to the client
    }

    // Getters
    public BlockPos getPos() { return pos; }
    public ItemStack getItemStack() { return itemStack; }
    public int getSlotId() { return slotId; }
    public boolean isAddOperation() { return isAddOperation; }
}
