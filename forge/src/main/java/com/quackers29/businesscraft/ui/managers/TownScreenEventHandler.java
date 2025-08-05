package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.ui.modal.core.BCModalScreen;
import com.quackers29.businesscraft.ui.modal.core.BCPopupScreen;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import com.quackers29.businesscraft.ui.components.basic.BCPanel;
import com.quackers29.businesscraft.ui.components.basic.BCComponent;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Manages event handling and routing for the Town Interface Screen.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class TownScreenEventHandler {
    
    // Event handler interfaces
    public interface SoundHandler {
        void playButtonClickSound();
    }
    
    public interface ModalStateProvider {
        BCModalScreen getActiveModal();
        BCPopupScreen getActivePopup();
        BCTabPanel getTabPanel();
    }
    
    private final SoundHandler soundHandler;
    private final ModalStateProvider stateProvider;
    private final BottomButtonManager buttonManager;
    
    /**
     * Creates a new event handler.
     * 
     * @param soundHandler Handler for sound effects
     * @param stateProvider Provider for modal and popup state
     * @param buttonManager Manager for bottom buttons
     */
    public TownScreenEventHandler(
            SoundHandler soundHandler, 
            ModalStateProvider stateProvider,
            BottomButtonManager buttonManager) {
        this.soundHandler = soundHandler;
        this.stateProvider = stateProvider;
        this.buttonManager = buttonManager;
    }
    
    /**
     * Handles mouse click events with proper priority ordering.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @param superHandler Fallback handler for unhandled events
     * @return True if the event was handled
     */
    public boolean handleMouseClicked(double mouseX, double mouseY, int button, 
                                    java.util.function.Function<Integer, Boolean> superHandler) {
        
        // Handle clicks on modal screens first, which take priority
        BCModalScreen activeModal = stateProvider.getActiveModal();
        if (activeModal != null) {
            if (activeModal.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // If popup is active, let it handle clicks first
        BCPopupScreen activePopup = stateProvider.getActivePopup();
        if (activePopup != null && activePopup.isVisible()) {
            if (activePopup.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Let the tab panel handle clicks first
        BCTabPanel tabPanel = stateProvider.getTabPanel();
        if (tabPanel != null && tabPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check if bottom buttons grid handled the click
        if (buttonManager != null && buttonManager.mouseClicked((int)mouseX, (int)mouseY, button)) {
            // Play button click sound
            soundHandler.playButtonClickSound();
            return true;
        }
        
        // Fallback to super handler
        return superHandler.apply(button);
    }
    
    /**
     * Handles mouse drag events with proper priority ordering.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @param dragX Drag X delta
     * @param dragY Drag Y delta
     * @param superHandler Fallback handler for unhandled events
     * @return True if the event was handled
     */
    public boolean handleMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY,
                                    java.util.function.Function<Integer, Boolean> superHandler) {
        
        // If modal is active, let it handle drag first
        BCModalScreen activeModal = stateProvider.getActiveModal();
        if (activeModal != null && activeModal.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        // If popup is active, let it handle drag events
        BCPopupScreen activePopup = stateProvider.getActivePopup();
        if (activePopup != null && activePopup.isVisible() && 
            activePopup.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        // Get active tab and forward drag events
        BCTabPanel tabPanel = stateProvider.getTabPanel();
        if (tabPanel != null) {
            String activeTabId = tabPanel.getActiveTabId();
            
            // For scrollable tabs (Resources, Population) - forward drag events to children
            if ("resources".equals(activeTabId) || "population".equals(activeTabId)) {
                BCPanel panel = tabPanel.getTabPanel(activeTabId);
                if (panel != null) {
                    // Forward to all children that are visible components
                    for (UIComponent child : panel.getChildren()) {
                        if (child instanceof BCComponent && ((BCComponent) child).isVisible()) {
                            if (((BCComponent) child).mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            // Let the tab panel handle drag events
            if (tabPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        
        // Fallback to super handler
        return superHandler.apply(button);
    }
    
    /**
     * Handles mouse release events.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button
     * @param superHandler Fallback handler for unhandled events
     * @return True if the event was handled
     */
    public boolean handleMouseReleased(double mouseX, double mouseY, int button,
                                     java.util.function.Function<Integer, Boolean> superHandler) {
        
        // Let the tab panel handle releases first
        BCTabPanel tabPanel = stateProvider.getTabPanel();
        if (tabPanel != null && tabPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        
        // Fallback to super handler
        return superHandler.apply(button);
    }
    
    /**
     * Handles mouse scroll events with proper priority ordering.
     * 
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param delta Scroll delta
     * @param superHandler Fallback handler for unhandled events
     * @return True if the event was handled
     */
    public boolean handleMouseScrolled(double mouseX, double mouseY, double delta,
                                     java.util.function.Function<Double, Boolean> superHandler) {
        
        // If modal is active, let it handle scroll first
        BCModalScreen activeModal = stateProvider.getActiveModal();
        if (activeModal != null && activeModal.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        // If popup is active, let it handle scroll events
        BCPopupScreen activePopup = stateProvider.getActivePopup();
        if (activePopup != null && activePopup.isVisible() && 
            activePopup.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        // Get active tab and forward scroll events to all scrollable tabs in the same way
        BCTabPanel tabPanel = stateProvider.getTabPanel();
        if (tabPanel != null) {
            String activeTabId = tabPanel.getActiveTabId();
            
            // Special case for scrollable tabs (resources and population)
            if ("resources".equals(activeTabId) || "population".equals(activeTabId)) {
                BCPanel panel = tabPanel.getTabPanel(activeTabId);
                if (panel != null) {
                    // Attempt direct scrolling on the panel's children
                    for (UIComponent child : panel.getChildren()) {
                        if (child instanceof BCComponent && ((BCComponent) child).isVisible()) {
                            if (child.mouseScrolled(mouseX, mouseY, delta)) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            // Let the tab panel handle scroll - this will properly forward to our tabs
            if (tabPanel.mouseScrolled(mouseX, mouseY, delta)) {
                return true;
            }
        }
        
        // Fallback to super handler
        return superHandler.apply(delta);
    }
    
    /**
     * Handles keyboard input events with popup priority.
     * 
     * @param keyCode Key code
     * @param scanCode Scan code
     * @param modifiers Modifiers
     * @param superHandler Fallback handler for unhandled events
     * @return True if the event was handled
     */
    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers,
                                  java.util.function.Function<Integer, Boolean> superHandler) {
        
        // If popup is active, let it handle keyboard input first
        BCPopupScreen activePopup = stateProvider.getActivePopup();
        if (activePopup != null && activePopup.isVisible()) {
            // Handle escape key specially to allow closing the popup
            if (keyCode == 256) { // ESCAPE key
                return activePopup.keyPressed(keyCode, scanCode, modifiers);
            }
            
            // For string input popups, completely consume 'e' key (inventory key) to prevent it from closing the popup
            if (keyCode == 69 && activePopup.isInputPopup()) { // 'e' key and input popup
                // Forward to popup for text input handling
                activePopup.keyPressed(keyCode, scanCode, modifiers);
                // Always consume the event
                return true;
            }
            
            // Let popup handle other keys normally
            if (activePopup.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            
            // If it's an input popup, consume all uncaught key events to prevent them from affecting the main screen
            if (activePopup.isInputPopup()) {
                return true;
            }
        }
        
        // Fallback to super handler
        return superHandler.apply(keyCode);
    }
    
    /**
     * Handles character input events with popup priority.
     * 
     * @param character Character typed
     * @param modifiers Modifiers
     * @param superHandler Fallback handler for unhandled events
     * @return True if the event was handled
     */
    public boolean handleCharTyped(char character, int modifiers,
                                 java.util.function.Function<Character, Boolean> superHandler) {
        
        // If popup is active, let it handle character input first
        BCPopupScreen activePopup = stateProvider.getActivePopup();
        if (activePopup != null && activePopup.isVisible()) {
            if (activePopup.charTyped(character, modifiers)) {
                return true;
            }
        }
        
        // Fallback to super handler
        return superHandler.apply(character);
    }
} 