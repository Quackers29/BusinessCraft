package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ClientHelper;
import net.minecraft.client.Minecraft;

/**
 * Fabric implementation of ClientHelper
 * Uses direct Mojang mappings for client access
 */
public class FabricClientHelper implements ClientHelper {
    @Override
    public Object getMinecraft() {
        return Minecraft.getInstance();
    }

    @Override
    public Object getClientLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public Object getCurrentScreen() {
        return Minecraft.getInstance().screen;
    }

    @Override
    public Object getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    public void executeOnClientThread(Runnable runnable) {
        Minecraft.getInstance().execute(runnable);
    }

    @Override
    public boolean isOnClientThread() {
        return Minecraft.getInstance().isSameThread();
    }

    @Override
    public Object getSoundManager() {
        return Minecraft.getInstance().getSoundManager();
    }

    @Override
    public Object getClientPlayer() {
        return Minecraft.getInstance().player;
    }
}
