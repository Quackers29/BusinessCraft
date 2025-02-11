package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;

public class ToggleButtonComponent implements UIComponent {
    private final Button button;
    private final int width;
    private final int height;
    private boolean visible = true;

    public ToggleButtonComponent(int x, int y, int width, int height, 
            Component text, Consumer<Button> onPress) {
        this.width = width;
        this.height = height;
        this.button = Button.builder(text, b -> onPress.accept(b))
            .pos(x, y)
            .size(width, height)
            .build();
    }

    @Override
    public void init(Consumer<Button> register) {
        register.accept(button);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        button.setX(x);
        button.setY(y);
        button.render(guiGraphics, mouseX, mouseY, 0);
    }

    @Override
    public void tick() {
        // No-op - vanilla buttons don't require ticking
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (button != null) {
            button.visible = visible;
        }
    }

    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }
}