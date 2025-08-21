package com.quackers29.businesscraft.ui.templates;

import com.quackers29.businesscraft.ui.components.basic.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import com.quackers29.businesscraft.ui.builders.*;
import com.quackers29.businesscraft.ui.layout.*;

/**
 * Provides standard screen templates for BusinessCraft interfaces.
 * These templates can be used as a starting point for creating consistent UIs.
 */
public class BCScreenTemplates {

    /**
     * Creates a standard information screen with a header, content area, and action buttons.
     * Useful for displaying information and simple actions to the player.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createInfoScreen(
            T menu, Inventory inventory, Component title, 
            Component headerText, Component contentText,
            List<ButtonConfig> buttons) {
        
        return BCScreenBuilder.create(menu, inventory, title, 256, 204)
            .withPadding(10)
            .withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10))
            .addComponent(BCComponentFactory.createHeaderLabel(headerText.getString(), 236))
            .addComponent(createContentPanel(contentText, 236, 110))
            .addComponent(createButtonsPanel(buttons, 236))
            .build();
    }
    
    /**
     * Creates a standard tabbed management screen with tabs for different categories.
     * Useful for screens that need to display different types of related information.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createTabbedScreen(
            T menu, Inventory inventory, Component title,
            List<TabConfig> tabs) {
        
        BCScreenBuilder<T> builder = BCScreenBuilder.create(menu, inventory, title, 256, 204)
            .withPadding(10)
            .withTabs(20);
        
        // Add each tab to the screen
        for (TabConfig tab : tabs) {
            builder.addTab(tab.id, tab.title, panel -> {
                panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 5));
                
                // Let the tab configurator set up the panel contents
                if (tab.configurator != null) {
                    tab.configurator.accept(panel);
                }
            });
        }
        
        return builder.build();
    }
    
    /**
     * Creates a standard resource management screen with a list of resources and action buttons.
     * Useful for screens that display and manage collections of resources.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createResourceScreen(
            T menu, Inventory inventory, Component title,
            Component headerText, 
            List<ResourceConfig> resources,
            List<ButtonConfig> actions) {
        
        return BCScreenBuilder.create(menu, inventory, title, 256, 204)
            .withPadding(10)
            .withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 10))
            .addComponent(BCComponentFactory.createHeaderLabel(headerText.getString(), 236))
            .addComponent(createResourceListPanel(resources, 236))
            .addComponent(createButtonsPanel(actions, 236))
            .build();
    }
    
    /**
     * Creates a standard settings screen with configurable options.
     * Useful for screens that allow players to change settings or preferences.
     */
    public static <T extends AbstractContainerMenu> AbstractContainerScreen<T> createSettingsScreen(
            T menu, Inventory inventory, Component title,
            List<SettingConfig> settings) {
        
        BCScreenBuilder<T> builder = BCScreenBuilder.create(menu, inventory, title, 256, 204)
            .withPadding(10)
            .withLayout(new BCGridLayout(2, 10, 5));
        
        // Add each setting as a label + control pair
        for (SettingConfig setting : settings) {
            builder.addComponent(BCComponentFactory.createBodyLabel(setting.label.getString(), 110));
            
            if (setting.control != null) {
                builder.addComponent(setting.control);
            }
        }
        
        return builder.build();
    }
    
    // Helper method to create a content panel
    private static BCPanel createContentPanel(Component text, int width, int height) {
        BCPanel panel = new BCPanel(width, height);
        panel.withBackgroundColor(0x40000000);
        panel.withBorderColor(0x80FFFFFF);
        panel.withPadding(5);
        
        BCLabel contentLabel = BCComponentFactory.createBodyLabel(text.getString(), width - 10);
        panel.addChild(contentLabel);
        
        return panel;
    }
    
    // Helper method to create a buttons panel
    private static BCPanel createButtonsPanel(List<ButtonConfig> buttons, int width) {
        BCPanel panel = new BCPanel(width, 30);
        panel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 10));
        
        for (ButtonConfig button : buttons) {
            BCButton bcButton = BCComponentFactory.createPrimaryButton(
                button.label.getString(), 
                b -> button.action.run(), 
                (width / buttons.size()) - 10
            );
            panel.addChild(bcButton);
        }
        
        return panel;
    }
    
    // Helper method to create a resource list panel
    private static BCPanel createResourceListPanel(List<ResourceConfig> resources, int width) {
        BCPanel panel = new BCPanel(width, 100);
        panel.withBackgroundColor(0x40000000);
        panel.withBorderColor(0x80FFFFFF);
        panel.withLayout(new BCGridLayout(3, 5, 5));
        panel.withPadding(5);
        
        for (ResourceConfig resource : resources) {
            BCPanel resourcePanel = new BCPanel(70, 30);
            resourcePanel.withLayout(new BCFlowLayout(BCFlowLayout.Direction.HORIZONTAL, 5));
            
            // Resource name
            BCLabel nameLabel = BCComponentFactory.createBodyLabel(
                resource.name.getString(), 
                40
            );
            resourcePanel.addChild(nameLabel);
            
            // Resource amount
            BCLabel amountLabel = BCComponentFactory.createDynamicLabel(
                () -> Component.literal(String.valueOf(resource.amountSupplier.get())),
                25
            );
            resourcePanel.addChild(amountLabel);
            
            panel.addChild(resourcePanel);
        }
        
        return panel;
    }
    
    /**
     * Configuration for a button
     */
    public static class ButtonConfig {
        private final Component label;
        private final Runnable action;
        
        public ButtonConfig(Component label, Runnable action) {
            this.label = label;
            this.action = action;
        }
        
        public static ButtonConfig of(Component label, Runnable action) {
            return new ButtonConfig(label, action);
        }
    }
    
    /**
     * Configuration for a tab
     */
    public static class TabConfig {
        private final String id;
        private final Component title;
        private final Consumer<BCPanel> configurator;
        
        public TabConfig(String id, Component title, Consumer<BCPanel> configurator) {
            this.id = id;
            this.title = title;
            this.configurator = configurator;
        }
        
        public static TabConfig of(String id, Component title, Consumer<BCPanel> configurator) {
            return new TabConfig(id, title, configurator);
        }
    }
    
    /**
     * Configuration for a resource
     */
    public static class ResourceConfig {
        private final Component name;
        private final Supplier<Integer> amountSupplier;
        private final ItemStack icon;
        
        public ResourceConfig(Component name, Supplier<Integer> amountSupplier, ItemStack icon) {
            this.name = name;
            this.amountSupplier = amountSupplier;
            this.icon = icon;
        }
        
        public static ResourceConfig of(Component name, Supplier<Integer> amountSupplier, ItemStack icon) {
            return new ResourceConfig(name, amountSupplier, icon);
        }
    }
    
    /**
     * Configuration for a setting
     */
    public static class SettingConfig {
        private final Component label;
        private final UIComponent control;
        
        public SettingConfig(Component label, UIComponent control) {
            this.label = label;
            this.control = control;
        }
        
        public static SettingConfig of(Component label, UIComponent control) {
            return new SettingConfig(label, control);
        }
    }
} 