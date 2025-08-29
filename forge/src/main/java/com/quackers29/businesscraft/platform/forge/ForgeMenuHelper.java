package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.MenuHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

/**
 * Enhanced Forge implementation of MenuHelper for TownInterfaceEntity migration.
 * Supports complex menu operations and menu type creation.
 */
public class ForgeMenuHelper implements MenuHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeMenuHelper.class);

    @Override
    public void refreshActiveMenu(Object player, String refreshType) {
        // TODO: Implement menu refresh logic for Forge
        // This method will be used to update active menus with new data
    }

    @Override
    public boolean openTownInterfaceMenu(Object player, int[] blockPos, String displayName) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("Cannot open town interface menu: player is not a ServerPlayer");
            return false;
        }

        BlockPos pos = new BlockPos(blockPos[0], blockPos[1], blockPos[2]);
        LOGGER.info("Opening Town Interface menu for player {} at position {} with display name '{}'",
                   serverPlayer.getName().getString(), pos, displayName);

        try {
            // Use NetworkHooks to open the town interface menu (Forge-specific)
            NetworkHooks.openScreen(serverPlayer, new net.minecraft.world.MenuProvider() {
                @Override
                public Component getDisplayName() {
                    LOGGER.debug("MenuProvider.getDisplayName() called, returning '{}'", displayName);
                    return Component.literal(displayName);
                }

                @Override
                public AbstractContainerMenu createMenu(int windowId,
                        Inventory inventory, Player player) {
                    LOGGER.debug("MenuProvider.createMenu() called with windowId={}, player={}", windowId, player.getName().getString());
                    try {
                        // Create the TownInterfaceMenu with the correct constructor
                        com.quackers29.businesscraft.menu.TownInterfaceMenu menu =
                            new com.quackers29.businesscraft.menu.TownInterfaceMenu(windowId, inventory, pos);
                        LOGGER.info("Successfully created TownInterfaceMenu with windowId {} for player {}", windowId, player.getName().getString());
                        return menu;
                    } catch (Exception e) {
                        LOGGER.error("Failed to create TownInterfaceMenu", e);
                        return null;
                    }
                }
            }, pos);
            LOGGER.info("NetworkHooks.openScreen() completed successfully for player {} at position {}", serverPlayer.getName().getString(), pos);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to open town interface menu", e);
            return false;
        }
    }

    @Override
    public <T> Supplier<Object> createSimpleMenuType(Object menuFactory) {
        if (menuFactory instanceof SimpleMenuFactory<?> factory) {
            return () -> new MenuType<>((windowId, inv) -> factory.create(windowId, inv), null);
        }
        return null;
    }

    @Override
    public <T> Supplier<Object> createDataDrivenMenuType(Object menuFactory) {
        if (menuFactory instanceof MenuFactory<?> factory) {
            return () -> IForgeMenuType.create((windowId, inv, data) -> factory.create(windowId, inv, data));
        }
        return null;
    }

    @Override
    public Supplier<Object> registerMenuType(String name, Supplier<Object> menuTypeSupplier) {
        // TODO: Implement menu type registration
        // This will integrate with the existing ModMenuTypes system
        LOGGER.debug("Menu type registration requested for '{}', but not yet implemented", name);
        return null;
    }

    @Override
    public boolean isMenuTypeRegistered(String name) {
        // TODO: Check if menu type is registered
        // This will integrate with the existing ModMenuTypes system
        LOGGER.debug("Menu type registration check requested for '{}', but not yet implemented", name);
        return false;
    }

    @Override
    public Object getMenuType(String name) {
        try {
            // Access the registered MenuType from ModMenuTypes
            if ("town_interface".equals(name)) {
                var menuTypeSupplier = com.quackers29.businesscraft.init.ModMenuTypes.TOWN_INTERFACE;
                if (menuTypeSupplier != null) {
                    MenuType<?> menuType = menuTypeSupplier.get();
                    LOGGER.debug("Retrieved MenuType '{}' from ModMenuTypes: {}", name, menuType);
                    return menuType;
                } else {
                    LOGGER.warn("MenuType supplier '{}' is null in ModMenuTypes", name);
                }
            } else {
                LOGGER.warn("MenuType '{}' not supported for retrieval", name);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to retrieve MenuType '{}' from ModMenuTypes", name, e);
        }
        return null;
    }

    // Functional interfaces for menu factories
    @FunctionalInterface
    public interface SimpleMenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory playerInventory);
    }

    @FunctionalInterface
    public interface MenuFactory<T extends AbstractContainerMenu> {
        T create(int containerId, Inventory playerInventory, FriendlyByteBuf data);
    }
}