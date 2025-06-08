package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.block.entity.TownBlockEntity;
import com.yourdomain.businesscraft.init.ModMenuTypes;
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.yourdomain.businesscraft.ui.components.display.VisitHistoryComponent.VisitEntry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.core.BlockPos;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TownBlockMenu extends AbstractContainerMenu {
    private final TownBlockEntity blockEntity;
    private final ContainerData data;
    private static final Logger LOGGER = LogManager.getLogger("BusinessCraft/TownBlockMenu");
    private Town cachedTown;
    private UUID townId;
    
    // Constants for data indices
    private static final int DATA_BREAD = 0;
    private static final int DATA_POPULATION = 1;
    private static final int DATA_SPAWN_ENABLED = 2;
    private static final int DATA_CAN_SPAWN = 3;
    private static final int DATA_SEARCH_RADIUS = 4;
    private static final int DATA_TOURIST_COUNT = 5;
    private static final int DATA_MAX_TOURISTS = 6;

    public TownBlockMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public TownBlockMenu(int id, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.TOWN_BLOCK.get(), id);
        this.blockEntity = entity instanceof TownBlockEntity ? 
            (TownBlockEntity) entity : null;
        this.data = blockEntity != null ? 
            blockEntity.getContainerData() : new SimpleContainerData(7);
        
        if (blockEntity != null) {
            blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER)
                .ifPresent(handler -> {
                    addSlot(new SlotItemHandler(handler, 0, 180, 20));
                });
            addDataSlots(data);
        }
        
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public int getBreadCount() {
        return getProviderValueOr(ITownDataProvider::getBreadCount, data.get(DATA_BREAD));
    }

    public int getPopulation() {
        return getProviderValueOr(ITownDataProvider::getPopulation, data.get(DATA_POPULATION));
    }

    /**
     * Get the current number of tourists from this town
     * 
     * @return the number of active tourists
     */
    public int getTouristCount() {
        return getProviderValueOr(ITownDataProvider::getTouristCount, data.get(DATA_TOURIST_COUNT));
    }
    
    /**
     * Get the maximum number of tourists this town can currently support
     * 
     * @return the maximum tourist capacity
     */
    public int getMaxTourists() {
        return getProviderValueOr(ITownDataProvider::getMaxTourists, data.get(DATA_MAX_TOURISTS));
    }

    public String getTownName() {
        // Special handling for town name since it's a String
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            String name = provider.getTownName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        }
        
        if (blockEntity != null) {
            String name = blockEntity.getTownName();
            if (!name.isEmpty()) {
                return name;
            }
            return "Unregistered";
        }
        return "Invalid";
    }

    public boolean isTouristSpawningEnabled() {
        return getProviderValueOr(ITownDataProvider::isTouristSpawningEnabled, data.get(DATA_SPAWN_ENABLED) == 1);
    }

    public int getSearchRadius() {
        return getProviderValueOr(ITownDataProvider::getSearchRadius, data.get(DATA_SEARCH_RADIUS));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();

            if (index < 36) {
                if (!this.moveItemStackTo(itemstack1, 36, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, 36, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()),
                player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 122 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 180));
        }
    }

    public TownBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public Town getTown() {
        if (blockEntity != null) {
            UUID townId = blockEntity.getTownId();
            if (townId != null) {
                Level level = blockEntity.getLevel();
                if (level instanceof ServerLevel sLevel) {
                    return TownManager.get(sLevel).getTown(townId);
                }
            }
        }
        return null;
    }

    public ContainerData getData() {
        return data;
    }

    public ITownDataProvider getTownDataProvider() {
        if (blockEntity != null) {
            return blockEntity.getTownDataProvider();
        }
        return null;
    }
    
    /**
     * Gets all resources in the town.
     * @return A map of items to their quantities.
     */
    public Map<Item, Integer> getAllResources() {
        if (blockEntity != null) {
            // First try client-side cache if available
            if (blockEntity.getLevel().isClientSide() && !blockEntity.getClientResources().isEmpty()) {
                return blockEntity.getClientResources();
            }
            
            // Fallback to server-side data if needed
            ITownDataProvider provider = blockEntity.getTownDataProvider();
            if (provider != null) {
                return provider.getAllResources();
            }
        }
        return Collections.emptyMap();
    }
    
    /**
     * Gets all items in the town's communal storage.
     * @return A map of items to their quantities in communal storage.
     */
    public Map<Item, Integer> getAllCommunalStorageItems() {
        if (blockEntity != null) {
            // First try client-side cache if available
            if (blockEntity.getLevel().isClientSide() && !blockEntity.getClientCommunalStorage().isEmpty()) {
                return blockEntity.getClientCommunalStorage();
            }
            
            // Fallback to server-side data if needed
            ITownDataProvider provider = blockEntity.getTownDataProvider();
            if (provider != null) {
                return provider.getAllCommunalStorageItems();
            }
        }
        return Collections.emptyMap();
    }
    
    /**
     * Gets all items in a player's personal storage for this town.
     * 
     * @param playerId The UUID of the player
     * @return A map of items to their quantities in the player's personal storage
     */
    public Map<Item, Integer> getPersonalStorageItems(UUID playerId) {
        if (blockEntity != null && playerId != null) {
            // First try client-side cache if available
            if (blockEntity.getLevel().isClientSide()) {
                Map<Item, Integer> personalItems = blockEntity.getClientPersonalStorage(playerId);
                if (!personalItems.isEmpty()) {
                    return personalItems;
                }
            }
            
            // Fallback to server-side data if needed
            ITownDataProvider provider = blockEntity.getTownDataProvider();
            if (provider != null) {
                return provider.getPersonalStorageItems(playerId);
            }
        }
        return Collections.emptyMap();
    }
    
    /**
     * Gets the visit history for this town
     * @return List of visit entries
     */
    public List<VisitEntry> getVisitHistory() {
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            // Use the provider directly to get the visit history (single source of truth)
            return mapVisitHistoryToEntries(provider.getVisitHistory());
        }
        
        if (blockEntity != null) {
            // Fallback to using the block entity if provider is null
            return mapVisitHistoryToEntries(blockEntity.getVisitHistory());
        }
        
        return Collections.emptyList();
    }
    
    /**
     * Maps VisitHistoryRecord objects to VisitEntry objects for display
     * @param records The history records to map
     * @return A list of VisitEntry objects ready for UI display
     */
    private List<VisitEntry> mapVisitHistoryToEntries(List<VisitHistoryRecord> records) {
        return records.stream()
            .map(record -> {
                // Resolve the town name for display
                String townName = "Unknown";
                if (record.getOriginTownId() != null) {
                    townName = resolveTownName(record.getOriginTownId());
                }
                
                // Calculate direction from block position to visit origin 
                String direction = "";
                BlockPos originPos = record.getOriginPos();
                if (originPos != null && blockEntity != null) {
                    BlockPos townPos = blockEntity.getBlockPos();
                    if (originPos.getZ() < townPos.getZ()) direction += "N";
                    else if (originPos.getZ() > townPos.getZ()) direction += "S";
                    
                    if (originPos.getX() > townPos.getX()) direction += "E";
                    else if (originPos.getX() < townPos.getX()) direction += "W";
                    
                    // Calculate distance
                    int distance = (int) Math.sqrt(townPos.distSqr(originPos));
                    direction += " " + distance + "m";
                }
                
                return new VisitEntry(
                    townName,  // visitorName (using the resolved town name)
                    record.getTimestamp(),
                    record.getCount(),
                    direction,
                    record.getOriginPos()
                );
            })
            .collect(Collectors.<VisitEntry>toList());
    }
    
    /**
     * Resolves a town name from its UUID for display purposes
     */
    private String resolveTownName(UUID townId) {
        if (townId == null) return "Unknown";
        
        if (blockEntity != null) {
            // Use the block entity's town name cache for client-side resolution
            String name = blockEntity.getTownNameFromId(townId);
            // If the name returned is the fallback format, log it for debugging
            if (name.startsWith("Town-") && LOGGER.isDebugEnabled()) {
                LOGGER.debug("Falling back to UUID format for town {}", townId);
            }
            return name;
        }
        
        // Fallback - just show a portion of the UUID
        return "Town-" + townId.toString().substring(0, 8);
    }

    /**
     * A utility method to reduce boilerplate when getting values from providers
     * 
     * @param <T> The return type of the provider method
     * @param getter A function that extracts a value from an ITownDataProvider
     * @param defaultValue The default value to return if no provider exists
     * @return The value from the provider, or the default if unavailable
     */
    private <T> T getProviderValueOr(Function<ITownDataProvider, T> getter, T defaultValue) {
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            return getter.apply(provider);
        }
        return defaultValue;
    }
    
    /**
     * Gets the position of the town block
     */
    public BlockPos getPos() {
        if (blockEntity != null) {
            return blockEntity.getBlockPos();
        }
        return BlockPos.ZERO;
    }
}