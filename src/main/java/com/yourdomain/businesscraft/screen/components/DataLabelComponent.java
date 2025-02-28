package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.Button;

public class DataLabelComponent implements UIComponent {
    private boolean visible = true;
    private final Supplier<String> textSupplier;
    private final int color;
    private final int maxWidth;

    public DataLabelComponent(Supplier<String> textSupplier, int color, int maxWidth) {
        this.textSupplier = textSupplier;
        this.color = color;
        this.maxWidth = maxWidth;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        guiGraphics.drawString(Minecraft.getInstance().font, textSupplier.get(), x, y, color);
    }

    @Override public void tick() {}
    @Override public int getWidth() { return maxWidth; }
    @Override public int getHeight() { return 12; }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void init(Consumer<Button> register) {
        // No buttons to register for a label
    }
}