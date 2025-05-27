package com.yourdomain.businesscraft.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.menu.TownInterfaceMenu;
import com.yourdomain.businesscraft.screen.components.*;
import com.yourdomain.businesscraft.platform.Platform;
import com.yourdomain.businesscraft.screen.tabs.ResourcesTab;
import com.yourdomain.businesscraft.screen.tabs.OverviewTab;
import com.yourdomain.businesscraft.screen.tabs.PopulationTab;
import com.yourdomain.businesscraft.screen.tabs.SettingsTab;
import com.yourdomain.businesscraft.screen.managers.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.opengl.GL11;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.SetTownNamePacket;
import com.yourdomain.businesscraft.network.PlayerExitUIPacket;
import com.yourdomain.businesscraft.network.SetSearchRadiusPacket;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.text.SimpleDateFormat;
import java.util.Date;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import net.minecraft.client.gui.screens.Screen;
import com.yourdomain.businesscraft.screen.components.BCModalGridScreen;
import com.yourdomain.businesscraft.screen.components.BCModalGridFactory;
import com.yourdomain.businesscraft.screen.components.BCModalInventoryScreen;
import com.yourdomain.businesscraft.screen.components.BCModalInventoryFactory;
import com.yourdomain.businesscraft.menu.TradeMenu;
import com.yourdomain.businesscraft.menu.StorageMenu;
import com.yourdomain.businesscraft.data.cache.TownDataCache;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Town Interface Screen showcases the BusinessCraft UI system capabilities.
 * This screen demonstrates various UI components and layouts using the enhanced BCTabPanel.
 */
