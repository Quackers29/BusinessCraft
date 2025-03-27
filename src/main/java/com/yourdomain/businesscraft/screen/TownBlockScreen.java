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
import net.minecraft.client.gui.components.events.GuiEventListener;
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
import java.util.UUID;
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
import com.yourdomain.businesscraft.screen.components.StateResourceListComponent;
import com.yourdomain.businesscraft.screen.components.StateVisitHistoryComponent;
import com.yourdomain.businesscraft.screen.state.TownInterfaceState;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.client.PlatformPathKeyHandler;
import com.yourdomain.businesscraft.network.PlayerExitUIPacket;
import java.util.Map;
import java.util.HashMap;
import java.lang.StringBuilder;
import net.minecraft.core.BlockPos;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.network.SetPlatformPathCreationModePacket;


public class TownBlockScreen extends AbstractContainerScreen<TownBlockMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(BusinessCraft.MOD_ID,
            "textures/gui/town_block_gui.png");
    private static final Logger LOGGER = LoggerFactory.getLogger(TownBlockScreen.class);
    private final List<UIComponent> components = new ArrayList<>();
    
    // Add state for town name editing
    private boolean isEditingTownName = false;
    private TownNameEditorComponent nameEditor;
    
    // Add a platforms tab
    private PlatformsTab platformsTab;
    private boolean isSettingPlatformPath = false;
    private UUID platformBeingEdited = null;
    
    // Path creation mode
    private boolean isInPathCreationMode = false;
    
    // For displaying instructions
    private Component instructionsText = null;

    // State management
    private final TownInterfaceState state;

    public TownBlockScreen(TownBlockMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 204;

        // Initialize state management
        this.state = new TownInterfaceState();
        
        // Initialize state with menu data
        updateStateFromMenu();
        
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
    
    private void updateStateFromMenu() {
        // Update town data
        state.setTownName(menu.getTownName());
        state.setPopulation(menu.getPopulation());
        state.setTouristCount(menu.getTouristCount());
        state.setMaxTourists(menu.getMaxTourists());
        
        // Update resources
        Map<String, Integer> resourceMap = new HashMap<>();
        menu.getAllResources().forEach((item, count) -> 
            resourceMap.put(item.getDescription().getString(), count));
        state.setResources(resourceMap);
        
        // Update visit history
        List<TownInterfaceState.VisitHistoryEntry> historyEntries = new ArrayList<>();
        menu.getVisitHistory().forEach(entry -> {
            String direction = calculateDirection(entry.getOriginPos());
            historyEntries.add(new TownInterfaceState.VisitHistoryEntry(
                entry.getTimestamp(),
                entry.getTownName(),
                entry.getCount(),
                direction
            ));
        });
        state.setVisitHistory(historyEntries);
    }
    
    private String calculateDirection(BlockPos originPos) {
        if (originPos == null || originPos.equals(BlockPos.ZERO)) {
            return "Unknown";
        }
        
        BlockPos playerPos = Minecraft.getInstance().player.blockPosition();
        StringBuilder direction = new StringBuilder();
        
        if (originPos.getZ() < playerPos.getZ()) direction.append("N");
        else if (originPos.getZ() > playerPos.getZ()) direction.append("S");
        
        if (originPos.getX() > playerPos.getX()) direction.append("E");
        else if (originPos.getX() < playerPos.getX()) direction.append("W");
        
        int distance = (int) Math.sqrt(playerPos.distSqr(originPos));
        return direction + " " + distance + "m";
    }
    
    private List<UIComponent> createTownInfoComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new DataLabelComponent(() -> "Town: " + menu.getTownName(), 0xFFFFFF, 200));
        
        // Display population and tourist count separately
        comps.add(new DataLabelComponent(() -> "Population: " + menu.getPopulation(), 0xFFFFFF, 200));
        comps.add(new DataLabelComponent(() -> "Tourists: " + menu.getTouristCount() + "/" + menu.getMaxTourists(), 0xFFFFFF, 200));
        
        return comps;
    }
    
    private List<UIComponent> createResourceComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new StateResourceListComponent(state));
        comps.add(new SlotComponent());
        return comps;
    }
    
    private List<UIComponent> createHistoryComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        comps.add(new StateVisitHistoryComponent(state));
        return comps;
    }
    
    private List<UIComponent> createSettingsComponents(TownBlockMenu menu) {
        List<UIComponent> comps = new ArrayList<>();
        
        int buttonWidth = 150;
        int buttonHeight = 20;
        
        // Platforms button
        comps.add(new ToggleButtonComponent(0, 0, buttonWidth, buttonHeight,
            Component.translatable("businesscraft.platforms_tab"),
            button -> showPlatformsTab()
        ));
        
        // Radius button - we need to capture the button parameter in mouseClicked
        comps.add(new DataBoundButtonComponent(
            () -> Component.literal("Radius: " + menu.getSearchRadius()),
            (button) -> handleRadiusChange(0), // 0 = left click by default
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
        
        // Clear previous widgets to prevent lingering buttons
        this.clearWidgets();
        
        // Initialize the main UI components
        components.forEach(component -> 
            component.init(this::addRenderableWidget)
        );
        
        // Initialize the name editor
        nameEditor.init(this::addRenderableWidget);
        
        // Always create a fresh platforms tab instance
        platformsTab = new PlatformsTab(this);
        
        // Initialize it but keep it hidden
        platformsTab.init(leftPos, topPos, imageWidth, imageHeight);
        platformsTab.setVisible(false);  // Hidden by default
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Update state from menu each render
        updateStateFromMenu();
        
        // Clean up platform buttons if needed
        if (platformsTab != null && !platformsTab.isVisible()) {
            // Check for and remove any buttons from platform entry positions
            for (GuiEventListener child : new ArrayList<>(this.children())) {
                if (child instanceof Button button) {
                    // Check button position - if it's in the platform content area and not a tab button
                    int tabY = topPos + 5;
                    int tabHeight = 20;
                    boolean isTabButton = button.getY() >= tabY && button.getY() <= tabY + tabHeight;
                    
                    // Check if button is in platforms position range
                    boolean isInPlatformArea = button.getY() > topPos + 30 && 
                                            button.getX() > leftPos + 120;
                    
                    if (!isTabButton && isInPlatformArea && platformsTab != null && !platformsTab.isVisible()) {
                        // Stray platform button detected, remove it
                        this.removeWidget(button);
                    }
                }
            }
        }
        
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
        
        // If showing platforms tab, render it with a clean background
        if (platformsTab != null && platformsTab.isVisible()) {
            // Draw a dark gradient background for the entire screen
            renderDarkBackground(guiGraphics);
            
            // Draw just the title at the top
            guiGraphics.drawCenteredString(
                this.font, 
                this.title, 
                this.width / 2, 
                topPos - 15, 
                0xFFFFFF
            );
            
            // Render just the tab component with tabs - with enhanced styling
            TabComponent tabComponent = (TabComponent) components.get(0);
            
            // Draw tab background
            int tabY = topPos + 5;
            int tabHeight = 20;
            int tabWidth = tabComponent.getWidth();
            guiGraphics.fill(leftPos + 8, tabY, leftPos + 8 + tabWidth, tabY + tabHeight, 0x60000000);
            
            // Render the tabs
            tabComponent.renderTabsOnly(guiGraphics, leftPos + 8, topPos + 5, mouseX, mouseY);
            
            // Render the platforms tab content
            platformsTab.render(guiGraphics, mouseX, mouseY, partialTicks);
            
            // Render instruction text if present (for platform path setting)
            if (instructionsText != null) {
                drawInstructionText(guiGraphics, instructionsText);
            }
            
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        
        // Normal rendering for main UI
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        
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
        
        // Render instruction text if present (for tourist path setting)
        if (instructionsText != null) {
            guiGraphics.drawCenteredString(
                Minecraft.getInstance().font,
                instructionsText,
                this.width / 2,
                20,
                0xFFFFFF
            );
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
        // Handle radius button right-click
        for (GuiEventListener listener : children()) {
            if (listener instanceof Button uiButton && uiButton.isMouseOver(mouseX, mouseY)) {
                // Check if it's the radius button by checking its message
                Component message = ((Button)listener).getMessage();
                if (message != null && message.getString().startsWith("Radius:")) {
                    handleRadiusChange(button);
                    return true;
                }
            }
        }
        
        // Handle platform tab clicks if it's visible
        if (platformsTab != null && platformsTab.isVisible()) {
            // Check if a tab was clicked first
            TabComponent tabComponent = (TabComponent) components.get(0);
            int tabWidth = tabComponent.getWidth() / 4; // Assuming 4 tabs
            int tabHeight = 20;
            int tabY = topPos + 5;
            
            // Check if click was in the tab area
            if (mouseY >= tabY && mouseY <= tabY + tabHeight) {
                for (int i = 0; i < 4; i++) { // For each tab
                    int tabX = leftPos + 8 + (i * tabWidth);
                    if (mouseX >= tabX && mouseX <= tabX + tabWidth) {
                        // A tab was clicked, exit platforms view and switch to that tab
                        showMainTab();
                        
                        // Find and click the actual tab button to trigger its event
                        for (GuiEventListener listener : children()) {
                            if (listener instanceof Button tabButton) {
                                if (tabButton.isMouseOver(mouseX, mouseY)) {
                                    tabButton.onPress();
                                    return true;
                                }
                            }
                        }
                        return true;
                    }
                }
            }
            
            // If not a tab click, handle platform tab UI
            if (platformsTab.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        
        // Handle town name editor clicks if it's visible
        if (isEditingTownName) {
            // Get button positions and dimensions for direct click detection
            Button confirmButton = nameEditor.getConfirmButton();
            Button cancelButton = nameEditor.getCancelButton();
            
            // Check for direct clicks on buttons using their screen coordinates
            if (confirmButton.visible && mouseX >= confirmButton.getX() && mouseX <= confirmButton.getX() + confirmButton.getWidth() &&
                mouseY >= confirmButton.getY() && mouseY <= confirmButton.getY() + confirmButton.getHeight()) {
                nameEditor.handleConfirmButtonClick();
                return true;
            }
            
            if (cancelButton.visible && mouseX >= cancelButton.getX() && mouseX <= cancelButton.getX() + cancelButton.getWidth() &&
                mouseY >= cancelButton.getY() && mouseY <= cancelButton.getY() + cancelButton.getHeight()) {
                nameEditor.handleCancelButtonClick();
                return true;
            }
            
            if (isClickInsideEditor(mouseX, mouseY)) {
                // If not a direct button press, try forwarding to component with correct coordinates
                if (nameEditor.mouseClicked(mouseX - (leftPos + 28), mouseY - (topPos + 50), button)) {
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            } else {
                // Click outside editor cancels
                handleEditCancelled();
                return true;
            }
        }
        
        // Platform path creation mode
        if (isSettingPlatformPath && platformBeingEdited != null) {
            // Forward the click to the game for block selection
            return false;
        }
        
        // Regular path creation mode
        if (isInPathCreationMode) {
            // Forward the click to the game for block selection
            return false;
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

    private void handleRadiusChange(int mouseButton) {
        // Get the current radius from the menu's data provider if possible
        int currentRadius = menu.getSearchRadius();
        int newRadius = currentRadius;
        
        // Calculate new radius based on key combinations
        boolean isShift = hasShiftDown();
        
        // Use mouseButton to determine increase/decrease
        // mouseButton 0 = left click (increase), 1 = right click (decrease)
        boolean isDecrease = (mouseButton == 1);
        
        if (isShift && isDecrease) {
            newRadius -= 10;
        } else if (isDecrease) {
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
        
        // Get the current town name
        String currentTownName = menu.getTownName();
        
        // Set the text in the edit box and focus it
        EditBoxComponent editBox = nameEditor.getEditBox();
        if (editBox != null) {
            editBox.setText(currentTownName);
            editBox.setFocused(true);
        }
    }
    
    private void handleTownNameConfirmed(String newName) {
        // Send packet to update town name
        ModMessages.sendToServer(new SetTownNamePacket(
            menu.getBlockEntity().getBlockPos(), 
            newName
        ));
        
        // Provide immediate client-side feedback
        TownBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity != null && minecraft.level.isClientSide) {
            blockEntity.setClientTownName(newName);
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

    /**
     * Shows the platforms tab for platform management
     */
    public void showPlatformsTab() {
        // Always create a fresh platforms tab instance to avoid state issues
        platformsTab = new PlatformsTab(this);
        
        // Force a refresh of the block entity's platforms from server data
        TownBlockEntity blockEntity = menu.getBlockEntity();
        
        // Hide all main UI buttons
        hideAllMainButtons();
        
        // Add platform tab components to the screen
        addPlatformComponentsToScreen();
        
        // Refresh the platforms data
        refreshPlatforms();
        
        // Set a title for the platforms view
        setInstructionsText(Component.translatable("businesscraft.manage_platforms"));
    }
    
    /**
     * Adds platform tab components to the screen
     */
    private void addPlatformComponentsToScreen() {
        if (platformsTab != null) {
            // Make sure to clear any existing platform components first
            clearPlatformButtons();
            
            // Initialize and make visible
            platformsTab.init(leftPos, topPos, imageWidth, imageHeight);
            platformsTab.setVisible(true);
        }
    }
    
    /**
     * Clears all platform-specific buttons from the screen
     */
    private void clearPlatformButtons() {
        // Create a copy of children to avoid concurrent modification
        List<GuiEventListener> children = new ArrayList<>(this.children());
        
        // Remove platform-specific buttons (keeping only the core UI elements)
        for (GuiEventListener listener : children) {
            if (listener instanceof Button) {
                // Preserve tab buttons by checking their position
                boolean isTabButton = false;
                if (listener instanceof Button button) {
                    int tabY = topPos + 5;
                    int tabHeight = 20;
                    if (button.getY() >= tabY && button.getY() <= tabY + tabHeight) {
                        isTabButton = true;
                    }
                }
                
                if (!isTabButton) {
                    this.removeWidget(listener);
                }
            }
        }
    }
    
    /**
     * Shows the main tab, hiding any specialized tabs
     */
    public void showMainTab() {
        // Always clear ALL widgets first for a clean slate
        this.clearWidgets();
        
        // Make sure platforms tab cleans up its buttons
        if (platformsTab != null) {
            platformsTab.cleanupButtons();
            platformsTab.setVisible(false);
            platformsTab = null; // Completely remove the platforms tab reference
        }
        
        // Clear any path setting mode
        isInPathCreationMode = false;
        isSettingPlatformPath = false;
        platformBeingEdited = null;
        
        // Hide any instructions
        instructionsText = null;
        
        // Get the active tab ID
        String activeTabId = "town";
        if (!components.isEmpty() && components.get(0) instanceof TabComponent) {
            TabComponent tabComponent = (TabComponent) components.get(0);
            activeTabId = tabComponent.getActiveTabId();
        }
        
        // Completely reset UI
        // First clear references to all components
        components.clear();
        
        // Set up the UI from scratch as if it's the first initialization
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
        
        // Initialize everything
        components.forEach(component -> {
            component.setVisible(true);
            component.init(this::addRenderableWidget);
        });
        
        // Now we need to find and click the correct tab button to activate it
        if (!activeTabId.equals("town")) {
            // Find the tab button based on the stored ID
            int tabIndex = 0; // Default to first tab
            if (activeTabId.equals("resources")) tabIndex = 1;
            else if (activeTabId.equals("history")) tabIndex = 2;
            else if (activeTabId.equals("settings")) tabIndex = 3;
            
            // Find the tab button in the children list and click it
            int finalTabIndex = tabIndex;
            List<GuiEventListener> allChildren = new ArrayList<>(this.children());
            allChildren.stream()
                .filter(child -> child instanceof Button)
                .map(child -> (Button)child)
                .filter(button -> {
                    // Identify buttons in the tab row (this is an approximation)
                    int tabY = topPos + 5;
                    int tabHeight = 20;
                    return button.getY() >= tabY && button.getY() <= tabY + tabHeight;
                })
                .skip(finalTabIndex) // Skip to the tab we want
                .findFirst()
                .ifPresent(Button::onPress); // Simulate click
        }
    }
    
    /**
     * Shows the path editor for a specific platform
     */
    public void showPathEditor(UUID platformId) {
        // Set the path creation mode
        isSettingPlatformPath = true;
        platformBeingEdited = platformId;
        
        // Set instructions
        setInstructionsText(Component.translatable("businesscraft.platform_path_instructions"));
        
        // Hide the platforms tab
        if (platformsTab != null) {
            platformsTab.setVisible(false);
        }
        
        // Register the platform with the key handler so it can detect ESC key
        PlatformPathKeyHandler.setActivePlatform(
            menu.getBlockEntity().getBlockPos(),
            platformId
        );
        
        // Close the screen so the player can interact with the world
        // We'll send a packet to the server to enter platform path creation mode
        ModMessages.sendToServer(new SetPlatformPathCreationModePacket(
            menu.getBlockEntity().getBlockPos(),
            platformId,
            true
        ));
        
        // Close the UI to allow world interaction
        this.onClose();
    }
    
    /**
     * Refreshes the platforms display
     */
    public void refreshPlatforms() {
        if (platformsTab != null) {
            platformsTab.refreshPlatforms();
        }
    }
    
    /**
     * Gets the block position of the town block
     */
    public BlockPos getBlockPos() {
        return menu.getBlockEntity().getBlockPos();
    }
    
    /**
     * Gets the list of platforms from the town block
     */
    public List<Platform> getPlatforms() {
        TownBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity != null) {
            return blockEntity.getPlatforms();
        }
        return new ArrayList<>();
    }

    /**
     * Hides all main UI buttons when in special modes
     */
    private void hideAllMainButtons() {
        // Store current children for later restoration
        List<GuiEventListener> currentChildren = new ArrayList<>(this.children());
        
        // Clear all children
        this.clearWidgets();
        
        // Re-add only the tab component buttons to allow navigation
        TabComponent tabComponent = (TabComponent) components.get(0);
        tabComponent.readdTabButtons(this::addRenderableWidget);
        
        // Keep the tab component in our UI list but hide its content
        components.get(0).setVisible(false);
        
        // This will trigger a re-init of platform tab buttons
        if (platformsTab != null) {
            platformsTab.init(leftPos, topPos, imageWidth, imageHeight);
        }
    }
    
    /**
     * Shows all main UI buttons when returning to normal mode
     */
    private void showAllMainButtons() {
        // Re-initialize all components from scratch
        this.clearWidgets();
        
        // Re-initialize everything
        components.forEach(component -> {
            component.setVisible(true);
            component.init(this::addRenderableWidget);
        });
        
        // Ensure platforms tab is not visible
        if (platformsTab != null) {
            platformsTab.setVisible(false);
        }
        
        // Clear instructions
        instructionsText = null;
    }
    
    /**
     * Sets instruction text to display at the top of the screen
     */
    private void setInstructionsText(Component text) {
        this.instructionsText = text;
    }

    /**
     * Allow platform tab to register its buttons
     */
    public <T extends Button> T addPlatformButton(T button) {
        return this.addRenderableWidget(button);
    }

    /**
     * Allow removing a button from the screen
     */
    public void removePlatformButton(GuiEventListener button) {
        this.removeWidget(button);
    }

    /**
     * Renders a dark background for special screens
     */
    private void renderDarkBackground(GuiGraphics guiGraphics) {
        // Fill the entire screen with a dark gradient
        guiGraphics.fill(0, 0, this.width, this.height, 0x80000000);
        
        // Draw a subtle border around the main content area
        int borderColor = 0x40AAAAAA;
        int contentX = leftPos - 5;
        int contentY = topPos - 20;
        int contentWidth = imageWidth + 10;
        int contentHeight = imageHeight + 30;
        
        // Border lines
        guiGraphics.fill(contentX, contentY, contentX + contentWidth, contentY + 1, borderColor);
        guiGraphics.fill(contentX, contentY, contentX + 1, contentY + contentHeight, borderColor);
        guiGraphics.fill(contentX, contentY + contentHeight - 1, contentX + contentWidth, contentY + contentHeight, borderColor);
        guiGraphics.fill(contentX + contentWidth - 1, contentY, contentX + contentWidth, contentY + contentHeight, borderColor);
    }
    
    /**
     * Draws instruction text with enhanced styling
     */
    private void drawInstructionText(GuiGraphics guiGraphics, Component text) {
        int textWidth = this.font.width(text);
        int textX = this.width / 2 - textWidth / 2;
        int textY = 20;
        
        // Draw background
        guiGraphics.fill(textX - 5, textY - 2, textX + textWidth + 5, textY + 12, 0xA0000000);
        
        // Draw text with shadow
        guiGraphics.drawString(
            this.font,
            text,
            textX,
            textY,
            0xFFFFDD,
            true // with shadow
        );
    }

    /**
     * Called when the screen is closed
     */
    @Override
    public void onClose() {
        // If not in path creation mode, send packet to show extended platform indicators
        if (!isInPathCreationMode && !isSettingPlatformPath) {
            ModMessages.sendToServer(new PlayerExitUIPacket(menu.getBlockEntity().getBlockPos()));
        }
        
        super.onClose();
    }
}