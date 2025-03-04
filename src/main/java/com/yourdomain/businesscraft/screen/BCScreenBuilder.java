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
    
    // Animation settings
    private BCAnimation.AnimationType screenEnterAnimation = BCAnimation.AnimationType.FADE;
    private BCAnimation.AnimationType screenExitAnimation = BCAnimation.AnimationType.FADE;
    private BCAnimation.AnimationType tabChangeAnimation = BCAnimation.AnimationType.SLIDE_LEFT;
    private BCAnimation.EasingFunction screenAnimationEasing = BCAnimation.EasingFunction.EASE_OUT;
    private BCAnimation.EasingFunction tabAnimationEasing = BCAnimation.EasingFunction.EASE_IN_OUT;
    private long screenAnimationDuration = 250; // milliseconds
    private long tabAnimationDuration = 150; // milliseconds
    private boolean animationsEnabled = true;
    
    // Background and border settings
    private int backgroundColor = 0xAA000000;
    private int borderColor = 0xFF555555;
    private boolean drawBorder = true;
    
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
     * Configure the screen background color
     */
    public BCScreenBuilder<T> withBackgroundColor(int color) {
        this.backgroundColor = color;
        return this;
    }
    
    /**
     * Configure the screen border color
     */
    public BCScreenBuilder<T> withBorderColor(int color) {
        this.borderColor = color;
        return this;
    }
    
    /**
     * Enable or disable the screen border
     */
    public BCScreenBuilder<T> withBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        return this;
    }
    
    /**
     * Configure the screen enter animation
     */
    public BCScreenBuilder<T> withEnterAnimation(BCAnimation.AnimationType type, BCAnimation.EasingFunction easing, long duration) {
        this.screenEnterAnimation = type;
        this.screenAnimationEasing = easing;
        this.screenAnimationDuration = duration;
        return this;
    }
    
    /**
     * Configure the screen exit animation
     */
    public BCScreenBuilder<T> withExitAnimation(BCAnimation.AnimationType type, BCAnimation.EasingFunction easing, long duration) {
        this.screenExitAnimation = type;
        this.screenAnimationEasing = easing;
        this.screenAnimationDuration = duration;
        return this;
    }
    
    /**
     * Configure the tab change animation
     */
    public BCScreenBuilder<T> withTabAnimation(BCAnimation.AnimationType type, BCAnimation.EasingFunction easing, long duration) {
        this.tabChangeAnimation = type;
        this.tabAnimationEasing = easing;
        this.tabAnimationDuration = duration;
        return this;
    }
    
    /**
     * Enable or disable animations
     */
    public BCScreenBuilder<T> enableAnimations(boolean enabled) {
        this.animationsEnabled = enabled;
        return this;
    }
    
    /**
     * Build the screen
     */
    public AbstractContainerScreen<T> build() {
        // Create a new screen that will use our components
        AbstractContainerScreen<T> finalScreen = new AbstractContainerScreen<T>(
                screen.getMenu(), playerInventory, screen.getTitle()) {
            
            private BCAnimation screenAnimation;
            private BCAnimation tabAnimation;
            private String pendingTabId;
            
            @Override
            protected void init() {
                super.init();
                this.width = screenWidth;
                this.height = screenHeight;
                
                // Create screen enter animation
                if (animationsEnabled && screenEnterAnimation != BCAnimation.AnimationType.NONE) {
                    screenAnimation = new BCAnimation(screenEnterAnimation, screenAnimationEasing, screenAnimationDuration);
                    screenAnimation.start();
                }
                
                // Initialize components
                if (tabPanel != null) {
                    tabPanel.position(leftPos + padding, topPos + padding);
                    
                    // Add tab change listener if animations are enabled
                    if (animationsEnabled && tabChangeAnimation != BCAnimation.AnimationType.NONE) {
                        tabPanel.setTabChangeListener((oldId, newId) -> {
                            tabAnimation = new BCAnimation(tabChangeAnimation, tabAnimationEasing, tabAnimationDuration);
                            tabAnimation.start();
                            pendingTabId = newId;
                            return false; // Don't change immediately, wait for animation
                        });
                    }
                    
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
                guiGraphics.fill(leftPos, topPos, leftPos + screenWidth, topPos + screenHeight, backgroundColor);
                
                // Draw border if enabled
                if (drawBorder) {
                    guiGraphics.hLine(leftPos, leftPos + screenWidth - 1, topPos, borderColor);
                    guiGraphics.hLine(leftPos, leftPos + screenWidth - 1, topPos + screenHeight - 1, borderColor);
                    guiGraphics.vLine(leftPos, topPos, topPos + screenHeight - 1, borderColor);
                    guiGraphics.vLine(leftPos + screenWidth - 1, topPos, topPos + screenHeight - 1, borderColor);
                }
                
                // Apply layout if specified
                if (mainLayout != null && tabPanel == null) {
                    BCPanel container = new BCPanel(screenWidth, screenHeight);
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
                // Update animations
                if (screenAnimation != null && screenAnimation.isRunning()) {
                    screenAnimation.update();
                }
                
                if (tabAnimation != null && tabAnimation.isRunning()) {
                    tabAnimation.update();
                    
                    // If tab animation completed, actually change the tab
                    if (tabAnimation.isCompleted() && pendingTabId != null) {
                        tabPanel.setActiveTabWithoutAnimation(pendingTabId);
                        pendingTabId = null;
                    }
                }
                
                // Apply screen animation transformations
                if (screenAnimation != null && !screenAnimation.isCompleted()) {
                    float alpha = screenAnimation.getAlpha();
                    float translateX = screenAnimation.getTranslationX(screenWidth);
                    float translateY = screenAnimation.getTranslationY(screenHeight);
                    float scale = screenAnimation.getScale();
                    
                    // Apply transformations
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(leftPos + screenWidth / 2f, topPos + screenHeight / 2f, 0);
                    guiGraphics.pose().scale(scale, scale, 1.0f);
                    guiGraphics.pose().translate(-screenWidth / 2f + translateX, -screenHeight / 2f + translateY, 0);
                    
                    // Render with alpha
                    int originalAlpha = (backgroundColor >> 24) & 0xFF;
                    int animatedAlpha = (int)(originalAlpha * alpha);
                    int animatedBackgroundColor = (backgroundColor & 0x00FFFFFF) | (animatedAlpha << 24);
                    
                    // Draw background with animated alpha
                    guiGraphics.fill(0, 0, screenWidth, screenHeight, animatedBackgroundColor);
                    
                    super.render(guiGraphics, mouseX, mouseY, partialTick);
                    
                    // Render components with animation
                    if (tabPanel != null) {
                        tabPanel.render(guiGraphics, padding, padding, mouseX - leftPos, mouseY - topPos);
                    } else {
                        for (UIComponent component : components) {
                            if (component.isVisible()) {
                                component.render(guiGraphics, 
                                    component.getX() - leftPos, 
                                    component.getY() - topPos, 
                                    mouseX - leftPos, 
                                    mouseY - topPos);
                            }
                        }
                    }
                    
                    guiGraphics.pose().popPose();
                } else {
                    // Normal rendering without animation
                    super.render(guiGraphics, mouseX, mouseY, partialTick);
                    
                    // Apply tab animation transformations
                    if (tabAnimation != null && !tabAnimation.isCompleted()) {
                        // We're mid-animation between tabs
                        guiGraphics.pose().pushPose();
                        guiGraphics.pose().translate(leftPos, topPos, 0);
                        
                        // Render tab headers
                        tabPanel.renderTabsOnly(guiGraphics, padding, padding, mouseX - leftPos, mouseY - topPos);
                        
                        // Render old and new tab content with animation
                        String currentTabId = tabPanel.getActiveTabId();
                        BCPanel currentTab = tabPanels.get(currentTabId);
                        BCPanel pendingTab = tabPanels.get(pendingTabId);
                        
                        if (currentTab != null && pendingTab != null) {
                            float translateX = tabAnimation.getTranslationX(tabPanel.getWidth());
                            
                            // Render current tab sliding out
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(padding + translateX, padding + tabHeight, 0);
                            currentTab.render(guiGraphics, 0, 0, mouseX - leftPos - padding, mouseY - topPos - padding - tabHeight);
                            guiGraphics.pose().popPose();
                            
                            // Render pending tab sliding in
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(padding + translateX + tabPanel.getWidth(), padding + tabHeight, 0);
                            pendingTab.render(guiGraphics, 0, 0, mouseX - leftPos - padding, mouseY - topPos - padding - tabHeight);
                            guiGraphics.pose().popPose();
                        }
                        
                        guiGraphics.pose().popPose();
                    } else {
                        // Normal tab rendering
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
                }
            }
        };
        
        // Store dimensions in the screen
        finalScreen.width = screenWidth;
        finalScreen.height = screenHeight;
        
        return finalScreen;
    }
    
    /**
     * Create a builder for a template town info screen
     */
    public static <M extends AbstractContainerMenu> BCScreenBuilder<M> createTownInfoTemplate(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCScreenBuilder<M> builder = create(menu, inventory, title, width, height)
            .withPadding(8)
            .withBackgroundColor(0xE0000000)
            .withBorderColor(0xFF666666)
            .withTabs(20)
            .withTabAnimation(BCAnimation.AnimationType.SLIDE_LEFT, BCAnimation.EasingFunction.EASE_OUT, 200);
        
        return builder;
    }
    
    /**
     * Create a builder for a template resource management screen
     */
    public static <M extends AbstractContainerMenu> BCScreenBuilder<M> createResourceTemplate(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCScreenBuilder<M> builder = create(menu, inventory, title, width, height)
            .withPadding(8)
            .withBackgroundColor(0xE0221111)
            .withBorderColor(0xFF662222)
            .withEnterAnimation(BCAnimation.AnimationType.SCALE, BCAnimation.EasingFunction.BOUNCE, 400);
        
        return builder;
    }
    
    /**
     * Create a builder for a template settings screen
     */
    public static <M extends AbstractContainerMenu> BCScreenBuilder<M> createSettingsTemplate(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCScreenBuilder<M> builder = create(menu, inventory, title, width, height)
            .withPadding(10)
            .withBackgroundColor(0xE0112233)
            .withBorderColor(0xFF225588)
            .withLayout(new BCFlowLayout(BCFlowLayout.Direction.VERTICAL, 8));
        
        return builder;
    }
    
    /**
     * Create a builder for a template dialog screen
     */
    public static <M extends AbstractContainerMenu> BCScreenBuilder<M> createDialogTemplate(
            M menu, Inventory inventory, Component title, int width, int height) {
        
        BCScreenBuilder<M> builder = create(menu, inventory, title, width, height)
            .withPadding(15)
            .withBackgroundColor(0xF0333333)
            .withBorderColor(0xFF888888)
            .withEnterAnimation(BCAnimation.AnimationType.SCALE, BCAnimation.EasingFunction.EASE_OUT, 250)
            .withExitAnimation(BCAnimation.AnimationType.FADE, BCAnimation.EasingFunction.EASE_IN, 150);
        
        return builder;
    }
} 