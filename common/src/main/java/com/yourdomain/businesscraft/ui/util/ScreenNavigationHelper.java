package com.yourdomain.businesscraft.ui.util;

import com.yourdomain.businesscraft.api.PlatformAccess;
import com.yourdomain.businesscraft.network.packets.ui.OpenTownInterfacePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;

/**
 * Utility class to handle common screen navigation patterns
 */
public class ScreenNavigationHelper {
    
    /**
     * Returns to the main town interface screen from any other screen
     * Uses proper server-side menu opening for ContainerData synchronization
     * 
     * @param minecraft The Minecraft client instance
     * @param player The player whose inventory will be used in the new screen
     * @param townBlockPos The position of the town block entity
     */
    public static void returnToTownInterface(Minecraft minecraft, Player player, BlockPos townBlockPos) {
        // Send a packet to the server to open the TownInterface menu properly
        // This ensures proper ContainerData synchronization unlike direct client-side creation
        PlatformAccess.getNetworkMessages().sendToServer(new OpenTownInterfacePacket(townBlockPos));
    }
} 