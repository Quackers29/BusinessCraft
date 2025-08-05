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
import com.quackers29.businesscraft.network.packets.storage.CommunalStorageResponsePacket;
import com.quackers29.businesscraft.network.ModMessages;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.function.Supplier;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Packet for interacting with town communal storage.
 * Allows players to add or remove items from the storage.
 */
public class CommunalStoragePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommunalStoragePacket.class);
    private final ItemStack itemStack;
    private final int slotId;
    private final boolean isAddOperation; // true = add to storage, false = remove from storage

    public CommunalStoragePacket(BlockPos pos, ItemStack itemStack, int slotId, boolean isAddOperation) {
        super(pos);
        this.itemStack = itemStack;
        this.slotId = slotId;
        this.isAddOperation = isAddOperation;
    }

    public CommunalStoragePacket(FriendlyByteBuf buf) {
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
    public static void encode(CommunalStoragePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static CommunalStoragePacket decode(FriendlyByteBuf buf) {
        return new CommunalStoragePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Get the player who sent the packet
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // If position is null, we can't process the operation
            if (pos == null) {
                LOGGER.warn("Received null position in communal storage packet from player {}", player.getName().getString());
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
            
            // Special case: If slotId is -1, this is a request for all communal storage data
            if (slotId == -1) {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Received request for all communal storage data from player {}", player.getName().getString());
                
                // Get the current communal storage contents
                var storageItems = town.getAllCommunalStorageItems();
                
                // Send a response with all communal storage items
                ModMessages.sendToPlayer(new CommunalStorageResponsePacket(storageItems), player);
                return;
            }
            
            // Check if the item is empty (only do this check for regular operations, not for data requests)
            if (itemStack.isEmpty()) {
                LOGGER.warn("Empty item in communal storage packet from player {} for slot {}", 
                    player.getName().getString(), slotId);
                return;
            }
            
            // Process the storage operation
            int itemCount = itemStack.getCount();
            Item item = itemStack.getItem();
            
            // Add to or remove from communal storage
            boolean success;
            if (isAddOperation) {
                // Add items to storage
                success = town.addToCommunalStorage(item, itemCount);
                
                if (success) {
                    // Send success message to player
                    player.sendSystemMessage(Component.literal("Added " + itemCount + " " + 
                        item.getDescription().getString() + " to " + town.getName() + "'s communal storage."));
                }
            } else {
                // Remove items from storage (negative count)
                success = town.addToCommunalStorage(item, -itemCount);
                
                if (success) {
                    // Send success message to player
                    player.sendSystemMessage(Component.literal("Removed " + itemCount + " " + 
                        item.getDescription().getString() + " from " + town.getName() + "'s communal storage."));
                } else {
                    // Send failure message
                    player.sendSystemMessage(Component.literal("Not enough " + 
                        item.getDescription().getString() + " in communal storage."));
                }
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
                
                // Force the TownManager to save changes
                townManager.markDirty();
                
                // Send a response to update the UI
                ModMessages.sendToPlayer(new CommunalStorageResponsePacket(town.getAllCommunalStorageItems()), player);
            }
        });
        
        ctx.get().setPacketHandled(true);
    }
} 