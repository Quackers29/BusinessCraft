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
<<<<<<< Updated upstream
=======
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.screen.components.UIComponent;
import com.yourdomain.businesscraft.screen.components.DataLabelComponent;
import com.yourdomain.businesscraft.screen.components.ToggleButtonComponent;
import com.yourdomain.businesscraft.screen.components.DataBoundButtonComponent;
import java.util.ArrayList;
import java.util.List;
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes

public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/town_block_gui.png");
    private Button setPathButton;
    private Button toggleTouristsButton;
<<<<<<< Updated upstream
=======
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlockScreen.class);
    private final List<UIComponent> components = new ArrayList<>();
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes

    public TownBlockScreen(TownBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // Initialize components
        components.add(new DataLabelComponent(
            () -> "Bread: " + menu.getBreadCount(),
            0xFFFFFF, 100
        ));
        components.add(new DataLabelComponent(
            () -> "Population: " + menu.getPopulation() + "/" + ConfigLoader.minPopForTourists,
            0xFFFFFF, 150
        ));
        
        // Path button
        components.add(new ToggleButtonComponent(0, 0, 100, 20, 
            Component.translatable("gui.businesscraft.set_tourist_path"),
            button -> {
                ModMessages.sendToServer(new SetPathCreationModePacket(menu.getBlockEntity().getBlockPos(), true));
            }
        ));
        
        // Spawn toggle button
        components.add(new ToggleButtonComponent(0, 0, 100, 20, 
            Component.translatable("gui.businesscraft.toggle_tourists"), 
            button -> {
                ModMessages.sendToServer(new ToggleTouristSpawningPacket(menu.getBlockEntity().getBlockPos()));
            }
        ));
        
        // Radius button
        components.add(new DataBoundButtonComponent(
            () -> Component.literal("Radius: " + menu.getBlockEntity().getSearchRadius()),
            (button) -> handleRadiusChange(),
            100, 20
        ));
    }

    @Override
    protected void init() {
        super.init();
        components.forEach(component -> 
            component.init(this::addRenderableWidget)
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
<<<<<<< Updated upstream
<<<<<<< Updated upstream
        renderTooltip(guiGraphics, mouseX, mouseY);
=======
=======
>>>>>>> Stashed changes
        
        int yPos = topPos + 10;
        for (UIComponent component : components) {
            component.render(guiGraphics, leftPos + 10, yPos, mouseX, mouseY);
            yPos += component.getHeight() + 5;
        }
        
        LOGGER.debug("Screen Render - Enabled: {}, CanSpawn: {}",
            menu.isTouristSpawningEnabled(), menu.getData().get(3));
        
>>>>>>> Stashed changes
        int breadCount = menu.getBreadCount();
        int population = menu.getPopulation();
        String townName = menu.getTownName();
        
<<<<<<< Updated upstream
        guiGraphics.drawString(this.font, "Town: " + townName, leftPos + 10, topPos + 10, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Bread: " + breadCount, leftPos + 10, topPos + 20, 0xFFFFFF);
        guiGraphics.drawString(this.font, "Population: " + population, leftPos + 10, topPos + 30, 0xFFFFFF);
=======
        if (menu.getBlockEntity() != null) {
            LOGGER.debug("Client State - Enabled: {}, CanSpawn: {}", 
                menu.isTouristSpawningEnabled(), menu.getData().get(3));
        }
<<<<<<< Updated upstream
>>>>>>> Stashed changes
=======
>>>>>>> Stashed changes
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    private void handleRadiusChange() {
        int currentRadius = menu.getBlockEntity().getSearchRadius();
        int newRadius = currentRadius;
        
        boolean isShift = hasShiftDown();
        boolean isControl = hasControlDown();
        
        if (isShift && isControl) {
            newRadius -= 10;
        } else if (isControl) {
            newRadius -= 1;
        } else if (isShift) {
            newRadius += 10;
        } else {
            newRadius += 1;
        }
        
        newRadius = Math.max(1, Math.min(newRadius, 100));
        ModMessages.sendToServer(new SetSearchRadiusPacket(
            menu.getBlockEntity().getBlockPos(), newRadius
        ));
    }
}