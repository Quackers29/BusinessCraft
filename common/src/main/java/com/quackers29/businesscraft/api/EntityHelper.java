package com.quackers29.businesscraft.api;

public interface EntityHelper {
    Object getTouristEntityType();

    void setPathCreationTarget(Object player, java.util.UUID townId);

    void clearPathCreationTarget(Object player);

    Object getPlayerByUUID(Object level, java.util.UUID uuid);
}
