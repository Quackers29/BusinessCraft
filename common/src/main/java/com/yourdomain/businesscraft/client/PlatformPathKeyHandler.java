package com.yourdomain.businesscraft.client;

// BusinessCraft moved to platform-specific module
import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
@Mod.EventBusSubscriber(modid = "businesscraft", value = Dist.CLIENT)
public class PlatformPathKeyHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static boolean isActive = false;
    private static BlockPos townPos;
    private static UUID platformId;
    
    /**
     * Set the active platform for path creation
     */
    public static void setActivePlatform(BlockPos pos, UUID id) {
        isActive = true;
        townPos = pos;
        platformId = id;
        
        LOGGER.debug("Platform path key handler activated for platform {} at {}", id, pos);
    }
    
    /**
     * Clear the active platform
     */
    public static void clearActivePlatform() {
        isActive = false;
        townPos = null;
        platformId = null;
        
        LOGGER.debug("Platform path key handler deactivated");
    }
    
    /**
     * Handle key press events
     */
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onKeyPress(InputEvent.Key event) {
        if (!isActive) return;
        
        // Check for ESC key
        if (event.getKey() == GLFW.GLFW_KEY_ESCAPE && event.getAction() == GLFW.GLFW_PRESS) {
            LOGGER.debug("ESC key pressed, exiting platform path creation mode");
            
            // Send packet to exit path creation mode
            PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformPathCreationModePacket(
                townPos,
                platformId,
                false
            ));
            
            // Display message to player
            Minecraft.getInstance().player.displayClientMessage(
                Component.translatable("businesscraft.platform_path_cancelled"),
                false
            );
            
            // Clear active platform
            clearActivePlatform();
        }
    }
} 