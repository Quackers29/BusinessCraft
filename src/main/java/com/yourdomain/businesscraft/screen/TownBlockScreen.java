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
import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.screen.components.ResourceListComponent;
import net.minecraft.client.Minecraft;
import com.yourdomain.businesscraft.screen.components.VisitHistoryComponent;


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
        
        // History Tab - Tourist visits
        List<UIComponent> historyTabComponents = createHistoryComponents(menu);
        
        // Settings Tab - Buttons
        List<UIComponent> settingsTabComponents = createSettingsComponents(menu);
        
        tabComponent.addTab("town", Component.translatable("gui.businesscraft.tab.town"), townTabComponents);
        tabComponent.addTab("resources", Component.translatable("gui.businesscraft.tab.resources"), resourcesTabComponents);
        tabComponent.addTab("history", Component.translatable("gui.businesscraft.tab.history"), historyTabComponents);
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
        comps.add(new ResourceListComponent(() -> menu.getAllResources(), 200));
        comps.add(new SlotComponent());
        return comps;
    }
    
    private List<UIComponent> createHistoryComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new VisitHistoryComponent(() -> menu.getVisitHistory(), 200));
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
            () -> Component.literal("Radius: " + menu.getSearchRadius()),
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
        
        if (tabComponent.getActiveTabId().equals("resources")) {
            // Special handling for resources tab
            List<UIComponent> resourceComponents = tabComponent.getActiveComponents();
            if (resourceComponents.size() >= 2) {
                // ResourceListComponent should be the first component
                ResourceListComponent resourceList = (ResourceListComponent) resourceComponents.get(0);
                // SlotComponent should be the second component
                SlotComponent slot = (SlotComponent) resourceComponents.get(1);
                
                // Position the resource list on the left
                resourceList.render(guiGraphics, leftPos + 10, yPos, mouseX, mouseY);
                
                // Position the slot on the right
                slot.render(guiGraphics, leftPos + 180, yPos, mouseX, mouseY);
                
                // Add a label for the slot
                guiGraphics.drawString(Minecraft.getInstance().font, "Add:", leftPos + 180, yPos - 10, 0xFFFFFF);
            }
            
            // Show slot tooltip
            renderTooltip(guiGraphics, mouseX, mouseY);
        } else if (tabComponent.getActiveTabId().equals("history")) {
            // Special handling for history tab
            List<UIComponent> historyComponents = tabComponent.getActiveComponents();
            if (historyComponents.size() >= 1 && historyComponents.get(0) instanceof VisitHistoryComponent historyComponent) {
                historyComponent.render(guiGraphics, leftPos + 10, yPos, mouseX, mouseY);
            }
        } else {
            // Standard rendering for other tabs
            for (UIComponent component : tabComponent.getActiveComponents()) {
                component.render(guiGraphics, leftPos + 10, yPos, mouseX, mouseY);
                yPos += component.getHeight() + 8;
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    /**
     * Override the renderLabels method to prevent the inventory label from being displayed
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Only draw the title, not the inventory label
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 4210752);
        // We intentionally do not call super.renderLabels to avoid drawing the inventory label
    }

    /**
     * Handle mouse scrolling for resource list
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        TabComponent tabComponent = (TabComponent) components.get(0);
        
        // Only handle scrolling in resources or history tab
        String activeTabId = tabComponent.getActiveTabId();
        if (activeTabId.equals("resources")) {
            List<UIComponent> resourceComponents = tabComponent.getActiveComponents();
            if (resourceComponents.size() >= 1 && resourceComponents.get(0) instanceof ResourceListComponent resourceList) {
                // Pass scroll event to resource list component
                if (resourceList.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        } else if (activeTabId.equals("history")) {
            List<UIComponent> historyComponents = tabComponent.getActiveComponents();
            if (historyComponents.size() >= 1 && historyComponents.get(0) instanceof VisitHistoryComponent historyComponent) {
                // Pass scroll event to history component
                if (historyComponent.mouseScrolled(mouseX, mouseY, delta)) {
                    return true;
                }
            }
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void handleRadiusChange() {
        // Get the current radius from the menu's data provider if possible
        int currentRadius = menu.getSearchRadius();
        int newRadius = currentRadius;
        
        // Calculate new radius based on key combinations
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
        
        // Clamp to reasonable values
        newRadius = Math.max(1, Math.min(newRadius, 100));
        
        // Send packet to update
        ModMessages.sendToServer(new SetSearchRadiusPacket(menu.getBlockEntity().getBlockPos(), newRadius));
    }
}