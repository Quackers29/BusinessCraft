package com.yourdomain.businesscraft.forge.platform;

import com.yourdomain.businesscraft.api.EventHelper;
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
    public void registerForgeEvent(Object listener) {
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
        com.yourdomain.businesscraft.forge.event.ForgeModEvents.setActiveTownBlock(pos);
    }

    @Override
    public void clearActiveTownBlock() {
        com.yourdomain.businesscraft.forge.event.ForgeModEvents.setActiveTownBlock(null);
    }
}
