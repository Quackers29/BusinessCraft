package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Component that wraps Minecraft's EditBox for text input
 */
public class EditBoxComponent implements UIComponent {
    private final int width;
    private final int height;
    private final Supplier<String> initialTextSupplier;
    private final Consumer<String> onTextChanged;
    private final int maxLength;
    private EditBox editBox;
    private boolean visible = true;

    public EditBoxComponent(int width, int height, Supplier<String> initialTextSupplier, 
                           Consumer<String> onTextChanged, int maxLength) {
        this.width = width;
        this.height = height;
        this.initialTextSupplier = initialTextSupplier;
        this.onTextChanged = onTextChanged;
        this.maxLength = maxLength;
    }

    @Override
    public void init(Consumer<Button> register) {
        // EditBox is not a Button, so we create it but don't register it
        editBox = new EditBox(Minecraft.getInstance().font, 0, 0, width, height, Component.literal(""));
        editBox.setMaxLength(maxLength);
        editBox.setValue(initialTextSupplier.get());
        editBox.setResponder(onTextChanged);
        editBox.setVisible(visible);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        if (!visible) return;
        
        editBox.setX(x);
        editBox.setY(y);
        editBox.render(guiGraphics, mouseX, mouseY, 0);
    }

    @Override
    public void tick() {
        if (editBox != null) {
            editBox.tick();
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        if (editBox != null) {
            editBox.setVisible(visible);
        }
    }
    
    public String getText() {
        return editBox != null ? editBox.getValue() : "";
    }
    
    public void setText(String text) {
        if (editBox != null) {
            editBox.setValue(text);
        }
    }
    
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (editBox != null && visible) {
            return editBox.mouseClicked(mouseX, mouseY, button);
        }
        return false;
    }
    
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editBox != null && visible) {
            return editBox.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }
    
    public boolean charTyped(char c, int modifiers) {
        if (editBox != null && visible) {
            return editBox.charTyped(c, modifiers);
        }
        return false;
    }
    
    public void setFocused(boolean focused) {
        if (editBox != null) {
            editBox.setFocused(focused);
        }
    }
    
    public boolean isFocused() {
        return editBox != null && editBox.isFocused();
    }
} 