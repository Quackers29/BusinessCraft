package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

/**
 * Platform-agnostic block entity registration using the RegistryHelper abstraction.
 * This system works across different mod loaders (Forge, Fabric, etc.).
 */
public class ModBlockEntities {
        // Platform abstraction helper
        private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();

        // Platform-agnostic block entity registrations
        public static Supplier<BlockEntityType<TownInterfaceEntity>> TOWN_INTERFACE_ENTITY;

        /**
         * Initialize all block entity registrations.
         * This should be called during mod initialization.
         */
        public static void initialize() {
                // Register block entities using platform abstraction
                TOWN_INTERFACE_ENTITY = REGISTRY.registerBlockEntity("town_interface",
                                () -> BlockEntityType.Builder.of(
                                        (pos, state) -> new TownInterfaceEntity(pos, state),
                                        ModBlocks.TOWN_INTERFACE.get()).build(null));
        }
} 