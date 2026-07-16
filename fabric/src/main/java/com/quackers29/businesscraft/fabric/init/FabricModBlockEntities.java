package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.fabric.block.entity.FabricTownInterfaceEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fabric block entity registration using direct Fabric API calls.
 */
public class FabricModBlockEntities {
    private static final Logger LOGGER = LoggerFactory.getLogger(FabricModBlockEntities.class);
    private static final String MOD_ID = "businesscraft";

    // Store the registered block entity type for later retrieval
    public static BlockEntityType<?> TOWN_INTERFACE_ENTITY_TYPE;
    private static boolean registrationAttempted = false;
    private static boolean registrationSuccessful = false;

    public static BlockEntityType<?> getTownInterfaceEntityType() {
        return TOWN_INTERFACE_ENTITY_TYPE;
    }

    public static void register() {
        DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "FabricModBlockEntities.register() called");
        if (registrationAttempted && registrationSuccessful) {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION,
                    "Block entities already registered successfully, skipping");
            return;
        }

        registrationAttempted = true;
        DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "Attempting block entity registration");

        net.minecraft.world.level.block.Block townInterfaceBlock = BuiltInRegistries.BLOCK
                .get(new ResourceLocation("businesscraft", "town_interface"));
        if (townInterfaceBlock == null) {
            LOGGER.warn("Town interface block not found in registry - blocks may not be registered yet");
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION,
                    "Found town interface block in registry: {}", townInterfaceBlock);
        }

        try {
            registerBlockEntities();
            registrationSuccessful = true;
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION,
                    "Block entity registration completed successfully");
        } catch (Exception e) {
            LOGGER.error("Block entity registration failed", e);
            try {
                Thread.sleep(1000);
                registerBlockEntities();
                registrationSuccessful = true;
                LOGGER.info("Block entity registration succeeded on second attempt");
            } catch (Exception e2) {
                LOGGER.error("Block entity registration failed on second attempt", e2);
                if (e2 instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Register block entities using direct Fabric API calls
     */
    private static void registerBlockEntities() {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION, "Registering FabricTownInterfaceEntity");

            net.minecraft.world.level.block.Block townInterfaceBlock = BuiltInRegistries.BLOCK
                    .get(new ResourceLocation("businesscraft", "town_interface"));

            if (townInterfaceBlock != null) {
                DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION,
                        "Creating block entity type for town interface block");

                TOWN_INTERFACE_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(
                        FabricTownInterfaceEntity::new,
                        townInterfaceBlock).build();

                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                        new ResourceLocation(MOD_ID, "town_interface_entity"), TOWN_INTERFACE_ENTITY_TYPE);

                DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION,
                        "Successfully registered FabricTownInterfaceEntity: {}", TOWN_INTERFACE_ENTITY_TYPE);
            } else {
                LOGGER.warn("Skipping block association - town interface block not found; registering type without block");
                TOWN_INTERFACE_ENTITY_TYPE = FabricBlockEntityTypeBuilder.create(
                        FabricTownInterfaceEntity::new).build();

                Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                        new ResourceLocation(MOD_ID, "town_interface_entity"), TOWN_INTERFACE_ENTITY_TYPE);
                DebugConfig.debug(LOGGER, DebugConfig.MOD_INITIALIZATION,
                        "Registered block entity type without block association: {}", TOWN_INTERFACE_ENTITY_TYPE);
            }

        } catch (Exception e) {
            LOGGER.error("Failed to register block entities", e);
        }
    }

}
