package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.world.inventory.Slot;
import java.util.function.Consumer;

public class SlotComponent implements UIComponent {
    private final int x;
    private final int y;
    private boolean visible = true;

    public SlotComponent() {
        this.x = 142;  // Match menu slot X position
        this.y = 10;   // Match menu slot Y position
    }

    @Override
    public void render(GuiGraphics guiGraphics, int parentX, int parentY, int mouseX, int mouseY) {
        guiGraphics.fill(parentX + x, parentY + y, parentX + x + 16, parentY + y + 16, 0x80FFFFFF);
    }

    @Override public void init(Consumer<Button> register) {}
    @Override public void tick() {}
    @Override public int getWidth() { return 18; }
    @Override public int getHeight() { return 18; }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
} 