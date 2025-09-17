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
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeModEntityTypes {
    public static RegistryObject<EntityType<TouristEntity>> TOURIST = ForgeRegistryHelper.ENTITY_TYPES.register("tourist",
            () -> EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.95F) // Same size as regular villager
                .clientTrackingRange(10)
                .build("tourist"));

    public static void register() {
        // Entity registration is handled by RegistryObject above
    }

    // Event handler to register entity attributes
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TOURIST.get(), TouristEntity.createAttributes().build());
    }
}
