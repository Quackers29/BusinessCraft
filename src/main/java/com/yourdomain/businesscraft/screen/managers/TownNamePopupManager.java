package com.yourdomain.businesscraft.screen.managers;

import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.packets.town.SetTownNamePacket;
import com.yourdomain.businesscraft.screen.components.BCComponentFactory;
import com.yourdomain.businesscraft.screen.components.BCPopupScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.player.LocalPlayer;
import java.util.function.Consumer;

/**
 * Manages town name popup creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class TownNamePopupManager {
    
    /**
     * Creates and shows a town name change popup.
     * 
     * @param currentTownName The current town name to display as default
     * @param blockPos The position of the town block
     * @param onPopupClosed Callback when popup is closed (receives the popup instance)
     * @return The created popup screen
     */
    public static BCPopupScreen showChangeTownNamePopup(
            String currentTownName, 
            BlockPos blockPos, 
            Consumer<BCPopupScreen> onPopupClosed) {
        
        // Create a popup for changing the town name
        BCPopupScreen popup = BCComponentFactory.createStringInputPopup(
            "Change Town Name", 
            currentTownName, // Initial value
            result -> {
                // Handle the result
                if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                    String newName = result.getStringValue().trim();
                    
                    // Send packet to update town name on the server
                    ModMessages.sendToServer(
                        new SetTownNamePacket(blockPos, newName)
                    );
                    
                    // Provide immediate client-side feedback
                    sendChatMessage("Changing town name to: " + newName);
                }
            }
        );
        
        // Position the popup at screen center
        positionPopupAtCenter(popup);
        
        // Set close handler
        popup.setClosePopupHandler(button -> {
            if (onPopupClosed != null) {
                onPopupClosed.accept(null); // Clear the popup reference
            }
        });
        
        return popup;
    }
    
    /**
     * Positions a popup at the center of the screen.
     * 
     * @param popup The popup to position
     */
    private static void positionPopupAtCenter(BCPopupScreen popup) {
        // Get screen dimensions
        Minecraft minecraft = Minecraft.getInstance();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        
        // Calculate exact center position
        int popupWidth = 300; // Same as in createStringInputPopup
        int popupHeight = 150; // Same as in createStringInputPopup
        int centerX = screenWidth / 2 - popupWidth / 2;
        int centerY = screenHeight / 2 - popupHeight / 2;
        
        // Directly position the popup at the center of the screen
        popup.position(centerX, centerY);
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