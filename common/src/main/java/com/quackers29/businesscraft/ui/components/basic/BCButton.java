package com.quackers29.businesscraft.ui.components.basic;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Enhanced button component for BusinessCraft UI system.
 * Supports multiple button styles, states, and custom rendering.
 */
public class BCButton extends BCComponent {
    // Button types
    public enum ButtonType {
        PRIMARY,
        SECONDARY,
        SUCCESS,
        DANGER,
        WARNING,
        INFO,
        OUTLINE,
        TRANSPARENT
    }
    
    // Button state
    private boolean pressed = false;
    
    // Button customization
    private ButtonType type = ButtonType.PRIMARY;
    private int textColor = UiTheme.TEXT;
    private boolean shadow = false;
    protected Consumer<Button> onPress;
    private Component text;
    private ResourceLocation iconTexture = null;
    private int iconU = 0;
    private int iconV = 0;
    private int iconWidth = 0;
    private int iconHeight = 0;
    private int iconPadding = 4;
    private boolean drawBackground = true;
    
    private static final int PRIMARY_COLOR = UiTheme.BUTTON;
    private static final int SECONDARY_COLOR = UiTheme.SECTION;
    private static final int SUCCESS_COLOR = UiTheme.ON_BG;
    private static final int DANGER_COLOR = UiTheme.OFF_BG;
    private static final int WARNING_COLOR = UiTheme.GOLD;
    private static final int INFO_COLOR = UiTheme.AQUA;
    private static final int OUTLINE_COLOR = 0x40FFFFFF;       // Transparent white outline
    private static final int TRANSPARENT_COLOR = 0x00000000;   // Fully transparent
    
    /**
     * Create a new button using a translation key
     */
    public BCButton(String translationKey, Consumer<Button> onPress, int width, int height) {
        this(Component.translatable(translationKey), onPress, width, height);
    }
    
    /**
     * Create a new button with a component
     */
    public BCButton(Component text, Consumer<Button> onPress, int width, int height) {
        super(width, height);
        this.text = text;
        this.onPress = onPress;
    }
    
    /**
     * Create a new button with a component and a click handler that accepts the button
     */
    public BCButton(Component text, Consumer<BCButton> onClick, int width) {
        super(width, 20);
        this.text = text;
        this.onPress = button -> onClick.accept(this);
    }
    
    /**
     * Get the button's background color based on its type and state
     */
    private int getBackgroundColor(boolean hovered) {
        if (!drawBackground) {
            return 0x00000000; // Fully transparent
        }
        
        int baseColor;
        switch (type) {
            case PRIMARY:
                baseColor = hovered ? UiTheme.BUTTON_HOVER : PRIMARY_COLOR;
                break;
            case SECONDARY:
                baseColor = hovered ? UiTheme.BUTTON_HOVER : SECONDARY_COLOR;
                break;
            case SUCCESS:
                baseColor = SUCCESS_COLOR;
                break;
            case DANGER:
                baseColor = DANGER_COLOR;
                break;
            case WARNING:
                baseColor = WARNING_COLOR;
                break;
            case INFO:
                baseColor = INFO_COLOR;
                break;
            case OUTLINE:
                baseColor = OUTLINE_COLOR;
                break;
            case TRANSPARENT:
                baseColor = TRANSPARENT_COLOR;
                break;
            default:
                baseColor = hovered ? UiTheme.BUTTON_HOVER : PRIMARY_COLOR;
        }

        if (hovered && type != ButtonType.PRIMARY && type != ButtonType.SECONDARY) {
            // Add alpha without overflowing
            int alpha = (baseColor >> 24) & 0xFF;
            int rgb = baseColor & 0xFFFFFF;
            alpha = Math.min(alpha + 0x20, 0xFF);
            return (alpha << 24) | rgb;
        }
        
        return baseColor;
    }
    
    /**
     * Get the button's border color based on its type
     */
    private int getBorderColor() {
        switch (type) {
            case OUTLINE:
                return 0xA0FFFFFF; // Semi-transparent white
            case TRANSPARENT:
                return 0x40FFFFFF; // Very transparent white
            default:
                return 0x80FFFFFF; // Moderately transparent white
        }
    }
    
