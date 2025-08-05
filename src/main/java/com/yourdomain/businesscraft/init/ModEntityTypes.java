package com.yourdomain.businesscraft.init;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.entity.TouristEntity;
import com.yourdomain.businesscraft.platform.PlatformServices;
import com.yourdomain.businesscraft.platform.RegistryHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.function.Supplier;

// Legacy Forge imports - kept for backwards compatibility during transition
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {
    // Legacy Forge registration system - kept for backwards compatibility
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BusinessCraft.MOD_ID);
    
    // Platform abstraction helper
    private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();
    
    // Platform-agnostic entity registrations
    public static Supplier<EntityType<TouristEntity>> TOURIST_PLATFORM;
    
    // Legacy Forge registered entities - kept for backwards compatibility
    public static final RegistryObject<EntityType<TouristEntity>> TOURIST = ENTITY_TYPES.register("tourist",
        () -> EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F) // Same size as regular villager
            .clientTrackingRange(10)
            .build(new ResourceLocation(BusinessCraft.MOD_ID, "tourist").toString())
    );
    
    /**
     * Initialize platform-agnostic entity registration.
     * This will eventually replace the legacy Forge system.
     */
    public static void initializePlatformRegistration() {
        // Register entities using platform abstraction
        TOURIST_PLATFORM = REGISTRY.registerEntity("tourist_platform",
            () -> EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.95F) // Same size as regular villager
                .clientTrackingRange(10)
                .build(new ResourceLocation(BusinessCraft.MOD_ID, "tourist_platform").toString())
        );
    }
    
    // Event handler to register entity attributes
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(TOURIST.get(), TouristEntity.createAttributes().build());
        // Also register attributes for platform version if it exists
        if (TOURIST_PLATFORM != null) {
            event.put(TOURIST_PLATFORM.get(), TouristEntity.createAttributes().build());
        }
    }
} 