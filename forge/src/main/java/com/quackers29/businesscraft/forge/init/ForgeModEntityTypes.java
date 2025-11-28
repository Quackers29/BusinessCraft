package com.quackers29.businesscraft.forge.init;

import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * Forge-specific entity type registrations
 */
import com.quackers29.businesscraft.init.CommonModEntityTypes;

/**
 * Forge-specific entity type registrations
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeModEntityTypes {
    public static RegistryObject<EntityType<TouristEntity>> TOURIST;

    public static void register() {
        CommonModEntityTypes.register();
        // We need to cast the Supplier back to RegistryObject for Forge-specific event
        // handling if needed,
        // but for general usage, we can just use the common supplier.
        // However, ForgeModEntityTypes.TOURIST is public static and might be used
        // elsewhere as RegistryObject.
        // Let's check usages of ForgeModEntityTypes.TOURIST.
        // Actually, RegistryHelper on Forge returns a Supplier which IS a
        // RegistryObject.
        // So we can cast it.
        TOURIST = (RegistryObject<EntityType<TouristEntity>>) CommonModEntityTypes.TOURIST;
    }

    // Event handler to register entity attributes
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TOURIST.get(), TouristEntity.createAttributes().build());
    }
}
