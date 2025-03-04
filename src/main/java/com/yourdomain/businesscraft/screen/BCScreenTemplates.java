package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.screen.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Pre-configured screen templates for common BusinessCraft interfaces.
 * These templates provide ready-to-use screen configurations with
 * appropriate layouts, styling, and animations.
 */
public class BCScreenTemplates {
    
    /**
     * Creates a town management screen template.
     * Includes tabs for Info, Resources, History, and Settings.
     * 
     * @param menu The container menu
     * @param inventory The player inventory
     * @param title The screen title
     * @param width Screen width
     * @param height Screen height
     * @return A configured screen builder
     */
    public static <M extends AbstractContainerMenu> AbstractContainerScreen<M> createTownManagementScreen(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCTheme theme = BCTheme.get();
        
        // Create the screen builder with appropriate styling
        BCScreenBuilder<M> builder = BCScreenBuilder.create(menu, inventory, title, width, height)
            .withPadding(theme.getMediumPadding())
            .withBackgroundColor(theme.getBackgroundColor())
            .withBorderColor(theme.getBorderColor())
            .withTabs(24)
            .withTabAnimation(BCAnimation.AnimationType.SLIDE_LEFT, BCAnimation.EasingFunction.EASE_OUT, 200)
            .withEnterAnimation(BCAnimation.AnimationType.FADE, BCAnimation.EasingFunction.EASE_OUT, 300);
        
        // Add Info tab
        builder.addTab("info", Component.translatable("businesscraft.ui.tab.info"), panel -> {
            panel.withLayout(new BCGridLayout(2, 3, 8));
            
            // Town name and basic info
            BCPanel infoPanel = BCComponentFactory.createPanel(panel.getWidth() / 2 - 4, 120);
            infoPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 6));
            infoPanel.withBorderColor(theme.getPrimaryColor());
            infoPanel.withBackgroundColor(theme.getSurfaceColor());
            infoPanel.withPadding(theme.getSmallPadding());
            
            // Town stats panel
            BCPanel statsPanel = BCComponentFactory.createPanel(panel.getWidth() / 2 - 4, 120);
            statsPanel.withLayout(new BCGridLayout(2, 3, 4));
            statsPanel.withBorderColor(theme.getPrimaryColor());
            statsPanel.withBackgroundColor(theme.getSurfaceColor());
            statsPanel.withPadding(theme.getSmallPadding());
            
            // Resource summary panel
            BCPanel resourceSummary = BCComponentFactory.createPanel(panel.getWidth() - 8, 100);
            resourceSummary.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 8));
            resourceSummary.withBorderColor(theme.getSecondaryColor());
            resourceSummary.withBackgroundColor(theme.getSurfaceColor());
            resourceSummary.withPadding(theme.getSmallPadding());
            
            // Map view panel
            BCPanel mapView = BCComponentFactory.createPanel(panel.getWidth() - 8, panel.getHeight() - 236);
            mapView.withBorderColor(theme.getPrimaryColor());
            mapView.withBackgroundColor(theme.getBackgroundSecondaryColor());
            mapView.withPadding(0);
            
            // Add components to the panel in grid layout
            panel.addChild(infoPanel);      // Cell 0,0
            panel.addChild(statsPanel);     // Cell 1,0
            panel.addChild(resourceSummary); // Cell 0,1 and 1,1 (spans columns)
            panel.addChild(mapView);        // Cell 0,2 and 1,2 (spans columns)
        });
        
        // Add Resources tab
        builder.addTab("resources", Component.translatable("businesscraft.ui.tab.resources"), panel -> {
            panel.withLayout(new BCGridLayout(1, 2, 8));
            
            // Resource list panel
            BCPanel resourceListPanel = BCComponentFactory.createPanel(panel.getWidth() - 8, 180);
            resourceListPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 2));
            resourceListPanel.withBorderColor(theme.getPrimaryColor());
            resourceListPanel.withBackgroundColor(theme.getSurfaceColor());
            resourceListPanel.withPadding(theme.getSmallPadding());
            
            // Resource details panel
            BCPanel resourceDetailsPanel = BCComponentFactory.createPanel(panel.getWidth() - 8, panel.getHeight() - 188);
            resourceDetailsPanel.withLayout(new BCGridLayout(2, 3, 6));
            resourceDetailsPanel.withBorderColor(theme.getSecondaryColor());
            resourceDetailsPanel.withBackgroundColor(theme.getSurfaceColor());
            resourceDetailsPanel.withPadding(theme.getMediumPadding());
            
            // Add panels to the main panel
            panel.addChild(resourceListPanel);
            panel.addChild(resourceDetailsPanel);
        });
        
        // Add History tab
        builder.addTab("history", Component.translatable("businesscraft.ui.tab.history"), panel -> {
            panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));
            
            // Timeline panel
            BCPanel timelinePanel = BCComponentFactory.createPanel(panel.getWidth() - 8, 60);
            timelinePanel.withBorderColor(theme.getPrimaryColor());
            timelinePanel.withBackgroundColor(theme.getSurfaceColor());
            timelinePanel.withPadding(theme.getSmallPadding());
            
            // Events panel
            BCPanel eventsPanel = BCComponentFactory.createPanel(panel.getWidth() - 8, panel.getHeight() - 68);
            eventsPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 2));
            eventsPanel.withBorderColor(theme.getSecondaryColor());
            eventsPanel.withBackgroundColor(theme.getSurfaceColor());
            eventsPanel.withPadding(theme.getMediumPadding());
            
            // Add panels to the main panel
            panel.addChild(timelinePanel);
            panel.addChild(eventsPanel);
        });
        
        // Add Settings tab
        builder.addTab("settings", Component.translatable("businesscraft.ui.tab.settings"), panel -> {
            panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
            panel.withPadding(theme.getMediumPadding());
            
            // Settings panel
            BCPanel settingsPanel = BCComponentFactory.createPanel(panel.getWidth() - 16, panel.getHeight() - 20);
            settingsPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));
            settingsPanel.withBorderColor(theme.getPrimaryColor());
            settingsPanel.withBackgroundColor(theme.getSurfaceColor());
            settingsPanel.withPadding(theme.getMediumPadding());
            
            // Add panels to the main panel
            panel.addChild(settingsPanel);
        });
        
        return builder.build();
    }
    
    /**
     * Creates a dialog screen template.
     * Perfect for confirmations, alerts, or simple data entry.
     * 
     * @param menu The container menu
     * @param inventory The player inventory
     * @param title The screen title
     * @param width Screen width
     * @param height Screen height
     * @return A configured screen builder
     */
    public static <M extends AbstractContainerMenu> AbstractContainerScreen<M> createDialogScreen(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCTheme theme = BCTheme.get();
        
        // Create the screen builder with appropriate styling
        BCScreenBuilder<M> builder = BCScreenBuilder.create(menu, inventory, title, width, height)
            .withPadding(theme.getLargePadding())
            .withBackgroundColor(theme.getDialogBackgroundColor())
            .withBorderColor(theme.getBorderColor())
            .withEnterAnimation(BCAnimation.AnimationType.SCALE, BCAnimation.EasingFunction.EASE_OUT, 250)
            .withExitAnimation(BCAnimation.AnimationType.FADE, BCAnimation.EasingFunction.EASE_IN, 150);
        
        // Create the content panel
        BCPanel contentPanel = BCComponentFactory.createPanel(width - (theme.getLargePadding() * 2), height - (theme.getLargePadding() * 2));
        contentPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 12));
        contentPanel.withBackgroundColor(theme.getSurfaceColor());
        contentPanel.withBorderColor(theme.getPrimaryColor());
        contentPanel.withPadding(theme.getMediumPadding());
        
        // Add the content panel to the screen
        builder.addComponent(contentPanel);
        
        // Configure the layout
        builder.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 0));
        
        return builder.build();
    }
    
    /**
     * Creates a resource management screen template.
     * Designed for inventory, trading, and resource allocation.
     * 
     * @param menu The container menu
     * @param inventory The player inventory
     * @param title The screen title
     * @param width Screen width
     * @param height Screen height
     * @return A configured screen builder
     */
    public static <M extends AbstractContainerMenu> AbstractContainerScreen<M> createResourceScreen(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCTheme theme = BCTheme.get();
        
        // Create the screen builder with appropriate styling
        BCScreenBuilder<M> builder = BCScreenBuilder.create(menu, inventory, title, width, height)
            .withPadding(theme.getMediumPadding())
            .withBackgroundColor(theme.getBackgroundSecondaryColor())
            .withBorderColor(theme.getSecondaryVariantColor())
            .withEnterAnimation(BCAnimation.AnimationType.SLIDE_UP, BCAnimation.EasingFunction.EASE_OUT, 300);
        
        // Create the main panel layout
        BCPanel mainPanel = BCComponentFactory.createPanel(width - (theme.getMediumPadding() * 2), height - (theme.getMediumPadding() * 2));
        mainPanel.withLayout(new BCGridLayout(2, 2, 8));
        mainPanel.withBackgroundColor(theme.getSurfaceColor());
        mainPanel.withBorderColor(theme.getSecondaryColor());
        mainPanel.withPadding(theme.getSmallPadding());
        
        // Add the main panel to the screen
        builder.addComponent(mainPanel);
        
        // Configure the layout
        builder.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 0));
        
        return builder.build();
    }
    
    /**
     * Creates a quest/mission screen template.
     * Designed for displaying quests, missions, and objectives.
     * 
     * @param menu The container menu
     * @param inventory The player inventory
     * @param title The screen title
     * @param width Screen width
     * @param height Screen height
     * @return A configured screen builder
     */
    public static <M extends AbstractContainerMenu> AbstractContainerScreen<M> createQuestScreen(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCTheme theme = BCTheme.get();
        
        // Create the screen builder with appropriate styling
        BCScreenBuilder<M> builder = BCScreenBuilder.create(menu, inventory, title, width, height)
            .withPadding(theme.getMediumPadding())
            .withBackgroundColor(theme.getBackgroundVariantColor())
            .withBorderColor(theme.getSecondaryVariantColor())
            .withEnterAnimation(BCAnimation.AnimationType.SLIDE_LEFT, BCAnimation.EasingFunction.EASE_OUT, 300)
            .withTabs(24)
            .withTabAnimation(BCAnimation.AnimationType.FADE, BCAnimation.EasingFunction.EASE_IN_OUT, 200);
        
        // Add Active Quests tab
        builder.addTab("active", Component.translatable("businesscraft.ui.tab.active_quests"), panel -> {
            panel.withLayout(new BCGridLayout(2, 2, 10));
            panel.withPadding(theme.getSmallPadding());
            
            // Quest list panel
            BCPanel questListPanel = BCComponentFactory.createPanel((panel.getWidth() / 3) - 5, panel.getHeight() - 10);
            questListPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 2));
            questListPanel.withBorderColor(theme.getPrimaryColor());
            questListPanel.withBackgroundColor(theme.getSurfaceColor());
            questListPanel.withPadding(theme.getSmallPadding());
            
            // Quest details panel
            BCPanel questDetailsPanel = BCComponentFactory.createPanel(((panel.getWidth() * 2) / 3) - 5, panel.getHeight() - 10);
            questDetailsPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));
            questDetailsPanel.withBorderColor(theme.getSecondaryColor());
            questDetailsPanel.withBackgroundColor(theme.getSurfaceColor());
            questDetailsPanel.withPadding(theme.getMediumPadding());
            
            // Add panels to the main panel
            panel.addChild(questListPanel);
            panel.addChild(questDetailsPanel);
        });
        
        // Add Completed Quests tab
        builder.addTab("completed", Component.translatable("businesscraft.ui.tab.completed_quests"), panel -> {
            panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));
            panel.withPadding(theme.getSmallPadding());
            
            // Completed quest list panel
            BCPanel completedListPanel = BCComponentFactory.createPanel(panel.getWidth() - 10, panel.getHeight() - 10);
            completedListPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 2));
            completedListPanel.withBorderColor(theme.getPrimaryColor());
            completedListPanel.withBackgroundColor(theme.getSurfaceColor());
            completedListPanel.withPadding(theme.getMediumPadding());
            
            // Add panels to the main panel
            panel.addChild(completedListPanel);
        });
        
        // Add Available Quests tab
        builder.addTab("available", Component.translatable("businesscraft.ui.tab.available_quests"), panel -> {
            panel.withLayout(new BCGridLayout(2, 2, 10));
            panel.withPadding(theme.getSmallPadding());
            
            // Available quest list panel
            BCPanel availableListPanel = BCComponentFactory.createPanel((panel.getWidth() / 3) - 5, panel.getHeight() - 10);
            availableListPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 2));
            availableListPanel.withBorderColor(theme.getPrimaryColor());
            availableListPanel.withBackgroundColor(theme.getSurfaceColor());
            availableListPanel.withPadding(theme.getSmallPadding());
            
            // Quest preview panel
            BCPanel questPreviewPanel = BCComponentFactory.createPanel(((panel.getWidth() * 2) / 3) - 5, panel.getHeight() - 10);
            questPreviewPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));
            questPreviewPanel.withBorderColor(theme.getSecondaryColor());
            questPreviewPanel.withBackgroundColor(theme.getSurfaceColor());
            questPreviewPanel.withPadding(theme.getMediumPadding());
            
            // Add panels to the main panel
            panel.addChild(availableListPanel);
            panel.addChild(questPreviewPanel);
        });
        
        return builder.build();
    }
} 