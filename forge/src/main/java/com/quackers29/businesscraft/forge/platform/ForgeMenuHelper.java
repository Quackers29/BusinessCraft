package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.MenuHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge implementation of MenuHelper
 */
public class ForgeMenuHelper implements MenuHelper {
    @Override
    public <M, U> void registerScreenFactory(
        Object menuType,
        MenuHelper.ScreenFactory<M, U> screenFactory
    ) {
        // This will be called during client setup
        // For now, we'll store the factories and register them later
    }
}
