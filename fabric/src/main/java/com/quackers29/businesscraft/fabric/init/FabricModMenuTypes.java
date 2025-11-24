package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.menu.TownInterfaceMenu;
import com.quackers29.businesscraft.fabric.platform.FabricMenuTypeHelper;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric menu type registration
 * Uses Fabric's ScreenHandlerRegistry API to register menu types
 */
public class FabricModMenuTypes {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModMenuTypes.class);
    private static final String MOD_ID = "businesscraft";
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    public static void register() {
        if (registrationAttempted && registrationSuccessful) {
            return; // Already registered successfully
        }

        registrationAttempted = true;
        LOGGER.info("Registering Fabric menu types...");

        try {
            registerMenuTypes();
            registrationSuccessful = true;
            LOGGER.info("Fabric menu types registered successfully");
        } catch (Exception e) {
            LOGGER.error("Menu type registration failed: " + e.getMessage(), e);
        }
    }

    /**
     * Register Fabric-specific menu types
     */
    private static void registerMenuTypes() {
        try {
            // Register TownInterfaceMenu using ExtendedScreenHandlerType
            // Fabric API uses ResourceLocation (mapped from Identifier) when using Mojang
            // mappings
            // Use raw cast to bypass generic type mismatch if needed

            @SuppressWarnings("unchecked")
            ExtendedScreenHandlerType<TownInterfaceMenu> townInterfaceMenuType = (ExtendedScreenHandlerType<TownInterfaceMenu>) (Object) ScreenHandlerRegistry
                    .registerExtended(
                            new ResourceLocation(MOD_ID, "town_interface"),
                            (int syncId, net.minecraft.world.entity.player.Inventory inventory,
                                    net.minecraft.network.FriendlyByteBuf buf) -> new TownInterfaceMenu(syncId,
                                            inventory, buf));

            // Store in FabricMenuTypeHelper for PlatformAccess
            FabricMenuTypeHelper.setTownInterfaceMenuType(townInterfaceMenuType);

            LOGGER.info("Registered TownInterfaceMenu");
        } catch (Exception e) {
            LOGGER.error("Failed to register Fabric menu types: " + e.getMessage(), e);
            throw e;
        }
    }
}
