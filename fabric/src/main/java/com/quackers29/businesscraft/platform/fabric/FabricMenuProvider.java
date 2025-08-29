package com.quackers29.businesscraft.platform.fabric;

import com.quackers29.businesscraft.platform.BusinessCraftMenuProvider;
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
 * Fabric implementation of BusinessCraftMenuProvider.
 * Handles Fabric-specific menu opening mechanics using ExtendedScreenHandlerFactory.
 */
public class FabricMenuProvider implements BusinessCraftMenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricMenuProvider.class);

    @Override
    public boolean openTownInterfaceMenu(Object player, int[] position) {
        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            LOGGER.warn("Cannot open town interface menu: player is not a ServerPlayerEntity");
            return false;
        }

        BlockPos pos = new BlockPos(position[0], position[1], position[2]);

        try {
            // Use Fabric's ExtendedScreenHandlerFactory for menu opening
            serverPlayer.openHandledScreen(new ExtendedScreenHandlerFactory() {
                @Override
                public Text getDisplayName() {
                    return Text.literal("Town Interface");
                }

                @Override
                public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                    // This will be handled by the block entity when it's migrated
                    // For now, return null to indicate the menu should be handled differently
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
            LOGGER.error("Failed to open town interface menu", e);
            return false;
        }
    }

    @Override
    public boolean openTradeMenu(Object player) {
        // TODO: Implement when TradeMenu is migrated
        LOGGER.debug("Trade menu opening not yet implemented");
        return false;
    }

    @Override
    public boolean openStorageMenu(Object player) {
        // TODO: Implement when StorageMenu is migrated
        LOGGER.debug("Storage menu opening not yet implemented");
        return false;
    }

    @Override
    public boolean openPaymentBoardMenu(Object player, Object data) {
        // TODO: Implement when PaymentBoardMenu is migrated
        LOGGER.debug("Payment board menu opening not yet implemented");
        return false;
    }

    @Override
    public boolean canOpenMenu(Object player, String menuType) {
        return player instanceof ServerPlayerEntity;
    }

    @Override
    public String getMenuDisplayName(String menuType) {
        return switch (menuType) {
            case "town_interface" -> "Town Interface";
            case "trade" -> "Trade Menu";
            case "storage" -> "Storage Menu";
            case "payment_board" -> "Payment Board";
            default -> "Menu";
        };
    }
}
