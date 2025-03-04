package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.menu.TownBlockMenu;
import com.yourdomain.businesscraft.screen.components.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Example of how to use the new UI system.
 * This shows how to create a town block screen using the builder pattern.
 */
public class BCScreenExample {
    
    /**
     * Create a town block screen using the new UI system
     */
    public static AbstractContainerScreen<TownBlockMenu> createTownBlockScreen(
            TownBlockMenu menu, Inventory inventory, Component title) {
        
        // Example mock data (for demonstration only)
        int mockPopulation = 15;
        int mockTouristCapacity = 50;
        int mockCurrentTourists = 8;
        int mockSearchRadius = 64;
        
        // Create a screen builder
        return BCScreenBuilder.create(menu, inventory, title, 256, 204)
            .withPadding(8)
            .withTabs(20)
            .addTab("town", Component.translatable("gui.businesscraft.tab.town"), townPanel -> {
                // Configure town tab
                townPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
                
                // Add town name
                BCLabel townNameLabel = BCComponentFactory.createHeaderLabel("gui.businesscraft.town_name", 200);
                townPanel.addChild(townNameLabel);
                
                // Add population display
                BCLabel populationLabel = BCComponentFactory.createDynamicLabel(
                    () -> Component.translatable("gui.businesscraft.population", mockPopulation),
                    200
                );
                townPanel.addChild(populationLabel);
                
                // Add tourist capacity display
                BCLabel touristCapacityLabel = BCComponentFactory.createDynamicLabel(
                    () -> Component.translatable("gui.businesscraft.tourist_capacity", 
                        mockTouristCapacity, mockCurrentTourists),
                    200
                );
                townPanel.addChild(touristCapacityLabel);
            })
            .addTab("resources", Component.translatable("gui.businesscraft.tab.resources"), resourcesPanel -> {
                // Configure resources tab
                resourcesPanel.withLayout(new BCGridLayout(3, 5, 5));
                
                // Add resource displays (simplified example)
                for (int i = 0; i < 9; i++) {
                    final int slot = i;
                    final int mockResourceAmount = 10 + i * 5; // Example mock data
                    
                    BCPanel resourcePanel = new BCPanel(60, 60);
                    resourcePanel.withBackgroundColor(0x40000000);
                    resourcePanel.withBorderColor(0x80FFFFFF);
                    
                    // Add resource name
                    BCLabel resourceName = BCComponentFactory.createBodyLabel(
                        "gui.businesscraft.resource." + i, 
                        55
                    );
                    resourceName.position(3, 3);
                    resourcePanel.addChild(resourceName);
                    
                    // Add resource amount
                    BCLabel resourceAmount = BCComponentFactory.createDynamicLabel(
                        () -> Component.literal(String.valueOf(mockResourceAmount)),
                        55
                    );
                    resourceAmount.position(3, 20);
                    resourcePanel.addChild(resourceAmount);
                    
                    // Add to grid
                    resourcesPanel.addChild(resourcePanel);
                }
            })
            .addTab("history", Component.translatable("gui.businesscraft.tab.history"), historyPanel -> {
                // Configure history tab
                historyPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
                
                // Add history header
                BCLabel historyHeader = BCComponentFactory.createHeaderLabel(
                    "gui.businesscraft.tourist_history", 
                    200
                );
                historyPanel.addChild(historyHeader);
                
                // Add history entries (simplified example)
                for (int i = 0; i < 5; i++) {
                    BCLabel historyEntry = BCComponentFactory.createBodyLabel(
                        "gui.businesscraft.history.entry." + i, 
                        200
                    );
                    historyPanel.addChild(historyEntry);
                }
            })
            .addTab("settings", Component.translatable("gui.businesscraft.tab.settings"), settingsPanel -> {
                // Configure settings tab
                settingsPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10));
                
                // Add tourist spawning toggle
                BCButton touristSpawningButton = BCComponentFactory.createPrimaryButton(
                    "gui.businesscraft.toggle_tourist_spawning",
                    button -> System.out.println("Toggle tourist spawning clicked"),
                    200
                );
                settingsPanel.addChild(touristSpawningButton);
                
                // Add path creation mode toggle
                BCButton pathCreationButton = BCComponentFactory.createSecondaryButton(
                    "gui.businesscraft.toggle_path_creation",
                    button -> System.out.println("Toggle path creation clicked"),
                    200
                );
                settingsPanel.addChild(pathCreationButton);
                
                // Add search radius setting
                BCLabel searchRadiusLabel = BCComponentFactory.createBodyLabel(
                    "gui.businesscraft.search_radius", 
                    200
                );
                settingsPanel.addChild(searchRadiusLabel);
                
                // Add search radius buttons
                BCPanel radiusButtonsPanel = BCComponentFactory.createFlowPanel(
                    200, 20, BCFlowLayout.Direction.HORIZONTAL, 5
                );
                
                // Decrease button
                BCButton decreaseButton = BCComponentFactory.createSecondaryButton(
                    "gui.businesscraft.decrease",
                    button -> System.out.println("Decrease search radius clicked"),
                    60
                );
                radiusButtonsPanel.addChild(decreaseButton);
                
                // Current value
                BCLabel radiusValueLabel = BCComponentFactory.createDynamicLabel(
                    () -> Component.literal(String.valueOf(mockSearchRadius)),
                    60
                );
                radiusButtonsPanel.addChild(radiusValueLabel);
                
                // Increase button
                BCButton increaseButton = BCComponentFactory.createSecondaryButton(
                    "gui.businesscraft.increase",
                    button -> System.out.println("Increase search radius clicked"),
                    60
                );
                radiusButtonsPanel.addChild(increaseButton);
                
                settingsPanel.addChild(radiusButtonsPanel);
            })
            .build();
    }
} 