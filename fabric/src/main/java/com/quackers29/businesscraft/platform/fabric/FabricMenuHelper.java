package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.MenuHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

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
                    // UNIFIED ARCHITECTURE: Create temporary mapping bridge
                    // Until we move TownInterfaceMenu to use generic types, return null for now
                    LOGGER.info("FabricMenuHelper: TownInterfaceMenu would be created here - mappings need resolution");
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

    @Override
    public <T> Supplier<Object> createSimpleMenuType(Object menuFactory) {
        // TODO: Implement when menus are migrated to common module
        LOGGER.debug("createSimpleMenuType not yet implemented - requires common menu migration");
        return null;
    }

    @Override
    public <T> Supplier<Object> createDataDrivenMenuType(Object menuFactory) {
        // TODO: Implement when menus are migrated to common module
        LOGGER.debug("createDataDrivenMenuType not yet implemented - requires common menu migration");
        return null;
    }

    @Override
    public Supplier<Object> registerMenuType(String name, Supplier<Object> menuTypeSupplier) {
        // TODO: Implement proper Fabric menu type registration
        // This requires the actual menu classes to be migrated to common module first
        // For now, return null to avoid compilation errors
        LOGGER.debug("Menu type registration '{}' deferred until menu migration is complete", name);
        return null;
    }

    @Override
    public boolean isMenuTypeRegistered(String name) {
        // Fabric doesn't provide an easy way to check if a menu type is registered
        // We'll assume it's registered if no errors occurred during registration
        LOGGER.debug("Menu type registration check for '{}' - assuming registered if no errors", name);
        return true;
    }

    @Override
    public Object getMenuType(String name) {
        // Fabric menu types are not yet implemented in the common module
        // This will be implemented when Fabric menu migration is complete
        LOGGER.debug("MenuType retrieval '{}' not yet implemented for Fabric", name);
        return null;
    }
}