package com.quackers29.businesscraft.ui.screens;

import com.quackers29.businesscraft.menu.TouristMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class TouristScreen extends AbstractContainerScreen<TouristMenu> {

    // Use standard villager texture
    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/container/villager2.png");

    public TouristScreen(TouristMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.imageHeight = 166;
        this.inventoryLabelX = 107; // Match MerchantScreen default
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render custom text "No trades available"
        // Move it up as requested
        guiGraphics.drawCenteredString(this.font, Component.literal("No trades available"), this.leftPos + 209,
                this.topPos + 20, 0xFFFFFF);

        // Render stats in the left area (Trades area)
        int ageTicks = this.menu.getJourneyAge();
        int expiryTicks = this.menu.getTimeLeft();

        String ageText = formatTime(ageTicks);
        String expiryText = formatTime(expiryTicks);

        guiGraphics.drawString(this.font, Component.literal("Journey Duration:"), this.leftPos + 8, this.topPos + 40,
                0x404040, false);
        guiGraphics.drawString(this.font, Component.literal(ageText), this.leftPos + 8, this.topPos + 50, 0x404040,
                false);

        guiGraphics.drawString(this.font, Component.literal("Time Remaining:"), this.leftPos + 8, this.topPos + 70,
                0x404040, false);
        guiGraphics.drawString(this.font, Component.literal(expiryText), this.leftPos + 8, this.topPos + 80, 0x404040,
                false);
    }

    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
    }
}
