package com.yourdomain.businesscraft.client;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.network.ModMessages;
import com.yourdomain.businesscraft.network.SetPlatformPathCreationModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.UUID;

/**
 * Handles key inputs for exiting platform path creation mode
 */
@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, value = Dist.CLIENT)
public class PlatformPathKeyHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static BlockPos activeTownBlockPos = null;
    private static UUID activePlatformId = null;
    
    /**
     * Sets the active town block and platform ID for path creation
     */
    public static void setActivePlatform(BlockPos pos, UUID platformId) {
        LOGGER.debug("Setting active platform {} for town block at {}", platformId, pos);
        activeTownBlockPos = pos;
        activePlatformId = platformId;
    }
    
    /**
     * Clears the active platform
     */
    public static void clearActivePlatform() {
        activeTownBlockPos = null;
        activePlatformId = null;
        LOGGER.debug("Cleared active platform");
    }
    
    /**
     * Handles key presses to detect ESC key
     */
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        // Only handle if we have an active platform
        if (activeTownBlockPos == null || activePlatformId == null) {
            return;
        }
        
        // Handle ESC key press
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            LOGGER.debug("ESC pressed while in platform path creation mode");
            
            // Exit platform path creation mode
            ModMessages.sendToServer(new SetPlatformPathCreationModePacket(
                activeTownBlockPos,
                activePlatformId,
                false
            ));
            
            // Clear the active platform
            clearActivePlatform();
            
            // Show the town block screen again
            Minecraft.getInstance().execute(() -> {
                // Open the town block screen again
                Minecraft.getInstance().setScreen(null);
            });
        }
    }
} 