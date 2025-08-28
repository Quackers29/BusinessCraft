package com.quackers29.businesscraft.ui.components.basic;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

/**
 * Base interface for all UI components in the BusinessCraft UI framework.
 * Unified architecture implementation for cross-platform compatibility.
 */
public interface UIComponent {
    void init(Consumer<Button> register);
    void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY);
    void tick();
    int getWidth();
    int getHeight();
    void setVisible(boolean visible);
    
    /**
     * Get the X coordinate of the component
     */
    int getX();
    
    /**
     * Get the Y coordinate of the component
     */
    int getY();
    
    /**
     * Check if the component is visible
     */
    boolean isVisible();
    
    /**
     * Handle mouse click
     * @return true if the click was handled
     */
    default boolean mouseClicked(double mouseX, double mouseY, int button) { return false; }
    
    /**
     * Handle mouse drag
     * @return true if the drag was handled
     */
    default boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) { return false; }
    
    /**
     * Handle mouse release
     * @return true if the release was handled
     */
    default boolean mouseReleased(double mouseX, double mouseY, int button) { return false; }
    
    /**
     * Handle mouse scroll
     * @return true if the scroll was handled
     */
    default boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) { return false; }
    
    /**
     * Handle key press
     * @return true if the key press was handled
     */
    default boolean keyPressed(int keyCode, int scanCode, int modifiers) { return false; }
    
    /**
     * Handle character typed
     * @return true if the character was handled
     */
    default boolean charTyped(char codePoint, int modifiers) { return false; }
}