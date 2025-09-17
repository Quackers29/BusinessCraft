package com.yourdomain.businesscraft.network.packets.storage;

import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
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
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.yourdomain.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.yourdomain.businesscraft.api.PlatformAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Packet for interacting with a player's personal storage in a town.
 * Allows players to add or remove items from their own personal storage.
 */
public class PersonalStoragePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonalStoragePacket.class);
    private final ItemStack itemStack;
    private final int slotId;
    private final boolean isAddOperation; // true = add to storage, false = remove from storage
    private final UUID playerId; // The player's UUID for personal storage

    public PersonalStoragePacket(BlockPos pos, ItemStack itemStack, int slotId, boolean isAddOperation, UUID playerId) {
        super(pos);
        this.itemStack = itemStack;
        this.slotId = slotId;
        this.isAddOperation = isAddOperation;
        this.playerId = playerId;
    }

    public PersonalStoragePacket(FriendlyByteBuf buf) {
        super(buf);
        this.itemStack = buf.readItem();
        this.slotId = buf.readInt();
        this.isAddOperation = buf.readBoolean();
        this.playerId = buf.readUUID();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeItem(itemStack);
        buf.writeInt(slotId);
        buf.writeBoolean(isAddOperation);
        buf.writeUUID(playerId);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PersonalStoragePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PersonalStoragePacket decode(FriendlyByteBuf buf) {
        return new PersonalStoragePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Get the player who sent the packet
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // Verify player ID matches sender (security check)
            if (!player.getUUID().equals(playerId)) {
                LOGGER.warn("Player {} attempted to access personal storage of another player {}!", 
                    player.getName().getString(), playerId);
                return;
            }
            
            // Check if the item is empty
            if (itemStack.isEmpty()) {
                LOGGER.warn("Received empty item in personal storage packet from player {}", player.getName().getString());
                return;
            }
            
            // If position is null, we can't process the operation
            if (pos == null) {
                LOGGER.warn("Received null position in personal storage packet from player {}", player.getName().getString());
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
            
            // Process the storage operation
            int itemCount = itemStack.getCount();
            Item item = itemStack.getItem();
            
            // Add to or remove from personal storage
            boolean success;
            if (isAddOperation) {
                // Add items to storage
                success = town.addToPersonalStorage(playerId, item, itemCount);
                
                if (success) {
                    // Send success message to player
                    player.sendSystemMessage(Component.literal("Added " + itemCount + " " + 
                        item.getDescription().getString() + " to your personal storage."));
                }
            } else {
                // Remove items from storage (negative count)
                success = town.addToPersonalStorage(playerId, item, -itemCount);
                
                if (success) {
                    // Send success message to player
                    player.sendSystemMessage(Component.literal("Removed " + itemCount + " " + 
                        item.getDescription().getString() + " from your personal storage."));
                } else {
                    // Send failure message
                    player.sendSystemMessage(Component.literal("Not enough " + 
                        item.getDescription().getString() + " in your personal storage."));
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
                PlatformAccess.getNetworkMessages().sendToPlayer(new PersonalStorageResponsePacket(town.getPersonalStorageItems(playerId)), player);
            }
        });
        
        ctx.get().setPacketHandled(true);
    }
} 