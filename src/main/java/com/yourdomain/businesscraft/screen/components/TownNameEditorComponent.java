package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Composite component for editing town names with text input and ok/cancel buttons
 */
public class TownNameEditorComponent implements UIComponent {
    private static final int SPACING = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LABEL_HEIGHT = 12;
    
    private final int width;
    private final Supplier<String> getCurrentName;
    private final Consumer<String> onNameConfirmed;
    private final Runnable onCancel;
    
    private EditBoxComponent editBox;
    private Button confirmButton;
    private Button cancelButton;
    private final List<UIComponent> subComponents = new ArrayList<>();
    private boolean visible = true;
    private String currentValue = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(TownNameEditorComponent.class);

    public TownNameEditorComponent(int width, Supplier<String> getCurrentName, 
                                  Consumer<String> onNameConfirmed, Runnable onCancel) {
        this.width = width;
        this.getCurrentName = getCurrentName;
        this.onNameConfirmed = onNameConfirmed;
        this.onCancel = onCancel;
        // Initialize the current value with the town name
        this.currentValue = getCurrentName.get();
        createComponents();
    }
    
    private void createComponents() {
        // Create the edit box for name input
        editBox = new EditBoxComponent(
            width - 10, 
            20, 
            getCurrentName, 
            newValue -> {
                // Store the user's input in our currentValue field
                this.currentValue = newValue;
                // Log the updated value to verify it's working
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Town name edited to: {}", newValue);
                }
            }, 
            30
        );
        
        // Ensure the edit box text is set to the current value
        editBox.setText(currentValue);
        
        subComponents.add(editBox);
        
        // Create Ok and Cancel buttons side by side
        int buttonWidth = (width - SPACING) / 2;
        
