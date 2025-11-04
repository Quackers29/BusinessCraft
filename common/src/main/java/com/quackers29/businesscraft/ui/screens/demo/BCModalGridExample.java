package com.quackers29.businesscraft.ui.screens.demo;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import com.quackers29.businesscraft.ui.modal.specialized.BCModalGridScreen;
import com.quackers29.businesscraft.ui.modal.factories.BCModalGridFactory;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import com.quackers29.businesscraft.ui.templates.BCScreenTemplates;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.layout.BCGridLayout;
import com.quackers29.businesscraft.ui.components.basic.*;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import java.util.function.Consumer;
import com.quackers29.businesscraft.ui.templates.BCScreenTemplates;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.layout.BCGridLayout;
import com.quackers29.businesscraft.ui.components.basic.*;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;

/**
 * This class demonstrates various ways to use the BCModalGridScreen component
 * with different types of data and configurations.
 * 
 * Note: This is an example class for documentation purposes. It is not intended
 * to be used directly in the game.
 */
public class BCModalGridExample {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCModalGridExample.class);

    /**
     * Example 1: Simple string list with default settings
     */
    public static void showSimpleStringList(Screen parentScreen) {
        // Create a list of string items
        List<String> items = Arrays.asList(
            "First Item", 
            "Second Item", 
            "Third Item", 
            "Fourth Item",
            "Fifth Item"
        );
        
        // Use the factory to create a simple string list
        BCModalGridScreen<String> screen = BCModalGridFactory.createStringListScreen(
            Component.literal("Simple String List"),
            parentScreen,
            items,
            selectedItem -> {
                // Handle item selection
                com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
                if (clientHelper != null) {
                    Object playerObj = clientHelper.getClientPlayer();
                    if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
                        player.displayClientMessage(
                            Component.literal("Selected: " + selectedItem), 
                            false
                        );
                    }
                }
            },
            BCModalGridFactory.Themes.BC_DEFAULT
        );
        
        // Show the screen
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object minecraftObj = clientHelper.getMinecraft();
            if (minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                minecraft.setScreen(screen);
            }
        }
    }
    
    /**
     * Example 2: Custom data class with multiple columns and custom appearance
     */
    public static void showCustomDataGrid(Screen parentScreen) {
        // Create sample data with a custom class
        List<Person> people = Arrays.asList(
            new Person("John Doe", 30, "Developer"),
            new Person("Jane Smith", 28, "Designer"),
            new Person("Alex Johnson", 35, "Manager"),
            new Person("Sarah Williams", 32, "Architect"),
            new Person("Michael Brown", 29, "Analyst")
        );
        
        // Create a custom modal grid screen
        BCModalGridScreen<Person> screen = new BCModalGridScreen<>(
            Component.literal("Employee Directory"),
            parentScreen,
            null // No close callback
        );
        
        // Configure the columns using the builder pattern
        screen.addColumn("Name", person -> person.name)
              .addColumn("Age", person -> String.valueOf(person.age))
              .addColumn("Job", person -> person.job)
              .withData(people)
              .withBackButtonText("Return")
              .withPanelSize(0.6f, 0.5f) // 60% width, 50% height
              .withRowHeight(24) // Taller rows
              .withTitleScale(2.0f) // Larger title
              .withColors(
                  0xFF001020, // Dark blue background
                  0xFF4488CC, // Light blue border
                  0xFFFFFFFF, // White title
                  0xFFAADDFF, // Light blue header
                  0xFFDDFFFF  // Cyan text
              )
              .withAlternatingRowColors(0x40AACCFF, 0x20AACCFF)
              .withScrollbarColors(0x40FFFFFF, 0xA088CCFF, 0xFF88CCFF)
              .withRowClickHandler(person -> {
                  // Handle row click
                  com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
                  if (clientHelper != null) {
                      Object playerObj = clientHelper.getClientPlayer();
                      if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
                          player.displayClientMessage(
                              Component.literal("Selected person: " + person.name),
                              false
                          );
                      }
                  }
              });
        
        // Show the screen
        com.quackers29.businesscraft.api.ClientHelper clientHelper2 = PlatformAccess.getClient();
        if (clientHelper2 != null) {
            Object minecraftObj = clientHelper2.getMinecraft();
            if (minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                minecraft.setScreen(screen);
            }
        }
    }
    
    /**
     * Example 3: Items and quantities with custom styles
     */
    public static void showItemInventory(Screen parentScreen) {
        // Create a map of items and quantities
        Map<Item, Integer> inventory = new HashMap<>();
        inventory.put(Items.DIAMOND, 12);
        inventory.put(Items.EMERALD, 8);
        inventory.put(Items.GOLD_INGOT, 24);
        inventory.put(Items.IRON_INGOT, 42);
        inventory.put(Items.COAL, 64);
        inventory.put(Items.OAK_LOG, 32);
        inventory.put(Items.STONE, 128);
        
        // Use the factory method for resources
        BCModalGridScreen<Map.Entry<Item, Integer>> screen = BCModalGridFactory.createResourceListScreen(
            Component.literal("Town Resources"),
            parentScreen,
            inventory,
            modalScreen -> {
                // Optional callback when closing the screen
                LOGGER.info("Resource screen closed");
            }
        );
        
        // Customize further if needed
        screen.withTitleScale(1.75f)
              .withColors(
                  BCModalGridFactory.Themes.SUCCESS[0],  // Green background
                  BCModalGridFactory.Themes.SUCCESS[1],  // Green border
                  BCModalGridFactory.Themes.SUCCESS[2],  // Light green title
                  BCModalGridFactory.Themes.SUCCESS[3],  // Medium green header
                  BCModalGridFactory.Themes.SUCCESS[4]   // Light green text
              );
        
        // Show the screen
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object minecraftObj = clientHelper.getMinecraft();
            if (minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                minecraft.setScreen(screen);
            }
        }
    }
    
    /**
     * Example 4: Dynamic grid with tabs
     */
    public static void showTabsWithGrids(Screen parentScreen) {
        // Create a tabbed interface with our BCTabPanel
        BCTabPanel tabPanel = new BCTabPanel(400, 300, 20);
        
        // Configure tab style
        tabPanel.withTabStyle(0xA0CCDDFF, 0x80555555, 0xFFFFFFFF)
                .withTabBorder(true, 0xA0AAAAAA)
                .withContentStyle(0x80222222, 0xA0AAAAAA);
        
        // Position the tab panel
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object minecraftObj = clientHelper.getMinecraft();
            if (minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                int screenWidth = minecraft.getWindow().getGuiScaledWidth();
                int screenHeight = minecraft.getWindow().getGuiScaledHeight();
                tabPanel.position((screenWidth - 400) / 2, (screenHeight - 300) / 2);
            }
        }
        
        // Create a screen that hosts the tab panel
        Screen hostScreen = new Screen(Component.literal("Tabbed Grids")) {
            @Override
            protected void init() {
                super.init();
                
                // Add tab content with grid screens
                createTabContent();
                
                // Initialize the tab panel
                tabPanel.init(this::addRenderableWidget);
            }
            
            private void createTabContent() {
                // Tab 1: People
                BCPanel peoplePanel = new BCPanel(tabPanel.getWidth(), tabPanel.getHeight() - 20);
                peoplePanel.withPadding(10).withBackgroundColor(0x00000000);
                
                // Create a button to open the people grid
                BCButton peopleButton = BCComponentFactory.createPrimaryButton(
                    "View People Directory", 
                    b -> showCustomDataGrid(this),
                    200
                );
                peoplePanel.addChild(peopleButton);
                
                // Tab 2: Resources
                BCPanel resourcesPanel = new BCPanel(tabPanel.getWidth(), tabPanel.getHeight() - 20);
                resourcesPanel.withPadding(10).withBackgroundColor(0x00000000);
                
                // Create a button to open the resources grid
                BCButton resourcesButton = BCComponentFactory.createPrimaryButton(
                    "View Resources Inventory", 
                    b -> showItemInventory(this),
                    200
                );
                resourcesPanel.addChild(resourcesButton);
                
                // Tab 3: Simple List
                BCPanel listPanel = new BCPanel(tabPanel.getWidth(), tabPanel.getHeight() - 20);
                listPanel.withPadding(10).withBackgroundColor(0x00000000);
                
                // Create a button to open the simple list
                BCButton listButton = BCComponentFactory.createPrimaryButton(
                    "View Simple List", 
                    b -> showSimpleStringList(this),
                    200
                );
                listPanel.addChild(listButton);
                
                // Add the tabs
                tabPanel.addTab("people", Component.literal("People"), peoplePanel);
                tabPanel.addTab("resources", Component.literal("Resources"), resourcesPanel);
                tabPanel.addTab("list", Component.literal("Simple List"), listPanel);
                
                // Set default active tab
                tabPanel.setActiveTab("people");
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
                // Render darkened background
                this.renderBackground(guiGraphics);
                
                // Render tab panel
                tabPanel.render(guiGraphics, tabPanel.getX(), tabPanel.getY(), mouseX, mouseY);
                
                // Render buttons and other widgets
                super.render(guiGraphics, mouseX, mouseY, partialTicks);
            }
            
            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                // Let tab panel handle clicks
                if (tabPanel.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                return super.mouseClicked(mouseX, mouseY, button);
            }
            
            @Override
            public void onClose() {
                // Return to parent screen
                com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
                if (clientHelper != null) {
                    Object minecraftObj = clientHelper.getMinecraft();
                    if (minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                        minecraft.setScreen(parentScreen);
                    }
                }
            }
        };
        
        // Show the tabbed screen
        com.quackers29.businesscraft.api.ClientHelper clientHelper2 = PlatformAccess.getClient();
        if (clientHelper2 != null) {
            Object minecraftObj = clientHelper2.getMinecraft();
            if (minecraftObj instanceof net.minecraft.client.Minecraft minecraft) {
                minecraft.setScreen(hostScreen);
            }
        }
    }
    
    /**
     * Example custom data class
     */
    private static class Person {
        public final String name;
        public final int age;
        public final String job;
        
        public Person(String name, int age, String job) {
            this.name = name;
            this.age = age;
            this.job = job;
        }
    }
} 
