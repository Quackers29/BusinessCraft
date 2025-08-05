package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.function.Supplier;

// Legacy Forge imports - kept for backwards compatibility during transition
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
        // Legacy Forge registration system - kept for backwards compatibility
        public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister
                        .create(ForgeRegistries.BLOCK_ENTITY_TYPES, BusinessCraft.MOD_ID);

        // Platform abstraction helper
        private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();

        // Platform-agnostic block entity registrations
        public static Supplier<BlockEntityType<TownInterfaceEntity>> TOWN_INTERFACE_ENTITY_PLATFORM;

        // Legacy Forge registered block entities - kept for backwards compatibility
        public static final RegistryObject<BlockEntityType<TownInterfaceEntity>> TOWN_INTERFACE_ENTITY = BLOCK_ENTITIES
                        .register("town_interface",
                                        () -> BlockEntityType.Builder.of(TownInterfaceEntity::new,
                                                        ModBlocks.TOWN_INTERFACE.get()).build(null));

        /**
         * Initialize platform-agnostic block entity registration.
         * This will eventually replace the legacy Forge system.
         */
        public static void initializePlatformRegistration() {
                // Register block entities using platform abstraction
                TOWN_INTERFACE_ENTITY_PLATFORM = REGISTRY.registerBlockEntity("town_interface_platform",
                                () -> BlockEntityType.Builder.of(TownInterfaceEntity::new,
                                                ModBlocks.TOWN_INTERFACE_PLATFORM.get()).build(null));
        }
} 