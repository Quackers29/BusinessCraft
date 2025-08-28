package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.network.packets.town.SetTownNamePacket;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.modal.core.BCPopupScreen;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.platform.PlatformServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.client.player.LocalPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;
import com.quackers29.businesscraft.debug.DebugConfig;

/**
 * Manages town name popup creation and handling.
 * Extracted from TownInterfaceScreen to improve code organization.
 * Now supports both static and instance-based usage.
 * 
 * TODO: Restore full functionality when BCPopupScreen is migrated to common
 */
public class TownNamePopupManager {
    
    /**
     * Simple wrapper screen to display BCPopupScreen components.
     * This allows BCPopupScreen (which extends BCPanel) to be displayed as a full screen.
     */
    private static class PopupWrapperScreen extends Screen {
        private final BCPopupScreen popup;
        private final Screen parentScreen;
        
        public PopupWrapperScreen(BCPopupScreen popup, Screen parentScreen) {
            super(Component.literal("Town Name Editor"));
            this.popup = popup;
            this.parentScreen = parentScreen;
        }
        
        @Override
        protected void init() {
            super.init();
            if (popup != null) {
                // Set popup bounds to center on screen
                int popupWidth = popup.getWidth();
                int popupHeight = popup.getHeight();
                int x = (this.width - popupWidth) / 2;
                int y = (this.height - popupHeight) / 2;
                
                popup.setParentBounds(x, y, popupWidth, popupHeight);
            }
        }
        
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Render dark background
            this.renderBackground(graphics);
            
            // Render the popup
            if (popup != null) {
                popup.render(graphics, 0, 0, mouseX, mouseY);
            }
            
