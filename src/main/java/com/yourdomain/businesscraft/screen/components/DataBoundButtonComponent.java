package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataBoundButtonComponent implements UIComponent {
    private final Supplier<Component> textSupplier;
    private final Consumer<Button> onPress;
    private final int width;
    private final int height;
    private Button button;
    private boolean visible = true;

    public DataBoundButtonComponent(Supplier<Component> textSupplier, 
                                  Consumer<Button> onPress,
                                  int width, int height) {
        this.textSupplier = textSupplier;
        this.onPress = onPress;
        this.width = width;
        this.height = height;
        createButton();
    }

    private void createButton() {
        this.button = Button.builder(textSupplier.get(), b -> onPress.accept(b))
            .size(width, height)
            .build();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        button.setMessage(textSupplier.get());
        button.setX(x);
        button.setY(y);
        button.render(guiGraphics, mouseX, mouseY, 0);
    }

    @Override public void tick() {}
    @Override public int getWidth() { return width; }
    @Override public int getHeight() { return height; }

    @Override
    public void init(Consumer<Button> register) {
        register.accept(button);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (button != null) button.visible = visible;
    }
}