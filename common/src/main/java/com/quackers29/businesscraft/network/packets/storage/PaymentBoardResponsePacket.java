package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.platform.PlatformServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.ArrayList;

/**
 * Platform-agnostic server-to-client packet that sends payment board reward data.
 * 
 * Enhanced MultiLoader approach: Common module defines packet structure and logic,
 * platform modules handle platform-specific operations through PlatformServices.
 */
public class PaymentBoardResponsePacket {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentBoardResponsePacket.class);
    private final List<Object> rewards;
    
    /**
     * Create packet for sending.
     */
    public PaymentBoardResponsePacket(List<Object> rewards) {
        this.rewards = new ArrayList<>(rewards);
    }
    
    /**
     * Create packet from network buffer (decode constructor).
     * Uses a static decode method pattern for network deserialization.
     */
    public static PaymentBoardResponsePacket decode(Object buffer) {
        // For Enhanced MultiLoader Template, we'll receive rewards as a serialized format
        // The rewards will be handled through platform services for proper deserialization
        int size = PlatformServices.getNetworkHelper().readInt(buffer);
        List<Object> rewards = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            // Read reward entry data - simplified format for Enhanced MultiLoader compatibility
            String rewardData = PlatformServices.getNetworkHelper().readString(buffer);
            rewards.add(rewardData);
        }
        
        return new PaymentBoardResponsePacket(rewards);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeInt(buffer, rewards.size());
        
        for (Object reward : rewards) {
            // For Enhanced MultiLoader compatibility, serialize rewards as strings
            String rewardData = reward.toString();
            PlatformServices.getNetworkHelper().writeString(buffer, rewardData);
        }
    }
    
    /**
     * Handle the packet on the client side.
     * This method contains the core client-side logic which is platform-agnostic.
     */
    public void handleClient() {
        LOGGER.debug("Received payment board update with {} rewards", rewards.size());
        
        try {
            // Get the current client screen through platform services
            Object currentScreen = PlatformServices.getPlatformHelper().getCurrentScreen();
            
            if (currentScreen != null) {
                // Try to update the payment board screen if it's currently open
                String screenClassName = currentScreen.getClass().getSimpleName();
                
                if (screenClassName.equals("PaymentBoardScreen")) {
                    LOGGER.debug("Updating PaymentBoardScreen with reward data");
                    // Platform-specific screen updating will be handled by platform implementations
                    PlatformServices.getPlatformHelper().updatePaymentBoardScreen(currentScreen, rewards);
                } else {
                    LOGGER.debug("Current screen is not PaymentBoardScreen: {}", screenClassName);
                }
            } else {
                LOGGER.debug("No current screen to update with payment board data");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling payment board update", e);
        }
    }
    
    /**
     * Returns the rewards for external access
     */
    public List<Object> getRewards() {
        return rewards;
    }
}