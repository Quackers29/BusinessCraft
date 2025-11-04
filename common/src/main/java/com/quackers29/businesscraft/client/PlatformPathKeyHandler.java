package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.EventCallbacks;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.UUID;

/**
 * Handles key inputs for exiting platform path creation mode
 */
public class PlatformPathKeyHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    
    private static boolean isActive = false;
    private static BlockPos townPos;
    private static UUID platformId;
    
    /**
     * Initialize event callbacks. Should be called during mod initialization.
     */
    public static void initialize() {
        PlatformAccess.getEvents().registerKeyInputCallback(PlatformPathKeyHandler::onKeyPress);
    }
    
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
    private static boolean onKeyPress(int keyCode, int action) {
        if (!isActive) return false;
        
        // Check for ESC key
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            LOGGER.debug("ESC key pressed, exiting platform path creation mode");
            
            // Send packet to exit path creation mode
            PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformPathCreationModePacket(
                townPos,
                platformId,
                false
            ));
            
            // Display message to player
            com.quackers29.businesscraft.api.ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object playerObj = clientHelper.getClientPlayer();
                if (playerObj instanceof net.minecraft.world.entity.player.Player player) {
                    player.displayClientMessage(
                        Component.translatable("businesscraft.platform_path_cancelled"),
                        false
                    );
                }
            }
            
            // Clear active platform
            clearActivePlatform();
            return false; // Don't cancel the event
        }
        
        return false;
    }
} 
