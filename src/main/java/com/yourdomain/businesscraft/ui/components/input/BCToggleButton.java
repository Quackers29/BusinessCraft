package com.yourdomain.businesscraft.ui.components.input;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import java.util.function.Consumer;
import com.yourdomain.businesscraft.ui.components.basic.BCComponent;
import com.yourdomain.businesscraft.ui.components.basic.BCButton;

/**
 * Toggle button component for BusinessCraft UI system.
 * Provides a button that can be toggled on and off.
 */
public class BCToggleButton extends BCButton {
    private boolean toggled;
    private int toggledBackgroundColor = 0xA0335599;   // Semi-transparent blue when toggled
    private int untoggledBackgroundColor = 0x80333333; // Semi-transparent gray when untoggled
    private int toggledBorderColor = 0xFFFFFFFF;       // White border when toggled
    private int untoggledBorderColor = 0x80FFFFFF;     // Semi-transparent white border when untoggled
    private Consumer<BCToggleButton> toggleHandler;
    
    /**
     * Create a new toggle button with standard Button consumer
     */
    public BCToggleButton(boolean initialState, Consumer<Button> onToggle) {
        super(Component.empty(), onToggle, 120, 20);
        this.toggled = initialState;
        this.withBackground(false); // We'll handle our own background
    }
    
    /**
     * Create a new toggle button with special toggle handler
     * (workaround for type erasure)
     */
    public static BCToggleButton createWithToggleHandler(boolean initialState, Consumer<BCToggleButton> toggleHandler) {
        BCToggleButton button = new BCToggleButton(initialState, (Button b) -> {});
        button.toggleHandler = toggleHandler;
        return button;
    }
    
    /**
     * Set the toggled state
     */
    public BCToggleButton setToggled(boolean toggled) {
        this.toggled = toggled;
        return this;
    }
    
    /**
     * Get the toggled state
     */
    public boolean isToggled() {
        return toggled;
    }
    
    /**
     * Set the background color when toggled
     */
    public BCToggleButton withToggledBackgroundColor(int color) {
        this.toggledBackgroundColor = color;
        return this;
    }
    
    /**
     * Set the background color when untoggled
     */
    public BCToggleButton withUntoggledBackgroundColor(int color) {
        this.untoggledBackgroundColor = color;
        return this;
    }
    
    /**
     * Set the border color when toggled
     */
    public BCToggleButton withToggledBorderColor(int color) {
        this.toggledBorderColor = color;
        return this;
    }
    
    /**
     * Set the border color when untoggled
     */
    public BCToggleButton withUntoggledBorderColor(int color) {
        this.untoggledBorderColor = color;
        return this;
    }
    
    /**
     * Handle mouse click to toggle the button state
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (mouseX >= getX() && mouseX < getX() + getWidth() && 
            mouseY >= getY() && mouseY < getY() + getHeight() && button == 0) {
            // Toggle the state
            toggled = !toggled;
            
            // Play button sound
            playButtonSound();
            
            // Call the original handler if set
            if (onPress != null) {
                onPress.accept(null);
            }
            
            // Call the toggle handler if set
            if (toggleHandler != null) {
                toggleHandler.accept(this);
            }
            
            return true;
        }
        return false;
    }
    
    /**
     * Play the button click sound
     */
    private void playButtonSound() {
        Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)
        );
    }
    
    /**
     * Render the toggle button
     */
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= getX() && mouseX < getX() + getWidth() && 
                         mouseY >= getY() && mouseY < getY() + getHeight();
        
        // Choose colors based on toggle state
        int bgColor = toggled ? toggledBackgroundColor : untoggledBackgroundColor;
        int borderColor = toggled ? toggledBorderColor : untoggledBorderColor;
        
        // Make colors more opaque when hovered
        if (hovered) {
            bgColor = makeMoreOpaque(bgColor);
            borderColor = makeMoreOpaque(borderColor);
        }
        
        // Draw background
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Draw border
        if (borderColor != 0) {
            guiGraphics.hLine(x, x + width - 1, y, borderColor);
            guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
            guiGraphics.vLine(x, y, y + height - 1, borderColor);
            guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        }
        
        // Draw the toggle indicator
        int indicatorSize = height - 6;
        int indicatorY = y + 3;
        int indicatorX;
        
        if (toggled) {
            // Right side for on
            indicatorX = x + width - indicatorSize - 3;
        } else {
            // Left side for off
            indicatorX = x + 3;
        }
        
        // Draw indicator background (lighter than button background)
        int indicatorColor = toggled ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.fill(indicatorX, indicatorY, indicatorX + indicatorSize, indicatorY + indicatorSize, indicatorColor);
        
        // Draw text label if provided
        if (getText() != null && !getText().getString().isEmpty()) {
            int textColor = toggled ? 0xFFFFFFFF : 0xFFAAAAAA;
            int textX = x + width / 2 - Minecraft.getInstance().font.width(getText()) / 2;
            int textY = y + (height - 8) / 2;
            guiGraphics.drawString(Minecraft.getInstance().font, getText(), textX, textY, textColor);
        }
    }
    
    /**
     * Make a color more opaque for hover state
     */
    private int makeMoreOpaque(int color) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        // Increase alpha by 25% of remaining transparency
        a = Math.min(255, a + ((255 - a) / 4));
        
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
} 