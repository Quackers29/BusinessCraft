package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.EventHelper;
import com.quackers29.businesscraft.forge.event.ForgeEventCallbackHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.function.Consumer;

/**
 * Forge implementation of EventHelper
 */
public class ForgeEventHelper implements EventHelper {
    @Override
    public void registerModEvent(Object listener) {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.register(listener);
    }

    @Override
    public void registerPlatformEvent(Object listener) {
        MinecraftForge.EVENT_BUS.register(listener);
    }

    @Override
    public void addServerStoppingListener(Consumer<Void> listener) {
        // This will be handled in the main mod class
    }

    @Override
    public void addServerStartedListener(Consumer<Void> listener) {
        // This will be handled in the main mod class
    }

    @Override
    public void addLevelUnloadListener(Consumer<Void> listener) {
        // This will be handled in the main mod class
    }

    @Override
    public void setActiveTownBlock(net.minecraft.core.BlockPos pos) {
        com.quackers29.businesscraft.forge.event.ForgeModEvents.setActiveTownBlock(pos);
    }

    @Override
    public void clearActiveTownBlock() {
        com.quackers29.businesscraft.forge.event.ForgeModEvents.setActiveTownBlock(null);
    }

    @Override
    public void registerPlayerTickCallback(EventCallbacks.PlayerTickCallback callback) {
        ForgeEventCallbackHandler.registerPlayerTickCallback(callback);
    }

    @Override
    public void registerPlayerLoginCallback(EventCallbacks.PlayerLoginCallback callback) {
        ForgeEventCallbackHandler.registerPlayerLoginCallback(callback);
    }

    @Override
    public void registerPlayerLogoutCallback(EventCallbacks.PlayerLogoutCallback callback) {
        ForgeEventCallbackHandler.registerPlayerLogoutCallback(callback);
    }

    @Override
    public void registerRightClickBlockCallback(EventCallbacks.RightClickBlockCallback callback) {
        ForgeEventCallbackHandler.registerRightClickBlockCallback(callback);
    }

    @Override
    public void registerClientTickCallback(EventCallbacks.ClientTickCallback callback) {
        ForgeEventCallbackHandler.registerClientTickCallback(callback);
    }

    @Override
    public void registerKeyInputCallback(EventCallbacks.KeyInputCallback callback) {
        ForgeEventCallbackHandler.registerKeyInputCallback(callback);
    }

    @Override
    public void registerMouseScrollCallback(EventCallbacks.MouseScrollCallback callback) {
        ForgeEventCallbackHandler.registerMouseScrollCallback(callback);
    }

    @Override
    public void registerRenderLevelCallback(EventCallbacks.RenderLevelCallback callback) {
        ForgeEventCallbackHandler.registerRenderLevelCallback(callback);
    }

    @Override
    public void registerLevelUnloadCallback(EventCallbacks.LevelUnloadCallback callback) {
        ForgeEventCallbackHandler.registerLevelUnloadCallback(callback);
    }
}
