package com.yourdomain.businesscraft.screen;

import com.yourdomain.businesscraft.screen.components.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builder for creating screens in BusinessCraft.
 * Simplifies screen creation with a fluent API.
 * 
 * @param <T> The menu type for the screen
 */
public class BCScreenBuilder<T extends AbstractContainerMenu> {
    private final AbstractContainerScreen<T> screen;
    private final List<UIComponent> components = new ArrayList<>();
    private BCTabPanel tabPanel = null;
    private final Map<String, BCPanel> tabPanels = new HashMap<>();
    private BCLayout mainLayout = null;
    private int padding = 5;
    private int screenWidth;
    private int screenHeight;
    private int tabHeight = 20; // Default tab height
    private final Inventory playerInventory;
    
    /**
     * Create a new screen builder for the specified screen
     */
    public BCScreenBuilder(AbstractContainerScreen<T> screen, Inventory playerInventory) {
        this.screen = screen;
        this.playerInventory = playerInventory;
        // Store dimensions for later use
        this.screenWidth = screen.width;
        this.screenHeight = screen.height;
    }
    
    /**
     * Create a new screen with the specified menu, inventory, and title
     */
    public static <M extends AbstractContainerMenu> BCScreenBuilder<M> create(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        // Create a new screen with the specified parameters
        AbstractContainerScreen<M> screen = new AbstractContainerScreen<M>(menu, inventory, title) {
            @Override
            protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
                // This will be overridden by the builder
            }
        };
        
        // Create builder
        BCScreenBuilder<M> builder = new BCScreenBuilder<>(screen, inventory);
        builder.screenWidth = width;
        builder.screenHeight = height;
        
        return builder;
    }
    
    /**
     * Add a component to the screen
     */
    public BCScreenBuilder<T> addComponent(UIComponent component) {
        components.add(component);
        return this;
    }
    
    /**
     * Set the main layout for the screen
     */
    public BCScreenBuilder<T> withLayout(BCLayout layout) {
        this.mainLayout = layout;
        return this;
    }
    
    /**
     * Set the padding for the screen
     */
    public BCScreenBuilder<T> withPadding(int padding) {
        this.padding = padding;
        return this;
    }
    
    /**
     * Create a tabbed interface for the screen
     */
    public BCScreenBuilder<T> withTabs(int tabHeight) {
        this.tabHeight = tabHeight;
        // Create a tab panel that fills the screen
        tabPanel = new BCTabPanel(screenWidth - (padding * 2), screenHeight - (padding * 2), tabHeight);
        
        return this;
    }
    
    /**
     * Add a tab to the tabbed interface
     */
    public BCScreenBuilder<T> addTab(String id, Component title, Consumer<BCPanel> configurator) {
        if (tabPanel == null) {
            throw new IllegalStateException("Must call withTabs before adding tabs");
        }
        
        // Create a panel for the tab content
        BCPanel tabContent = new BCPanel(tabPanel.getWidth(), tabPanel.getHeight() - tabHeight);
        
        // Let the caller configure the tab content
        configurator.accept(tabContent);
        
        // Add the tab to the panel
        tabPanel.addTab(id, title, tabContent);
        tabPanels.put(id, tabContent);
        
        return this;
    }
    
    /**
     * Build the screen
     */
    public AbstractContainerScreen<T> build() {
        // Create a new screen that will use our components
        AbstractContainerScreen<T> finalScreen = new AbstractContainerScreen<T>(
                screen.getMenu(), playerInventory, screen.getTitle()) {
            
            @Override
            protected void init() {
                super.init();
                
                // Initialize components
                if (tabPanel != null) {
                    tabPanel.position(leftPos + padding, topPos + padding);
                    tabPanel.init(this::addRenderableWidget);
                } else {
                    for (UIComponent component : components) {
                        component.init(this::addRenderableWidget);
                    }
                }
            }
            
            @Override
            protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
                // Draw screen background
                guiGraphics.fill(leftPos, topPos, leftPos + width, topPos + height, 0xFF000000);
                
                // Apply layout if specified
                if (mainLayout != null && tabPanel == null) {
                    BCPanel container = new BCPanel(width, height);
                    container.position(leftPos, topPos);
                    container.withLayout(mainLayout);
                    container.withPadding(padding);
                    
                    for (UIComponent component : components) {
                        container.addChild(component);
                    }
                    
                    container.layoutChildren();
                }
            }
            
            @Override
            public void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
                // Override to prevent default label rendering
            }
            
            @Override
            public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
                super.render(guiGraphics, mouseX, mouseY, partialTick);
                
                // Render components
                if (tabPanel != null) {
                    tabPanel.render(guiGraphics, leftPos + padding, topPos + padding, mouseX, mouseY);
                } else {
                    for (UIComponent component : components) {
                        if (component.isVisible()) {
                            component.render(guiGraphics, component.getX(), component.getY(), mouseX, mouseY);
                        }
                    }
                }
            }
        };
        
        return finalScreen;
    }
} 