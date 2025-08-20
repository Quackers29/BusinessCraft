package com.quackers29.businesscraft.network.packets.storage;

import com.quackers29.businesscraft.debug.DebugConfig;
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
        // For Enhanced MultiLoader Template, we'll receive RewardEntry objects directly
        // This preserves all metadata including UUID, timestamps, and status
        int size = PlatformServices.getNetworkHelper().readInt(buffer);
        List<Object> rewards = new ArrayList<>();
        
        for (int i = 0; i < size; i++) {
            // Read RewardEntry object directly - preserves all original data
            Object rewardEntry = PlatformServices.getNetworkHelper().readRewardEntry(buffer);
            rewards.add(rewardEntry);
        }
        
        return new PaymentBoardResponsePacket(rewards);
    }
    
    /**
     * Encode packet data for network transmission.
     */
    public void encode(Object buffer) {
        PlatformServices.getNetworkHelper().writeInt(buffer, rewards.size());
        
        for (Object reward : rewards) {
            // For Enhanced MultiLoader compatibility, pass RewardEntry objects directly through packet
            // This preserves all metadata including UUID, timestamps, and status
            PlatformServices.getNetworkHelper().writeRewardEntry(buffer, reward);
        }
    }
    
    /**
     * Handle the packet on the client side.
     * This method contains the core client-side logic which is platform-agnostic.
     */
    public void handleClient() {
        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Received payment board update with {} rewards", rewards.size());
        
        try {
            // Get the current client screen through platform services
            Object currentScreen = PlatformServices.getPlatformHelper().getCurrentScreen();
            
            if (currentScreen != null) {
                // Try to update the payment board screen if it's currently open
                String screenClassName = currentScreen.getClass().getSimpleName();
                
                if (screenClassName.equals("PaymentBoardScreen")) {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Updating PaymentBoardScreen with reward data");
                    // Platform-specific screen updating will be handled by platform implementations
                    PlatformServices.getPlatformHelper().updatePaymentBoardScreen(currentScreen, rewards);
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "Current screen is not PaymentBoardScreen: {}", screenClassName);
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, "No current screen to update with payment board data");
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