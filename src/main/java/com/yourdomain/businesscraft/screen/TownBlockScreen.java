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
import com.yourdomain.businesscraft.network.ToggleTouristSpawningPacket;
import com.yourdomain.businesscraft.network.SetSearchRadiusPacket;

public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/town_block_gui.png");
    private Button setPathButton;
    private Button toggleTouristsButton;

    public TownBlockScreen(TownBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        
        setPathButton = Button.builder(Component.translatable("gui.businesscraft.set_tourist_path"), (button) -> {
            ModMessages.sendToServer(new SetPathCreationModePacket(menu.getBlockEntity().getBlockPos(), true));
        }).pos(leftPos + 10, topPos + 40).size(100, 20).build();

        toggleTouristsButton = Button.builder(Component.translatable("gui.businesscraft.toggle_tourists"), (button) -> {
            ModMessages.sendToServer(new ToggleTouristSpawningPacket(menu.getBlockEntity().getBlockPos()));
        }).pos(leftPos + 10, topPos + 65).size(100, 20).build();

        Button radiusButton = Button.builder(
            Component.literal("Radius: " + menu.getBlockEntity().getSearchRadius()), 
            (button) -> {
                int currentRadius = menu.getBlockEntity().getSearchRadius();
                int newRadius = (currentRadius % 20) + 1; // Cycle 1-20
                ModMessages.sendToServer(new SetSearchRadiusPacket(menu.getBlockEntity().getBlockPos(), newRadius));
                button.setMessage(Component.literal("Radius: " + newRadius));
            })
            .pos(leftPos + 10, topPos + 90)
            .size(100, 20)
            .build();

        addRenderableWidget(setPathButton);
        addRenderableWidget(toggleTouristsButton);
        addRenderableWidget(radiusButton);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        int breadCount = menu.getBreadCount();
        int population = menu.getPopulation();
        String townName = menu.getTownName();
        
        guiGraphics.drawString(this.font, "Town: " + townName, leftPos + 10, topPos + 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Bread: " + breadCount, leftPos + 10, topPos + 20, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Population: " + population, leftPos + 10, topPos + 30, 0xFFFFFF);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }
}