        // Initialize with default positions - these will be updated during render
        // but having initial values helps with debugging
        confirmButton = Button.builder(Component.translatable("gui.ok"), button -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Confirm button clicked with value: {}", currentValue);
            }
            onNameConfirmed.accept(currentValue);
        })
        .pos(0, 0) // Position will be set in render
        .size(buttonWidth, BUTTON_HEIGHT)
        .build();
        
        cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cancel button clicked");
            }
            onCancel.run();
        })
        .pos(0, 0) // Position will be set in render
        .size(buttonWidth, BUTTON_HEIGHT)
        .build();
        
        // Add buttons to components list for proper management
        subComponents.add(new UIComponent() {
            @Override
            public void init(Consumer<Button> register) {
                // Will be handled in parent's init
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
                // Rendering is handled by parent
            }
            
            @Override
            public int getWidth() {
                return width;
            }
            
            @Override
            public int getHeight() {
                return BUTTON_HEIGHT;
            }
            
            @Override
            public void setVisible(boolean visible) {
                // Will be handled by parent
            }
            
            @Override
            public void tick() {
                // Nothing to tick
            }
        });
    }

    @Override
    public void init(Consumer<Button> register) {
        // Initialize the edit box
        editBox.init(register);
        
        // Register the buttons
        register.accept(confirmButton);
        register.accept(cancelButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        // Draw title
        guiGraphics.drawString(
            Minecraft.getInstance().font, 
            Component.translatable("gui.businesscraft.edit_town_name"), 
            x, y, 0xFFFFFF
        );
        
        // Draw edit box with label
        int editBoxY = y + LABEL_HEIGHT + 8;
        guiGraphics.drawString(
            Minecraft.getInstance().font, 
            Component.translatable("gui.businesscraft.town_name"), 
            x, editBoxY - 10, 0xFFFFFF
        );
        editBox.render(guiGraphics, x, editBoxY, mouseX, mouseY);
        
        // Draw buttons below - OK on right, Cancel on left
        int buttonY = editBoxY + 30;
        int buttonWidth = buttonWidth();
        
        // Position cancel button (left)
        cancelButton.setX(x);
        cancelButton.setY(buttonY);
        
        // Position confirm button (right)
        confirmButton.setX(x + buttonWidth + SPACING);
        confirmButton.setY(buttonY);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Rendering buttons - cancel at {},{} confirm at {},{}", 
                cancelButton.getX(), cancelButton.getY(), 
                confirmButton.getX(), confirmButton.getY());
        }
        
        // Render buttons
        cancelButton.render(guiGraphics, mouseX, mouseY, 0);
        confirmButton.render(guiGraphics, mouseX, mouseY, 0);
    }
    
    private int buttonWidth() {
        return (width - SPACING) / 2;
    }

    @Override
    public void tick() {
        editBox.tick();
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return LABEL_HEIGHT + 8 + 20 + 30 + BUTTON_HEIGHT; // Total height of all elements
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        editBox.setVisible(visible);
        confirmButton.visible = visible;
        cancelButton.visible = visible;
        
        // When showing the editor, focus on the edit box
        if (visible) {
            editBox.setFocused(true);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("TownNameEditorComponent.mouseClicked at {}, {}", mouseX, mouseY);
            LOGGER.debug("Button positions during click - cancel at {},{} confirm at {},{}", 
                cancelButton.getX(), cancelButton.getY(), 
                confirmButton.getX(), confirmButton.getY());
        }
        
        // Let the edit box handle clicks first
        if (editBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Convert component-relative coordinates to screen coordinates for button checks
        // This is needed because in the component's mouseClicked method, coordinates are relative to component
        double adjustedMouseX = mouseX + cancelButton.getX() - cancelButton.getWidth(); // Rough estimate
        double adjustedMouseY = mouseY + cancelButton.getY() - 20; // Rough estimate
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Adjusted click coordinates: {}, {}", adjustedMouseX, adjustedMouseY);
        }
        
        // First check the confirm button directly using adjusted coordinates
        if (isPointInButton(confirmButton, adjustedMouseX, adjustedMouseY)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Confirm button hit in component with adjusted coordinates");
            }
            onNameConfirmed.accept(currentValue);
            return true;
        }
        
        // Then check the cancel button directly using adjusted coordinates
        if (isPointInButton(cancelButton, adjustedMouseX, adjustedMouseY)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Cancel button hit in component with adjusted coordinates");
            }
            onCancel.run();
            return true;
        }
        
        // Fall back to original logic as a last resort
        if (mouseY >= 0 && mouseY < BUTTON_HEIGHT) {
            // Check OK button area
            if (mouseX >= (width / 2) && mouseX < width) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("OK button click detected with standard logic");
                }
                onNameConfirmed.accept(currentValue);
                return true;
            }
            
            // Check Cancel button area
            if (mouseX >= 0 && mouseX < (width / 2)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Cancel button click detected with standard logic");
                }
                onCancel.run();
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Helper method to check if a point is inside a button
     */
    private boolean isPointInButton(Button button, double x, double y) {
        boolean result = x >= button.getX() && x < button.getX() + button.getWidth() &&
                         y >= button.getY() && y < button.getY() + button.getHeight();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Point ({},{}) in button ({},{},{},{}): {}", 
                x, y, button.getX(), button.getY(), button.getWidth(), button.getHeight(), result);
        }
        
        return result;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;
        
        // Handle Enter key to submit 
        if (keyCode == 257 || keyCode == 335) { // Enter or numpad Enter
            onNameConfirmed.accept(currentValue);
            return true;
        }
        
        // Handle Escape to cancel
        if (keyCode == 256) { // Escape
            onCancel.run();
            return true;
        }
        
        return editBox.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public boolean charTyped(char c, int modifiers) {
        if (!visible) return false;
        return editBox.charTyped(c, modifiers);
    }

    /**
     * Gets the edit box component to allow external control like focusing
     * @return The edit box component
     */
    public EditBoxComponent getEditBox() {
        return editBox;
    }
    
    /**
     * External method to handle confirm button click
     */
    public void handleConfirmButtonClick() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Confirm button clicked externally with value: {}", currentValue);
            LOGGER.debug("Button positions - confirm at {},{} dimensions {}x{}", 
                confirmButton.getX(), confirmButton.getY(), 
                confirmButton.getWidth(), confirmButton.getHeight());
        }
        
        // Make sure the confirm button's action is executed directly
        try {
            onNameConfirmed.accept(currentValue);
        } catch (Exception e) {
            LOGGER.error("Error in confirm button handler", e);
        }
    }
    
    /**
     * External method to handle cancel button click
     */
    public void handleCancelButtonClick() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cancel button clicked externally");
            LOGGER.debug("Button positions - cancel at {},{} dimensions {}x{}", 
                cancelButton.getX(), cancelButton.getY(), 
                cancelButton.getWidth(), cancelButton.getHeight());
        }
        
        // Make sure the cancel button's action is executed directly
        try {
            onCancel.run();
        } catch (Exception e) {
            LOGGER.error("Error in cancel button handler", e);
        }
    }
    
    /**
     * Gets the confirm button
     */
    public Button getConfirmButton() {
        return confirmButton;
    }
    
    /**
     * Gets the cancel button
     */
    public Button getCancelButton() {
        return cancelButton;
    }
} 