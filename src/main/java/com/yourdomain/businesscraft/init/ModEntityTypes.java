package com.yourdomain.businesscraft.init;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.entity.TouristEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BusinessCraft.MOD_ID);
    
    // Register the Tourist entity type
    public static final RegistryObject<EntityType<TouristEntity>> TOURIST = ENTITY_TYPES.register("tourist",
        () -> EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F) // Same size as regular villager
            .clientTrackingRange(10)
            .build(new ResourceLocation(BusinessCraft.MOD_ID, "tourist").toString())
    );
    
    // Event handler to register entity attributes
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TOURIST.get(), TouristEntity.createAttributes().build());
    }
} 