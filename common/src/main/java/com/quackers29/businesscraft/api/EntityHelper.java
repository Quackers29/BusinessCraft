package com.quackers29.businesscraft.api;

import net.minecraft.world.entity.EntityType;

/**
 * Platform-agnostic interface for accessing entity types.
 * Implementations will provide access to registered entity types.
 */
public interface EntityHelper {
    /**
     * Get the Tourist entity type
     */
    EntityType<?> getTouristEntityType();
}
