package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import java.util.function.Consumer;

public class SlotComponent implements UIComponent {
    private int x;
    private int y;
    private boolean visible = true;
    private final int slotSize;

    public SlotComponent() {
        this.x = 142;  // Default position
        this.y = 10;   // Default position
        this.slotSize = 18;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        this.x = x;
        this.y = y;
        guiGraphics.fill(x, y, x + slotSize, y + slotSize, 0x80FFFFFF);
    }

    @Override public void init(Consumer<Button> register) {}
    @Override public void tick() {}
    @Override public int getWidth() { return slotSize; }
    @Override public int getHeight() { return slotSize; }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public int getX() {
        return x;
    }
    
    @Override
    public int getY() {
        return y;
    }
} 