package com.quackers29.businesscraft.api;

import java.util.function.Consumer;

import net.minecraft.core.BlockPos;

public interface EventHelper {
    void registerModEvent(Object listener);

    void registerPlatformEvent(Object listener);

    void addServerStoppingListener(Consumer<Void> listener);

    void addServerStartedListener(Consumer<Void> listener);

    void addLevelUnloadListener(Consumer<Void> listener);

    void setActiveTownBlock(BlockPos pos);

    void clearActiveTownBlock();

    void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback);

    void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback);

    void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback);

    void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback);

    void registerClientTickCallback(EventCallbacks.ClientTickCallback callback);

    void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback);

    void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback);

    void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback);

    void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback);
}
