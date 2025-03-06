package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;

/**
 * The Town Interface Screen showcases the BusinessCraft UI system capabilities.
 * This screen demonstrates various UI components and layouts using the enhanced BCTabPanel.
 */
public class TownInterfaceScreen extends AbstractContainerScreen<TownInterfaceMenu> {
    private BCTabPanel tabPanel;
    private BCTheme customTheme;

    // UI colors - lighter and more visible
    private static final int PRIMARY_COLOR = 0xA0335599;       // Semi-transparent blue
    private static final int SECONDARY_COLOR = 0xA0884466;     // Semi-transparent purple
    private static final int BACKGROUND_COLOR = 0x80222222;    // Semi-transparent dark gray
    private static final int BORDER_COLOR = 0xA0AAAAAA;        // Light gray
    private static final int ACTIVE_TAB_COLOR = 0xA0CCDDFF;    // Light blue for active tab
    private static final int INACTIVE_TAB_COLOR = 0x80555555;  // Medium gray for inactive tabs
    private static final int TEXT_COLOR = 0xFFFFFFFF;          // White text
    private static final int TEXT_HIGHLIGHT = 0xFFDDFFFF;      // Light cyan highlight text

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set custom dimensions for the screen
        this.imageWidth = 256;
        this.imageHeight = 204;
        
        // Move the inventory label off-screen to hide it
        this.inventoryLabelY = 300;  // Position it below the visible area
        
