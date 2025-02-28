package com.yourdomain.businesscraft.client;

import com.yourdomain.businesscraft.BusinessCraft;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Handles key inputs for toggling the Town Debug Overlay (F4)
 */
@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, value = Dist.CLIENT)
public class TownDebugKeyHandler {
    
    /**
     * Handles key presses to detect F4 key
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Only handle key press events (not releases)
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }
        
        // Handle F4 key
        if (event.getKey() == GLFW.GLFW_KEY_F4) {
            // Toggle the debug overlay visibility
            TownDebugOverlay.toggleVisibility();
        }
    }
} 