package com.yourdomain.businesscraft.ui.interfaces;

import com.yourdomain.businesscraft.ui.managers.TownScreenEventHandler;

/**
 * Composite interface for screen event handling capabilities.
 * Combines multiple related interfaces to reduce interface pollution in BaseTownScreen.
 */
public interface ScreenEventCapabilities extends 
    TownScreenEventHandler.SoundHandler,
    TownScreenEventHandler.ModalStateProvider {
}