package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import java.util.Map;
import java.util.UUID;

/**
 * Menu container for the Town Interface block.
 * This class manages the data displayed in the Town Interface UI.
 */
public class TownInterfaceMenu extends AbstractContainerMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/TownInterfaceMenu");
    private final BlockPos pos;
    private final Level level;
    private Town town;
    private UUID townId;
    
    // Town properties for UI display (fallbacks)
    private String townName = "New Town";
    private int townLevel = 1;
    private int townPopulation = 5; // Default population to 5 like TownBlock
    private int townReputation = 50;
    
    // Tourism data (fallbacks)
    private int currentTourists = 0;
    private int maxTourists = 5;
    
    // Economy data
    private int goldCoins = 0;
    private int silverCoins = 0;
    private int bronzeCoins = 0;
    
    // Settings
    private boolean autoCollectEnabled = false;
    private boolean taxesEnabled = false;

    /**
     * Constructor for server-side menu creation
     */
    public TownInterfaceMenu(int windowId, Inventory inv, BlockPos pos) {
        super(ModMenuTypes.TOWN_INTERFACE.get(), windowId);
        this.pos = pos;
        this.level = inv.player.level();
        
        // Get town from TownManager
        if (level instanceof ServerLevel serverLevel) {
            TownManager townManager = TownManager.get(serverLevel);
            
            // Find the town at this position by iterating through all towns
            Map<UUID, Town> allTowns = townManager.getAllTowns();
            for (Town t : allTowns.values()) {
                if (t.getPosition().equals(pos)) {
                    this.town = t;
                    this.townId = t.getId();
                    LOGGER.debug("Found town with ID {} at position {}", this.townId, pos);
                    break;
                }
            }
            
            // If no town found, try to get from block entity
            if (this.town == null) {
                LOGGER.debug("No town found exactly at position {}, checking block entity", pos);
                if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                    UUID entityTownId = townEntity.getTownId();
                    if (entityTownId != null) {
                        this.town = townManager.getTown(entityTownId);
                        this.townId = entityTownId;
                        LOGGER.debug("Found town with ID {} from block entity", this.townId);
                    }
                }
            }
            
            if (this.town == null) {
                LOGGER.debug("No town found at or associated with position {}", pos);
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
        // No inventory slots to manage
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) < 64;
    }
    
    // Getters for UI data
    
    public String getTownName() {
        if (town != null) {
            return town.getName();
        }
        
        // Try to get name from town entity
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
            return townEntity.getTownName();
        }
        
        return "Unknown Town";
    }
    
    public int getTownPopulation() {
        if (town != null) {
            return town.getPopulation();
        }
        
        // Try to get population from town entity
        if (level != null && level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
            return townEntity.getPopulation();
        }
        
        return townPopulation; // Default fallback is 5
    }
    
    /**
     * Gets the current number of tourists in this town
     */
    public int getCurrentTourists() {
        if (town != null) {
            return town.getTouristCount();
        }
        
        // Try to get tourist count from town entity
        if (level != null && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the tourist count
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return townFromEntity.getTouristCount();
                    }
                }
            }
        }
        
        return currentTourists; // Default fallback
    }
    
    /**
     * Gets the maximum number of tourists this town can support
     */
    public int getMaxTourists() {
        if (town != null) {
            return town.getMaxTourists();
        }
        
        // Try to get max tourists from town entity
        if (level != null && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                // In TownBlockEntity, we need to get the Town and then get the max tourists
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return townFromEntity.getMaxTourists();
                    }
                }
            }
        }
        
        return maxTourists; // Default fallback
    }
    
    public int getTownLevel() {
        return townLevel;
    }
    
    public int getTownReputation() {
        return townReputation;
    }
    
    public int getGoldCoins() {
        return goldCoins;
    }
    
    public int getSilverCoins() {
        return silverCoins;
    }
    
    public int getBronzeCoins() {
        return bronzeCoins;
    }
    
    public boolean isAutoCollectEnabled() {
        return autoCollectEnabled;
    }
    
    public void setAutoCollectEnabled(boolean enabled) {
        this.autoCollectEnabled = enabled;
        // In a real implementation, this would trigger a server-side update
    }
    
    public boolean isTaxesEnabled() {
        return taxesEnabled;
    }
    
    public void setTaxesEnabled(boolean enabled) {
        this.taxesEnabled = enabled;
        // In a real implementation, this would trigger a server-side update
    }
    
    /**
     * Gets the search radius for this town
     * @return the search radius value
     */
    public int getSearchRadius() {
        if (town != null) {
            return town.getSearchRadius();
        }
        
        // Try to get search radius from town entity
        if (level != null && !level.isClientSide()) {
            if (level.getBlockEntity(pos) instanceof TownBlockEntity townEntity) {
                UUID entityTownId = townEntity.getTownId();
                if (entityTownId != null && level instanceof ServerLevel serverLevel) {
                    Town townFromEntity = TownManager.get(serverLevel).getTown(entityTownId);
                    if (townFromEntity != null) {
                        return townFromEntity.getSearchRadius();
                    }
                }
            }
        }
        
        // Default fallback
        return 10; // Default search radius
    }
    
    /**
     * Gets the block position of this town interface
     * @return the position
     */
    public BlockPos getPos() {
        return pos;
    }
} 