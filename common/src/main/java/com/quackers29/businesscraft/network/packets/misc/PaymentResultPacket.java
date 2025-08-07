package com.quackers29.businesscraft.network.packets.misc;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Platform-agnostic client-bound packet that sends the result of a resource trade back to the client.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class PaymentResultPacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentResultPacket.class);
    private final Object paymentItem;

    /**
     * Create packet for sending.
     */
    public PaymentResultPacket(Object paymentItem) {
        this.paymentItem = paymentItem;
    }

    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static PaymentResultPacket decode(Object buffer) {
        Object itemStack = PlatformServices.getNetworkHelper().readItemStack(buffer);
        return new PaymentResultPacket(itemStack);
    }

    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeItemStack(buffer, paymentItem);
    }

    /**
     * Handle the packet on the client side.
     * This method contains the core client-side logic which is platform-agnostic.
     */
    public void handle(Object player) {
        // This is a client-side packet, player parameter is not used
        LOGGER.debug("Payment result received: {}", paymentItem);
        
        // Check if we're on client side
        if (!PlatformServices.getPlatformHelper().isClientSide()) {
            LOGGER.warn("PaymentResultPacket received on server side - ignoring");
            return;
        }
        
        // Execute on client main thread through platform services
        PlatformServices.getPlatformHelper().executeClientTask(() -> {
            // Update trade screen output through platform services
            PlatformServices.getPlatformHelper().updateTradeScreenOutput(paymentItem);
        });
    }
    
    /**
     * Get the payment item for this packet.
     */
    public Object getPaymentItem() {
        return paymentItem;
    }
}