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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.network.packets.misc.BaseBlockEntityPacket;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.api.PlatformAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.quackers29.businesscraft.menu.TradeMenu;

/**
 * Packet for trading resources with towns.
 * Adds input items to town resources and gives emeralds as payment if
 * available.
 */
public class TradeResourcePacket extends BaseBlockEntityPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(TradeResourcePacket.class);
    private final ItemStack itemToTrade;
    private final int slotId;

    // Number of items needed to receive 1 emerald -- LEGACY
    // private static final int ITEMS_PER_EMERALD = 10;

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

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Get the player who sent the packet
            Object senderObj = PlatformAccess.getNetwork().getSender(context);
            if (!(senderObj instanceof ServerPlayer player))
                return;

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
                LOGGER.warn("No town block entity found at position {} for player {}", pos,
                        player.getName().getString());
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
                LOGGER.warn("No town found for town block at position {} for player {}", pos,
                        player.getName().getString());
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

            // 1. Resolve Currency Item from Config
            String currencyId = com.quackers29.businesscraft.config.ConfigLoader.currencyItem;
            Item currencyItem = Items.EMERALD; // Default
            try {
                // Try to parse the configured item ID
                net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(currencyId);
                Item configuredItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(loc);
                if (configuredItem != Items.AIR) {
                    currencyItem = configuredItem;
                } else {
                    LOGGER.warn("Configured currency item '{}' not found, defaulting to Emerald", currencyId);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to parse currency item '{}', defaulting to Emerald", currencyId, e);
            }

            // Determine Resource ID and Market Price
            String resourceId;
            com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                    .getFor(itemToTrade.getItem());
            if (type != null) {
                resourceId = type.getId();
            } else {
                resourceId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemToTrade.getItem())
                        .toString();
            }

            float unitPrice = com.quackers29.businesscraft.economy.GlobalMarket.get().getPrice(resourceId);

            // Get current currency count before calculation
            long currentCurrency = town.getResourceCount(currencyItem);
            DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "BEFORE TRADE: Town {} has {} currency ({})",
                    town.getName(), currentCurrency, currencyItem);

            // Calculate paymentAmt to give based on the trade
            long paymentAmt = calculatePayment(itemToTrade, town, currencyItem, unitPrice);

            // Create payment item
            ItemStack payment = ItemStack.EMPTY;
            if (paymentAmt > 0) {
                payment = new ItemStack(currencyItem, (int) paymentAmt);

                // Deduct currency from town resources
                town.addResource(currencyItem, -paymentAmt);

                // Explicitly mark the town manager as dirty to persist changes
                townManager.markDirty();

                // Force the townInterfaceEntity to update and sync
                townInterfaceEntity.setChanged();

                // Ensure town data is properly synchronized
                townInterfaceEntity.syncTownData();

                // Get updated currency count after deduction
                long newCurrency = town.getResourceCount(currencyItem);

                DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS,
                        "AFTER TRADE: Town {} now has {} currency (deducted {})",
                        town.getName(), newCurrency, paymentAmt);

                // Send feedback to the player about the trade AND the deduction
                player.sendSystemMessage(Component.literal("Traded " + itemCount + " " +
                        itemToTrade.getItem().getDescription().getString() +
                        " to " + town.getName() + " for " + paymentAmt + " " + currencyItem.getDescription().getString()
                        + "."));

                // Add explicit deduction notification
                player.sendSystemMessage(
                        Component.literal("§6" + paymentAmt + " " + currencyItem.getDescription().getString()
                                + " were deducted from town resources."));

                // Record the trade in the Global Market
                com.quackers29.businesscraft.economy.GlobalMarket.get().recordTrade(resourceId, itemCount, unitPrice);

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
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    /**
     * Calculate payment based on the trade amount and item value
     */
    private long calculatePayment(ItemStack itemToTrade, Town town, Item currencyItem, float unitPrice) {
        int itemCount = itemToTrade.getCount();

        // distinct from "emeralds", we call it payment units
        // Use the passed unitPrice (from GlobalMarket)
        float baseValue = unitPrice;

        // Calculation: Total Value = Item Count * Base Value
        // We cast to int, so fractional values might be lost if not accummulated.
        // Current logic doesn't support fractional accumulation (yet), so we just floor
        // it.
        long paymentCount = (long) (itemCount * baseValue);

        // Check if town has enough currency
        long availableCurrency = town.getResourceCount(currencyItem);
        DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS,
                "Trade calculation: {} items * {} value = {} payment, town has {} currency available, currency item: {}",
                itemCount, baseValue, paymentCount, availableCurrency, currencyItem);

        // If no payment would be awarded, return early
        if (paymentCount <= 0) {
            return 0;
        }

        // Ensure the town has enough currency to pay
        if (availableCurrency < paymentCount) {
            LOGGER.warn("Town {} doesn't have enough currency for trade! Requested: {}, Available: {}",
                    town.getName(), paymentCount, availableCurrency);
            // Cap the payment to what's available
            return availableCurrency;
        }

        return paymentCount;
    }
}