        // Create a custom theme for this screen with lighter colors
        customTheme = BCTheme.builder()
            .primaryColor(PRIMARY_COLOR)
            .secondaryColor(SECONDARY_COLOR)
            .successColor(0xA0339944)
            .dangerColor(0xA0993333)
            .textLight(TEXT_COLOR)
            .textDark(0xFF202020)
            .panelBackground(BACKGROUND_COLOR)
            .panelBorder(BORDER_COLOR)
            .roundedCorners(true)
            .build();
    }

    @Override
    protected void init() {
        super.init();
        
        // Apply our custom theme for this screen
        BCTheme.setActiveTheme(customTheme);
        
        // Create tab panel with proper dimensions
        int tabPanelWidth = this.imageWidth - 20;
        int tabPanelHeight = this.imageHeight - 20;
        this.tabPanel = new BCTabPanel(tabPanelWidth, tabPanelHeight, 20);
        
        // Position tab panel properly within the screen
        this.tabPanel.position(this.leftPos + 10, this.topPos + 10);
        
        // Set tab styling with our lighter colors
        this.tabPanel.withTabStyle(ACTIVE_TAB_COLOR, INACTIVE_TAB_COLOR, TEXT_COLOR)
                     .withTabBorder(true, BORDER_COLOR)
                     .withContentStyle(BACKGROUND_COLOR, BORDER_COLOR);
        
        // Create and configure tabs with proper spacing
        createOverviewTab();
        createEconomyTab();
        createPopulationTab();
        createSettingsTab();
        
        // Make sure Overview is the default active tab
        if (this.tabPanel.getActiveTabId() == null) {
            this.tabPanel.setActiveTab("overview");
        }
        
        // Initialize the tab panel
        this.tabPanel.init(this::addRenderableWidget);
    }
    
    private void createOverviewTab() {
        // Create panel with correct dimensions for content area
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Create the welcome header with animation
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("WELCOME TO YOUR TOWN", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT)
                  .withShadow(true)
                  .withAlpha(0.0f);
        
        // Add animation to fade in the title
        panel.addChild(titleLabel);
        titleLabel.animate("alpha", 1.0f, 500);
        
        // Create an info panel with town details
        BCPanel infoPanel = BCComponentFactory.createContainerPanel(panel.getInnerWidth(), 130);
        infoPanel.withCornerRadius(5)
                 .withBackgroundColor(BACKGROUND_COLOR)
                 .withBorderColor(BORDER_COLOR);
        infoPanel.withLayout(new BCGridLayout(1, 10, 10));
        
        // Add town info items
        BCLabel townNameLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Town Name: " + getTownName()),
            panel.getInnerWidth() - 20
        );
        townNameLabel.withTextColor(TEXT_COLOR).withShadow(true);
        infoPanel.addChild(townNameLabel);
        
        BCLabel mayorLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Mayor: " + getMayorName()),
            panel.getInnerWidth() - 20
        );
        mayorLabel.withTextColor(TEXT_COLOR).withShadow(true);
        infoPanel.addChild(mayorLabel);
        
        BCLabel populationLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Population: " + getTownPopulation()),
            panel.getInnerWidth() - 20
        );
        populationLabel.withTextColor(TEXT_COLOR).withShadow(true);
        infoPanel.addChild(populationLabel);
        
        // Add info panel to main panel with fade-in animation
        infoPanel.withAlpha(0.0f);
        panel.addChild(infoPanel);
        infoPanel.animate("alpha", 1.0f, 800);
        
        // Add action buttons panel
        BCPanel buttonPanel = new BCPanel(panel.getInnerWidth(), 30);
        buttonPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 10));
        
        // Add action buttons
        BCButton editButton = BCComponentFactory.createPrimaryButton("Edit Details", b -> {
            // Implement edit functionality
        }, 100);
        buttonPanel.addChild(editButton);
        
        BCButton visitButton = BCComponentFactory.createSecondaryButton("Visit Center", b -> {
            // Implement visit functionality
        }, 100);
        buttonPanel.addChild(visitButton);
        
        buttonPanel.withAlpha(0.0f);
        panel.addChild(buttonPanel);
        buttonPanel.animate("alpha", 1.0f, 1000);
        
        // Add the panel to the tab
        this.tabPanel.addTab("overview", Component.literal("Overview"), panel);
    }
    
    private void createEconomyTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("ECONOMY", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Create a panel for resource list
        BCPanel resourcePanel = new BCPanel(panel.getInnerWidth(), 120);
        resourcePanel.withPadding(5)
                     .withBackgroundColor(BACKGROUND_COLOR)
                     .withBorderColor(BORDER_COLOR)
                     .withCornerRadius(3);
        
        // Set grid layout for resources
        resourcePanel.withLayout(new BCGridLayout(2, 5, 5));
        
        // Add resource items
        String[] resources = {"Gold", "Wood", "Stone", "Food", "Iron", "Coal", "Gems", "Oil"};
        int[] amounts = {1250, 842, 1500, 750, 325, 980, 147, 520};
        
        for (int i = 0; i < resources.length; i++) {
            // Resource name
            BCLabel nameLabel = BCComponentFactory.createBodyLabel(resources[i], 100);
            nameLabel.withTextColor(TEXT_HIGHLIGHT);
            resourcePanel.addChild(nameLabel);
            
            // Resource amount
            BCLabel amountLabel = BCComponentFactory.createBodyLabel(String.valueOf(amounts[i]), 80);
            amountLabel.withTextColor(TEXT_COLOR);
            amountLabel.withAlignment(BCLabel.TextAlignment.RIGHT);
            resourcePanel.addChild(amountLabel);
        }
        
        panel.addChild(resourcePanel);
        
        // Add resource controls
        BCPanel controlPanel = new BCPanel(panel.getInnerWidth(), 30);
        controlPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 10));
        
        BCButton tradeButton = BCComponentFactory.createPrimaryButton("Trade Resources", b -> {
            // Implement trade functionality
        }, 120);
        controlPanel.addChild(tradeButton);
        
        BCButton manageButton = BCComponentFactory.createSecondaryButton("Manage Storage", b -> {
            // Implement storage management
        }, 120);
        controlPanel.addChild(manageButton);
        
        panel.addChild(controlPanel);
        
        // Add the panel to the tab
        this.tabPanel.addTab("economy", Component.literal("Economy"), panel);
    }
    
    private void createPopulationTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("POPULATION", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Create citizen panel with lighter border and background
        BCPanel citizenPanel = new BCPanel(panel.getInnerWidth(), 120);
        citizenPanel.withPadding(8)
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withCornerRadius(3);
        
        // Add grid layout with improved spacing
        citizenPanel.withLayout(new BCGridLayout(3, 8, 8));
        
        // Sample citizen data
        String[] names = {"John Smith", "Emma Johnson", "Alex Lee", "Sofia Garcia", "Michael Brown", "Lisa Wang", "David Miller"};
        String[] jobs = {"Miner", "Farmer", "Builder", "Trader", "Blacksmith", "Scholar", "Guard"};
        int[] levels = {3, 2, 4, 1, 5, 2, 3};
        
        // Add citizen entries with better styling
        for (int i = 0; i < names.length; i++) {
            // Name
            BCLabel nameLabel = BCComponentFactory.createBodyLabel(names[i], 100);
            nameLabel.withTextColor(TEXT_HIGHLIGHT);
            citizenPanel.addChild(nameLabel);
            
            // Job
            BCLabel jobLabel = BCComponentFactory.createBodyLabel(jobs[i], 80);
            jobLabel.withTextColor(TEXT_COLOR);
            citizenPanel.addChild(jobLabel);
            
            // Level
            BCLabel levelLabel = BCComponentFactory.createBodyLabel("Level " + levels[i], 60);
            levelLabel.withAlignment(BCLabel.TextAlignment.RIGHT);
            levelLabel.withTextColor(TEXT_COLOR);
            citizenPanel.addChild(levelLabel);
        }
        
        panel.addChild(citizenPanel);
        
        // Create population controls
        BCPanel controlPanel = new BCPanel(panel.getInnerWidth(), 30);
        controlPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 10));
        
        // Add control buttons
        BCButton assignButton = BCComponentFactory.createPrimaryButton("Assign Jobs", b -> {
            // Implement job assignment
        }, 100);
        controlPanel.addChild(assignButton);
        
        BCButton recruitButton = BCComponentFactory.createSecondaryButton("Recruit Citizens", b -> {
            // Implement recruitment
        }, 120);
        controlPanel.addChild(recruitButton);
        
        panel.addChild(controlPanel);
        
        // Add the panel to the tab
        this.tabPanel.addTab("population", Component.literal("Population"), panel);
    }
    
    private void createSettingsTab() {
        // Create panel for content
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.withPadding(10)
             .withBackgroundColor(0x00000000) // Transparent background
             .withCornerRadius(3);
        
        // Create a flow layout for the panel
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
        
        // Add title
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("SETTINGS", panel.getInnerWidth());
        titleLabel.withTextColor(TEXT_HIGHLIGHT).withShadow(true);
        panel.addChild(titleLabel);
        
        // Create settings form with better styling
        BCPanel settingsForm = new BCPanel(panel.getInnerWidth(), 140);
        settingsForm.withLayout(new BCGridLayout(2, 8, 8))
                    .withBackgroundColor(BACKGROUND_COLOR)
                    .withBorderColor(BORDER_COLOR)
                    .withCornerRadius(5);
        
        // Add settings
        // Town name setting
        BCLabel nameLabel = BCComponentFactory.createBodyLabel("Town Name:", 100);
        nameLabel.withTextColor(TEXT_COLOR);
        settingsForm.addChild(nameLabel);
        
        // Create a simple edit box component
        BCLabel nameValueLabel = BCComponentFactory.createBodyLabel(getTownName(), 150);
        nameValueLabel.withTextColor(TEXT_HIGHLIGHT);
        settingsForm.addChild(nameValueLabel);
        
        // Tax rate setting
        BCLabel taxLabel = BCComponentFactory.createBodyLabel("Tax Rate:", 100);
        taxLabel.withTextColor(TEXT_COLOR);
        settingsForm.addChild(taxLabel);
        
        // Create a simple value label
        BCLabel taxValueLabel = BCComponentFactory.createBodyLabel("5%", 150);
        taxValueLabel.withTextColor(TEXT_HIGHLIGHT);
        settingsForm.addChild(taxValueLabel);
        
        // PvP toggle
        BCLabel pvpLabel = BCComponentFactory.createBodyLabel("PvP Enabled:", 100);
        pvpLabel.withTextColor(TEXT_COLOR);
        settingsForm.addChild(pvpLabel);
        
        // Create a button instead of a toggle
        BCButton pvpButton = BCComponentFactory.createSecondaryButton("Disabled", b -> {
            // Toggle PvP setting
        }, 80);
        settingsForm.addChild(pvpButton);
        
        // Public toggle
        BCLabel publicLabel = BCComponentFactory.createBodyLabel("Public Town:", 100);
        publicLabel.withTextColor(TEXT_COLOR);
        settingsForm.addChild(publicLabel);
        
        // Create a button instead of a toggle
        BCButton publicButton = BCComponentFactory.createPrimaryButton("Enabled", b -> {
            // Toggle public setting
        }, 80);
        settingsForm.addChild(publicButton);
        
        panel.addChild(settingsForm);
        
        // Add control buttons
        BCPanel controlPanel = new BCPanel(panel.getInnerWidth(), 30);
        controlPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 10));
        
        BCButton saveButton = BCComponentFactory.createSuccessButton("Save Settings", b -> {
            // Save settings
        }, 120);
        controlPanel.addChild(saveButton);
        
        BCButton resetButton = BCComponentFactory.createDangerButton("Reset Defaults", b -> {
            // Reset to defaults
        }, 120);
        controlPanel.addChild(resetButton);
        
        panel.addChild(controlPanel);
        
        // Add the panel to the tab
        this.tabPanel.addTab("settings", Component.literal("Settings"), panel);
    }
    
    // Helper methods to get data from the menu
    private String getTownName() {
        return "Prosperityville";
    }
    
    private String getMayorName() {
        return "Mayor Goodway";
    }
    
    private int getTownPopulation() {
        return 42;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Draw the dimmed background
        this.renderBackground(graphics);
        
        // Draw a semi-transparent background for the entire window
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0x80222222);
        
        // Draw border
        graphics.hLine(this.leftPos, this.leftPos + this.imageWidth - 1, this.topPos, BORDER_COLOR);
        graphics.hLine(this.leftPos, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, BORDER_COLOR);
        graphics.vLine(this.leftPos, this.topPos, this.topPos + this.imageHeight - 1, BORDER_COLOR);
        graphics.vLine(this.leftPos + this.imageWidth - 1, this.topPos, this.topPos + this.imageHeight - 1, BORDER_COLOR);
        
        // Render the tab panel
        this.tabPanel.render(graphics, this.tabPanel.getX(), this.tabPanel.getY(), mouseX, mouseY);
        
        // Render the screen title
        graphics.drawCenteredString(this.font, this.title, this.leftPos + this.imageWidth / 2, this.topPos - 12, TEXT_COLOR);
        
        // Draw any tooltips last (so they appear on top)
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);  // Move tooltips to front layer
        this.renderTooltip(graphics, mouseX, mouseY);
        graphics.pose().popPose();
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Background rendering is handled in render() method
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Let the tab panel handle clicks first
        if (this.tabPanel.mouseClicked(mouseX, mouseY, button)) {
                    return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Let the tab panel handle drags first
        if (this.tabPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Let the tab panel handle releases first
        if (this.tabPanel.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // Let the tab panel handle scroll events
        if (this.tabPanel.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
} 