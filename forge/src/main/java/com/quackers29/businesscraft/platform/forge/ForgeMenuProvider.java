package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.BusinessCraftMenuProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
// Using fully qualified name to avoid collision with BusinessCraftMenuProvider
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge implementation of BusinessCraftMenuProvider.
 * Handles Forge-specific menu opening mechanics using NetworkHooks.
 */
public class ForgeMenuProvider implements BusinessCraftMenuProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeMenuProvider.class);

    @Override
    public boolean openTownInterfaceMenu(Object player, int[] position) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("Cannot open town interface menu: player is not a ServerPlayer");
            return false;
        }

        try {
            // Use Forge's NetworkHooks to open the menu
            // The actual menu implementation will be handled by the block entity
            NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    return Component.literal("Town Interface");
                }

                @Override
                public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
                    // This will be handled by the block entity when it's migrated
                    // For now, return null to indicate the menu should be handled differently
                    return null;
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
        return player instanceof ServerPlayer;
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
