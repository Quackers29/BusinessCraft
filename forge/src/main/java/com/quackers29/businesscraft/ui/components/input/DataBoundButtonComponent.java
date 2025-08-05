package com.quackers29.businesscraft.ui.components.input;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;
import java.util.function.Supplier;
import com.quackers29.businesscraft.ui.components.basic.UIComponent;

public class DataBoundButtonComponent implements UIComponent {
    private final Supplier<Component> textSupplier;
    private final Consumer<Button> onPress;
    private final int width;
    private final int height;
    private Button button;
    private boolean visible = true;
    private int x, y;

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
        if (!visible) return;
        
        this.x = x;
        this.y = y;
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