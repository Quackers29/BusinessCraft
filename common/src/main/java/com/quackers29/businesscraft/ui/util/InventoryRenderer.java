package com.quackers29.businesscraft.ui.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * Utility class for rendering inventory UI components.
 * Provides reusable methods for drawing inventory slots, borders, and other common UI elements.
 */
public class InventoryRenderer {
    // Common UI color constants
    public static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    public static final int SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    public static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    public static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    public static final int BUTTON_COLOR = 0xA0446688;        // Button blue
    public static final int BUTTON_HOVER_COLOR = 0xA066AADD;  // Button hover blue
    public static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    public static final int TEXT_SHADOW_COLOR = 0xFF000000;   // Black text shadow
    public static final int LABEL_BG_COLOR = 0x80000000;      // Semi-transparent black for text background
    public static final int SLOT_BORDER_COLOR = 0xFF555555;   // Darker gray for slot borders
    public static final int INVENTORY_BORDER_COLOR = 0xFFBBBBBB; // Lighter gray for inventory border
    public static final int SLOT_BG_COLOR = 0x50000000;       // Slot background color
    
    // Common UI element sizes
    public static final int SLOT_SIZE = 18;    // Standard Minecraft slot size
    public static final int BUTTON_HEIGHT = 20; // Standard button height
    
    /**
     * Draws a standard inventory interface with proper borders and slot backgrounds.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param x The x position of the inventory
     * @param y The y position of the inventory
     * @param invWidth The total width of the inventory (in slots)
     * @param invHeight The height of the main inventory (in slots)
     * @param hotbarHeight The height of the hotbar (usually 1)
     * @param hotbarSpacing The spacing between the main inventory and hotbar
     */
    public static void drawInventoryWithHotbar(GuiGraphics guiGraphics, int x, int y, 
            int invWidth, int invHeight, int hotbarHeight, int hotbarSpacing) {
        
        int mainInvY = y;
        int hotbarY = mainInvY + (invHeight * SLOT_SIZE) + hotbarSpacing;
        
        // Draw main inventory background
        guiGraphics.fill(x - 2, mainInvY - 2, 
                x + (invWidth * SLOT_SIZE) + 2, mainInvY + (invHeight * SLOT_SIZE) + 2, 
                0x70000000);
        
        // Draw main inventory border
        drawBorder(guiGraphics, x - 2, mainInvY - 2, 
                invWidth * SLOT_SIZE + 4, invHeight * SLOT_SIZE + 4, 
                INVENTORY_BORDER_COLOR, 2);
        
        // Draw hotbar background
        guiGraphics.fill(x - 2, hotbarY - 2, 
                x + (invWidth * SLOT_SIZE) + 2, hotbarY + (hotbarHeight * SLOT_SIZE) + 2, 
                0x70000000);
        
        // Draw hotbar border
        drawBorder(guiGraphics, x - 2, hotbarY - 2, 
                invWidth * SLOT_SIZE + 4, hotbarHeight * SLOT_SIZE + 4, 
                INVENTORY_BORDER_COLOR, 2);
        
        // Draw slot grid for main inventory
        drawSlotGrid(guiGraphics, x, mainInvY, invWidth, invHeight);
        
        // Draw slot grid for hotbar
        drawSlotGrid(guiGraphics, x, hotbarY, invWidth, hotbarHeight);
    }
    
