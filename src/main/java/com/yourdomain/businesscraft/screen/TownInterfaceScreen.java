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
 * This screen demonstrates various UI components and layouts using the BCTabPanel.
 */
public class TownInterfaceScreen extends AbstractContainerScreen<TownInterfaceMenu> {
    private BCTabPanel tabPanel;
    private List<BCPanel> tabPanels = new ArrayList<>();

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Set custom dimensions for the screen
        this.imageWidth = 256;
        this.imageHeight = 204;
        
        // Move the inventory label off-screen to hide it
        this.inventoryLabelY = 300;  // Position it below the visible area
    }

    @Override
    protected void init() {
        super.init();
        
        // Create tab panel with proper dimensions
        int tabPanelWidth = this.imageWidth - 20;
        int tabPanelHeight = this.imageHeight - 20;
        this.tabPanel = new BCTabPanel(tabPanelWidth, tabPanelHeight, 20);
        
        // Position tab panel properly within the screen
        this.tabPanel.position(this.leftPos + 10, this.topPos + 10);
        
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
        // Create panel with correct dimensions for content area (below tab buttons)
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.position(0, 20); // Position relative to tab panel, below tab buttons
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        panel.withBackgroundColor(0x00000000); // Completely transparent background
        
        // Add large centered title with high contrast
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("OVERVIEW TAB", this.tabPanel.getWidth() - 20);
        titleLabel.position(10, 10);
        titleLabel.withTextColor(0xFFFFFF); // Bright white text
        titleLabel.withShadow(true); // Add shadow for better visibility
        panel.addChild(titleLabel);
        
        // Create an info panel with town details
        BCPanel infoPanel = new BCPanel(210, 130);
        infoPanel.position(5, 40); // Add some extra space after title
        infoPanel.withBackgroundColor(0x40000000); // Semi-transparent background
        infoPanel.withBorderColor(0x80FFFFFF);
        infoPanel.withPadding(5);
        
        // Use a grid layout for town info
        BCGridLayout infoLayout = new BCGridLayout(1, 3, 3);
        infoPanel.withLayout(infoLayout);
        
        // Add town info items in order - make text brighter with shadows
        BCLabel townNameLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Town Name: " + menu.getTownName()),
            200
        );
        townNameLabel.withTextColor(0xFFFFFF);
        townNameLabel.withShadow(true);
        infoPanel.addChild(townNameLabel);
        
        BCLabel mayorLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Mayor: " + menu.getMayorName()),
            200
        );
        mayorLabel.withTextColor(0xFFFFFF);
        mayorLabel.withShadow(true);
        infoPanel.addChild(mayorLabel);
        
        BCLabel levelLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Town Level: " + menu.getTownLevel()),
            200
        );
        levelLabel.withTextColor(0xFFFFFF);
        levelLabel.withShadow(true);
        infoPanel.addChild(levelLabel);
        
        BCLabel popLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Population: " + menu.getTownPopulation()),
            200
        );
        popLabel.withTextColor(0xFFFFFF);
        popLabel.withShadow(true);
        infoPanel.addChild(popLabel);
        
        BCLabel repLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Reputation: " + menu.getTownReputation() + "%"),
            200
        );
        repLabel.withTextColor(0xFFFFFF);
        repLabel.withShadow(true);
        infoPanel.addChild(repLabel);
        
        BCLabel treasuryLabel = BCComponentFactory.createDynamicLabel(
            () -> Component.literal("Treasury: " + menu.getGoldCoins() + "g " + 
                                 menu.getSilverCoins() + "s " + 
                                 menu.getBronzeCoins() + "b"),
            200
        );
        treasuryLabel.withTextColor(0xFFFFFF);
        treasuryLabel.withShadow(true);
        infoPanel.addChild(treasuryLabel);
        
        panel.addChild(infoPanel);
        
        // Add tab to panel - make sure only the first tab is visible initially
        panel.setVisible("overview".equals(this.tabPanel.getActiveTabId()) || this.tabPanel.getActiveTabId() == null);
        this.tabPanel.addTab("overview", Component.literal("Overview"), panel);
    }
    
    private void createEconomyTab() {
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.position(0, 20); // Position relative to tab panel, below tab buttons
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        panel.withBackgroundColor(0x00000000); // Completely transparent background
        
        // Add large centered title with high visibility
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("ECONOMY TAB", this.tabPanel.getWidth() - 20);
        titleLabel.position(10, 10);
        titleLabel.withTextColor(0xFFFFFF); // Bright white text
        titleLabel.withShadow(true); // Add shadow for better visibility
        panel.addChild(titleLabel);
        
        // Create a panel for resource list with slight transparency
        BCPanel resourcesPanel = new BCPanel(210, 130);
        resourcesPanel.position(5, 40);
        resourcesPanel.withBackgroundColor(0x40000000); // Semi-transparent background
        resourcesPanel.withBorderColor(0x80FFFFFF);
        resourcesPanel.withPadding(5);
        
        // Create a grid layout with fixed positions
        BCGridLayout gridLayout = new BCGridLayout(3, 5, 5);
        resourcesPanel.withLayout(gridLayout);
        
        // Create resources list with high contrast
        DataLabelComponent woodLabel = BCComponentFactory.createDataLabel(() -> "Wood: 50", 0xFFFFFF, 65);
        resourcesPanel.addChild(woodLabel);
        
        DataLabelComponent stoneLabel = BCComponentFactory.createDataLabel(() -> "Stone: 35", 0xFFFFFF, 65);
        resourcesPanel.addChild(stoneLabel);
        
        DataLabelComponent ironLabel = BCComponentFactory.createDataLabel(() -> "Iron: 10", 0xFFFFFF, 65);
        resourcesPanel.addChild(ironLabel);
        
        DataLabelComponent coalLabel = BCComponentFactory.createDataLabel(() -> "Coal: 42", 0xFFFFFF, 65);
        resourcesPanel.addChild(coalLabel);
        
        DataLabelComponent goldLabel = BCComponentFactory.createDataLabel(() -> "Gold: 5", 0xFFFFFF, 65);
        resourcesPanel.addChild(goldLabel);
        
        DataLabelComponent foodLabel = BCComponentFactory.createDataLabel(() -> "Food: 120", 0xFFFFFF, 65);
        resourcesPanel.addChild(foodLabel);
        
        panel.addChild(resourcesPanel);
        
        // Set initial visibility
        panel.setVisible("economy".equals(this.tabPanel.getActiveTabId()));
        
        // Add tab
        this.tabPanel.addTab("economy", Component.literal("Economy"), panel);
    }
    
    private void createPopulationTab() {
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.position(0, 20); // Position relative to tab panel, below tab buttons
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        panel.withBackgroundColor(0x00000000); // Completely transparent background
        
        // Add large centered title with high visibility
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("POPULATION TAB", this.tabPanel.getWidth() - 20);
        titleLabel.position(10, 10);
        titleLabel.withTextColor(0xFFFFFF); // Bright white text
        titleLabel.withShadow(true); // Add shadow for better visibility
        panel.addChild(titleLabel);
        
        // Create a panel for citizen list with slight transparency
        BCPanel citizensPanel = new BCPanel(210, 130);
        citizensPanel.position(5, 40);
        citizensPanel.withBackgroundColor(0x40000000); // Semi-transparent background
        citizensPanel.withBorderColor(0x80FFFFFF);
        citizensPanel.withPadding(5);
        
        // Use a grid layout for citizens
        BCGridLayout citizensLayout = new BCGridLayout(2, 5, 5);
        citizensPanel.withLayout(citizensLayout);
        
        // Add citizens with high contrast text
        BCLabel john = BCComponentFactory.createBodyLabel("John Smith", 100);
        john.withTextColor(0xFFFFFF);
        john.withShadow(true);
        citizensPanel.addChild(john);
        
        BCLabel johnJob = BCComponentFactory.createBodyLabel("Farmer", 100);
        johnJob.withTextColor(0xFFFFFF);
        johnJob.withShadow(true);
        citizensPanel.addChild(johnJob);
        
        BCLabel mary = BCComponentFactory.createBodyLabel("Mary Johnson", 100);
        mary.withTextColor(0xFFFFFF);
        mary.withShadow(true);
        citizensPanel.addChild(mary);
        
        BCLabel maryJob = BCComponentFactory.createBodyLabel("Merchant", 100);
        maryJob.withTextColor(0xFFFFFF);
        maryJob.withShadow(true);
        citizensPanel.addChild(maryJob);
        
        BCLabel robert = BCComponentFactory.createBodyLabel("Robert Lee", 100);
        robert.withTextColor(0xFFFFFF);
        robert.withShadow(true);
        citizensPanel.addChild(robert);
        
        BCLabel robertJob = BCComponentFactory.createBodyLabel("Blacksmith", 100);
        robertJob.withTextColor(0xFFFFFF);
        robertJob.withShadow(true);
        citizensPanel.addChild(robertJob);
        
        BCLabel sarah = BCComponentFactory.createBodyLabel("Sarah Williams", 100);
        sarah.withTextColor(0xFFFFFF);
        sarah.withShadow(true);
        citizensPanel.addChild(sarah);
        
        BCLabel sarahJob = BCComponentFactory.createBodyLabel("Guard", 100);
        sarahJob.withTextColor(0xFFFFFF);
        sarahJob.withShadow(true);
        citizensPanel.addChild(sarahJob);
        
        BCLabel michael = BCComponentFactory.createBodyLabel("Michael Brown", 100);
        michael.withTextColor(0xFFFFFF);
        michael.withShadow(true);
        citizensPanel.addChild(michael);
        
        BCLabel michaelJob = BCComponentFactory.createBodyLabel("Miner", 100);
        michaelJob.withTextColor(0xFFFFFF);
        michaelJob.withShadow(true);
        citizensPanel.addChild(michaelJob);
        
        panel.addChild(citizensPanel);
        
        // Set initial visibility
        panel.setVisible("population".equals(this.tabPanel.getActiveTabId()));
        
        this.tabPanel.addTab("population", Component.literal("Population"), panel);
    }
    
    private void createSettingsTab() {
        BCPanel panel = new BCPanel(this.tabPanel.getWidth(), this.tabPanel.getHeight() - 20);
        panel.position(0, 20); // Position relative to tab panel, below tab buttons
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
        panel.withBackgroundColor(0x00000000); // Completely transparent background
        
        // Add large centered title with high visibility
        BCLabel titleLabel = BCComponentFactory.createHeaderLabel("SETTINGS TAB", this.tabPanel.getWidth() - 20);
        titleLabel.position(10, 10);
        titleLabel.withTextColor(0xFFFFFF); // Bright white text
        titleLabel.withShadow(true); // Add shadow for better visibility
        panel.addChild(titleLabel);
        
        // Create a panel for settings with slight transparency
        BCPanel settingsPanel = new BCPanel(210, 130);
        settingsPanel.position(5, 40);
        settingsPanel.withBackgroundColor(0x40000000); // Semi-transparent background
        settingsPanel.withBorderColor(0x80FFFFFF);
        settingsPanel.withPadding(5);
        
        // Use a grid layout for form elements
        BCGridLayout settingsLayout = new BCGridLayout(2, 10, 10);
        settingsPanel.withLayout(settingsLayout);
        
        // Town name setting - grid layout will position these
        BCLabel nameLabel = BCComponentFactory.createBodyLabel("Town Name:", 100);
        nameLabel.withTextColor(0xFFFFFF);
        nameLabel.withShadow(true);
        settingsPanel.addChild(nameLabel);
        
        settingsPanel.addChild(BCComponentFactory.createEditBox(
            100, 
            () -> menu.getTownName(), 
            text -> menu.setTownName(text), 
            32
        ));
        
        // Auto-collect setting
        BCLabel collectLabel = BCComponentFactory.createBodyLabel("Auto Collect:", 100);
        collectLabel.withTextColor(0xFFFFFF);
        collectLabel.withShadow(true);
        settingsPanel.addChild(collectLabel);
        
        settingsPanel.addChild(BCComponentFactory.createToggleButton(
            Component.literal(menu.isAutoCollectEnabled() ? "Enabled" : "Disabled"),
            button -> menu.setAutoCollectEnabled(!menu.isAutoCollectEnabled()),
            100
        ));
        
        // Taxes setting
        BCLabel taxLabel = BCComponentFactory.createBodyLabel("Taxes:", 100);
        taxLabel.withTextColor(0xFFFFFF);
        taxLabel.withShadow(true);
        settingsPanel.addChild(taxLabel);
        
        settingsPanel.addChild(BCComponentFactory.createToggleButton(
            Component.literal(menu.isTaxesEnabled() ? "Enabled" : "Disabled"),
            button -> menu.setTaxesEnabled(!menu.isTaxesEnabled()),
            100
        ));
        
        // Action buttons
        BCLabel actionLabel = BCComponentFactory.createBodyLabel("Actions:", 100);
        actionLabel.withTextColor(0xFFFFFF);
        actionLabel.withShadow(true);
        settingsPanel.addChild(actionLabel);
        
        settingsPanel.addChild(BCComponentFactory.createPrimaryButton(
            "Save Settings",
            button -> {
                // Already saved by the individual controls
            },
            100
        ));
        
        panel.addChild(settingsPanel);
        
        // Set initial visibility
        panel.setVisible("settings".equals(this.tabPanel.getActiveTabId()));
        
        this.tabPanel.addTab("settings", Component.literal("Settings"), panel);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // First render the background
        this.renderBackground(graphics);
        
        // Draw the main background
        renderBg(graphics, partialTicks, mouseX, mouseY);
        
        // Draw the title at the top of the container
        Component titleComponent = Component.literal("Town Interface");
        int titleWidth = this.font.width(titleComponent);
        graphics.drawString(this.font, titleComponent, this.leftPos + (this.imageWidth - titleWidth) / 2, this.topPos + 5, 0xFFFFFF);
        
        // Draw tab buttons
        if (this.tabPanel != null) {
            int tabX = this.leftPos + 10;
            int tabWidth = (this.imageWidth - 20) / 4; // 4 tabs
            int tabY = this.topPos + 20;
            
            // Draw tab buttons
            String[] tabNames = {"Overview", "Economy", "Population", "Settings"};
            String[] tabIds = {"overview", "economy", "population", "settings"};
            
            for (int i = 0; i < 4; i++) {
                // Highlight active tab
                if (tabIds[i].equals(this.tabPanel.getActiveTabId())) {
                    graphics.fill(tabX + (i * tabWidth), tabY, tabX + ((i+1) * tabWidth), tabY + 20, 0x80FFFFFF);
                }
                
                // Draw tab label
                String tabName = tabNames[i];
                int labelWidth = this.font.width(tabName);
                graphics.drawString(this.font, tabName, tabX + (i * tabWidth) + (tabWidth - labelWidth) / 2, tabY + 6, 0xFFFFFF);
            }
            
            // Draw content area background
            int contentX = this.leftPos + 10;
            int contentY = this.topPos + 45;
            int contentWidth = this.imageWidth - 20;
            int contentHeight = this.imageHeight - 55;
            graphics.fill(contentX, contentY, contentX + contentWidth, contentY + contentHeight, 0x60000000);
            
            // Draw content based on active tab
            String activeTabId = this.tabPanel.getActiveTabId();
            if (activeTabId != null) {
                // Create a scissor test to keep content within bounds
                enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
                
                int textStartX = contentX + 10; // Add padding from content area edge
                int textStartY = contentY + 10;
                
                // Draw tab title
                graphics.drawString(this.font, activeTabId.toUpperCase() + " TAB", textStartX, textStartY, 0xFFFFFF);
                
                // Draw content specific to each tab
                textStartY += 20; // Move down past the title
                
                if ("overview".equals(activeTabId)) {
                    drawOverviewContent(graphics, textStartX, textStartY);
                } else if ("economy".equals(activeTabId)) {
                    drawEconomyContent(graphics, textStartX, textStartY);
                } else if ("population".equals(activeTabId)) {
                    drawPopulationContent(graphics, textStartX, textStartY);
                } else if ("settings".equals(activeTabId)) {
                    drawSettingsContent(graphics, textStartX, textStartY);
                }
                
                disableScissor();
                
                // Draw the button AFTER all content and OUTSIDE the scissor test
                int buttonX = this.leftPos + (this.imageWidth / 2) - 30;
                int buttonY = this.topPos + this.imageHeight - 35;
                drawButton(graphics, buttonX, buttonY, 60, 20, activeTabId.substring(0, 3).toUpperCase());
            }
        }
        
        this.renderTooltip(graphics, mouseX, mouseY);
    }
    
    private void drawOverviewContent(GuiGraphics graphics, int x, int y) {
        // Draw town overview information directly
        graphics.drawString(this.font, "Town Name: " + menu.getTownName(), x, y, 0xFFFFFF);
        graphics.drawString(this.font, "Mayor: " + menu.getMayorName(), x, y + 15, 0xFFFFFF);
        graphics.drawString(this.font, "Town Level: " + menu.getTownLevel(), x, y + 30, 0xFFFFFF);
        graphics.drawString(this.font, "Population: " + menu.getTownPopulation(), x, y + 45, 0xFFFFFF);
        graphics.drawString(this.font, "Reputation: " + menu.getTownReputation() + "%", x, y + 60, 0xFFFFFF);
        graphics.drawString(this.font, "Treasury: " + menu.getGoldCoins() + "g " + 
                         menu.getSilverCoins() + "s " + 
                         menu.getBronzeCoins() + "b", x, y + 75, 0xFFFFFF);
    }
    
    private void drawEconomyContent(GuiGraphics graphics, int x, int y) {
        // Draw economy information directly
        graphics.drawString(this.font, "Town Resources:", x, y, 0xFFFFFF);
        
        int col1X = x;
        int col2X = x + 80;
        int col3X = x + 160;
        
        // Add little icons or decorations before resource names
        graphics.drawString(this.font, "⛏ Wood: 50", col1X, y + 20, 0xFFFFFF);
        graphics.drawString(this.font, "🪨 Stone: 35", col2X, y + 20, 0xFFFFFF);
        graphics.drawString(this.font, "⚙ Iron: 10", col3X, y + 20, 0xFFFFFF);
        
        graphics.drawString(this.font, "🔥 Coal: 42", col1X, y + 40, 0xFFFFFF);
        graphics.drawString(this.font, "💰 Gold: 5", col2X, y + 40, 0xFFFFFF);
        graphics.drawString(this.font, "🍞 Food: 120", col3X, y + 40, 0xFFFFFF);
        
        // Draw a divider line
        graphics.hLine(x, x + 230, y + 60, 0x80FFFFFF);
        
        // Draw resource trends
        graphics.drawString(this.font, "Resource Trends:", x, y + 70, 0xFFFFFF);
        graphics.drawString(this.font, "Production: +5/day", x, y + 85, 0xFFFF55);
        graphics.drawString(this.font, "Consumption: -3/day", x, y + 100, 0xFF5555);
    }
    
    private void drawPopulationContent(GuiGraphics graphics, int x, int y) {
        // Draw population information directly
        graphics.drawString(this.font, "Town Citizens:", x, y, 0xFFFFFF);
        
        int col1X = x;
        int col2X = x + 130;
        
        // Draw citizen table with improved formatting
        // Headers
        graphics.fill(col1X - 5, y + 15, col1X + 120, y + 17, 0x80FFFFFF);
        graphics.fill(col2X - 5, y + 15, col2X + 70, y + 17, 0x80FFFFFF);
        
        graphics.drawString(this.font, "Name", col1X, y + 20, 0xAAAAAA);
        graphics.drawString(this.font, "Profession", col2X, y + 20, 0xAAAAAA);
        
        graphics.fill(col1X - 5, y + 27, col1X + 120, y + 29, 0x80FFFFFF);
        graphics.fill(col2X - 5, y + 27, col2X + 70, y + 29, 0x80FFFFFF);
        
        // Citizens list
        graphics.drawString(this.font, "John Smith", col1X, y + 35, 0xFFFFFF);
        graphics.drawString(this.font, "Farmer", col2X, y + 35, 0xFFFFFF);
        
        graphics.drawString(this.font, "Mary Johnson", col1X, y + 50, 0xFFFFFF);
        graphics.drawString(this.font, "Merchant", col2X, y + 50, 0xFFFFFF);
        
        graphics.drawString(this.font, "Robert Lee", col1X, y + 65, 0xFFFFFF);
        graphics.drawString(this.font, "Blacksmith", col2X, y + 65, 0xFFFFFF);
        
        graphics.drawString(this.font, "Sarah Williams", col1X, y + 80, 0xFFFFFF);
        graphics.drawString(this.font, "Guard", col2X, y + 80, 0xFFFFFF);
        
        graphics.drawString(this.font, "Michael Brown", col1X, y + 95, 0xFFFFFF);
        graphics.drawString(this.font, "Miner", col2X, y + 95, 0xFFFFFF);
    }
    
    private void drawSettingsContent(GuiGraphics graphics, int x, int y) {
        // Draw settings information directly
        graphics.drawString(this.font, "Town Settings:", x, y, 0xFFFFFF);
        
        int leftMargin = x + 10;
        int valueX = x + 140;
        
        // Draw a settings box border
        graphics.fill(leftMargin - 10, y + 15, valueX + 80, y + 85, 0x60444444);
        graphics.hLine(leftMargin - 10, valueX + 80, y + 15, 0x80FFFFFF);
        graphics.hLine(leftMargin - 10, valueX + 80, y + 85, 0x80FFFFFF);
        graphics.vLine(leftMargin - 10, y + 15, y + 85, 0x80FFFFFF);
        graphics.vLine(valueX + 80, y + 15, y + 85, 0x80FFFFFF);
        
        // Setting labels and values with improved formatting
        graphics.drawString(this.font, "Town Name:", leftMargin, y + 25, 0xCCCCCC);
        graphics.drawString(this.font, menu.getTownName(), valueX, y + 25, 0xFFFFFF);
        
        graphics.drawString(this.font, "Auto Collect:", leftMargin, y + 45, 0xCCCCCC);
        String autoCollectValue = menu.isAutoCollectEnabled() ? "Enabled" : "Disabled";
        int autoCollectColor = menu.isAutoCollectEnabled() ? 0x55FF55 : 0xFF5555;
        graphics.drawString(this.font, autoCollectValue, valueX, y + 45, autoCollectColor);
        
        graphics.drawString(this.font, "Taxes:", leftMargin, y + 65, 0xCCCCCC);
        String taxesValue = menu.isTaxesEnabled() ? "Enabled" : "Disabled";
        int taxesColor = menu.isTaxesEnabled() ? 0x55FF55 : 0xFF5555;
        graphics.drawString(this.font, taxesValue, valueX, y + 65, taxesColor);
        
        // Information text
        graphics.drawString(this.font, "Click settings to toggle values", x, y + 100, 0xAAAAAA);
    }
    
    // Helper method to draw a button
    private void drawButton(GuiGraphics graphics, int x, int y, int width, int height, String text) {
        // Button background with gradient
        graphics.fill(x, y, x + width, y + height, 0xFF555555);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF666666);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF888888);
        
        // Button text
        int textWidth = this.font.width(text);
        int textX = x + (width - textWidth) / 2;
        int textY = y + (height - 8) / 2;
        graphics.drawString(this.font, text, textX, textY, 0xFFFFFF);
    }
    
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
        // Render the background
        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;
        
        // Draw background panel with clear borders - use a more attractive color scheme
        int bgColor = 0xCC333333;  // Darker transparent gray
        int fillColor = 0xAA222222; // Slightly darker than border for the fill
        int borderColor = 0xFFA0A0A0; // Light gray border
        
        // Draw main background
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, bgColor);
        graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, fillColor);
        
        // Draw outline
        graphics.hLine(x, x + this.imageWidth - 1, y, borderColor);
        graphics.hLine(x, x + this.imageWidth - 1, y + this.imageHeight - 1, borderColor);
        graphics.vLine(x, y, y + this.imageHeight - 1, borderColor);
        graphics.vLine(x + this.imageWidth - 1, y, y + this.imageHeight - 1, borderColor);
        
        // Draw a separator line below the tabs
        graphics.hLine(x + 10, x + this.imageWidth - 10, y + 30, borderColor);
    }
    
    // Improve mouse event handling to better support UI component interaction
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // First check if our UI components handle the click
        if (this.tabPanel != null) {
            int tabX = this.leftPos + 10;
            int tabWidth = (this.imageWidth - 20) / 4; // 4 tabs
            int tabY = this.topPos + 20;
            
            // Handle tab clicks
            if (mouseY >= tabY && mouseY < tabY + 20) {
                for (int i = 0; i < 4; i++) {
                    if (mouseX >= tabX + (i * tabWidth) && mouseX < tabX + ((i+1) * tabWidth)) {
                        String[] tabIds = {"overview", "economy", "population", "settings"};
                        if (i < tabIds.length) {
                            this.tabPanel.setActiveTab(tabIds[i]);
                            return true;
                        }
                    }
                }
            }
            
            // Handle the button click at the bottom of each tab
            int buttonX = this.leftPos + (this.imageWidth / 2) - 30;
            int buttonY = this.topPos + this.imageHeight - 35;
            int buttonWidth = 60;
            int buttonHeight = 20;
            
            if (mouseX >= buttonX && mouseX < buttonX + buttonWidth && 
                mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
                
                // Get which button was clicked based on active tab
                String activeTabId = this.tabPanel.getActiveTabId();
                if (activeTabId != null) {
                    String buttonText = activeTabId.substring(0, 3).toUpperCase();
                    
                    // Send a chat message with the button name
                    if (Minecraft.getInstance().player != null) {
                        // Create a text component with the button name
                        Component message = Component.literal(buttonText);
                        // Send the message
                        Minecraft.getInstance().player.sendSystemMessage(message);
                    }
                    
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    // Helper method to check if a point is within a rectangle
    private boolean isPointInBounds(int x, int y, int width, int height, double pointX, double pointY) {
        return pointX >= x && pointX < x + width && pointY >= y && pointY < y + height;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // No custom drag handling needed
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // No custom release handling needed
        return super.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // No custom key handling needed
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        // No custom char typing handling needed
        return super.charTyped(codePoint, modifiers);
    }

    // Helper methods for scissoring (clipping)
    private void enableScissor(int x1, int y1, int x2, int y2) {
        double scale = Minecraft.getInstance().getWindow().getGuiScale();
        int scaledX1 = (int)(x1 * scale);
        int scaledY1 = (int)(y1 * scale);
        int scaledX2 = (int)(x2 * scale);
        int scaledY2 = (int)(y2 * scale);
        
        int screenHeight = Minecraft.getInstance().getWindow().getHeight();
        
        // Flip Y coordinates because OpenGL has 0,0 at the bottom left
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(scaledX1, screenHeight - scaledY2, scaledX2 - scaledX1, scaledY2 - scaledY1);
    }

    private void disableScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
} 