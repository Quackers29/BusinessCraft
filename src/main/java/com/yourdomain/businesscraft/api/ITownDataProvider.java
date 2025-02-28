package com.yourdomain.businesscraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.UUID;

/**
 * Platform-agnostic interface for accessing town data
 */
public interface ITownDataProvider {
    UUID getTownId();
    String getTownName();
    
    // Legacy resource methods
    int getBreadCount();
    
    // New generic resource methods
    void addResource(Item item, int count);
    int getResourceCount(Item item);
    Map<Item, Integer> getAllResources();
    
    // Population methods
    int getPopulation();
    
    // Other town data
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
    
    // Legacy method - delegate to addResource
    default void addBread(int count) {
        addResource(net.minecraft.world.item.Items.BREAD, count);
    }
    
    BlockPos getPosition();
    void addVisitor(UUID fromTownId);
    int getTotalVisitors();
} 