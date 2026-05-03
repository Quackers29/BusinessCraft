package com.quackers29.businesscraft.client;

import com.quackers29.businesscraft.api.ClientHelper;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformPathCreationModePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class PlatformPathKeyHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    private static boolean isActive = false;
    private static BlockPos townPos;
    private static UUID platformId;

    public static void initialize() {
        PlatformAccess.getEvents().registerKeyInputCallback(PlatformPathKeyHandler::onKeyPress);
    }

    public static void setActivePlatform(BlockPos pos, UUID id) {
        isActive = true;
        townPos = pos;
        platformId = id;

        LOGGER.debug("Platform path key handler activated for platform {} at {}", id, pos);
    }

    public static void clearActivePlatform() {
        isActive = false;
        townPos = null;
        platformId = null;

        LOGGER.debug("Platform path key handler deactivated");
    }

    private static boolean onKeyPress(int keyCode, int action) {
        if (!isActive) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            LOGGER.debug("ESC key pressed, exiting platform path creation mode");

            PlatformAccess.getNetworkMessages().sendToServer(new SetPlatformPathCreationModePacket(
                townPos,
                platformId,
                false
            ));

            ClientHelper clientHelper = PlatformAccess.getClient();
            if (clientHelper != null) {
                Object playerObj = clientHelper.getClientPlayer();
                if (playerObj instanceof Player player) {
                    player.displayClientMessage(
                        Component.translatable("businesscraft.platform_path_cancelled"),
                        false
                    );
                }
            }

            clearActivePlatform();
            return false;
        }

        return false;
    }
}
