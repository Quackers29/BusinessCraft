package com.yourdomain.businesscraft.ui.modal.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles rendering operations for modal inventory screens.
 * Extracted from BCModalInventoryScreen to follow single responsibility principle.
 * Manages backgrounds, titles, separators, and overlays.
 */
public class ModalRenderingEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModalRenderingEngine.class);
    
    // Default colors
    private int backgroundColor = 0xD0000000;    // Darker semi-transparent black
    private int borderColor = 0xFFDDDDDD;        // Light gray
    private int titleColor = 0xFFFFFFFF;         // White
    private int overlayColor = 0xB0000000;       // Dark overlay
    
    /**
     * Sets the rendering colors.
     */
    public void setColors(int backgroundColor, int borderColor, int titleColor, int overlayColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        this.titleColor = titleColor;
        this.overlayColor = overlayColor;
    }
    
    /**
     * Renders the modal background with dark overlay.
     */
    public void renderModalBackground(GuiGraphics guiGraphics, int screenWidth, int screenHeight, boolean hasParent) {
        if (hasParent) {
            // Additional dark overlay for better contrast
            guiGraphics.fill(0, 0, screenWidth, screenHeight, overlayColor);
        }
    }
    
    /**
     * Renders the main panel background.
     */
    public void renderPanelBackground(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight) {
        // Draw main panel background
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, backgroundColor);
        
        // Draw border
        renderPanelBorder(guiGraphics, leftPos, topPos, imageWidth, imageHeight);
    }
    
    /**
     * Renders the panel border.
     */
    public void renderPanelBorder(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int imageHeight) {
        // Top border
        guiGraphics.hLine(leftPos, leftPos + imageWidth, topPos, borderColor);
        // Bottom border  
        guiGraphics.hLine(leftPos, leftPos + imageWidth, topPos + imageHeight, borderColor);
        // Left border
        guiGraphics.vLine(leftPos, topPos, topPos + imageHeight, borderColor);
        // Right border
        guiGraphics.vLine(leftPos + imageWidth, topPos, topPos + imageHeight, borderColor);
    }
    
    /**
     * Renders the modal title.
     */
    public void renderTitle(GuiGraphics guiGraphics, Font font, Component title, int leftPos, int topPos, int imageWidth) {
        if (title != null) {
            String titleText = title.getString();
            int titleWidth = font.width(titleText);
            int titleX = leftPos + (imageWidth - titleWidth) / 2;
            int titleY = topPos + 8;
            
            guiGraphics.drawString(font, titleText, titleX, titleY, titleColor);
        }
    }
    
    /**
     * Renders a horizontal separator line.
     */
    public void renderSeparator(GuiGraphics guiGraphics, int leftPos, int topPos, int imageWidth, int yOffset) {
        guiGraphics.hLine(
            leftPos + 10, 
            leftPos + imageWidth - 10, 
            topPos + yOffset,
            borderColor
        );
    }
    
    /**
     * Renders a section background with rounded corners effect.
     */
    public void renderSectionBackground(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + height, color);
        
        // Add subtle border
        guiGraphics.hLine(x, x + width, y, borderColor);
        guiGraphics.hLine(x, x + width, y + height, borderColor);
        guiGraphics.vLine(x, y, y + height, borderColor);
        guiGraphics.vLine(x + width, y, y + height, borderColor);
    }
    
    /**
     * Renders a label with consistent styling.
     */
    public void renderLabel(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color) {
        guiGraphics.drawString(font, text, x, y, color);
    }
    
    /**
     * Renders a centered label within a given width.
     */
    public void renderCenteredLabel(GuiGraphics guiGraphics, Font font, String text, int x, int y, int width, int color) {
        int textWidth = font.width(text);
        int centeredX = x + (width - textWidth) / 2;
        guiGraphics.drawString(font, text, centeredX, y, color);
    }
    
    /**
     * Renders a progress bar.
     */
    public void renderProgressBar(GuiGraphics guiGraphics, int x, int y, int width, int height, 
                                 float progress, int backgroundColor, int foregroundColor) {
        // Background
        guiGraphics.fill(x, y, x + width, y + height, backgroundColor);
        
        // Progress fill
        int progressWidth = (int) (width * Math.max(0, Math.min(1, progress)));
        if (progressWidth > 0) {
            guiGraphics.fill(x, y, x + progressWidth, y + height, foregroundColor);
        }
        
        // Border
        guiGraphics.hLine(x, x + width, y, borderColor);
        guiGraphics.hLine(x, x + width, y + height, borderColor);
        guiGraphics.vLine(x, y, y + height, borderColor);
        guiGraphics.vLine(x + width, y, y + height, borderColor);
    }
    
    /**
     * Renders an enhanced button with hover effects.
     */
    public void renderEnhancedButton(GuiGraphics guiGraphics, Font font, int x, int y, int width, int height, 
                                   String text, boolean isHovered, boolean isPressed) {
        
        // Button colors based on state
        int buttonColor = backgroundColor;
        int textColor = titleColor;
        
        if (isPressed) {
            buttonColor = 0xFF555555; // Darker when pressed
        } else if (isHovered) {
            buttonColor = 0xFF888888; // Lighter when hovered
        }
        
        // Draw button background
        guiGraphics.fill(x, y, x + width, y + height, buttonColor);
        
        // Draw button border
        guiGraphics.hLine(x, x + width, y, borderColor);
        guiGraphics.hLine(x, x + width, y + height, borderColor);
        guiGraphics.vLine(x, y, y + height, borderColor);
        guiGraphics.vLine(x + width, y, y + height, borderColor);
        
        // Draw button text (centered)
        renderCenteredLabel(guiGraphics, font, text, x, y + (height - font.lineHeight) / 2, width, textColor);
    }
    
    /**
     * Renders a flow line connecting two points (for trade flow visualization).
     */
    public void renderFlowLine(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY, int color) {
        // Draw the main line
        if (Math.abs(endX - startX) > Math.abs(endY - startY)) {
            // Horizontal line
            guiGraphics.hLine(Math.min(startX, endX), Math.max(startX, endX), startY, color);
        } else {
            // Vertical line
            guiGraphics.vLine(startX, Math.min(startY, endY), Math.max(startY, endY), color);
        }
        
        // Draw arrow at the end
        renderArrow(guiGraphics, endX, endY, startX < endX ? 1 : -1, color);
    }
    
    /**
     * Renders a simple arrow indicator.
     */
    public void renderArrow(GuiGraphics guiGraphics, int x, int y, int direction, int color) {
        int arrowSize = 3;
        
        if (direction > 0) {
            // Right arrow
            guiGraphics.vLine(x, y - arrowSize, y + arrowSize, color);
            guiGraphics.hLine(x - arrowSize, x, y - arrowSize, color);
            guiGraphics.hLine(x - arrowSize, x, y + arrowSize, color);
        } else {
            // Left arrow
            guiGraphics.vLine(x, y - arrowSize, y + arrowSize, color);
            guiGraphics.hLine(x, x + arrowSize, y - arrowSize, color);
            guiGraphics.hLine(x, x + arrowSize, y + arrowSize, color);
        }
    }
    
    /**
     * Renders a tooltip background.
     */
    public void renderTooltipBackground(GuiGraphics guiGraphics, int x, int y, int width, int height) {
        // Tooltip background with slight transparency
        int tooltipBg = 0xE0222222;
        guiGraphics.fill(x - 2, y - 2, x + width + 2, y + height + 2, tooltipBg);
        
        // Tooltip border
        guiGraphics.hLine(x - 2, x + width + 2, y - 2, borderColor);
        guiGraphics.hLine(x - 2, x + width + 2, y + height + 2, borderColor);
        guiGraphics.vLine(x - 2, y - 2, y + height + 2, borderColor);
        guiGraphics.vLine(x + width + 2, y - 2, y + height + 2, borderColor);
    }
    
    // Getters for colors
    public int getBackgroundColor() { return backgroundColor; }
    public int getBorderColor() { return borderColor; }
    public int getTitleColor() { return titleColor; }
    public int getOverlayColor() { return overlayColor; }
}