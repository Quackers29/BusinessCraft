package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.ui.modal.core.BCModalScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Manages visitor modal creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class VisitorModalManager {
    
    /**
     * Creates and shows a visitor list modal screen.
     * 
     * @param onModalClosed Callback when modal is closed (receives the modal instance)
     * @return The created modal screen
     */
    public static BCModalScreen showVisitorListModal(Consumer<BCModalScreen> onModalClosed) {
        // Create a modal with a list of visitors
        // Use the 2-column constructor for an appropriately sized window
        BCModalScreen modal = new BCModalScreen(
            "Town Visitors", 
            result -> {
                // Handle the result (OK or Back)
                if (result) {
                    sendChatMessage("Selected visitors from the list");
                }
                if (onModalClosed != null) {
                    onModalClosed.accept(null); // Clear the modal reference
                }
            },
            2 // Explicitly specify 2 columns for a narrower width
        );
        
        // Create some example data - just 12 items for a compact display
        List<String> visitorNames = createSampleVisitorData();
        
        // Set the data - this will adjust the height automatically
        modal.setData(visitorNames);
        
        return modal;
    }
    
    /**
     * Creates sample visitor data for demonstration.
     * In a real implementation, this would come from the town data.
     * 
     * @return List of visitor names
     */
    private static List<String> createSampleVisitorData() {
        List<String> visitorNames = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            visitorNames.add("Visitor " + i + " - Town " + (i * 10));
        }
        return visitorNames;
    }
    
    /**
     * Helper method to send a chat message to the player.
     * 
     * @param message The message to send
     */
    private static void sendChatMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }
} 