package com.quackers29.businesscraft.fabric.init;

import com.quackers29.businesscraft.entity.TouristEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

/**
 * Fabric-specific entity type registrations
 */
public class FabricModEntityTypes {
    public static EntityType<TouristEntity> TOURIST;

    public static void register() {
        ResourceLocation id = new ResourceLocation("businesscraft", "tourist");
        
        EntityType<TouristEntity> touristType = FabricEntityTypeBuilder.create(
            MobCategory.CREATURE,
            TouristEntity::create
        )
        .dimensions(EntityDimensions.fixed(0.6F, 1.95F))
        .trackRangeBlocks(10)
        .trackedUpdateRate(3)
        .build();

        TOURIST = Registry.register(BuiltInRegistries.ENTITY_TYPE, id, touristType);

        // Register default attributes (matches Forge)
        FabricDefaultAttributeRegistry.register(TOURIST, TouristEntity.createAttributes());
    }
}
