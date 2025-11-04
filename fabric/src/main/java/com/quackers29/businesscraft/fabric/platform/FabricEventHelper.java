package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.EventHelper;
import com.quackers29.businesscraft.fabric.event.FabricEventCallbackHandler;

import java.util.function.Consumer;

/**
 * Fabric implementation of EventHelper
 */
public class FabricEventHelper implements EventHelper {
    @Override
    public void registerModEvent(Object listener) {
        // Fabric doesn't use mod event bus - events are registered directly
        // This method is kept for interface compatibility but does nothing
    }

    @Override
    public void registerPlatformEvent(Object listener) {
        // Fabric doesn't use platform event bus - events are registered directly
        // This method is kept for interface compatibility but does nothing
    }

    @Override
    public void addServerStoppingListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    @Override
    public void addServerStartedListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    @Override
    public void addLevelUnloadListener(Consumer<Void> listener) {
        // Will be handled in FabricEventCallbackHandler
    }

    @Override
    public void setActiveTownBlock(Object pos) {
        // Implementation would go here
        // This is used for path creation mode tracking
    }

    @Override
    public void clearActiveTownBlock() {
        // Implementation would go here
    }
    
    @Override
    public void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback) {
        FabricEventCallbackHandler.registerPlayerTickCallback(callback);
    }
    
    @Override
    public void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback) {
        FabricEventCallbackHandler.registerPlayerLoginCallback(callback);
    }
    
    @Override
    public void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback) {
        FabricEventCallbackHandler.registerPlayerLogoutCallback(callback);
    }
    
    @Override
    public void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback) {
        FabricEventCallbackHandler.registerRightClickBlockCallback(callback);
    }
    
    @Override
    public void registerClientTickCallback(EventCallbacks.ClientTickCallback callback) {
        FabricEventCallbackHandler.registerClientTickCallback(callback);
    }
    
    @Override
    public void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback) {
        FabricEventCallbackHandler.registerKeyInputCallback(callback);
    }
    
    @Override
    public void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback) {
        FabricEventCallbackHandler.registerMouseScrollCallback(callback);
    }
    
    @Override
    public void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback) {
        FabricEventCallbackHandler.registerRenderLevelCallback(callback);
    }
    
    @Override
    public void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback) {
        FabricEventCallbackHandler.registerLevelUnloadCallback(callback);
    }
}
