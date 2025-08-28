package com.quackers29.businesscraft.ui.components.display;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;

/**
 * Data-bound label component that displays dynamic text.
 * Unified architecture implementation for cross-platform compatibility.
 */
public class DataLabelComponent implements UIComponent {
    private final Supplier<String> textSupplier;
    private final int color;
    private final int maxWidth;
    private final int width;
    private final int height;
    private boolean visible = true;
    private int x, y;

    public DataLabelComponent(Supplier<String> textSupplier, int color, int maxWidth) {
        this.textSupplier = textSupplier;
        this.color = color;
        this.maxWidth = maxWidth;
        this.width = maxWidth;
        this.height = 12;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        this.x = x;
        this.y = y;
        guiGraphics.drawString(Minecraft.getInstance().font, textSupplier.get(), x, y, color);
    }

    @Override public void tick() {}
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

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

    @Override
    public void init(Consumer<Button> register) {
        // No buttons to register for a label
    }
}