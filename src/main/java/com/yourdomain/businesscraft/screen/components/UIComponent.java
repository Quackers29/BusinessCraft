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
}