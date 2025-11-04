package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.ClientHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;

/**
 * Forge implementation of ClientHelper
 */
public class ForgeClientHelper implements ClientHelper {
    @Override
    public Object getMinecraft() {
        return Minecraft.getInstance();
    }

    @Override
    public Object getClientLevel() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level;
    }

    @Override
    public Object getCurrentScreen() {
        Minecraft mc = Minecraft.getInstance();
        return mc.screen;
    }

    @Override
    public Object getFont() {
        Minecraft mc = Minecraft.getInstance();
        return mc.font;
    }

    @Override
    public void executeOnClientThread(Runnable runnable) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(runnable);
    }

    @Override
    public boolean isOnClientThread() {
        return Minecraft.getInstance().isSameThread();
    }
}

