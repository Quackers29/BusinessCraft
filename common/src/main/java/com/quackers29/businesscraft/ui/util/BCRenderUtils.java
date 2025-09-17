package com.quackers29.businesscraft.ui.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Utility class for rendering common UI elements
 * Provides simplified methods for drawing rectangles, boxes, borders and other shapes
 * with consistent styling and less boilerplate code.
 */
public class BCRenderUtils {

    /**
     * Draw a box with borders between two points
     * 
     * @param graphics The GuiGraphics context
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param x2 Ending X coordinate
     * @param y2 Ending Y coordinate
     * @param backgroundColor Fill color for the box
     * @param borderColor Border color for the box
     */
    public static void drawBox(GuiGraphics graphics, int x1, int y1, int x2, int y2, int backgroundColor, int borderColor) {
        // Ensure correct coordinate order (x1,y1 is top-left and x2,y2 is bottom-right)
        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int right = Math.max(x1, x2);
        int bottom = Math.max(y1, y2);
        
        // Fill the box
        graphics.fill(left, top, right, bottom, backgroundColor);
        
        // Draw the border if borderColor is not -1 (which means no border)
        if (borderColor != -1) {
            // Top border
            graphics.hLine(left, right - 1, top, borderColor);
            // Bottom border
            graphics.hLine(left, right - 1, bottom - 1, borderColor);
            // Left border
            graphics.vLine(left, top, bottom - 1, borderColor);
            // Right border
            graphics.vLine(right - 1, top, bottom - 1, borderColor);
        }
    }
    
    /**
     * Draw a rounded box with a fill color and optional border
     * 
     * @param graphics The GuiGraphics context
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param x2 Ending X coordinate
     * @param y2 Ending Y coordinate
     * @param cornerRadius Radius for the rounded corners
     * @param backgroundColor Fill color for the box
     * @param borderColor Border color for the box, or -1 for no border
     */
    public static void drawRoundedBox(GuiGraphics graphics, int x1, int y1, int x2, int y2, 
                                     int cornerRadius, int backgroundColor, int borderColor) {
        // Ensure correct coordinate order
        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int right = Math.max(x1, x2);
        int bottom = Math.max(y1, y2);
        int width = right - left;
        int height = bottom - top;
        
        // Draw fill
        drawRoundedRect(graphics, left, top, width, height, cornerRadius, backgroundColor);
        
        // Draw border if specified
        if (borderColor != -1) {
            drawRoundedRectOutline(graphics, left, top, width, height, cornerRadius, borderColor);
        }
    }
    
    /**
     * Draw a rectangle with a gradient fill from top to bottom
     * 
     * @param graphics The GuiGraphics context
     * @param x1 Starting X coordinate
     * @param y1 Starting Y coordinate
     * @param x2 Ending X coordinate
     * @param y2 Ending Y coordinate
     * @param colorTop Top gradient color
     * @param colorBottom Bottom gradient color
     * @param borderColor Border color, or -1 for no border
     */
    public static void drawGradientBox(GuiGraphics graphics, int x1, int y1, int x2, int y2, 
                                      int colorTop, int colorBottom, int borderColor) {
        // Ensure correct coordinate order
        int left = Math.min(x1, x2);
        int top = Math.min(y1, y2);
        int right = Math.max(x1, x2);
        int bottom = Math.max(y1, y2);
        
        // Fill with gradient
        graphics.fillGradient(left, top, right, bottom, colorTop, colorBottom);
        
        // Draw border if needed
        if (borderColor != -1) {
            // Top border
            graphics.hLine(left, right - 1, top, borderColor);
            // Bottom border
            graphics.hLine(left, right - 1, bottom - 1, borderColor);
            // Left border
            graphics.vLine(left, top, bottom - 1, borderColor);
            // Right border
            graphics.vLine(right - 1, top, bottom - 1, borderColor);
        }
    }
    
