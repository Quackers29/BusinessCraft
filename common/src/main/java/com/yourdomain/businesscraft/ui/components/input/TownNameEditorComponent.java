package com.yourdomain.businesscraft.ui.components.input;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
import java.util.List;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
import java.util.function.Consumer;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
import java.util.function.Supplier;
import com.yourdomain.businesscraft.ui.components.basic.UIComponent;
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
    
    private BCEditBoxComponent editBox;
    private Button confirmButton;
    private Button cancelButton;
    private final List<UIComponent> subComponents = new ArrayList<>();
    private boolean visible = true;
    private String currentValue = "";
    private int x, y;
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
        editBox = new BCEditBoxComponent(
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
            private boolean visible = true;
            private int x, y;
            
            @Override
            public void init(Consumer<Button> register) {
                // Will be handled in parent's init
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
                // Rendering is handled by parent
                this.x = x;
                this.y = y;
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
                this.visible = visible;
            }
            
            @Override
            public void tick() {
                // Nothing to tick
            }
            
            @Override
            public boolean isVisible() {
                return visible;
            }
            
            @Override
            public int getX() {
                return x;
            }
            
            @Override
            public int getY() {
                return y;
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
        
        this.x = x;
        this.y = y;
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
        
        // Update any child components visibility
        for (UIComponent component : subComponents) {
            component.setVisible(visible);
        }
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        // Let the edit box handle clicks first
        if (editBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Simple logical button area checks as fallback for direct clicks
        if (mouseY >= 0 && mouseY < BUTTON_HEIGHT) {
            // Check OK button area (right half)
            if (mouseX >= (width / 2) && mouseX < width) {
                onNameConfirmed.accept(currentValue);
                return true;
            }
            
            // Check Cancel button area (left half)
            if (mouseX >= 0 && mouseX < (width / 2)) {
                onCancel.run();
                return true;
            }
        }
        
        return false;
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
    public BCEditBoxComponent getEditBox() {
        return editBox;
    }
    
    /**
     * External method to handle confirm button click
     */
    public void handleConfirmButtonClick() {
        onNameConfirmed.accept(currentValue);
    }
    
    /**
     * External method to handle cancel button click
     */
    public void handleCancelButtonClick() {
        onCancel.run();
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