package com.quackers29.businesscraft.api;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;

/**
 * Platform-agnostic interface for event handling operations.
 * Implementations will handle platform-specific event systems.
 */
public interface EventHelper {
    /**
     * Register a mod event bus listener
     */
    void registerModEvent(Object listener);

    /**
     * Register a platform event bus listener
     */
    void registerPlatformEvent(Object listener);

    /**
     * Add a server stopping event listener
     */
    void addServerStoppingListener(Consumer<Void> listener);

    /**
     * Add a server started event listener
     */
    void addServerStartedListener(Consumer<Void> listener);

    /**
     * Add a level unload event listener
     */
    void addLevelUnloadListener(Consumer<Void> listener);

    /**
     * Set the active town block for path creation mode
     */
    void setActiveTownBlock(BlockPos pos);

    /**
     * Clear the active town block
     */
    void clearActiveTownBlock();

    /**
     * Register a player tick callback (server-side)
     */
    void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback);

    /**
     * Register a player login callback
     */
    void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback);

    /**
     * Register a player logout callback
     */
    void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback);

    /**
     * Register a right-click block callback (server-side)
     */
    void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback);

    /**
     * Register a client tick callback
     */
    void registerClientTickCallback(EventCallbacks.ClientTickCallback callback);

    /**
     * Register a key input callback
     */
    void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback);

    /**
     * Register a mouse scroll callback
     */
    void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback);

    /**
     * Register a render level callback
     */
    void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback);

    /**
     * Register a level unload callback
     */
    void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback);
}
