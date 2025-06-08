package com.yourdomain.businesscraft.ui.interfaces;

import com.yourdomain.businesscraft.ui.managers.TownScreenRenderManager;

/**
 * Composite interface for screen rendering capabilities.
 * Combines multiple related interfaces to reduce interface pollution in BaseTownScreen.
 */
public interface ScreenRenderingCapabilities extends 
    TownScreenRenderManager.CacheUpdateProvider,
    TownScreenRenderManager.ScreenLayoutProvider,
    TownScreenRenderManager.ComponentProvider {
}