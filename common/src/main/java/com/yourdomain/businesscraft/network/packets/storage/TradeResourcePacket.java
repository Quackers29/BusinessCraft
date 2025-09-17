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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.debug.DebugConfig;
import com.yourdomain.businesscraft.block.entity.TownInterfaceEntity;
import com.yourdomain.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.yourdomain.businesscraft.network.packets.misc.PaymentResultPacket;
import com.yourdomain.businesscraft.api.PlatformAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.yourdomain.businesscraft.menu.TradeMenu;

import java.util.function.Supplier;

/**
 * Packet for trading resources with towns.
 * Adds input items to town resources and gives emeralds as payment if available.
 */
public class TradeResourcePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeResourcePacket.class);
    private final ItemStack itemToTrade;
    private final int slotId;
    
    // Number of items needed to receive 1 emerald
    private static final int ITEMS_PER_EMERALD = 10;

    public TradeResourcePacket(BlockPos pos, ItemStack itemToTrade, int slotId) {
        super(pos);
        this.itemToTrade = itemToTrade;
        this.slotId = slotId;
    }

    public TradeResourcePacket(FriendlyByteBuf buf) {
        super(buf);
        this.itemToTrade = buf.readItem();
        this.slotId = buf.readInt();
    }

    @Override
    public void toBytes(FriendlyByteBuf buf) {
        super.toBytes(buf);
        buf.writeItem(itemToTrade);
        buf.writeInt(slotId);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(TradeResourcePacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static TradeResourcePacket decode(FriendlyByteBuf buf) {
        return new TradeResourcePacket(buf);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // Get the player who sent the packet
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;
            
            // Check if the item to trade is empty
            if (itemToTrade.isEmpty()) {
                LOGGER.warn("Received empty item in trade packet from player {}", player.getName().getString());
                return;
            }
            
            // If position is null, we can't process the trade
            if (pos == null) {
                LOGGER.warn("Received null position in trade packet from player {}", player.getName().getString());
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
            
            // Add the item to the town's resources
            int itemCount = itemToTrade.getCount();
            town.addResource(itemToTrade.getItem(), itemCount);
            
            // MARK DIRTY AFTER ADDING RESOURCES
            townManager.markDirty();
            
            DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "Player {} traded {} x{} to town {}", 
                player.getName().getString(), 
                itemToTrade.getItem().getDescription().getString(), 
                itemCount,
                town.getName());
            
            // Get current emerald count before calculation
            int currentEmeralds = town.getResourceCount(Items.EMERALD);
            DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "BEFORE TRADE: Town {} has {} emeralds", town.getName(), currentEmeralds);
            
            // Calculate emeralds to give based on the trade
            int emeraldsToGive = calculateEmeralds(itemToTrade, town);
            
            // Create payment item (emeralds)
            ItemStack payment = ItemStack.EMPTY;
            if (emeraldsToGive > 0) {
                payment = new ItemStack(Items.EMERALD, emeraldsToGive);
                
                // Deduct emeralds from town resources
                town.addResource(Items.EMERALD, -emeraldsToGive);
                
                // Explicitly mark the town manager as dirty to persist changes
                townManager.markDirty();
                
                // Force the townInterfaceEntity to update and sync
                townInterfaceEntity.setChanged();
                
                // Ensure town data is properly synchronized
                townInterfaceEntity.syncTownData();
                
                // Get updated emerald count after deduction
                int newEmeralds = town.getResourceCount(Items.EMERALD);
                
                DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "AFTER TRADE: Town {} now has {} emeralds (deducted {})", 
                    town.getName(), newEmeralds, emeraldsToGive);
                    
                // Send feedback to the player about the trade AND the deduction
                player.sendSystemMessage(Component.literal("Traded " + itemCount + " " + 
                    itemToTrade.getItem().getDescription().getString() + 
                    " to " + town.getName() + " for " + emeraldsToGive + " emeralds."));
                
                // Add explicit deduction notification
                player.sendSystemMessage(Component.literal("§6" + emeraldsToGive + " emeralds were deducted from town resources."));
            } else {
                // Send feedback that no payment was given
                player.sendSystemMessage(Component.literal("Traded " + itemCount + " " + 
                    itemToTrade.getItem().getDescription().getString() + 
                    " to " + town.getName() + " but received no payment."));
            }
            
            // Send the payment result back to the client
            PlatformAccess.getNetworkMessages().sendToPlayer(new PaymentResultPacket(payment), player);
            
            // Sync the block entity to update client-side resource cache
            level.sendBlockUpdated(pos, townInterfaceEntity.getBlockState(), townInterfaceEntity.getBlockState(), 
                Block.UPDATE_ALL);
            
            // Ensure town data is properly synchronized with the block entity
            townInterfaceEntity.syncTownData();
            
            // Force the TownManager to save changes
            townManager.markDirty();
        });
        
        ctx.get().setPacketHandled(true);
    }
    
    /**
     * Calculate how many emeralds to give based on the trade amount
     */
    private int calculateEmeralds(ItemStack itemToTrade, Town town) {
        int itemCount = itemToTrade.getCount();
        int emeraldCount = itemCount / ITEMS_PER_EMERALD;
        
        // Check if town has enough emeralds
        int availableEmeralds = town.getResourceCount(Items.EMERALD);
        DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "Trade calculation: {} items = {} emeralds, town has {} emeralds available", 
            itemCount, emeraldCount, availableEmeralds);
        
        // If no emeralds would be awarded, return early
        if (emeraldCount <= 0) {
            return 0;
        }
        
        // Ensure the town has enough emeralds to pay
        if (availableEmeralds < emeraldCount) {
            LOGGER.warn("Town {} doesn't have enough emeralds for trade! Requested: {}, Available: {}", 
                town.getName(), emeraldCount, availableEmeralds);
            // Cap the emerald payment to what's available
            return availableEmeralds;
        }
        
        return emeraldCount;
    }
} 