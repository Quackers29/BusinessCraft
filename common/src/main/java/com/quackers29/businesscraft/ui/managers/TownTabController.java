package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.templates.TownInterfaceTheme;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import com.quackers29.businesscraft.ui.tabs.BaseTownTab;
import com.quackers29.businesscraft.ui.tabs.OverviewTab;
import com.quackers29.businesscraft.ui.tabs.ResourcesTab;
import com.quackers29.businesscraft.ui.tabs.PopulationTab;
import com.quackers29.businesscraft.ui.tabs.SettingsTab;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Manages tab creation, lifecycle, and configuration for the Town Interface.
 * Extracted from TownInterfaceScreen to improve code organization.
 */
public class TownTabController {
    
    // Tab configuration interface
    public interface TabDataProvider {
        // Screen positioning
        int getLeftPos();
        int getTopPos();
        int getScreenPadding();
        int getImageWidth();
        int getImageHeight();
        
        // Widget registration
        void addRenderableWidget(Button widget);
        
        // Tab content provider (for tab implementations)
        Object getTabContentProvider();
    }
    
    // Tab styling constants
    private static final int ACTIVE_TAB_COLOR = TownInterfaceTheme.ACTIVE_TAB_COLOR;
    private static final int INACTIVE_TAB_COLOR = TownInterfaceTheme.INACTIVE_TAB_COLOR;
    private static final int TEXT_COLOR = TownInterfaceTheme.TEXT_COLOR;
    private static final int BORDER_COLOR = TownInterfaceTheme.BORDER_COLOR;
    private static final int BACKGROUND_COLOR = TownInterfaceTheme.BACKGROUND_COLOR;
    
    private final TabDataProvider dataProvider;
    private BCTabPanel tabPanel;
    
    // Tab instances for lifecycle management
    private OverviewTab overviewTab;
    private ResourcesTab resourcesTab;
    private PopulationTab populationTab;
    private SettingsTab settingsTab;
    
    /**
     * Creates a new tab controller.
     * 
     * @param dataProvider Provider for tab data and configuration
     */
    public TownTabController(TabDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }
    
    /**
     * Initializes the tab panel and all tabs.
     * 
     * @return The configured tab panel
     */
    public BCTabPanel initializeTabs() {
        // Create and configure the tab panel
        createTabPanel();
        
        // Create all tabs
        createAllTabs();
        
        // Set default active tab
        setDefaultActiveTab();
        
        // Initialize the tab panel
        initializeTabPanel();
        
        return tabPanel;
    }
    
    /**
     * Creates and configures the tab panel with proper dimensions and styling.
     */
    private void createTabPanel() {
        // Calculate screen dimensions and padding
        int screenPadding = dataProvider.getScreenPadding();
        int screenContentWidth = dataProvider.getImageWidth() - (screenPadding * 2);
        int screenContentHeight = dataProvider.getImageHeight() - (screenPadding * 2);
        
        // Create tab panel with proper dimensions, positioned with padding
        this.tabPanel = new BCTabPanel(screenContentWidth, screenContentHeight, 20);
        this.tabPanel.position(
            dataProvider.getLeftPos() + screenPadding, 
            dataProvider.getTopPos() + screenPadding
        );
        
        // Set tab styling with theme colors
        this.tabPanel.withTabStyle(ACTIVE_TAB_COLOR, INACTIVE_TAB_COLOR, TEXT_COLOR)
                     .withTabBorder(true, BORDER_COLOR)
                     .withContentStyle(BACKGROUND_COLOR, BORDER_COLOR);
    }
    
    /**
     * Creates all tab instances and adds them to the tab panel.
     */
    private void createAllTabs() {
        // Calculate tab content dimensions
        int tabWidth = this.tabPanel.getWidth();
        int tabHeight = this.tabPanel.getHeight() - 20; // Account for tab header
        
        // Get the content provider for tab implementations
        Object provider = dataProvider.getTabContentProvider();
        if (!(provider instanceof TownInterfaceScreen)) {
            throw new IllegalStateException("Tab content provider must be a TownInterfaceScreen instance");
        }
        TownInterfaceScreen contentProvider = (TownInterfaceScreen) provider;
        
        // Create and configure each tab
        createOverviewTab(contentProvider, tabWidth, tabHeight);
        createResourcesTab(contentProvider, tabWidth, tabHeight);
        createPopulationTab(contentProvider, tabWidth, tabHeight);
        createSettingsTab(contentProvider, tabWidth, tabHeight);
    }
    
