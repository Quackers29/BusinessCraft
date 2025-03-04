package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.yourdomain.businesscraft.BusinessCraft;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Standard button component for BusinessCraft UI system.
 * Provides a consistent look and feel for all buttons in the mod.
 */
public class BCButton extends BCComponent {
    private static final ResourceLocation BUTTON_TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/bc_button.png");
    
    private static final int BUTTON_TEXTURE_WIDTH = 200;
    private static final int BUTTON_TEXTURE_HEIGHT = 20;
    
    // Button states
    private static final int NORMAL = 0;
    private static final int HOVERED = 1;
    private static final int DISABLED = 2;
    
    private final Supplier<Component> textSupplier;
    private final Consumer<Button> onPress;
    private Button vanillaButton;
    private boolean enabled = true;
    
    // Button type/style
    private ButtonType buttonType = ButtonType.PRIMARY;
    
    /**
     * Create a new button with the specified text, action, and dimensions
     */
    public BCButton(Supplier<Component> textSupplier, Consumer<Button> onPress, int width, int height) {
        super(width, height);
        this.textSupplier = textSupplier;
        this.onPress = onPress;
        createButton();
    }
    
    /**
     * Create a new button with fixed text
     */
    public BCButton(Component text, Consumer<Button> onPress, int width, int height) {
        this(() -> text, onPress, width, height);
    }
    
    /**
     * Create a new button with text from a translation key
     */
    public BCButton(String translationKey, Consumer<Button> onPress, int width, int height) {
        this(Component.translatable(translationKey), onPress, width, height);
    }
    
    /**
     * Create the underlying vanilla button
     */
    private void createButton() {
        this.vanillaButton = Button.builder(textSupplier.get(), b -> {
            if (onPress != null) {
                onPress.accept(b);
            }
        })
        .pos(0, 0)
        .size(width, height)
        .build();
    }
    
    /**
     * Set the button type/style
     */
    public BCButton withType(ButtonType type) {
        this.buttonType = type;
        return this;
    }
    
    /**
     * Set whether the button is enabled
     */
    public BCButton setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (vanillaButton != null) {
            vanillaButton.active = enabled;
        }
        return this;
    }
    
    @Override
    public void init(Consumer<Button> register) {
        vanillaButton.setX(x);
        vanillaButton.setY(y);
        vanillaButton.setWidth(width);
        vanillaButton.setHeight(height);
        vanillaButton.active = enabled;
        register.accept(vanillaButton);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Update message in case the supplier has changed
        vanillaButton.setMessage(textSupplier.get());
        
        // Instead of letting vanilla button render itself, we'll draw our custom style
        // but still use the vanilla button for input handling
        
        // Determine the state
        int state = NORMAL;
        if (!enabled) {
            state = DISABLED;
        } else if (isMouseOver(mouseX, mouseY)) {
            state = HOVERED;
        }
        
        // Draw the button texture
        renderButtonTexture(guiGraphics, state);
        
        // Draw the text
        Component message = textSupplier.get();
        int textColor = getTextColor(state);
        guiGraphics.drawCenteredString(
            Minecraft.getInstance().font,
            message,
            x + width / 2,
            y + (height - 8) / 2,
            textColor
        );
    }
    
    /**
     * Draw the button texture based on the button state
     */
    private void renderButtonTexture(GuiGraphics guiGraphics, int state) {
        int textureY = state * BUTTON_TEXTURE_HEIGHT;
        int textureX = buttonType.ordinal() * BUTTON_TEXTURE_WIDTH;
        
        // Draw button using 9-slice rendering for flexible sizing
        // Top left
        guiGraphics.blit(BUTTON_TEXTURE, x, y, textureX, textureY, 3, 3);
        // Top right
        guiGraphics.blit(BUTTON_TEXTURE, x + width - 3, y, textureX + BUTTON_TEXTURE_WIDTH - 3, textureY, 3, 3);
        // Bottom left
        guiGraphics.blit(BUTTON_TEXTURE, x, y + height - 3, textureX, textureY + BUTTON_TEXTURE_HEIGHT - 3, 3, 3);
        // Bottom right
        guiGraphics.blit(BUTTON_TEXTURE, x + width - 3, y + height - 3, 
                textureX + BUTTON_TEXTURE_WIDTH - 3, textureY + BUTTON_TEXTURE_HEIGHT - 3, 3, 3);
        
        // Top edge
        guiGraphics.blit(BUTTON_TEXTURE, x + 3, y, textureX + 3, textureY, width - 6, 3);
        // Bottom edge
        guiGraphics.blit(BUTTON_TEXTURE, x + 3, y + height - 3, textureX + 3, textureY + BUTTON_TEXTURE_HEIGHT - 3, width - 6, 3);
        // Left edge
        guiGraphics.blit(BUTTON_TEXTURE, x, y + 3, textureX, textureY + 3, 3, height - 6);
        // Right edge
        guiGraphics.blit(BUTTON_TEXTURE, x + width - 3, y + 3, textureX + BUTTON_TEXTURE_WIDTH - 3, textureY + 3, 3, height - 6);
        
        // Center
        guiGraphics.blit(BUTTON_TEXTURE, x + 3, y + 3, textureX + 3, textureY + 3, width - 6, height - 6);
    }
    
    /**
     * Get the text color based on button state
     */
    private int getTextColor(int state) {
        switch (state) {
            case NORMAL:
                return buttonType.getTextColor();
            case HOVERED:
                return buttonType.getHoverTextColor();
            case DISABLED:
                return buttonType.getDisabledTextColor();
            default:
                return 0xFFFFFF;
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (vanillaButton != null) {
            vanillaButton.visible = visible;
        }
    }
    
    /**
     * Predefined button types/styles
     */
    public enum ButtonType {
        PRIMARY(0xFFFFFF, 0xFFFFBF, 0x707070),    // Standard button
        SECONDARY(0xCCCCCC, 0xFFFFFF, 0x707070),  // Less prominent button
        DANGER(0xFFFFFF, 0xFFDDDD, 0x707070),     // Delete/warning actions
        SUCCESS(0xFFFFFF, 0xDDFFDD, 0x707070);    // Confirm/success actions
        
        private final int textColor;
        private final int hoverTextColor;
        private final int disabledTextColor;
        
        ButtonType(int textColor, int hoverTextColor, int disabledTextColor) {
            this.textColor = textColor;
            this.hoverTextColor = hoverTextColor;
            this.disabledTextColor = disabledTextColor;
        }
        
        public int getTextColor() {
            return textColor;
        }
        
        public int getHoverTextColor() {
            return hoverTextColor;
        }
        
        public int getDisabledTextColor() {
            return disabledTextColor;
        }
    }
} 