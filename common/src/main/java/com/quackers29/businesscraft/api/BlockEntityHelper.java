package com.quackers29.businesscraft.api;

/**
 * Platform-agnostic interface for accessing block entity types.
 * Implementations will provide access to registered block entity types.
 */
public interface BlockEntityHelper {
    /**
     * Get the Town Interface block entity type
     */
    Object getTownInterfaceEntityType();
}
