package com.quackers29.businesscraft.init;

import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.entity.TouristEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

public class CommonModEntityTypes {
    public static Supplier<EntityType<TouristEntity>> TOURIST;

    public static void register() {
        TOURIST = PlatformAccess.getRegistry().registerEntityType("tourist",
                () -> EntityType.Builder.<TouristEntity>of(TouristEntity::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.95F) // Same size as regular villager
                        .clientTrackingRange(10)
                        .build("tourist"));
    }
}
