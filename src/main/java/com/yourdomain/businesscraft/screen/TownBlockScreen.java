package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.menu.TownBlockMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.SetPathCreationModePacket;

public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/town_block_gui.png");
    private Button setPathButton;

    public TownBlockScreen(TownBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        setPathButton = Button.builder(Component.literal("Set Tourist Path"), button -> {
            // Send packet to server to enter path creation mode
            ModMessages.sendToServer(new SetPathCreationModePacket(menu.getBlockEntity().getBlockPos(), true));
            minecraft.setScreen(null); // Close the screen
        }).pos(leftPos + 10, topPos + 40).size(100, 20).build();
        
        addRenderableWidget(setPathButton);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Display bread count and population
        int breadCount = menu.getBreadCount();
        int population = menu.getPopulation();
        String townName = menu.getTownName();
        guiGraphics.drawString(this.font, "Town: " + townName, leftPos + 10, topPos + 0, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Bread: " + breadCount, leftPos + 10, topPos + 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Population: " + population, leftPos + 10, topPos + 20, 0xFFFFFF);
    }
}