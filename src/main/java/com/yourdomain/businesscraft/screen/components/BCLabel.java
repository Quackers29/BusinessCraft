package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Standard label component for BusinessCraft UI system.
 * Provides flexible text display with various alignment options.
 */
public class BCLabel extends BCComponent {
    private final Supplier<Component> textSupplier;
    private TextAlignment alignment = TextAlignment.LEFT;
    private int textColor = 0xFFFFFF; // Default white
    private int shadowColor = 0; // Default black/no shadow
    private boolean drawShadow = false;
    
    /**
     * Create a new label with the specified text and dimensions
     */
    public BCLabel(Supplier<Component> textSupplier, int width, int height) {
        super(width, height);
        this.textSupplier = textSupplier;
        // Make labels transparent by default
        this.backgroundColor = -1;
    }
    
    /**
     * Create a new label with fixed text
     */
    public BCLabel(Component text, int width, int height) {
        this(() -> text, width, height);
    }
    
    /**
     * Create a new label with text from a translation key
     */
    public BCLabel(String translationKey, int width, int height) {
        this(Component.translatable(translationKey), width, height);
    }
    
    /**
     * Set the text alignment
     */
    public BCLabel withAlignment(TextAlignment alignment) {
        this.alignment = alignment;
        return this;
    }
    
    /**
     * Set the text color
     */
    public BCLabel withTextColor(int color) {
        this.textColor = color;
        return this;
    }
    
    /**
     * Set whether to draw text shadow
     */
    public BCLabel withShadow(boolean drawShadow) {
        this.drawShadow = drawShadow;
        return this;
    }
    
    /**
     * Set the shadow color (only used if drawShadow is true)
     */
    public BCLabel withShadowColor(int shadowColor) {
        this.shadowColor = shadowColor;
        return this;
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component text = textSupplier.get();
        
        // Use the supplied style if available
        if (style != null) {
            text = text.copy().withStyle(style);
        }
        
        // Calculate text position based on alignment
        int textX = x;
        switch (alignment) {
            case CENTER:
                textX = x + width / 2 - Minecraft.getInstance().font.width(text) / 2;
                break;
            case RIGHT:
                textX = x + width - Minecraft.getInstance().font.width(text);
                break;
            case LEFT:
            default:
                // Already set to x
                break;
        }
        
        // Center vertically
        int textY = y + (height - 8) / 2;
        
        // Draw text with or without shadow
        if (drawShadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, text, textX, textY, textColor, true);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, text, textX, textY, textColor, false);
        }
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // Labels don't have buttons to register
    }
    
    /**
     * Text alignment options
     */
    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
} 