package com.quackers29.businesscraft.network.packets.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalInventoryScreen;
import com.quackers29.businesscraft.menu.TradeMenu;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Client-bound packet that sends the result of a resource trade (emerald payment) back to the client.
 */
public class PaymentResultPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentResultPacket.class);
    private final ItemStack paymentItem;

    public PaymentResultPacket(ItemStack paymentItem) {
        this.paymentItem = paymentItem;
    }

    public PaymentResultPacket(FriendlyByteBuf buf) {
        this.paymentItem = buf.readItem();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeItem(paymentItem);
    }
    
    /**
     * Static encode method needed by ModMessages registration
     */
    public static void encode(PaymentResultPacket msg, FriendlyByteBuf buf) {
        msg.toBytes(buf);
    }
    
    /**
     * Static decode method needed by ModMessages registration
     */
    public static PaymentResultPacket decode(FriendlyByteBuf buf) {
        return new PaymentResultPacket(buf);
    }

    public void handle(Object context) {
        PlatformAccess.getNetwork().enqueueWork(context, () -> {
            // Client-side handling
            handleClientSide();
        });
        PlatformAccess.getNetwork().setPacketHandled(context);
    }

    private void handleClientSide() {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
            "Payment result received: {}", paymentItem);
        
        // Get the client helper
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) {
            LOGGER.warn("ClientHelper not available (server side?)");
            return;
        }

        clientHelper.executeOnClientThread(() -> {
            Object currentScreen = clientHelper.getCurrentScreen();
            // Handle traditional TradeScreen first
            if (currentScreen instanceof com.quackers29.businesscraft.ui.screens.town.TradeScreen tradeScreen) {
                tradeScreen.setOutputItem(paymentItem);
            } 
            // Also check for our new modal inventory screen
            else if (currentScreen instanceof BCModalInventoryScreen<?> modalScreen) {
                // Check if the container is a TradeMenu
                if (modalScreen.getMenu() instanceof TradeMenu tradeMenu) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                        "Setting payment item in BCModalInventoryScreen: {}", paymentItem);
                    tradeMenu.setOutputItem(paymentItem.copy());
                }
            }
        });
    }
    
    /**
     * Returns the payment item
     */
    public ItemStack getPaymentItem() {
        return paymentItem;
    }
} 
