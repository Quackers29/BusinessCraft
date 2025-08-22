package com.quackers29.businesscraft.menu;

import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.debug.DebugConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * UNIFIED ARCHITECTURE: Simple, direct Town Interface Menu.
 * Works on both Forge and Fabric using direct TownManager access.
 */
public class TownInterfaceMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceMenu");
    private final BlockPos pos;
    private final Level level;
    private Town town;
    private UUID townId;
    
    // ContainerData for syncing values between server and client
    private static final int DATA_SEARCH_RADIUS = 0;
    private static final int DATA_TOURIST_COUNT = 1;
    private static final int DATA_MAX_TOURISTS = 2;
    private static final int DATA_POPULATION = 3;
    private SimpleContainerData data;

    /**
     * Constructor for server-side menu creation  
     */
    public TownInterfaceMenu(int windowId, Inventory inv, BlockPos pos) {
        super(null, windowId);  // Unified architecture - no MenuType needed
        this.pos = pos;
        this.level = inv.player.level();
        
        // Initialize container data with defaults
        this.data = new SimpleContainerData(4);
        this.data.set(DATA_SEARCH_RADIUS, 10);
        this.data.set(DATA_POPULATION, 5);
        this.data.set(DATA_TOURIST_COUNT, 0);
        this.data.set(DATA_MAX_TOURISTS, 5);
        addDataSlots(this.data);
        
        // Find the town at this position (server-side only)
        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            
            // Simple direct lookup
            for (Town t : townManager.getAllTowns()) {
                if (t.getX() == pos.getX() && t.getY() == pos.getY() && t.getZ() == pos.getZ()) {
                    this.town = t;
                    this.townId = t.getId();
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "Found town '{}' at position {}", t.getName(), pos);
                    updateDataSlots();
                    break;
                }
            }
            
            if (this.town == null) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_INTERFACE_MENU, "No town found at position {}", pos);
            }
        }
    }
    
    /**
     * Constructor for client-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, FriendlyByteBuf data) {
        this(windowId, inv, data.readBlockPos());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
    
    // Simple UI data getters - direct access to Town data
    
    public String getTownName() {
        return town != null ? town.getName() : "No Town";
    }
    
    public int getTownPopulation() {
        int populationFromData = data.get(DATA_POPULATION);
        return populationFromData > 0 ? populationFromData : (town != null ? town.getPopulation() : 5);
    }
    
    public int getCurrentTourists() {
        int touristsFromData = data.get(DATA_TOURIST_COUNT);
        return touristsFromData >= 0 ? touristsFromData : (town != null ? town.getTouristCount() : 0);
    }
    
    public int getMaxTourists() {
        int maxTouristsFromData = data.get(DATA_MAX_TOURISTS);
        return maxTouristsFromData > 0 ? maxTouristsFromData : (town != null ? town.getMaxTourists() : 5);
    }
    
    public int getSearchRadius() {
        int radiusFromData = data.get(DATA_SEARCH_RADIUS);
        return radiusFromData > 0 ? radiusFromData : (town != null ? town.getSearchRadius() : 10);
    }
    
    public BlockPos getBlockPos() {
        return pos;
    }

    private void updateDataSlots() {
        if (town != null) {
            data.set(DATA_POPULATION, town.getPopulation());
            data.set(DATA_TOURIST_COUNT, town.getTouristCount());
            data.set(DATA_MAX_TOURISTS, town.getMaxTourists());
            data.set(DATA_SEARCH_RADIUS, town.getSearchRadius());
        }
    }
    
    /**
     * Unified architecture - no MenuType needed
     */
    @Override
    public MenuType<?> getType() {
        return null;
    }
}