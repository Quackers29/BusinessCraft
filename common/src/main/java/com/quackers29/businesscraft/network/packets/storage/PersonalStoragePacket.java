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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.LoadedTownAccess;
import com.quackers29.businesscraft.network.PlayerItemExchange;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.network.packets.storage.PersonalStorageResponsePacket;
import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.UUID;

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

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Get the player who sent the packet
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player)) return;
            
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
            
            if (!(player.level() instanceof ServerLevel serverLevel)) {
                LOGGER.warn("Player level is not a ServerLevel");
                return;
            }
            Level level = serverLevel;

            TownInterfaceEntity townInterfaceEntity = LoadedTownAccess.getIfLoadedInReach(player, pos);
            if (townInterfaceEntity == null) {
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
            
            int itemCount = itemStack.getCount();
            if (itemCount <= 0) {
                return;
            }
            Item item = itemStack.getItem();

            boolean success;
            if (isAddOperation) {
                if (!PlayerItemExchange.tryTake(player, item, itemCount)) {
                    player.sendSystemMessage(Component.literal("You don't have enough " +
                            item.getDescription().getString() + " to deposit."));
                    success = false;
                } else {
                    success = town.addToPersonalStorage(playerId, item, itemCount);
                    if (!success) {
                        PlayerItemExchange.give(player, item, itemCount);
                    } else {
                        player.sendSystemMessage(Component.literal("Added " + itemCount + " " +
                                item.getDescription().getString() + " to your personal storage."));
                    }
                }
            } else {
                success = town.addToPersonalStorage(playerId, item, -itemCount);
                if (success) {
                    PlayerItemExchange.give(player, item, itemCount);
                    player.sendSystemMessage(Component.literal("Removed " + itemCount + " " +
                            item.getDescription().getString() + " from your personal storage."));
                } else {
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
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
} 
