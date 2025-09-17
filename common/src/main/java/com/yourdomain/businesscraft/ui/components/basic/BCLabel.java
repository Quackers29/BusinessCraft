package com.yourdomain.businesscraft.ui.components.basic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Enhanced label component for BusinessCraft UI system.
 * Provides text display with styling and alignment options.
 */
public class BCLabel extends BCComponent {
    public enum TextAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
    
    private Component text;
    private Supplier<Component> textSupplier;
    private int textColor = 0xFFFFFF;
    private boolean shadow = true;
    private TextAlignment alignment = TextAlignment.LEFT;
    private int lineHeight = 10;
    
    /**
     * Create a new label with static text
     */
    public BCLabel(String translationKey, int width, int lineHeight) {
        this(Component.translatable(translationKey), width, lineHeight);
    }
    
    /**
     * Create a new label with a component
     */
    public BCLabel(Component text, int width, int lineHeight) {
        super(width, lineHeight);
        this.text = text;
        this.textSupplier = null;
        this.lineHeight = lineHeight;
        this.height = lineHeight;
    }
    
    /**
     * Create a new label with dynamic text from a supplier
     */
    public BCLabel(Supplier<Component> textSupplier, int width, int lineHeight) {
        super(width, lineHeight);
        this.text = null;
        this.textSupplier = textSupplier;
        this.lineHeight = lineHeight;
        this.height = lineHeight;
    }
    
    /**
     * Set the text color
     */
    public BCLabel withTextColor(int color) {
        this.textColor = color;
        return this;
    }
    
    /**
     * Set whether to draw text with a shadow
     */
    public BCLabel withShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }
    
    /**
     * Set the text alignment
     */
    public BCLabel withAlignment(TextAlignment alignment) {
        this.alignment = alignment;
        return this;
    }
    
    /**
     * Set the text
     */
    public void setText(Component text) {
        this.text = text;
        this.textSupplier = null;
    }
    
    /**
     * Set the text supplier
     */
    public void setTextSupplier(Supplier<Component> textSupplier) {
        this.text = null;
        this.textSupplier = textSupplier;
    }
    
    /**
     * Get the current text
     */
    public Component getText() {
        if (textSupplier != null) {
            return textSupplier.get();
        }
        return text;
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component textToRender = getText();
        if (textToRender == null) return;
        
        // Apply alpha to text color
        int finalTextColor = applyAlpha(textColor);
        
        // Calculate text position based on alignment
        int textX = x;
        switch (alignment) {
            case CENTER:
                textX = x + (width / 2) - (Minecraft.getInstance().font.width(textToRender) / 2);
                break;
            case RIGHT:
                textX = x + width - Minecraft.getInstance().font.width(textToRender);
                break;
            case LEFT:
            default:
                // Already set to x
                break;
        }
        
        // Draw text
        if (shadow) {
            guiGraphics.drawString(Minecraft.getInstance().font, textToRender, textX, y, finalTextColor, true);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, textToRender, textX, y, finalTextColor, false);
        }
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // No buttons to register
    }
} 