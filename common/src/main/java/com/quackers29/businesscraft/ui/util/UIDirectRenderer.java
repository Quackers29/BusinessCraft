package com.quackers29.businesscraft.ui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

/**
 * Utility class for direct rendering of UI elements.
 * This approach allows for more control over layering and visibility compared to component-based rendering.
 */
public class UIDirectRenderer {
    
    /**
     * Directly render a button with the BusinessCraft styling
     * 
     * @param guiGraphics The graphics context
     * @param text The button text
     * @param x The x position
     * @param y The y position
     * @param width The width
     * @param height The height
     * @param isPrimary Whether to use primary (true) or secondary (false) styling
     * @param mouseX The mouse X coordinate
     * @param mouseY The mouse Y coordinate
     * @return true if the button is being hovered over
     */
    public static boolean renderButton(GuiGraphics guiGraphics, String text, int x, int y, int width, int height,
                                     boolean isPrimary, int mouseX, int mouseY) {
        // Check if mouse is hovering over the button
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        
        // Get theme colors from the original theme
        int baseColor = isPrimary ? 0xA0335599 : 0xA0884466; // Primary or Secondary
        int hoverColor = isPrimary ? 0xA0446699 : 0xA0995577; // Lighter versions
        
        // Draw button background - brighter if hovered
        int bgColor = hovered ? hoverColor : baseColor;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Draw button border
        int borderColor = 0xA0AAAAAA;
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Draw button text
        int textWidth = Minecraft.getInstance().font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        guiGraphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFFFF);
        
        return hovered;
    }
    
    /**
     * Directly render an input field
     * 
     * @param guiGraphics The graphics context
     * @param value The current text value
     * @param x The x position
     * @param y The y position
     * @param width The width
     * @param height The height
     * @param focused Whether the field is focused
     */
    public static void renderInputField(GuiGraphics guiGraphics, String value, int x, int y, int width, int height, boolean focused) {
        // Input field background
        guiGraphics.fill(x, y, x + width, y + height, 0xFF555555);
        
        // Border - brighter if focused
        int borderColor = focused ? 0xFFBBBBBB : 0xA0AAAAAA;
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Text
        guiGraphics.drawString(Minecraft.getInstance().font, value, x + 5, y + 6, 0xFFFFFFFF);
        
        // Cursor
        if (focused && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cursorX = x + 5 + Minecraft.getInstance().font.width(value);
            guiGraphics.fill(cursorX, y + 5, cursorX + 1, y + height - 5, 0xFFFFFFFF);
        }
    }
    
    /**
     * Render a container/panel background with the BusinessCraft styling
     */
    public static void renderPanelBackground(GuiGraphics guiGraphics, int x, int y, int width, int height,
                                           boolean withHeader, boolean isDialog) {
        // Main background - more opaque if it's a dialog
        int bgAlpha = isDialog ? 0xD0 : 0x80;
        int bgColor = bgAlpha << 24 | 0x222222;
        guiGraphics.fill(x, y, x + width, y + height, bgColor);
        
        // Border
        int borderColor = 0xA0AAAAAA;
        guiGraphics.hLine(x, x + width - 1, y, borderColor);
        guiGraphics.hLine(x, x + width - 1, y + height - 1, borderColor);
        guiGraphics.vLine(x, y, y + height - 1, borderColor);
        guiGraphics.vLine(x + width - 1, y, y + height - 1, borderColor);
        
        // Optional header gradient
        if (withHeader) {
            int headerHeight = 30;
            guiGraphics.fillGradient(x + 1, y + 1, x + width - 1, y + headerHeight, 0xC0335599, 0xC0223366);
        }
    }
    
    /**
     * Handle a mouse click on a directly rendered button
     * 
     * @param mouseX The mouse X coordinate
     * @param mouseY The mouse Y coordinate
     * @param x The button X position
     * @param y The button Y position
     * @param width The button width
     * @param height The button height
     * @param handler The handler to call if the button is clicked
     * @return true if the button was clicked
     */
    public static boolean handleButtonClick(double mouseX, double mouseY, int x, int y, int width, int height, 
                                          Consumer<Button> handler) {
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            if (handler != null) {
                handler.accept(null);
            }
            return true;
        }
        return false;
    }
}