    /**
     * Draw a filled rounded rectangle
     */
    private static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, 
                                       int cornerRadius, int color) {
        // Clamp corner radius to reasonable values
        cornerRadius = Math.min(cornerRadius, Math.min(width / 2, height / 2));
        
        // Draw center
        graphics.fill(x + cornerRadius, y, x + width - cornerRadius, y + height, color);
        
        // Draw left and right edges
        graphics.fill(x, y + cornerRadius, x + cornerRadius, y + height - cornerRadius, color);
        graphics.fill(x + width - cornerRadius, y + cornerRadius, x + width, y + height - cornerRadius, color);
        
        // Draw corners
        drawCircleQuadrant(graphics, x + cornerRadius, y + cornerRadius, cornerRadius, color, 0);
        drawCircleQuadrant(graphics, x + width - cornerRadius, y + cornerRadius, cornerRadius, color, 1);
        drawCircleQuadrant(graphics, x + width - cornerRadius, y + height - cornerRadius, cornerRadius, color, 2);
        drawCircleQuadrant(graphics, x + cornerRadius, y + height - cornerRadius, cornerRadius, color, 3);
    }
    
    /**
     * Draw a rounded rectangle outline
     */
    private static void drawRoundedRectOutline(GuiGraphics graphics, int x, int y, int width, int height, 
                                              int cornerRadius, int color) {
        // Clamp corner radius to reasonable values
        cornerRadius = Math.min(cornerRadius, Math.min(width / 2, height / 2));
        
        // Draw horizontal lines
        graphics.hLine(x + cornerRadius, x + width - cornerRadius - 1, y, color);
        graphics.hLine(x + cornerRadius, x + width - cornerRadius - 1, y + height - 1, color);
        
        // Draw vertical lines
        graphics.vLine(x, y + cornerRadius, y + height - cornerRadius - 1, color);
        graphics.vLine(x + width - 1, y + cornerRadius, y + height - cornerRadius - 1, color);
        
        // Draw corner outlines
        drawCircleQuadrantOutline(graphics, x + cornerRadius, y + cornerRadius, cornerRadius, color, 0);
        drawCircleQuadrantOutline(graphics, x + width - cornerRadius, y + cornerRadius, cornerRadius, color, 1);
        drawCircleQuadrantOutline(graphics, x + width - cornerRadius, y + height - cornerRadius, cornerRadius, color, 2);
        drawCircleQuadrantOutline(graphics, x + cornerRadius, y + height - cornerRadius, cornerRadius, color, 3);
    }
    
    /**
     * Draw a quadrant of a circle
     * Quadrant: 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left
     */
    private static void drawCircleQuadrant(GuiGraphics graphics, int centerX, int centerY, int radius, int color, int quadrant) {
        for (int dx = 0; dx <= radius; dx++) {
            for (int dy = 0; dy <= radius; dy++) {
                if (dx * dx + dy * dy <= radius * radius) {
                    int drawX = centerX;
                    int drawY = centerY;
                    
                    switch (quadrant) {
                        case 0: // top-left
                            drawX -= dx;
                            drawY -= dy;
                            break;
                        case 1: // top-right
                            drawX += dx - 1;
                            drawY -= dy;
                            break;
                        case 2: // bottom-right
                            drawX += dx - 1;
                            drawY += dy - 1;
                            break;
                        case 3: // bottom-left
                            drawX -= dx;
                            drawY += dy - 1;
                            break;
                    }
                    
                    graphics.fill(drawX, drawY, drawX + 1, drawY + 1, color);
                }
            }
        }
    }
    
    /**
     * Draw a quadrant outline of a circle
     * Quadrant: 0 = top-left, 1 = top-right, 2 = bottom-right, 3 = bottom-left
     */
    private static void drawCircleQuadrantOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color, int quadrant) {
        for (int i = 0; i <= 90; i++) {
            double angle = Math.toRadians(i + (quadrant * 90));
            int dx = (int) (Math.cos(angle) * radius);
            int dy = (int) (Math.sin(angle) * radius);
            
            int drawX = centerX;
            int drawY = centerY;
            
            switch (quadrant) {
                case 0: // top-left
                    drawX -= dx;
                    drawY -= dy;
                    break;
                case 1: // top-right
                    drawX += dx - 1;
                    drawY -= dy;
                    break;
                case 2: // bottom-right
                    drawX += dx - 1;
                    drawY += dy - 1;
                    break;
                case 3: // bottom-left
                    drawX -= dx;
                    drawY += dy - 1;
                    break;
            }
            
            graphics.fill(drawX, drawY, drawX + 1, drawY + 1, color);
        }
    }
    
    /**
     * Draw a complete circle
     * 
     * @param graphics The GuiGraphics context
     * @param centerX X coordinate of circle center
     * @param centerY Y coordinate of circle center
     * @param radius Circle radius
     * @param color Circle color
     */
    public static void drawCircle(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                if (x * x + y * y <= radius * radius) {
                    graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }
    
    /**
     * Draw a circle outline
     * 
     * @param graphics The GuiGraphics context
     * @param centerX X coordinate of circle center
     * @param centerY Y coordinate of circle center
     * @param radius Circle radius
     * @param color Circle color
     */
    public static void drawCircleOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color) {
        for (int i = 0; i < 360; i++) {
            double angle = Math.toRadians(i);
            int x = (int) (Math.cos(angle) * radius);
            int y = (int) (Math.sin(angle) * radius);
            graphics.fill(centerX + x, centerY + y, centerX + x + 1, centerY + y + 1, color);
        }
    }
    
    /**
     * Draw a texture with tiled scaling
     * 
     * @param graphics The GuiGraphics context
     * @param texture The texture resource location
     * @param x Left position
     * @param y Top position
     * @param width Width to draw
     * @param height Height to draw
     * @param u Texture U coordinate
     * @param v Texture V coordinate
     * @param regionWidth Width of the texture region
     * @param regionHeight Height of the texture region
     */
    public static void drawTiledTexture(GuiGraphics graphics, ResourceLocation texture, 
                                       int x, int y, int width, int height,
                                       int u, int v, int regionWidth, int regionHeight) {
        // Calculate number of tiles needed
        int tilesX = (int) Math.ceil((double) width / regionWidth);
        int tilesY = (int) Math.ceil((double) height / regionHeight);
        
        for (int tileY = 0; tileY < tilesY; tileY++) {
            for (int tileX = 0; tileX < tilesX; tileX++) {
                int drawX = x + tileX * regionWidth;
                int drawY = y + tileY * regionHeight;
                int drawWidth = Math.min(regionWidth, x + width - drawX);
                int drawHeight = Math.min(regionHeight, y + height - drawY);
                
                graphics.blit(texture, drawX, drawY, u, v, drawWidth, drawHeight);
            }
        }
    }
    
    /**
     * Apply alpha to a color value
     * 
     * @param color The color to modify
     * @param alpha Alpha value between 0.0 and 1.0
     * @return The color with the alpha applied
     */
    public static int applyAlpha(int color, float alpha) {
        if (alpha >= 1.0f) {
            return color;
        }
        
        int a = Math.max(0, Math.min(255, (int) (alpha * 255))) << 24;
        return (color & 0x00FFFFFF) | a;
    }
} 
