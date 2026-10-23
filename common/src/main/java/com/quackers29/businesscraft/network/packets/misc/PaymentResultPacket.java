package com.quackers29.businesscraft.network.packets.misc;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.ClientPacketDispatcher;

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
        PlatformAccess.getNetwork().enqueueWork(context, () -> ClientPacketDispatcher.dispatch(this));
        PlatformAccess.getNetwork().setPacketHandled(context);
    }
    
    /**
     * Returns the payment item
     */
    public ItemStack getPaymentItem() {
        return paymentItem;
    }
} 