            super.render(graphics, mouseX, mouseY, partialTick);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (popup != null && popup.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        
        @Override
        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            // Let popup handle keys first (including ESC)
            if (popup != null && popup.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            
            // Fallback ESC handling in case popup didn't handle it
            if (keyCode == 256) { // ESC key
                this.onClose();
                return true;
            }
            
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        
        @Override
        public boolean charTyped(char c, int modifiers) {
            if (popup != null && popup.charTyped(c, modifiers)) {
                return true;
            }
            return super.charTyped(c, modifiers);
        }
        
        @Override
        public void onClose() {
            // Return to parent screen
            if (parentScreen != null) {
                Minecraft.getInstance().setScreen(parentScreen);
            } else {
                super.onClose();
            }
        }
        
        @Override
        public boolean isPauseScreen() {
            return false; // Don't pause the game
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(TownNamePopupManager.class);
    
    private final BaseTownScreen<?> screen;
    private BCPopupScreen activePopup;
    
    /**
     * Creates a new TownNamePopupManager for instance-based usage.
     * 
     * @param screen The parent screen
     */
    public TownNamePopupManager(BaseTownScreen<?> screen) {
        this.screen = screen;
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownNamePopupManager instance created");
    }
    
    /**
     * Shows the change town name popup using the screen's context.
     */
    public void showChangeTownNamePopup() {
        try {
            if (screen.getMenu() == null) {
                throw new IllegalStateException("Screen menu is not available");
            }
            
            String currentTownName = getCurrentTownName();
            BlockPos blockPos = screen.getMenu().getBlockPos();
            
            if (blockPos == null) {
                throw new IllegalStateException("Block position is not available");
            }
            
            // Create the popup
            activePopup = BCPopupScreen.createStringInputPopup(
                "Change Town Name", 
                result -> {
                    // Handle the result
                    if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                        String newName = result.getStringValue().trim();
                        
                        // Send packet to update town name on the server
                        PlatformServices.getNetworkHelper().sendToServer(
                            new SetTownNamePacket(blockPos.getX(), blockPos.getY(), blockPos.getZ(), newName)
                        );
                        
                        // Provide immediate client-side feedback
                        TownNamePopupManager.sendChatMessage("Changing town name to: " + newName);
                    }
                    
                    // Close the popup
                    closeActivePopup();
                }
            );
            
            if (activePopup != null) {
                // Set initial value
                activePopup.setInitialValue(currentTownName);
                
                // Create wrapper screen and display it
                PopupWrapperScreen wrapper = new PopupWrapperScreen(activePopup, Minecraft.getInstance().screen);
                
                // Set close handler to return to the original screen
                activePopup.setClosePopupHandler(button -> {
                    wrapper.onClose(); // This will return to parent screen
                });
                
                Minecraft.getInstance().setScreen(wrapper);
                activePopup.focusInput(); // Focus the input field for immediate typing
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Town name popup displayed via wrapper screen with input focused and close handler set");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to show change town name popup", e);
            screen.sendChatMessage("Unable to open town name editor");
        }
    }
    
    /**
     * Gets the current town name from the screen's cache.
     */
    private String getCurrentTownName() {
        try {
            if (screen instanceof com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) {
                return ((com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen) screen).getCachedTownName();
            }
            
            // Fallback to generic screen name
            return "Unknown Town";
        } catch (Exception e) {
            LOGGER.warn("Failed to get current town name", e);
            return "Unknown Town";
        }
    }
    
    /**
     * Gets the active popup instance.
     */
    public BCPopupScreen getActivePopup() {
        return activePopup;
    }
    
    /**
     * Closes the currently active popup if one exists.
     */
    public void closeActivePopup() {
        if (activePopup != null) {
            activePopup = null;
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Active popup closed");
        }
    }
    
    /**
     * Cleanup method to be called when the screen is closed.
     */
    public void cleanup() {
        closeActivePopup();
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "TownNamePopupManager cleanup completed");
    }
    
    /**
     * Creates and shows a town name change popup.
     * TODO: Restore when BCPopupScreen is migrated to common
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
        try {
            // Create the popup
            BCPopupScreen[] popupRef = new BCPopupScreen[1]; // Array to hold reference for lambda
            
            BCPopupScreen popup = BCPopupScreen.createStringInputPopup(
                "Change Town Name", 
                result -> {
                    // Handle the result
                    if (result.isConfirmed() && !result.getStringValue().isEmpty()) {
                        String newName = result.getStringValue().trim();
                        
                        // Send packet to update town name on the server
                        PlatformServices.getNetworkHelper().sendToServer(
                            new SetTownNamePacket(blockPos.getX(), blockPos.getY(), blockPos.getZ(), newName)
                        );
                        
                        // Provide immediate client-side feedback
                        sendChatMessage("Changing town name to: " + newName);
                    }
                    
                    // Notify callback
                    if (onPopupClosed != null) {
                        onPopupClosed.accept(popupRef[0]);
                    }
                }
            );
            
            // Store reference for lambda
            popupRef[0] = popup;
            
            if (popup != null) {
                // Set initial value
                popup.setInitialValue(currentTownName);
                
                // Create wrapper screen and display it
                PopupWrapperScreen wrapper = new PopupWrapperScreen(popup, Minecraft.getInstance().screen);
                
                // Set close handler to return to the original screen
                popup.setClosePopupHandler(button -> {
                    wrapper.onClose(); // This will return to parent screen
                });
                
                Minecraft.getInstance().setScreen(wrapper);
                popup.focusInput(); // Focus the input field for immediate typing
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Static town name popup displayed via wrapper screen with input focused and close handler set");
            }
            
            return popup;
            
        } catch (Exception e) {
            LOGGER.error("Failed to create static town name popup", e);
            sendChatMessage("Unable to create town name editor");
            return null;
        }
    }
    
    /**
     * Positions a popup at the center of the screen.
     * Helper method for consistent popup positioning.
     * Note: BCPopupScreen handles its own positioning automatically.
     * 
     * @param popup The popup to position
     */
    public static void positionPopupAtCenter(BCPopupScreen popup) {
        if (popup != null) {
            // BCPopupScreen handles centering automatically
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Popup positioning handled automatically by BCPopupScreen");
        }
    }
    
    /**
     * Sends a chat message to the player.
     * Helper method for consistent messaging.
     * 
     * @param message The message to send
     */
    public static void sendChatMessage(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }
    
    /**
     * Sets the active popup on a screen.
     * Helper method for consistent popup management.
     * Note: This is a placeholder - popup management is handled by the parent screen.
     * 
     * @param screen The screen to set the popup on
     * @param popup The popup to set as active
     */
    public static void setScreenActivePopup(BaseTownScreen<?> screen, BCPopupScreen popup) {
        try {
            // Popup management is handled by parent screen implementation
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Popup management delegated to parent screen: " + screen.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.warn("Failed to delegate popup management to parent screen", e);
        }
    }
    
    /**
     * Clears the active popup from a screen.
     * Helper method for consistent popup cleanup.
     * Note: This is a placeholder - popup management is handled by the parent screen.
     * 
     * @param screen The screen to clear the popup from
     */
    public static void clearScreenActivePopup(BaseTownScreen<?> screen) {
        try {
            // Popup management is handled by parent screen implementation
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Popup cleanup delegated to parent screen: " + screen.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.warn("Failed to delegate popup cleanup to parent screen", e);
        }
    }
}