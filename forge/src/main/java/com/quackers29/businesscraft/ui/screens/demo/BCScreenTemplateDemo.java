package com.quackers29.businesscraft.ui.screens.demo;

import com.quackers29.businesscraft.ui.components.basic.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import com.quackers29.businesscraft.ui.templates.BCScreenTemplates;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.layout.BCGridLayout;
import com.quackers29.businesscraft.ui.components.basic.*;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import java.util.Arrays;
import com.quackers29.businesscraft.ui.templates.BCScreenTemplates;
import com.quackers29.businesscraft.ui.builders.BCComponentFactory;
import com.quackers29.businesscraft.ui.layout.BCFlowLayout;
import com.quackers29.businesscraft.ui.layout.BCGridLayout;
import com.quackers29.businesscraft.ui.components.basic.*;
import com.quackers29.businesscraft.ui.components.containers.BCTabPanel;
import java.util.List;
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
 * Demonstrates how to use the screen templates.
 * This class provides examples of creating different types of screens using the templates.
 */
public class BCScreenTemplateDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(BCScreenTemplateDemo.class);

    /**
     * Creates a demo information screen.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createInfoScreenDemo(
            T menu, Inventory inventory) {
        
        Component title = Component.literal("Business Information");
        Component header = Component.literal("Welcome to Your Business!");
        Component content = Component.literal(
                "You have earned 1,250 coins this week. " +
                "Click the buttons below to manage your business.");
        
        List<BCScreenTemplates.ButtonConfig> buttons = Arrays.asList(
            BCScreenTemplates.ButtonConfig.of(
                Component.literal("Manage Employees"),
                () -> LOGGER.info("Manage Employees button clicked")
            ),
            BCScreenTemplates.ButtonConfig.of(
                Component.literal("View Finances"),
                () -> LOGGER.info("View Finances button clicked")
            ),
            BCScreenTemplates.ButtonConfig.of(
                Component.literal("Upgrade Business"),
                () -> LOGGER.info("Upgrade Business button clicked")
            )
        );
        
        return BCScreenTemplates.createInfoScreen(
            menu, inventory, title, header, content, buttons
        );
    }
    
    /**
     * Creates a demo tabbed screen.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createTabbedScreenDemo(
            T menu, Inventory inventory) {
        
        Component title = Component.literal("Business Management");
        
        List<BCScreenTemplates.TabConfig> tabs = Arrays.asList(
            BCScreenTemplates.TabConfig.of(
                "overview",
                Component.literal("Overview"),
                configureOverviewTab()
            ),
            BCScreenTemplates.TabConfig.of(
                "employees",
                Component.literal("Employees"),
                configureEmployeesTab()
            ),
            BCScreenTemplates.TabConfig.of(
                "finances",
                Component.literal("Finances"),
                configureFinancesTab()
            ),
            BCScreenTemplates.TabConfig.of(
                "settings",
                Component.literal("Settings"),
                configureSettingsTab()
            )
        );
        
        return BCScreenTemplates.createTabbedScreen(
            menu, inventory, title, tabs
        );
    }
    
    /**
     * Creates a demo resource screen.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createResourceScreenDemo(
            T menu, Inventory inventory) {
        
        Component title = Component.literal("Business Resources");
        Component header = Component.literal("Available Resources");
        
        List<BCScreenTemplates.ResourceConfig> resources = Arrays.asList(
            BCScreenTemplates.ResourceConfig.of(
                Component.literal("Wood"),
                () -> 50,
                new ItemStack(Items.OAK_LOG)
            ),
            BCScreenTemplates.ResourceConfig.of(
                Component.literal("Stone"),
                () -> 35,
                new ItemStack(Items.STONE)
            ),
            BCScreenTemplates.ResourceConfig.of(
                Component.literal("Iron"),
                () -> 10,
                new ItemStack(Items.IRON_INGOT)
            ),
            BCScreenTemplates.ResourceConfig.of(
                Component.literal("Coal"),
                () -> 42,
                new ItemStack(Items.COAL)
            ),
            BCScreenTemplates.ResourceConfig.of(
                Component.literal("Gold"),
                () -> 5,
                new ItemStack(Items.GOLD_INGOT)
            )
        );
        
        List<BCScreenTemplates.ButtonConfig> actions = Arrays.asList(
            BCScreenTemplates.ButtonConfig.of(
                Component.literal("Buy Resources"),
                () -> LOGGER.info("Buy Resources button clicked")
            ),
            BCScreenTemplates.ButtonConfig.of(
                Component.literal("Sell Resources"),
                () -> LOGGER.info("Sell Resources button clicked")
            )
        );
        
        return BCScreenTemplates.createResourceScreen(
            menu, inventory, title, header, resources, actions
        );
    }
    
    /**
     * Creates a demo settings screen.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createSettingsScreenDemo(
            T menu, Inventory inventory) {
        
        Component title = Component.literal("Business Settings");
        
        // Create toggle buttons for settings
        DummyToggleButton autoCollectToggle = new DummyToggleButton(
            100, 20, 
            Component.literal("Enabled"), 
            Component.literal("Disabled"), 
            true, 
            button -> LOGGER.info("Auto-collect toggled: {}", button.isToggled())
        );
        
        DummyToggleButton notificationsToggle = new DummyToggleButton(
            100, 20, 
            Component.literal("Enabled"), 
            Component.literal("Disabled"), 
            true, 
            button -> LOGGER.info("Notifications toggled: {}", button.isToggled())
        );
        
        DummyEditBoxComponent nameEditor = new DummyEditBoxComponent(
            100, 20, Component.literal(""), text -> LOGGER.info("Business name changed: {}", text)
        );
        nameEditor.setValue("My Business");
        
        // Create the settings list
        List<BCScreenTemplates.SettingConfig> settings = Arrays.asList(
            BCScreenTemplates.SettingConfig.of(
                Component.literal("Business Name:"),
                nameEditor
            ),
            BCScreenTemplates.SettingConfig.of(
                Component.literal("Auto-collect:"),
                autoCollectToggle
            ),
            BCScreenTemplates.SettingConfig.of(
                Component.literal("Notifications:"),
                notificationsToggle
            )
        );
        
        return BCScreenTemplates.createSettingsScreen(
            menu, inventory, title, settings
        );
    }
    
    // Helper methods to configure tab contents
    
    private static Consumer<BCPanel> configureOverviewTab() {
        return panel -> {
            panel.addChild(BCComponentFactory.createHeaderLabel("Business Overview", 210));
            
            BCPanel infoPanel = new BCPanel(210, 130);
            infoPanel.withBackgroundColor(0x40000000);
            infoPanel.withBorderColor(0x80FFFFFF);
            infoPanel.withPadding(5);
            infoPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
            
            infoPanel.addChild(BCComponentFactory.createBodyLabel("Business Level: 3", 200));
            infoPanel.addChild(BCComponentFactory.createBodyLabel("Employees: 5", 200));
            infoPanel.addChild(BCComponentFactory.createBodyLabel("Weekly Income: 1,250 coins", 200));
            infoPanel.addChild(BCComponentFactory.createBodyLabel("Reputation: Good", 200));
            
            panel.addChild(infoPanel);
        };
    }
    
    private static Consumer<BCPanel> configureEmployeesTab() {
        return panel -> {
            panel.addChild(BCComponentFactory.createHeaderLabel("Employees", 210));
            
            BCPanel employeesPanel = new BCPanel(210, 100);
            employeesPanel.withBackgroundColor(0x40000000);
            employeesPanel.withBorderColor(0x80FFFFFF);
            employeesPanel.withPadding(5);
            employeesPanel.withLayout(new BCGridLayout(2, 5, 5));
            
            employeesPanel.addChild(BCComponentFactory.createBodyLabel("John Smith", 100));
            employeesPanel.addChild(BCComponentFactory.createBodyLabel("Manager", 100));
            
            employeesPanel.addChild(BCComponentFactory.createBodyLabel("Mary Johnson", 100));
            employeesPanel.addChild(BCComponentFactory.createBodyLabel("Sales", 100));
            
            employeesPanel.addChild(BCComponentFactory.createBodyLabel("Robert Lee", 100));
            employeesPanel.addChild(BCComponentFactory.createBodyLabel("Crafter", 100));
            
            panel.addChild(employeesPanel);
            
            // Add buttons
            BCPanel buttonsPanel = new BCPanel(210, 30);
            buttonsPanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 10));
            
            employeesPanel.addChild(BCComponentFactory.createPrimaryButtonWithTooltip(
                "Hire Employee", 
                "Hire a new employee for your business",
                button -> LOGGER.info("Hire Employee button clicked"),
                100
            ));
            
            employeesPanel.addChild(BCComponentFactory.createSecondaryButtonWithTooltip(
                "Fire Employee", 
                "Remove an employee from your business",
                button -> LOGGER.info("Fire Employee button clicked"),
                100
            ));
            
            panel.addChild(buttonsPanel);
        };
    }
    
    private static Consumer<BCPanel> configureFinancesTab() {
        return panel -> {
            panel.addChild(BCComponentFactory.createHeaderLabel("Finances", 210));
            
            BCPanel financePanel = new BCPanel(210, 130);
            financePanel.withBackgroundColor(0x40000000);
            financePanel.withBorderColor(0x80FFFFFF);
            financePanel.withPadding(5);
            financePanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
            
            financePanel.addChild(BCComponentFactory.createBodyLabel("Weekly Income: 1,250 coins", 200));
            financePanel.addChild(BCComponentFactory.createBodyLabel("Weekly Expenses: 750 coins", 200));
            financePanel.addChild(BCComponentFactory.createBodyLabel("Net Profit: 500 coins", 200));
            financePanel.addChild(BCComponentFactory.createBodyLabel("Current Balance: 3,750 coins", 200));
            
            panel.addChild(financePanel);
        };
    }
    
    private static Consumer<BCPanel> configureSettingsTab() {
        return panel -> {
            panel.addChild(BCComponentFactory.createHeaderLabel("Settings", 210));
            
            BCPanel settingsPanel = new BCPanel(210, 130);
            settingsPanel.withBackgroundColor(0x40000000);
            settingsPanel.withBorderColor(0x80FFFFFF);
            settingsPanel.withPadding(5);
            settingsPanel.withLayout(new BCGridLayout(2, 5, 5));
            
            // Business name setting
            settingsPanel.addChild(BCComponentFactory.createBodyLabel("Business Name:", 100));
            DummyEditBoxComponent nameEditor = new DummyEditBoxComponent(
                100, 20, Component.literal(""), text -> LOGGER.info("Business name changed: {}", text)
            );
            nameEditor.setValue("My Business");
            settingsPanel.addChild(nameEditor);
            
            // Auto-collect setting
            settingsPanel.addChild(BCComponentFactory.createBodyLabel("Auto-collect:", 100));
            DummyToggleButton autoCollectToggle = new DummyToggleButton(
                100, 20, 
                Component.literal("Enabled"), 
                Component.literal("Disabled"), 
                true, 
                button -> LOGGER.info("Auto-collect toggled: {}", button.isToggled())
            );
            settingsPanel.addChild(autoCollectToggle);
            
            // Notifications setting
            settingsPanel.addChild(BCComponentFactory.createBodyLabel("Notifications:", 100));
            DummyToggleButton notificationsToggle = new DummyToggleButton(
                100, 20, 
                Component.literal("Enabled"), 
                Component.literal("Disabled"), 
                true, 
                button -> LOGGER.info("Notifications toggled: {}", button.isToggled())
            );
            settingsPanel.addChild(notificationsToggle);
            
            panel.addChild(settingsPanel);
        };
    }
    
    /**
     * A dummy toggle button implementation for demonstration purposes.
     */
    private static class DummyToggleButton implements UIComponent {
        private int width;
        private int height;
        private Component enabledText;
        private Component disabledText;
        private boolean toggled;
        private Consumer<DummyToggleButton> onClick;
        private int x;
        private int y;
        private boolean visible = true;
        
        public DummyToggleButton(int width, int height, Component enabledText, 
                             Component disabledText, boolean initialState, 
                             Consumer<DummyToggleButton> onClick) {
            this.width = width;
            this.height = height;
            this.enabledText = enabledText;
            this.disabledText = disabledText;
            this.toggled = initialState;
            this.onClick = onClick;
        }
        
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, int offsetX, int offsetY) {
            if (!visible) {
                return;
            }
            
            this.x = offsetX;
            this.y = offsetY;
            
            // Draw a simple colored rectangle
            int bgColor = toggled ? 0xFF00AA00 : 0xFF555555;
            graphics.fill(offsetX, offsetY, offsetX + width, offsetY + height, bgColor);
            
            // Draw text
            Component text = toggled ? enabledText : disabledText;
            int textWidth = Minecraft.getInstance().font.width(text.getString());
            int textX = offsetX + (width - textWidth) / 2;
            int textY = offsetY + (height - 8) / 2;
            
            graphics.drawString(Minecraft.getInstance().font, text, textX, textY, 0xFFFFFFFF);
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!visible) {
                return false;
            }
            
            if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
                toggled = !toggled;
                if (onClick != null) {
                    onClick.accept(this);
                }
                return true;
            }
            return false;
        }
        
        public boolean isToggled() {
            return toggled;
        }
        
        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
        
        @Override
        public boolean isVisible() {
            return visible;
        }
        
        @Override
        public int getX() {
            return x;
        }
        
        @Override
        public int getY() {
            return y;
        }
        
        @Override
        public int getWidth() {
            return width;
        }
        
        @Override
        public int getHeight() {
            return height;
        }
        
        @Override
        public void tick() {
            // Nothing to do
        }
        
        @Override
        public void init(Consumer<Button> buttonConsumer) {
            // No buttons to register
        }
    }
    
    /**
     * A dummy edit box component implementation for demonstration purposes.
     */
    private static class DummyEditBoxComponent implements UIComponent {
        private int width;
        private int height;
        private Component placeholder;
        private Consumer<String> onTextChanged;
        private String value = "";
        private int x;
        private int y;
        private boolean visible = true;
        
        public DummyEditBoxComponent(int width, int height, Component placeholder, Consumer<String> onTextChanged) {
            this.width = width;
            this.height = height;
            this.placeholder = placeholder;
            this.onTextChanged = onTextChanged;
        }
        
        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, int offsetX, int offsetY) {
            if (!visible) {
                return;
            }
            
            this.x = offsetX;
            this.y = offsetY;
            
            // Draw background
            graphics.fill(offsetX, offsetY, offsetX + width, offsetY + height, 0xFF000000);
            graphics.fill(offsetX + 1, offsetY + 1, offsetX + width - 1, offsetY + height - 1, 0xFFFFFFFF);
            
            // Draw text
            String textToShow = value.isEmpty() ? placeholder.getString() : value;
            int textColor = value.isEmpty() ? 0xFF888888 : 0xFF000000;
            graphics.drawString(Minecraft.getInstance().font, textToShow, offsetX + 4, offsetY + 6, textColor);
        }
        
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
        }
        
        public void setValue(String text) {
            this.value = text;
            if (onTextChanged != null) {
                onTextChanged.accept(text);
            }
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public void setVisible(boolean visible) {
            this.visible = visible;
        }
        
        @Override
        public boolean isVisible() {
            return visible;
        }
        
        @Override
        public int getX() {
            return x;
        }
        
        @Override
        public int getY() {
            return y;
        }
        
        @Override
        public int getWidth() {
            return width;
        }
        
        @Override
        public int getHeight() {
            return height;
        }
        
        @Override
        public void tick() {
            // Nothing to do
        }
        
        @Override
        public void init(Consumer<Button> buttonConsumer) {
            // No buttons to register
        }
    }
} 