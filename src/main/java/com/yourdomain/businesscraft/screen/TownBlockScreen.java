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
import com.yourdomain.businesscraft.network.SetTownNamePacket;
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
import com.yourdomain.businesscraft.screen.components.TownNameEditorComponent;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.screen.components.ResourceListComponent;
import net.minecraft.client.Minecraft;
import com.yourdomain.businesscraft.screen.components.VisitHistoryComponent;
import com.yourdomain.businesscraft.screen.components.EditBoxComponent;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;


public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/town_block_gui.png");
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlockScreen.class);
    private final List<UIComponent> components = new ArrayList<>();
    
    // Add state for town name editing
    private boolean isEditingTownName = false;
    private TownNameEditorComponent nameEditor;

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
        
        // Create name editor but keep it hidden initially
        nameEditor = new TownNameEditorComponent(
            200,
            () -> menu.getTownName(),
            this::handleTownNameConfirmed,
            this::handleEditCancelled
        );
        nameEditor.setVisible(false);
    }
    
    private List<UIComponent> createTownInfoComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new DataLabelComponent(() -> "Town: " + menu.getTownName(), 0xFFFFFF, 200));
        comps.add(new DataLabelComponent(() -> "Population: " + menu.getPopulation() + "/" + ConfigLoader.minPopForTourists, 0xFFFFFF, 200));
        return comps;
    }
    
    private List<UIComponent> createResourceComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new ResourceListComponent(() -> menu.getAllResources(), 230));
        comps.add(new SlotComponent());
        return comps;
    }
    
    private List<UIComponent> createHistoryComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new VisitHistoryComponent(() -> menu.getVisitHistory(), 230));
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
        
        // Add Change Town Name button
        comps.add(new ToggleButtonComponent(0, 0, buttonWidth, buttonHeight,
            Component.translatable("gui.businesscraft.change_town_name"),
            button -> showTownNameEditor()
        ));
        
        return comps;
    }

    @Override
    protected void init() {
        super.init();
        components.forEach(component -> 
            component.init(this::addRenderableWidget)
        );
        
        // Initialize the name editor
        nameEditor.init(this::addRenderableWidget);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        
        // If editing town name, we render a different background and only the editor
        if (isEditingTownName) {
            // Draw the background texture
            guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            // Only draw the title
            guiGraphics.drawString(this.font, this.title, leftPos + this.titleLabelX, topPos + this.titleLabelY, 4210752);
            // Draw a semi-transparent backdrop 
            guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xA0000000);
            // Draw the editor
            nameEditor.render(guiGraphics, leftPos + 28, topPos + 50, mouseX, mouseY);
            return;
        }
        
        // Normal rendering for when not editing
        super.render(guiGraphics, mouseX, mouseY, delta);
        
        // Render tabs and tab content
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
                slot.render(guiGraphics, leftPos + 210, yPos, mouseX, mouseY);
                
                // Add a label for the slot
                guiGraphics.drawString(Minecraft.getInstance().font, "Add:", leftPos + 210, yPos - 10, 0xFFFFFF);
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
        // Skip if we're editing a town name
        if (isEditingTownName) return false;
        
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
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If editing town name, pass mouse clicks to the editor
        if (isEditingTownName) {
            boolean handled = nameEditor.mouseClicked(mouseX, mouseY, button);
            
            // If the click wasn't on the editor and it's a left-click, handle unfocusing
            if (!handled && button == 0) {
                EditBoxComponent editBox = nameEditor.getEditBox();
                if (editBox != null && editBox.isFocused()) {
                    editBox.setFocused(false);
                    return true;
                }
            }
            
            return handled || isClickInsideEditor(mouseX, mouseY);
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    /**
     * Checks if a click is inside the editor area
     */
    private boolean isClickInsideEditor(double mouseX, double mouseY) {
        // Calculate the editor bounds
        int editorX = leftPos + 28;
        int editorY = topPos + 50;
        int editorWidth = nameEditor.getWidth();
        int editorHeight = nameEditor.getHeight();
        
        // Check if the click is inside the editor bounds
        return mouseX >= editorX && mouseX <= editorX + editorWidth &&
               mouseY >= editorY && mouseY <= editorY + editorHeight;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If editing town name, pass key presses to the editor
        if (isEditingTownName) {
            if (nameEditor.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char c, int modifiers) {
        // If editing town name, pass character input to the editor
        if (isEditingTownName) {
            if (nameEditor.charTyped(c, modifiers)) {
                return true;
            }
        }
        
        return super.charTyped(c, modifiers);
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
    
    private void showTownNameEditor() {
        isEditingTownName = true;
        nameEditor.setVisible(true);
        
        // Force a fresh town name lookup to ensure we have the latest
        // Call getTownName() which may trigger a fresh lookup from the provider
        String currentTownName = menu.getTownName();
        LOGGER.debug("Showing town name editor with initial name: {}", currentTownName);
        
        // Get the current town name and set it in the edit box
        EditBoxComponent editBox = nameEditor.getEditBox();
        if (editBox != null) {
            // Set the text in the edit box
            editBox.setText(currentTownName);
            // Set focus to the edit box (so typing goes there immediately)
            editBox.setFocused(true);
        }
    }
    
    private void handleTownNameConfirmed(String newName) {
        // Log the name change attempt
        LOGGER.info("[DEBUG] Confirming name change to: {}", newName);
        
        // Send packet to update town name
        ModMessages.sendToServer(new SetTownNamePacket(
            menu.getBlockEntity().getBlockPos(), 
            newName
        ));
        
        // Force immediate UI refresh - simulate a server update by updating the block entity's cached name
        // This gives immediate feedback to the user before the server confirms the change
        TownBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity != null) {
            // Client-side only: Update the cached name directly for immediate feedback
            // The server will eventually send the real update, but this gives instant response
            if (minecraft.level.isClientSide) {
                LOGGER.info("[DEBUG] Setting client-side name to: {}", newName);
                blockEntity.setClientTownName(newName);
                
                // Verify the name was updated
                String currentName = menu.getTownName();
                LOGGER.info("[DEBUG] After name change, current name is: {}", currentName);
            }
        }
        
        // Hide editor and show tabs again
        hideEditor();
    }
    
    private void handleEditCancelled() {
        // Just hide the editor without saving
        hideEditor();
    }
    
    private void hideEditor() {
        isEditingTownName = false;
        nameEditor.setVisible(false);
    }
}