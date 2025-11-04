package com.quackers29.businesscraft.ui.managers;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.ui.screens.BaseTownScreen;
import com.quackers29.businesscraft.util.Result;
import com.quackers29.businesscraft.util.BCError;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Base class for modal managers providing common functionality.
 * Extracted to reduce code duplication and standardize modal handling patterns.
 */
public abstract class BaseModalManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseModalManager.class);
    
    /**
     * Prepares a parent screen for modal display by saving current tab state.
     * 
     * @param parentScreen The parent screen that will be returned to
     */
    protected static void prepareParentScreen(Screen parentScreen) {
        if (parentScreen instanceof BaseTownScreen) {
            ((BaseTownScreen<?>) parentScreen).saveActiveTab();
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Saved active tab for parent screen: {}", parentScreen.getClass().getSimpleName());
        }
    }
    
    /**
     * Creates a standardized callback that handles data refresh when modal closes.
     * 
     * @param parentScreen The parent screen to refresh
     * @param originalCallback The original callback to execute
     * @param <T> The type of modal screen
     * @return A combined callback that handles both refresh and original callback
     */
    protected static <T> Consumer<T> createStandardCallback(
            Screen parentScreen, 
            Consumer<T> originalCallback) {
        
        return modalScreen -> {
            // Execute original callback first
            if (originalCallback != null) {
                originalCallback.accept(modalScreen);
            }
            
            // Refresh data when modal closes
            refreshParentScreenData(parentScreen);
        };
    }
    
    /**
     * Refreshes data in the parent screen if it's a BaseTownScreen.
     * With the main sync issues fixed, this automatic refresh is less critical.
     * 
     * @param parentScreen The parent screen to refresh
     */
    protected static void refreshParentScreenData(Screen parentScreen) {
        if (parentScreen instanceof BaseTownScreen) {
            BaseTownScreen<?> townScreen = (BaseTownScreen<?>) parentScreen;
            
            // With the town name sync fix, we don't need to aggressively refresh cache
            // on every modal close. The data will sync properly through normal mechanisms.
            // Only refresh if the cache manager exists and there might be stale data
            if (townScreen.getCacheManager() != null) {
                // townScreen.getCacheManager().refreshCachedValues();  // Commented out - less aggressive refresh
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Modal closed - relying on normal data sync instead of forced cache refresh");
            }
        }
    }
    
    /**
     * Displays a modal screen using Minecraft's screen manager.
     * 
     * @param modalScreen The modal screen to display
     * @param <T> The type of modal screen
     */
    protected static <T extends Screen> void displayModal(T modalScreen) {
        com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
        if (clientHelper != null) {
            Object minecraft = clientHelper.getMinecraft();
            if (minecraft instanceof net.minecraft.client.Minecraft mc) {
                mc.setScreen(modalScreen);
                DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Displayed modal screen: {}", modalScreen.getClass().getSimpleName());
            }
        }
    }
    
    /**
     * Validates that required parameters are not null.
     * 
     * @param parentScreen The parent screen
     * @param parameterName The name of the parameter for error messages
     * @return Success if valid, or ValidationError if invalid
     */
    protected static Result<Void, BCError.ValidationError> validateParentScreen(Screen parentScreen, String parameterName) {
        if (parentScreen == null) {
            return Result.failure(BCError.ValidationError.required(parameterName));
        }
        return Result.success(null);
    }
    
    /**
     * Safely displays a modal screen with error handling.
     * 
     * @param modalScreen The modal screen to display
     * @param <T> The type of modal screen
     * @return Success if displayed, or UIError if failed
     */
    protected static <T extends Screen> Result<Void, BCError.UIError> safeDisplayModal(T modalScreen) {
        try {
            displayModal(modalScreen);
            return Result.success(null);
        } catch (Exception e) {
            return Result.failure(BCError.UIError.modalCreationFailed(
                modalScreen.getClass().getSimpleName(), 
                e.getMessage()
            ));
        }
    }
}
