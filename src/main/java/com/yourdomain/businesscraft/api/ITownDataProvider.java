package com.yourdomain.businesscraft.api;

import net.minecraft.core.BlockPos;
import java.util.UUID;

/**
 * Platform-agnostic interface for accessing town data
 */
public interface ITownDataProvider {
    UUID getTownId();
    String getTownName();
    int getBreadCount();
    int getPopulation();
    boolean isTouristSpawningEnabled();
    void setTouristSpawningEnabled(boolean enabled);
    BlockPos getPathStart();
    void setPathStart(BlockPos pos);
    BlockPos getPathEnd();
    void setPathEnd(BlockPos pos);
    int getSearchRadius();
    void setSearchRadius(int radius);
    boolean canSpawnTourists();
    void markDirty();
    void addBread(int count);
    BlockPos getPosition();
    void addVisitor(UUID fromTownId);
    int getTotalVisitors();
} 