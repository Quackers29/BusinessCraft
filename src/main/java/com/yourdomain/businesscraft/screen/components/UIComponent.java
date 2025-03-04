package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

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
}