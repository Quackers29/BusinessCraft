package com.yourdomain.businesscraft.screen.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Tab panel component for BusinessCraft UI system.
 * Provides a tabbed interface with content panels.
 */
public class BCTabPanel extends BCPanel {
    private final List<TabInfo> tabs = new ArrayList<>();
    private final Map<String, BCPanel> tabPanels = new HashMap<>();
    private final int tabHeight;
    private String activeTabId = null;
    private int tabButtonWidth;
    
    /**
     * Create a new tab panel with the specified dimensions
     */
    public BCTabPanel(int width, int height, int tabHeight) {
        super(width, height);
        this.tabHeight = tabHeight;
    }
    
    /**
     * Add a tab to the panel
     */
    public BCTabPanel addTab(String id, Component title, BCPanel contentPanel) {
        // Calculate tab button width based on number of tabs
        tabButtonWidth = (width - 4) / (tabs.size() + 1);
        
        // Create tab button
        BCButton tabButton = new BCButton(title, b -> setActiveTab(id), tabButtonWidth, tabHeight);
        
        // Add tab info
        tabs.add(new TabInfo(id, title, tabButton));
        tabPanels.put(id, contentPanel);
        
        // Set first tab as active by default
        if (activeTabId == null) {
            activeTabId = id;
        }
        
        return this;
    }
    
    /**
     * Set the active tab
     */
    public void setActiveTab(String tabId) {
        if (!tabPanels.containsKey(tabId)) {
            return;
        }
        
        // Hide previous tab content
        if (activeTabId != null) {
            tabPanels.get(activeTabId).setVisible(false);
        }
        
        // Show new tab content
        activeTabId = tabId;
        tabPanels.get(activeTabId).setVisible(true);
    }
    
    /**
     * Get the active tab ID
     */
    public String getActiveTabId() {
        return activeTabId;
    }
    
    /**
     * Get the active tab panel
     */
    public BCPanel getActiveTabPanel() {
        return tabPanels.get(activeTabId);
    }
    
    @Override
    protected void renderContent(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Render tab buttons
        int tabX = x + 2;
        for (TabInfo tab : tabs) {
            // Highlight active tab
            if (tab.id.equals(activeTabId)) {
                guiGraphics.fill(tabX, y, tabX + tabButtonWidth, y + tabHeight, 0x80FFFFFF);
            }
            
            // Render tab button
            tab.button.render(guiGraphics, tabX, y, mouseX, mouseY);
            tabX += tabButtonWidth;
        }
        
        // Render content area background
        guiGraphics.fill(x, y + tabHeight, x + width, y + height, backgroundColor);
        
        // Render active tab content
        if (activeTabId != null) {
            BCPanel activePanel = tabPanels.get(activeTabId);
            activePanel.render(guiGraphics, x, y + tabHeight, mouseX, mouseY);
        }
    }
    
    @Override
    public void init(Consumer<Button> register) {
        // Register tab buttons
        for (TabInfo tab : tabs) {
            tab.button.init(register);
        }
        
        // Initialize all tab panels
        for (BCPanel panel : tabPanels.values()) {
            panel.init(register);
            
            // Hide all panels except the active one
            panel.setVisible(panel == tabPanels.get(activeTabId));
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        // When showing the panel, only show the active tab content
        if (visible && activeTabId != null) {
            for (Map.Entry<String, BCPanel> entry : tabPanels.entrySet()) {
                entry.getValue().setVisible(entry.getKey().equals(activeTabId));
            }
        }
    }
    
    /**
     * Information about a tab
     */
    private static class TabInfo {
        final String id;
        final Component title;
        final BCButton button;
        
        TabInfo(String id, Component title, BCButton button) {
            this.id = id;
            this.title = title;
            this.button = button;
        }
    }
} 