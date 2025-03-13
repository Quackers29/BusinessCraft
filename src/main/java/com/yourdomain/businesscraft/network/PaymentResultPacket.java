package com.yourdomain.businesscraft.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

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
        LOGGER.debug("Payment result received: {}", paymentItem);
        
        // Get the current screen and if it's TradeScreen, update the output slot
        // We need to use a safer approach with client.execute() since this is client-side processing
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        
        client.execute(() -> {
            if (client.screen instanceof com.yourdomain.businesscraft.screen.TradeScreen tradeScreen) {
                tradeScreen.setOutputItem(paymentItem);
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