public class TownInterfaceScreen extends BaseTownScreen<TownInterfaceMenu> 
        implements BottomButtonManager.ButtonActionHandler {
    
    // State tracking for toggle buttons
    private boolean pvpEnabled = false;
    private boolean publicTownEnabled = true;
    
    // Manager instances specific to this screen
    private BottomButtonManager buttonManager;
    
    // Cache the current search radius for UI updates
    private int currentSearchRadius;

    public TownInterfaceScreen(TownInterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        
        // Initialize cached values from the menu
        this.currentSearchRadius = menu.getSearchRadius();
    }
    
    @Override
    protected void initializeSpecificManagers() {
        // Initialize managers specific to TownInterfaceScreen
        this.buttonManager = new BottomButtonManager(this);
        this.eventHandler = new TownScreenEventHandler(this, this, this.buttonManager);
        this.renderManager = new TownScreenRenderManager(this, this, this);
    }
    
    @Override
    protected void performAdditionalInit() {
        // Initialize the button manager with screen position
        buttonManager.updateScreenPosition(this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        buttonManager.createBottomButtonsGrid();
    }
    
    @Override
    protected void performAdditionalCleanup() {
        // Send a packet to register the player exit UI to show platform indicators
        BlockPos blockPos = this.menu.getBlockPos();
        if (blockPos != null) {
            // Send a packet to the server to register player exit UI
            ModMessages.sendToServer(new PlayerExitUIPacket(blockPos));
        }
    }
    
    // ===== Interface Implementations =====
    
    // ButtonActionHandler implementation
    @Override
    public void onEditDetails() {
        showChangeTownNamePopup();
    }
    
    @Override
    public void onViewVisitors() {
        showVisitorListModal();
    }
    
    @Override
    public void onTradeResources() {
        showTradeResourcesModal();
    }
    
    @Override
    public void onManageStorage() {
        showStorageModal();
    }
    
    @Override
    public void onAssignJobs() {
        sendChatMessage("Button pressed: Assign Jobs");
    }
    
    @Override
    public void onViewVisitorHistory() {
        showVisitorHistoryScreen();
    }
    
    @Override
    public void onSaveSettings() {
        sendChatMessage("Button pressed: Save Settings");
    }
    
    @Override
    public void onResetDefaults() {
        sendChatMessage("Button pressed: Reset Defaults");
    }
    
    @Override
    public void onManagePlatforms() {
        openPlatformManagementScreen();
    }
    
    @Override
    public void onGenericAction(String action) {
        sendChatMessage("Button pressed: " + action);
    }
    
    // ===== Specific Interface Implementations =====
    
    @Override
    public void setCurrentSearchRadius(int radius) {
        this.currentSearchRadius = radius;
    }
    
    @Override
    public int getSearchRadiusFromMenu() {
        return menu.getSearchRadius();
    }
    
    @Override
    public BottomButtonManager getButtonManager() {
        return buttonManager;
    }


    


    
    /**
     * Opens the platform management screen
     * This method is used by tab implementations.
     */
    public void openPlatformManagementScreen() {
        // Get real platform data from the menu rather than using sample data
        List<Platform> platforms = menu.getPlatforms();
        
        // Create the platform management screen
        PlatformManagementScreen platformScreen = new PlatformManagementScreen(
            menu.getBlockPos(), platforms);
        
        // Close this screen and open the platform screen
        // This ensures when the user navigates back from the platform screen,
        // they return to the town interface
        this.minecraft.setScreen(platformScreen);
    }
    
    /**
     * Handles changes to the search radius
     * Implements the behavior from TownBlockScreen
     * This method is used by tab implementations.
     */
    public void handleRadiusChange(int mouseButton) {
        // Get the current radius from our cached value
        int newRadius = currentSearchRadius;
        
        // Calculate new radius based on key combinations
        boolean isShift = hasShiftDown();
        
        // Use mouseButton to determine increase/decrease
        // mouseButton 0 = left click (increase), 1 = right click (decrease)
        boolean isDecrease = (mouseButton == 1);
        
        if (isShift && isDecrease) {
            newRadius -= 10;
        } else if (isDecrease) {
            newRadius -= 1;
        } else if (isShift) {
            newRadius += 10;
        } else {
            newRadius += 1;
        }
        
        // Clamp to reasonable values
        newRadius = Math.max(1, Math.min(newRadius, 100));
        
        // Update our cached value immediately for UI feedback
        currentSearchRadius = newRadius;
        cacheManager.updateCachedSearchRadius(newRadius);
        
        // Send packet to update the server
        ModMessages.sendToServer(new SetSearchRadiusPacket(menu.getBlockPos(), newRadius));
        
        // Use feedback message since we can't update the UI directly
        String message = "Search radius " + (isDecrease ? "decreased" : "increased") + " to " + newRadius;
        sendChatMessage(message);
        
        // Also update the menu's cached value if the method is available
        if (menu instanceof TownInterfaceMenu) {
            ((TownInterfaceMenu) menu).setClientSearchRadius(newRadius);
        }
        
        // Play a click sound for feedback
        playButtonClickSound();
    }
    
    /**
     * Gets the current search radius for UI display
     * @return The current search radius
     */
    public int getCurrentSearchRadius() {
        return currentSearchRadius;
    }
    
    // Helper methods to get data from the cache manager
    public String getCachedTownName() {
        return cacheManager.getCachedTownName();
    }
    
    public int getCachedPopulation() {
        return cacheManager.getCachedPopulation();
    }
    
    // Changed from private to public to allow access from tab implementations
    public Map<Item, Integer> getCachedResources() {
        return cacheManager.getCachedResources();
    }
    
    public String getTouristString() {
        return cacheManager.getTouristString();
    }
    




    /**
     * Show the change town name popup
     */
    private void showChangeTownNamePopup() {
        activePopup = TownNamePopupManager.showChangeTownNamePopup(
            getCachedTownName(),
            menu.getBlockPos(),
            popup -> activePopup = null
        );
        
        // Initialize the popup
        activePopup.init(this::addRenderableWidget);
        
        // Focus the input field
        activePopup.focusInput();
    }



    /**
     * Show the visitor list modal screen
     */
    private void showVisitorListModal() {
        activeModal = VisitorModalManager.showVisitorListModal(
            modal -> activeModal = null
        );
    }

    /**
     * Shows the trade resources modal screen with input and output slots
     */
    private void showTradeResourcesModal() {
        TradeModalManager.showTradeResourcesModal(
            this,
            this.menu.getBlockPos(),
            screen -> {
                // Optional callback when screen is closed
                // We can use this to refresh data if needed
            }
        );
    }
    
    /**
     * Shows the storage modal screen with 2x9 chest-like storage
     */
    private void showStorageModal() {
        StorageModalManager.showStorageModal(
            this,
            this.menu.getBlockPos(),
            this.menu,
            screen -> {
                // Optional callback when screen is closed
                // We can use this to refresh data if needed
            }
        );
    }
    
    /**
     * Close the active modal if one exists
     */
    @Override
    protected void closeActiveModal() {
        this.activeModal = null;
    }
    
    /**
     * Show the visitor history screen
     */
    private void showVisitorHistoryScreen() {
        VisitorHistoryManager.showVisitorHistoryScreen(
            this,
            menu.getBlockPos(),
            this.tabPanel,
            screen -> {
                // Optional callback when screen is closed
            }
        );
    }


} 