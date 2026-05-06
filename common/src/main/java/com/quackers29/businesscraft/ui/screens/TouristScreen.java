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
    private static final String TITLE_DATA_SEPARATOR = "||";
    private static final int TEXT_COLOR = 0x404040;
    private static final int XP_BAR_TEXTURE_U = 0;
    private static final int XP_BAR_BG_TEXTURE_V = 186;
    private static final int XP_BAR_FILL_TEXTURE_V = 191;
    private static final int XP_BAR_WIDTH = 102;
    private static final int XP_BAR_HEIGHT = 5;
    private static final int XP_BAR_X = 136;
    private static final int XP_BAR_Y = 16;
    private static final int STATIC_NOVICE_XP_FILL = 24;
    private final String originTownName;

    public TouristScreen(TouristMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 276;
        this.imageHeight = 166;
        this.inventoryLabelX = 107; // Match MerchantScreen default

        String rawTitle = title.getString();
        String parsedOriginTown = "Unknown";
        int separatorIndex = rawTitle.indexOf(TITLE_DATA_SEPARATOR);
        if (separatorIndex >= 0) {
            parsedOriginTown = rawTitle.substring(separatorIndex + TITLE_DATA_SEPARATOR.length());
        }

        this.originTownName = parsedOriginTown;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Render custom text "No trades available"
        guiGraphics.drawCenteredString(this.font, Component.literal("No trades available"), this.leftPos + 209,
                this.topPos + 20, 0xFFFFFF);
    }

    private String formatTime(int ticks) {
        int seconds = ticks / 20;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%dm %ds", minutes, seconds);
    }

    private String ellipsizeToWidth(String text, int maxWidth) {
        if (text == null || text.isBlank()) {
            return "Unknown";
        }
        if (this.font.width(text) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        int availableWidth = Math.max(0, maxWidth - this.font.width(suffix));
        return this.font.plainSubstrByWidth(text, availableWidth) + suffix;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawCenteredString(this.font, Component.literal("Tourist - Novice"), 186, 6, TEXT_COLOR);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TEXT_COLOR,
                false);

        int leftPanelX = 8;
        int originTownY = 22;
        int journeyY = 50;
        int timeRemainingY = 74;
        int valueWidth = 95;

        guiGraphics.drawString(this.font, Component.literal("Origin Town:"), leftPanelX, originTownY, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal(ellipsizeToWidth(this.originTownName, valueWidth)), leftPanelX,
                originTownY + 10, TEXT_COLOR, false);

        guiGraphics.drawString(this.font, Component.literal("Journey Duration:"), leftPanelX, journeyY, TEXT_COLOR, false);
        guiGraphics.drawString(this.font, Component.literal(formatTime(this.menu.getJourneyAge())), leftPanelX, journeyY + 10,
                TEXT_COLOR, false);

        guiGraphics.drawString(this.font, Component.literal("Time Remaining:"), leftPanelX, timeRemainingY, TEXT_COLOR,
                false);
        guiGraphics.drawString(this.font, Component.literal(formatTime(this.menu.getTimeLeft())), leftPanelX,
                timeRemainingY + 10, TEXT_COLOR, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight, 512, 256);
        guiGraphics.blit(TEXTURE, x + XP_BAR_X, y + XP_BAR_Y, XP_BAR_TEXTURE_U, XP_BAR_BG_TEXTURE_V, XP_BAR_WIDTH,
                XP_BAR_HEIGHT, 512, 256);
        guiGraphics.blit(TEXTURE, x + XP_BAR_X, y + XP_BAR_Y, XP_BAR_TEXTURE_U, XP_BAR_FILL_TEXTURE_V,
                STATIC_NOVICE_XP_FILL, XP_BAR_HEIGHT, 512, 256);
    }
}