    /**
     * Draws a grid of inventory slots with borders and backgrounds.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param startX The starting X position
     * @param startY The starting Y position
     * @param columns Number of columns in the grid
     * @param rows Number of rows in the grid
     */
    public static void drawSlotGrid(GuiGraphics guiGraphics, int startX, int startY, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                
                // Draw slot background
                guiGraphics.fill(x, y, x + 16, y + 16, SLOT_BG_COLOR);
                
                // Draw slot border
                drawSlotBorder(guiGraphics, x - 1, y - 1, 18, 18, SLOT_BORDER_COLOR);
            }
        }
    }
    
    /**
     * Draws a single inventory slot with border.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param x The x position
     * @param y The y position
     * @param color The slot background color
     * @param borderColor The border color
     */
    public static void drawSlot(GuiGraphics guiGraphics, int x, int y, int color, int borderColor) {
        // Draw slot background
        guiGraphics.fill(x, y, x + 16, y + 16, color);
        
        // Draw slot border
        drawSlotBorder(guiGraphics, x - 1, y - 1, 18, 18, borderColor);
    }
    
    /**
     * Draws a border with the specified thickness.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param x The x position
     * @param y The y position
     * @param width The width of the bordered area
     * @param height The height of the bordered area
     * @param color The border color
     * @param thickness The border thickness in pixels
     */
    public static void drawBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color, int thickness) {
        // Top border
        guiGraphics.fill(x, y, x + width, y + thickness, color);
        // Left border
        guiGraphics.fill(x, y, x + thickness, y + height, color);
        // Bottom border
        guiGraphics.fill(x, y + height - thickness, x + width, y + height, color);
        // Right border
        guiGraphics.fill(x + width - thickness, y, x + width, y + height, color);
    }
    
    /**
     * Draws a standard 1px border.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param x The x position
     * @param y The y position
     * @param width The width of the bordered area
     * @param height The height of the bordered area
     * @param color The border color
     */
    public static void drawSlotBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        drawBorder(guiGraphics, x, y, width, height, color, 1);
    }
    
    /**
     * Draws a button with proper styling and hover effects.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param x The x position
     * @param y The y position
     * @param width The button width
     * @param height The button height
     * @param text The button text
     * @param font The font to use
     * @param isHovered Whether the button is being hovered over
     */
    public static void drawButton(GuiGraphics guiGraphics, int x, int y, int width, int height, 
            String text, Font font, boolean isHovered) {
        
        int buttonColor = isHovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        
        // Draw button background
        guiGraphics.fill(x, y, x + width, y + height, buttonColor);
        
        // Draw button border
        drawBorder(guiGraphics, x, y, width, height, TEXT_COLOR, 1);
        
        // Draw centered text
        drawCenteredString(guiGraphics, font, text, x + width / 2, y + (height - 8) / 2, TEXT_COLOR, true);
    }
    
    /**
     * Draws a button with proper styling, hover effects, and custom color.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param x The x position
     * @param y The y position
     * @param width The button width
     * @param height The button height
     * @param text The button text
     * @param font The font to use
     * @param isHovered Whether the button is being hovered over
     * @param customColor Custom button color to use instead of default
     */
    public static void drawButton(GuiGraphics guiGraphics, int x, int y, int width, int height, 
            String text, Font font, boolean isHovered, int customColor) {
        
        int buttonColor = isHovered ? adjustBrightness(customColor, 1.3f) : customColor;
        
        // Draw button background
        guiGraphics.fill(x, y, x + width, y + height, buttonColor);
        
        // Draw button border
        drawBorder(guiGraphics, x, y, width, height, TEXT_COLOR, 1);
        
        // Draw centered text
        drawCenteredString(guiGraphics, font, text, x + width / 2, y + (height - 8) / 2, TEXT_COLOR, true);
    }
    
    /**
     * Adjust the brightness of a color for hover effects
     */
    private static int adjustBrightness(int color, float factor) {
        int alpha = (color >> 24) & 0xFF;
        int red = (int) Math.min(255, ((color >> 16) & 0xFF) * factor);
        int green = (int) Math.min(255, ((color >> 8) & 0xFF) * factor);
        int blue = (int) Math.min(255, (color & 0xFF) * factor);
        
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    /**
     * Draws a text label with background for better visibility.
     * 
     * @param guiGraphics The GuiGraphics instance to draw with
     * @param font The font to use
     * @param text The text to display
     * @param x The x position
     * @param y The y position
     */
    public static void drawLabel(GuiGraphics guiGraphics, Font font, String text, int x, int y) {
        // Draw background
        int textWidth = font.width(text);
        guiGraphics.fill(x - 2, y - 2, x + textWidth + 2, y + 10, LABEL_BG_COLOR);
        
        // Draw text with shadow
        guiGraphics.drawString(font, text, x, y, TEXT_COLOR, true);
    }
    
    /**
     * Draws text centered at the specified position.
     *
     * @param guiGraphics The GuiGraphics instance
     * @param font The font to use for rendering
     * @param text The text to render
     * @param x The x position (will be centered)
     * @param y The y position
     * @param color The text color
     * @param withShadow Whether to render with a shadow
     */
    public static void drawCenteredString(GuiGraphics guiGraphics, Font font, String text, int x, int y, int color, boolean withShadow) {
        int textX = x - font.width(text) / 2;
        if (withShadow) {
            guiGraphics.drawString(font, text, textX, y, color, true);
        } else {
            guiGraphics.drawString(font, text, textX, y, color);
        }
    }
    
    /**
     * Draws an arrow pointing from start to end coordinates.
     *
     * @param guiGraphics The GuiGraphics instance
     * @param startX The starting x position
     * @param startY The starting y position
     * @param endX The ending x position
     * @param endY The ending y position
     * @param color The arrow color
     */
    public static void drawArrow(GuiGraphics guiGraphics, int startX, int startY, int endX, int endY, int color) {
        // Draw the arrow line
        guiGraphics.fill(startX, startY - 1, endX, startY + 1, color);
        
        // Draw the arrow head
        for (int i = 0; i < 4; i++) {
            guiGraphics.fill(endX - i, startY - i - 1, endX - i + 1, startY + i + 2, color);
        }
    }
    
    /**
     * Utility method to check if the mouse is over a UI element like a button
     * 
     * @param mouseX The mouse X position
     * @param mouseY The mouse Y position
     * @param screenX The screen X position (top left of screen)
     * @param screenY The screen Y position (top left of screen)
     * @param elementX The element X position (relative to screen)
     * @param elementY The element Y position (relative to screen)
     * @param elementWidth The element width
     * @param elementHeight The element height
     * @return true if the mouse is over the element, false otherwise
     */
    public static boolean isMouseOverElement(int mouseX, int mouseY, int screenX, int screenY,
                                           int elementX, int elementY, int elementWidth, int elementHeight) {
        return mouseX >= screenX + elementX && mouseX < screenX + elementX + elementWidth &&
               mouseY >= screenY + elementY && mouseY < screenY + elementY + elementHeight;
    }
    
    /**
     * Utility method to check if the mouse is over a UI element in a centered screen
     * 
     * @param mouseX The mouse X position
     * @param mouseY The mouse Y position
     * @param screenWidth The total screen width
     * @param screenHeight The total screen height
     * @param elementX The element X position (relative to the centered screen)
     * @param elementY The element Y position (relative to the centered screen)
     * @param elementWidth The element width
     * @param elementHeight The element height
     * @return true if the mouse is over the element, false otherwise
     */
    public static boolean isMouseOverElementCentered(int mouseX, int mouseY, int screenWidth, int screenHeight,
                                                    int elementX, int elementY, int elementWidth, int elementHeight) {
        int x = (screenWidth - elementWidth) / 2;
        int y = (screenHeight - elementHeight) / 2;
        
        return isMouseOverElement(mouseX, mouseY, x, y, elementX, elementY, elementWidth, elementHeight);
    }
} 
