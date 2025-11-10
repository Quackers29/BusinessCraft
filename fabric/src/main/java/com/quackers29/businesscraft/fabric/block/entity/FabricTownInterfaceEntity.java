package com.quackers29.businesscraft.fabric.block.entity;

import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.fabric.init.FabricModBlockEntities;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.Item;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fabric-specific implementation of TownInterfaceEntity.
 * Since the common TownInterfaceEntity extends MenuProvider (Forge-specific),
 * we need a Fabric-compatible implementation that replicates the functionality.
 *
 * This class provides the same business logic as the common TownInterfaceEntity
 * but without Forge-specific dependencies.
 */
public class FabricTownInterfaceEntity extends BlockEntity {
    private UUID townId;
    private String name;
    
    // Constructor that accepts type - used by FabricBlockEntityTypeBuilder factory
    public FabricTownInterfaceEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    // Constructor that gets type from FabricModBlockEntities - used for manual creation
    public FabricTownInterfaceEntity(BlockPos pos, BlockState state) {
        this(FabricModBlockEntities.TOWN_INTERFACE_ENTITY_TYPE, pos, state);
    }
    
    /**
     * Helper method to find TownManager.get() method compatible with ServerWorld
     * Avoids loading Forge's ServerLevel class name which triggers dependency loading
     */
    private static java.lang.reflect.Method findTownManagerGetMethod(Class<?> townManagerClass, Object serverWorld) {
        for (java.lang.reflect.Method m : townManagerClass.getMethods()) {
            if ("get".equals(m.getName()) && m.getParameterCount() == 1) {
                // Check if parameter type is compatible with ServerWorld
                Class<?> paramType = m.getParameterTypes()[0];
                if (paramType.isInstance(serverWorld)) {
                    return m;
                }
            }
        }
        return null;
    }

    // Basic getters - TODO: Implement full functionality from common TownInterfaceEntity
    
    public UUID getTownId() {
        return townId;
    }
    
    public void setTownId(UUID id) {
        this.townId = id;
        if (getWorld() instanceof ServerWorld serverWorld) {
            try {
                Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                if (getMethod == null) {
                    this.name = "Unnamed";
                    return;
                }
                Object townManager = getMethod.invoke(null, serverWorld);
                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                Town town = (Town) getTownMethod.invoke(townManager, id);
                this.name = town != null ? town.getName() : "Unnamed";
            } catch (Exception e) {
                this.name = "Unnamed";
            }
        }
    }
    
    public String getTownName() {
        if (townId != null && getWorld() instanceof ServerWorld serverWorld) {
            try {
                Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                if (getMethod == null) {
                    return name != null ? name : "Initializing...";
                }
                Object townManager = getMethod.invoke(null, serverWorld);
                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                Town town = (Town) getTownMethod.invoke(townManager, townId);
                if (town != null) {
                    return town.getName();
                }
            } catch (Exception e) {
                // Fall through to return cached name
            }
        }
        return name != null ? name : "Initializing...";
    }
    
    public int getPopulation() {
        if (townId != null && getWorld() instanceof ServerWorld serverWorld) {
            try {
                Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                if (getMethod == null) {
                    return 5; // Default
                }
                Object townManager = getMethod.invoke(null, serverWorld);
                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                Town town = (Town) getTownMethod.invoke(townManager, townId);
                if (town != null) {
                    return town.getPopulation();
                }
            } catch (Exception e) {
                // Fall through to return default
            }
        }
        return 5; // Default
    }
    
    public int getSearchRadius() {
        if (townId != null && getWorld() instanceof ServerWorld serverWorld) {
            try {
                Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                if (getMethod == null) {
                    return 10; // Default
                }
                Object townManager = getMethod.invoke(null, serverWorld);
                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                Town town = (Town) getTownMethod.invoke(townManager, townId);
                if (town != null) {
                    return town.getSearchRadius();
                }
            } catch (Exception e) {
                // Fall through to return default
            }
        }
        return 10; // Default
    }
    
    public List<Platform> getPlatforms() {
        // TODO: Implement platform storage
        return new ArrayList<>();
    }
    
    public boolean addPlatform() {
        // TODO: Implement platform addition
        return false;
    }
    
    public boolean removePlatform(UUID platformId) {
        // TODO: Implement platform removal
        return false;
    }
    
    public Platform getPlatform(UUID platformId) {
        // TODO: Implement platform retrieval
        return null;
    }
    
    public boolean canAddMorePlatforms() {
        // TODO: Implement platform limit check
        return false;
    }
    
    public Map<Item, Integer> getClientResources() {
        // TODO: Implement client resource caching
        return Collections.emptyMap();
    }
    
    public Map<Item, Integer> getClientCommunalStorage() {
        // TODO: Implement client communal storage caching
        return Collections.emptyMap();
    }
    
    public ITownDataProvider getTownDataProvider() {
        if (townId != null && getWorld() instanceof ServerWorld serverWorld) {
            try {
                Class<?> townManagerClass = Class.forName("com.quackers29.businesscraft.town.TownManager");
                java.lang.reflect.Method getMethod = findTownManagerGetMethod(townManagerClass, serverWorld);
                if (getMethod == null) {
                    return null;
                }
                Object townManager = getMethod.invoke(null, serverWorld);
                java.lang.reflect.Method getTownMethod = townManagerClass.getMethod("getTown", UUID.class);
                Town town = (Town) getTownMethod.invoke(townManager, townId);
                if (town != null) {
                    return town;
                }
            } catch (Exception e) {
                // Fall through to return null
            }
        }
        return null;
    }
}