    /**
     * Generic method to create and configure a tab.
     * 
     * @param tabId The tab identifier
     * @param tabTitle The tab display title
     * @param tabInstance The tab instance to configure
     */
    private void createTab(String tabId, String tabTitle, BaseTownTab tabInstance) {
        // Initialize the tab
        tabInstance.init(dataProvider::addRenderableWidget);
        
        // Add the tab to the tab panel
        tabPanel.addTab(tabId, Component.literal(tabTitle), tabInstance.getPanel());
    }
    
    /**
     * Creates and configures the Overview tab.
     */
    private void createOverviewTab(TownInterfaceScreen contentProvider, int width, int height) {
        overviewTab = new OverviewTab(contentProvider, width, height);
        createTab("overview", "Overview", overviewTab);
    }
    
    /**
     * Creates and configures the Resources tab.
     */
    private void createResourcesTab(TownInterfaceScreen contentProvider, int width, int height) {
        resourcesTab = new ResourcesTab(contentProvider, width, height);
        createTab("resources", "Resources", resourcesTab);
    }
    
    /**
     * Creates and configures the Population tab.
     */
    private void createPopulationTab(TownInterfaceScreen contentProvider, int width, int height) {
        populationTab = new PopulationTab(contentProvider, width, height);
        createTab("population", "Population", populationTab);
    }
    
    /**
     * Creates and configures the Settings tab.
     */
    private void createSettingsTab(TownInterfaceScreen contentProvider, int width, int height) {
        settingsTab = new SettingsTab(contentProvider, width, height);
        createTab("settings", "Settings", settingsTab);
    }
    
    /**
     * Sets the default active tab to Overview.
     */
    private void setDefaultActiveTab() {
        // Make sure Overview is the default active tab
        if (this.tabPanel.getActiveTabId() == null) {
            this.tabPanel.setActiveTab("overview");
        }
    }
    
    /**
     * Initializes the tab panel with widget registration.
     */
    private void initializeTabPanel() {
        // Initialize the tab panel
        this.tabPanel.init(dataProvider::addRenderableWidget);
    }
    
    /**
     * Gets the configured tab panel.
     * 
     * @return The tab panel
     */
    public BCTabPanel getTabPanel() {
        return tabPanel;
    }
    
    /**
     * Gets a specific tab instance by ID.
     * 
     * @param tabId The tab ID
     * @return The tab instance, or null if not found
     */
    public Object getTab(String tabId) {
        switch (tabId) {
            case "overview":
                return overviewTab;
            case "resources":
                return resourcesTab;
            case "population":
                return populationTab;
            case "settings":
                return settingsTab;
            default:
                return null;
        }
    }
    
    /**
     * Refreshes a specific tab's content.
     * 
     * @param tabId The tab ID to refresh
     */
    public void refreshTab(String tabId) {
        Object tab = getTab(tabId);
        if (tab != null) {
            // Each tab type should implement a refresh method
            // For now, we'll use the visibility toggle approach
            if (tab instanceof OverviewTab) {
                ((OverviewTab) tab).update();
            }
            // Add refresh methods for other tab types as needed
        }
    }
    
    /**
     * Forces a refresh of all tabs.
     * Useful when data has changed and all tabs need to update their display.
     */
    public void forceRefreshAllTabs() {
        // Refresh overview tab
        if (overviewTab != null) {
            overviewTab.update();
        }
        
        // Refresh resources tab
        if (resourcesTab != null && resourcesTab instanceof ResourcesTab) {
            ((ResourcesTab) resourcesTab).forceRefresh();
        }
        
        // Refresh population tab
        if (populationTab != null && populationTab instanceof PopulationTab) {
            ((PopulationTab) populationTab).update();
        }
        
        // Refresh settings tab  
        if (settingsTab != null && settingsTab instanceof SettingsTab) {
            ((SettingsTab) settingsTab).update();
        }
    }
    
    /**
     * Cleans up tab resources when the screen is closed.
     */
    public void cleanup() {
        // Clean up tab instances
        overviewTab = null;
        resourcesTab = null;
        populationTab = null;
        settingsTab = null;
        
        // Clean up tab panel
        if (tabPanel != null) {
            // Tab panel cleanup would go here if needed
            tabPanel = null;
        }
    }
} 
