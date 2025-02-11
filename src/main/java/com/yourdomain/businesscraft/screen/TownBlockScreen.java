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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.screen.components.DataLabelComponent;
import com.yourdomain.businesscraft.screen.components.UIComponent;
import java.util.ArrayList;
import java.util.List;
import com.yourdomain.businesscraft.screen.components.ToggleButtonComponent;
import com.yourdomain.businesscraft.screen.components.DataBoundButtonComponent;
import com.yourdomain.businesscraft.screen.components.TabComponent;
import com.yourdomain.businesscraft.screen.components.SlotComponent;

public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/town_block_gui.png");
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlockScreen.class);
    private final List<UIComponent> components = new ArrayList<>();

    public TownBlockScreen(TownBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 204;

        TabComponent tabComponent = new TabComponent(240, 20);
        
        // Town Tab - Only indicators
        List<UIComponent> townTabComponents = createTownInfoComponents(menu);
        
        // Resources Tab - Bread management
        List<UIComponent> resourcesTabComponents = createResourceComponents(menu);
        
        // Settings Tab - Buttons
        List<UIComponent> settingsTabComponents = createSettingsComponents(menu);
        
        tabComponent.addTab("town", Component.translatable("gui.businesscraft.tab.town"), townTabComponents);
        tabComponent.addTab("resources", Component.translatable("gui.businesscraft.tab.resources"), resourcesTabComponents);
        tabComponent.addTab("settings", Component.translatable("gui.businesscraft.tab.settings"), settingsTabComponents);
        
        components.add(tabComponent);
    }
    
    private List<UIComponent> createTownInfoComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new DataLabelComponent(() -> "Town: " + menu.getTownName(), 0xFFFFFF, 200));
        comps.add(new DataLabelComponent(() -> "Population: " + menu.getPopulation() + "/" + ConfigLoader.minPopForTourists, 0xFFFFFF, 200));
        return comps;
    }
    
    private List<UIComponent> createResourceComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new DataLabelComponent(() -> "Bread: " + menu.getBreadCount(), 0xFFFFFF, 200));
        comps.add(new SlotComponent());
        return comps;
    }
    
    private List<UIComponent> createSettingsComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        int buttonWidth = 120;
        int buttonHeight = 20;
        
        comps.add(new ToggleButtonComponent(0, 0, buttonWidth, buttonHeight,
            Component.translatable("gui.businesscraft.set_tourist_path"),
            button -> ModMessages.sendToServer(new SetPathCreationModePacket(
                menu.getBlockEntity().getBlockPos(), true
            ))
        ));
        comps.add(new ToggleButtonComponent(0, 0, buttonWidth, buttonHeight,
            Component.translatable("gui.businesscraft.toggle_tourists"),
            button -> ModMessages.sendToServer(new ToggleTouristSpawningPacket(
                menu.getBlockEntity().getBlockPos()
            ))
        ));
        comps.add(new DataBoundButtonComponent(
            () -> Component.literal("Radius: " + menu.getBlockEntity().getSearchRadius()),
            (button) -> handleRadiusChange(),
            buttonWidth, buttonHeight
        ));
        return comps;
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
        
        // Render tabs
        components.get(0).render(guiGraphics, leftPos + 8, topPos + 5, mouseX, mouseY);
        
        // Clear previous components and render active tab
        TabComponent tabComponent = (TabComponent) components.get(0);
        int yPos = topPos + 30;
        for (UIComponent component : tabComponent.getActiveComponents()) {
            component.render(guiGraphics, leftPos + 10, yPos, mouseX, mouseY);
            yPos += component.getHeight() + 8;
        }
        
        // Only render bread slot in resources tab
        if (((TabComponent) components.get(0)).getActiveTabId().equals("resources")) {
            renderTooltip(guiGraphics, mouseX, mouseY); // Shows slot tooltip
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
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