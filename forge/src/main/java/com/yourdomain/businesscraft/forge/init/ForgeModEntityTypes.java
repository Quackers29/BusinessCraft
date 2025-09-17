package com.yourdomain.businesscraft.forge.init;

import com.yourdomain.businesscraft.entity.TouristEntity;
import com.yourdomain.businesscraft.forge.platform.ForgeRegistryHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge-specific entity type registrations
 */
@Mod.EventBusSubscriber(modid = "businesscraft", bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeModEntityTypes {
    // Register the Tourist entity type
    public static final EntityType<TouristEntity> TOURIST = EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
        .sized(0.6F, 1.95F) // Same size as regular villager
        .clientTrackingRange(10)
        .build(new ResourceLocation("businesscraft", "tourist").toString());

    public static void register() {
        ForgeRegistryHelper registry = (ForgeRegistryHelper) com.yourdomain.businesscraft.forge.BusinessCraftForge.REGISTRY;
        registry.registerEntityType("tourist", TOURIST);
    }

    // Event handler to register entity attributes
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TOURIST, TouristEntity.createAttributes().build());
    }
}
