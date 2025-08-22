package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.MenuHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric implementation of MenuHelper using Yarn mappings.
 * Implements Fabric-specific menu opening using ExtendedScreenHandlerFactory.
 */
public class FabricMenuHelper implements MenuHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricMenuHelper.class);
    
    @Override
    public void refreshActiveMenu(Object player, String refreshType) {
        LOGGER.debug("FABRIC MENU HELPER: refreshActiveMenu not yet implemented");
        // TODO: Implement Fabric-specific menu refreshing
    }
    
    @Override
    public boolean openTownInterfaceMenu(Object player, int[] blockPos, String displayName) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        
        BlockPos pos = new BlockPos(blockPos[0], blockPos[1], blockPos[2]);
        
        try {
            // Use Fabric's ExtendedScreenHandlerFactory for menu opening (Yarn mappings)
            serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.literal(displayName);
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    // TODO: Create Fabric TownInterfaceMenu equivalent
                    // For now, return null to enable compilation - this will be implemented when we move to unified architecture
                    LOGGER.warn("FabricMenuHelper: TownInterfaceMenu creation not yet implemented - unified architecture migration needed");
                    return null;
                }

                @Override
                public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                    // Write the BlockPos to the buffer for client-side menu creation
                    buf.writeBlockPos(pos);
                }
            });
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to open town interface menu for Fabric player", e);
            return false;
        }
    }
}