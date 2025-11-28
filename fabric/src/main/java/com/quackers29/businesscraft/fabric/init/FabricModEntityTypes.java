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
import com.quackers29.businesscraft.init.CommonModEntityTypes;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/**
 * Fabric-specific entity type registrations
 */
public class FabricModEntityTypes {
    public static EntityType<TouristEntity> TOURIST;

    public static void register() {
        CommonModEntityTypes.register();
        TOURIST = CommonModEntityTypes.TOURIST.get();

        // Register default attributes (matches Forge)
        FabricDefaultAttributeRegistry.register(TOURIST, TouristEntity.createAttributes());
    }
}
