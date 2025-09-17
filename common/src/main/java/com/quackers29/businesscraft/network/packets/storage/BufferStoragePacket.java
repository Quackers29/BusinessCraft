package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet for interacting with payment board buffer storage.
 * Allows players to add or remove items from the 2x9 buffer.
 */
public class BufferStoragePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(BufferStoragePacket.class);
    private final ItemStack itemStack;
    private final int slotId;
    private final boolean isAddOperation; // true = add to buffer, false = remove from buffer

    public BufferStoragePacket(BlockPos pos, ItemStack itemStack, int slotId, boolean isAddOperation) {
        super(pos);
        this.itemStack = itemStack;
        this.slotId = slotId;
        this.isAddOperation = isAddOperation;
    }

    public BufferStoragePacket(FriendlyByteBuf buf) {
        super(buf);
        this.itemStack = buf.readItem();
        this.slotId = buf.readInt();
        this.isAddOperation = buf.readBoolean();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeItem(itemStack);
        buf.writeInt(slotId);
        buf.writeBoolean(isAddOperation);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(BufferStoragePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static BufferStoragePacket decode(FriendlyByteBuf buf) {
        return new BufferStoragePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Get the player who sent the packet
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // If position is null, we can't process the operation
            if (pos == null) {
                LOGGER.warn("Received null position in buffer storage packet from player {}", player.getName().getString());
                return;
            }
            
            // Get the level
            Level level = player.level();
            if (!(level instanceof ServerLevel serverLevel)) {
                LOGGER.warn("Player level is not a ServerLevel");
                return;
            }
            
            // Get the town block entity at the position
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof TownInterfaceEntity townInterfaceEntity)) {
                LOGGER.warn("No town block entity found at position {} for player {}", pos, player.getName().getString());
                return;
            }
            
            // Get the town manager
            TownManager townManager = TownManager.get(serverLevel);
            if (townManager == null) {
                LOGGER.error("Town manager is null");
                return;
            }
            
            // Get the town from the town manager
            Town town = townManager.getTown(townInterfaceEntity.getTownId());
            if (town == null) {
                LOGGER.warn("No town found for town block at position {} for player {}", pos, player.getName().getString());
                return;
            }
            
            // Special case: If slotId is -1, this is a request for all buffer storage data
            if (slotId == -1) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Received request for all buffer storage data from player {}", player.getName().getString());
                
                // Send a response with slot-based buffer storage data
                PlatformAccess.getNetworkMessages().sendToPlayer(new BufferSlotStorageResponsePacket(town.getPaymentBoard().getBufferStorageSlots()), player);
                return;
            }
            
            // Check if the item is empty (only do this check for regular operations, not for data requests)
            if (itemStack.isEmpty()) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Empty item in buffer storage packet from player {} for slot {} - this is normal for item removal", 
                    player.getName().getString(), slotId);
                return;
            }
            
            // Process the buffer storage operation
            int itemCount = itemStack.getCount();
            Item item = itemStack.getItem();
            
            // Add to or remove from buffer storage
            boolean success;
            if (isAddOperation) {
                // Add items to buffer storage
                success = town.addToCommunalStorage(item, itemCount);
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Buffer storage ADD - Player {} {} {} {} to town {}", 
                    player.getName().getString(), 
                    success ? "successfully added" : "failed to add",
                    itemCount, item.getDescription().getString(), town.getName());
            } else {
                // Remove items from buffer storage (negative count)
                success = town.addToCommunalStorage(item, -itemCount);
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Buffer storage REMOVE - Player {} {} {} {} from town {}", 
                    player.getName().getString(), 
                    success ? "successfully removed" : "failed to remove",
                    itemCount, item.getDescription().getString(), town.getName());
            }
            
            if (success) {
                // Mark dirty after storage update
                townManager.markDirty();
                
                // Force the townInterfaceEntity to update and sync
                townInterfaceEntity.setChanged();
                
                // Ensure town data is properly synchronized
                townInterfaceEntity.syncTownData();
                
                // Sync the block entity to update client-side resource cache
                level.sendBlockUpdated(pos, townInterfaceEntity.getBlockState(), townInterfaceEntity.getBlockState(), 
                    Block.UPDATE_ALL);
                
                // Send a response to update the UI with slot-based data
                PlatformAccess.getNetworkMessages().sendToPlayer(new BufferSlotStorageResponsePacket(town.getPaymentBoard().getBufferStorageSlots()), player);
            }
        });
        
        ctx.get().setPacketHandled(true);
    }
}
