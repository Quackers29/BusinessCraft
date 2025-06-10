package com.yourdomain.businesscraft.ui.managers;

import com.yourdomain.businesscraft.ui.screens.BaseTownScreen;
import com.yourdomain.businesscraft.util.Result;
import com.yourdomain.businesscraft.util.BCError;
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
            LOGGER.debug("Saved active tab for parent screen: {}", parentScreen.getClass().getSimpleName());
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
     * 
     * @param parentScreen The parent screen to refresh
     */
    protected static void refreshParentScreenData(Screen parentScreen) {
        if (parentScreen instanceof BaseTownScreen) {
            BaseTownScreen<?> townScreen = (BaseTownScreen<?>) parentScreen;
            
            if (townScreen.getCacheManager() != null) {
                townScreen.getCacheManager().refreshCachedValues();
                LOGGER.debug("Refreshed cache data for parent screen");
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
        Minecraft.getInstance().setScreen(modalScreen);
        LOGGER.debug("Displayed modal screen: {}", modalScreen.getClass().getSimpleName());
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