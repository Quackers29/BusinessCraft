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
        
        confirmButton = Button.builder(Component.translatable("gui.ok"), button -> {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Confirm button clicked with value: {}", currentValue);
            }
            onNameConfirmed.accept(currentValue);
        })
        .size(buttonWidth, BUTTON_HEIGHT)
        .build();
        
        cancelButton = Button.builder(Component.translatable("gui.cancel"), button -> {
            onCancel.run();
        })
        .size(buttonWidth, BUTTON_HEIGHT)
        .build();
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
        
        // Draw buttons below - SWAP positions (OK on right, Cancel on left)
        int buttonY = editBoxY + 30;
        cancelButton.setX(x);
        cancelButton.setY(buttonY);
        confirmButton.setX(x + buttonWidth() + SPACING);
        confirmButton.setY(buttonY);
        
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
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        // Let the edit box handle clicks first
        if (editBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        // Check if click is on confirm button
        if (isMouseOver(confirmButton, mouseX, mouseY)) {
            confirmButton.onPress();
            return true;
        }
        
        // Check if click is on cancel button
        if (isMouseOver(cancelButton, mouseX, mouseY)) {
            cancelButton.onPress();
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if mouse coordinates are over a button
     */
    private boolean isMouseOver(Button button, double mouseX, double mouseY) {
        return mouseX >= button.getX() && mouseX < button.getX() + button.getWidth() &&
               mouseY >= button.getY() && mouseY < button.getY() + button.getHeight();
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
} 