    /**
     * Implementation of the required renderContent method from BCComponent
     */
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        
        // Draw button background with rounded corners
        if (drawBackground) {
            int backgroundColor = getBackgroundColor(hovered);
            int borderColor = getBorderColor();
            
            // Override the default background rendering
            this.backgroundColor = -1; // Disable default background
            
            // Draw background
            drawRoundedRect(guiGraphics, x, y, getWidth(), getHeight(), backgroundColor);
            
            // Draw border
            if (type == ButtonType.OUTLINE || type == ButtonType.TRANSPARENT || hovered) {
                drawRoundedRectOutline(guiGraphics, x, y, getWidth(), getHeight(), borderColor);
            }
        }
        
        // Get font from ClientHelper
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper == null) return;
        
        Object fontObj = clientHelper.getFont();
        if (!(fontObj instanceof net.minecraft.client.gui.Font font)) return;
        
        // Calculate text position
        int textX = x + (getWidth() - font.width(text)) / 2;
        int textY = y + (getHeight() - 8) / 2;
        
        // Adjust for icon if present
        if (iconTexture != null && iconWidth > 0 && iconHeight > 0) {
            int totalWidth = iconWidth + iconPadding + font.width(text);
            textX = x + (getWidth() - totalWidth) / 2 + iconWidth + iconPadding;
            
            // Draw icon
            int iconX = x + (getWidth() - totalWidth) / 2;
            int iconY = y + (getHeight() - iconHeight) / 2;
            guiGraphics.blit(iconTexture, iconX, iconY, iconU, iconV, iconWidth, iconHeight);
        }
        
        int color = textColor;
        boolean dropShadow = shadow;
        if (textColor == UiTheme.TEXT || textColor == 0xFFFFFF) {
            if (type == ButtonType.SUCCESS) {
                color = UiTheme.ON_FG;
                dropShadow = false;
            } else if (type == ButtonType.DANGER) {
                color = UiTheme.OFF_FG;
                dropShadow = false;
            }
        }
        if (dropShadow) {
            guiGraphics.drawString(font, text, textX, textY, color);
        } else {
            guiGraphics.drawString(font, text, textX, textY, color, false);
        }
    }
    
    /**
     * Draw a rounded rectangle
     */
    private void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        int cornerRadius = 3;
        
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
    private void drawRoundedRectOutline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        int cornerRadius = 3;
        
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
    private void drawCircleQuadrant(GuiGraphics graphics, int centerX, int centerY, int radius, int color, int quadrant) {
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
    private void drawCircleQuadrantOutline(GuiGraphics graphics, int centerX, int centerY, int radius, int color, int quadrant) {
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
    
    // Mouse event handling
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOver(mouseX, mouseY) && button == 0) {
            this.pressed = true;
            if (this.onPress != null) {
                this.onPress.accept(null);
            }
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.pressed && button == 0) {
            this.pressed = false;
            return true;
        }
        return false;
    }
    
    private boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= getX() && mouseX < getX() + getWidth() && 
               mouseY >= getY() && mouseY < getY() + getHeight();
    }
    
    // Fluent API for customization
    
    public BCButton withType(ButtonType type) {
        this.type = type;
        return this;
    }
    
    public BCButton withTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }
    
    public BCButton withShadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }
    
    public BCButton withIcon(ResourceLocation texture, int u, int v, int width, int height) {
        this.iconTexture = texture;
        this.iconU = u;
        this.iconV = v;
        this.iconWidth = width;
        this.iconHeight = height;
        return this;
    }
    
    public BCButton withIconPadding(int padding) {
        this.iconPadding = padding;
        return this;
    }
    
    public BCButton withBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
        return this;
    }
    
    public BCButton withText(Component text) {
        this.text = text;
        return this;
    }
    
    public BCButton withText(String translationKey) {
        this.text = Component.translatable(translationKey);
        return this;
    }
    
    public Component getText() {
        return this.text;
    }
} 
