package com.quackers29.businesscraft.fabric.platform;

import com.quackers29.businesscraft.api.ClientHelper;

/**
 * Fabric implementation of ClientHelper
 * Uses Fabric's MinecraftClient.getInstance() for client access
 */
public class FabricClientHelper implements ClientHelper {
    @Override
    public Object getMinecraft() {
        try {
            // Fabric uses MinecraftClient instead of Minecraft
            Class<?> minecraftClientClass = Class.forName("net.minecraft.client.MinecraftClient");
            return minecraftClientClass.getMethod("getInstance").invoke(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getClientLevel() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return null;
            return mc.getClass().getField("world").get(mc);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getCurrentScreen() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return null;
            return mc.getClass().getField("currentScreen").get(mc);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getFont() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return null;
            return mc.getClass().getField("textRenderer").get(mc);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void executeOnClientThread(Runnable runnable) {
        try {
            Object mc = getMinecraft();
            if (mc != null) {
                // Fabric's execute method
                mc.getClass().getMethod("execute", Runnable.class).invoke(mc, runnable);
            } else {
                runnable.run();
            }
        } catch (Exception e) {
            runnable.run();
        }
    }

    @Override
    public boolean isOnClientThread() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return false;
            return (Boolean) mc.getClass().getMethod("isOnThread").invoke(mc);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Object getSoundManager() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return null;
            return mc.getClass().getMethod("getSoundManager").invoke(mc);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Object getClientPlayer() {
        try {
            Object mc = getMinecraft();
            if (mc == null) return null;
            return mc.getClass().getField("player").get(mc);
        } catch (Exception e) {
            return null;
        }
    }
}

