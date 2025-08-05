package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.BusinessCraft;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.platform.RegistryHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.function.Supplier;

// Forge imports for event handling - will be abstracted later
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Platform-agnostic entity registration using the RegistryHelper abstraction.
 * This system works across different mod loaders (Forge, Fabric, etc.).
 */
@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEntityTypes {
    // Platform abstraction helper
    private static final RegistryHelper REGISTRY = PlatformServices.getRegistryHelper();
    
    // Platform-agnostic entity registrations
    public static Supplier<EntityType<TouristEntity>> TOURIST;
    
    /**
     * Initialize all entity registrations.
     * This should be called during mod initialization.
     */
    public static void initialize() {
        // Register entities using platform abstraction
        TOURIST = REGISTRY.registerEntity("tourist",
            () -> EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
                .sized(0.6F, 1.95F) // Same size as regular villager
                .clientTrackingRange(10)
                .build(new ResourceLocation(BusinessCraft.MOD_ID, "tourist").toString())
        );
    }
    
    /**
     * Event handler to register entity attributes.
     * TODO: This will be abstracted to platform services in a future phase.
     */
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        if (TOURIST != null) {
            event.put(TOURIST.get(), TouristEntity.createAttributes().build());
        }
    }
} 