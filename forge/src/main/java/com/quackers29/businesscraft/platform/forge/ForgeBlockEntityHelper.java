package com.quackers29.businesscraft.platform.forge;

import com.quackers29.businesscraft.platform.BlockEntityHelper;
import com.quackers29.businesscraft.platform.InventoryHelper;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.platform.ITownManagerService;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.network.ModMessages;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket;
import com.quackers29.businesscraft.network.packets.misc.PaymentResultPacket;
import com.quackers29.businesscraft.platform.PlatformServices;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
import com.quackers29.businesscraft.town.data.RewardEntry;
import com.quackers29.businesscraft.debug.DebugConfig;
import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.MenuProvider;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.util.HashSet;

/**
 * Forge implementation of the BlockEntityHelper interface using the capability system.
 * This class provides cross-platform block entity capability management for
 * inventory attachment and custom data storage.
 */
public class ForgeBlockEntityHelper implements BlockEntityHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgeBlockEntityHelper.class);
    
    // Thread-safe storage for custom data attachments
    private final Map<BlockEntity, Map<String, Object>> customDataStorage = new ConcurrentHashMap<>();
    
    // Track capability attachments for proper cleanup
    private final Map<BlockEntity, Map<String, LazyOptional<?>>> capabilityAttachments = new ConcurrentHashMap<>();
    
    @Override
    public Object attachInventory(BlockEntity blockEntity, InventoryHelper.PlatformInventory inventory, @Nullable Direction side) {
        // For Forge, we expect the inventory to wrap an IItemHandler
        
        // Get the ItemStackHandler from the inventory wrapper
        Object platformInventory = inventory.getPlatformInventory();
        if (!(platformInventory instanceof IItemHandler itemHandler)) {
            throw new IllegalArgumentException("Platform inventory must be an IItemHandler for Forge");
        }
        
        // Create LazyOptional capability
        LazyOptional<IItemHandler> lazyOptional = LazyOptional.of(() -> itemHandler);
        
        // Store capability attachment for cleanup
        capabilityAttachments
            .computeIfAbsent(blockEntity, k -> new HashMap<>())
            .put(getInventoryCapabilityKey() + (side != null ? "_" + side.getName() : ""), lazyOptional);
        
        return lazyOptional;
    }
    
    @Override
    public @Nullable InventoryHelper.PlatformInventory getInventory(BlockEntity blockEntity, @Nullable Direction side) {
        // This would typically query the block entity's getCapability method
        // For now, we'll return null as we need the block entity to implement the capability system
        // This will be properly implemented when we abstract the TownInterfaceEntity
        return null;
    }
    
    @Override
    public <T> Object attachData(BlockEntity blockEntity, String key, T data) {
        customDataStorage
            .computeIfAbsent(blockEntity, k -> new ConcurrentHashMap<>())
            .put(key, data);
        
        // Return a handle that can be used for removal
        return new DataHandle(blockEntity, key);
    }
    
    @Override
    public @Nullable <T> T getData(BlockEntity blockEntity, String key, Class<T> dataClass) {
        Map<String, Object> entityData = customDataStorage.get(blockEntity);
        if (entityData != null) {
            Object data = entityData.get(key);
            if (dataClass.isInstance(data)) {
                return dataClass.cast(data);
            }
        }
        return null;
    }
    
    @Override
    public void removeData(BlockEntity blockEntity, String key) {
        Map<String, Object> entityData = customDataStorage.get(blockEntity);
        if (entityData != null) {
            entityData.remove(key);
            if (entityData.isEmpty()) {
                customDataStorage.remove(blockEntity);
            }
        }
    }
    
    @Override
    public void invalidateAllAttachments(BlockEntity blockEntity) {
        // Invalidate all capability attachments
        Map<String, LazyOptional<?>> attachments = capabilityAttachments.remove(blockEntity);
        if (attachments != null) {
            attachments.values().forEach(LazyOptional::invalidate);
        }
        
        // Remove custom data
        customDataStorage.remove(blockEntity);
    }
    
    @Override
    public boolean hasCapability(BlockEntity blockEntity, String capabilityKey, @Nullable Direction side) {
        // Check if we have a stored capability attachment
        Map<String, LazyOptional<?>> attachments = capabilityAttachments.get(blockEntity);
        if (attachments != null) {
            String fullKey = capabilityKey + (side != null ? "_" + side.getName() : "");
            LazyOptional<?> capability = attachments.get(fullKey);
            return capability != null && capability.isPresent();
        }
        return false;
    }
    
    @Override
    public String getInventoryCapabilityKey() {
        return "inventory";
    }
    
    // ==== NEW METHODS FOR ENHANCED MULTILOADER PACKET SUPPORT ====
    
    // @Override
    public @Nullable Object getBlockEntity(Object player, int x, int y, int z) {
        // Platform service call - removed debug logging
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("Player object is not a ServerPlayer: {}", player);
            return null;
        }
        
        Level level = serverPlayer.level();
        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        
        // Block entity found - removed debug logging
            
        return blockEntity;
    }
    
    // @Override
    public @Nullable Object getTownDataProvider(Object blockEntity) {
        // Platform service call - removed debug logging
            
        if (blockEntity instanceof ITownDataProvider provider) {
            // Found town data provider - removed debug logging
            return provider;
        }
        
        // Block entity is not a town data provider - removed debug logging
        return null;
    }
    
    // @Override
    public boolean isTouristSpawningEnabled(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.isTouristSpawningEnabled();
        }
        return false;
    }
    
    // @Override
    public void setTouristSpawningEnabled(Object townDataProvider, boolean enabled) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.setTouristSpawningEnabled(enabled);
        }
    }
    
    // @Override
    public void markTownDataDirty(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.markDirty();
        }
    }
    
    // @Override
    public void syncTownData(Object blockEntity) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            townInterface.syncTownData();
        }
    }
    
    // @Override
    public @Nullable String getTownName(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.getTownName();
        }
        return null;
    }
    
    // @Override
    public void setTownName(Object townDataProvider, String townName) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.setTownName(townName);
        } else {
            LOGGER.warn("Cannot set town name: townDataProvider is not an ITownDataProvider instance");
        }
    }
    
    // @Override
    public @Nullable String getTownId(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.getTownId() != null ? provider.getTownId().toString() : null;
        }
        return null;
    }
    
    // @Override
    public boolean isTownDataInitialized(Object townDataProvider) {
        // TODO: Implement isTownDataInitialized - method doesn't exist in current ITownDataProvider
        LOGGER.warn("isTownDataInitialized not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public int getSearchRadius(Object townDataProvider) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            return provider.getSearchRadius();
        }
        return 100; // Default fallback
    }
    
    // @Override
    public void setSearchRadius(Object townDataProvider, int radius) {
        if (townDataProvider instanceof ITownDataProvider provider) {
            provider.setSearchRadius(radius);
        }
    }
    
    // @Override
    public boolean canAddMorePlatforms(Object blockEntity) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            return townInterface.canAddMorePlatforms();
        }
        return false;
    }
    
    // @Override
    public boolean addPlatform(Object blockEntity) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            Object result = townInterface.addPlatform();
            return result != null; // Return true if platform was successfully added
        }
        return false;
    }
    
    // @Override
    public void markBlockEntityChanged(Object blockEntity) {
        if (blockEntity instanceof BlockEntity be) {
            be.setChanged();
        }
    }
    
    // @Override
    public boolean deletePlatform(Object blockEntity, int platformIndex) {
        // TODO: Implement deletePlatform - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("deletePlatform not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean removePlatform(Object blockEntity, String platformId) {
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                java.util.UUID uuid = java.util.UUID.fromString(platformId);
                return townInterface.removePlatform(uuid);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid platform ID format: {}", platformId);
                return false;
            }
        }
        return false;
    }
    
    // @Override
    public int getPlatformCount(Object blockEntity) {
        // TODO: Implement getPlatformCount - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("getPlatformCount not yet implemented for Forge");
        return 0;
    }
    
    // @Override
    public void setPlatformEnabled(Object blockEntity, int platformIndex, boolean enabled) {
        // TODO: Implement setPlatformEnabled - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("setPlatformEnabled not yet implemented for Forge");
    }
    
    // @Override
    public boolean isPlatformEnabled(Object blockEntity, int platformIndex) {
        // TODO: Implement isPlatformEnabled - method doesn't exist in current TownInterfaceEntity
        LOGGER.warn("isPlatformEnabled not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public @Nullable Object getClientBlockEntity(int x, int y, int z) {
        // TODO: Implement client-side block entity access
        // This would require access to Minecraft.getInstance().level
        LOGGER.warn("getClientBlockEntity not yet implemented for Forge");
        return null;
    }
    
    // ==== PLACEHOLDER IMPLEMENTATIONS FOR NEW PACKET-RELATED METHODS ====
    // These need full implementation based on existing TownInterfaceEntity methods
    
    // @Override
    public boolean setPlatformDestinationEnabled(Object blockEntity, String platformId, String townId, boolean enabled) {
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Setting destination {} to {} for platform {} on block entity {}", townId, enabled, platformId, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                UUID platformUUID = UUID.fromString(platformId);
                UUID townUUID = UUID.fromString(townId);
                
                Object platformObj = townInterface.getPlatform(platformUUID);
                Platform platform = platformObj instanceof Platform ? (Platform) platformObj : null;
                if (platform == null) {
                    LOGGER.warn("Platform not found with ID: {}", platformId);
                    return false;
                }
                
                if (enabled) {
                    platform.enableDestination(townUUID);
                } else {
                    platform.disableDestination(townUUID);
                }
                
                // Mark the block entity as changed to trigger NBT save
                townInterface.setChanged();
                
                LOGGER.info("FORGE BLOCK ENTITY HELPER: Successfully set destination {} to {} for platform {}", townId, enabled, platformId);
                return true;
                
            } catch (IllegalArgumentException e) {
                LOGGER.error("FORGE BLOCK ENTITY HELPER: Invalid UUID format - platformId: {}, townId: {}", platformId, townId);
                return false;
            }
        }
        
        LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
        return false;
    }
    
    // @Override
    public Map<String, String> getAllTownsForDestination(Object blockEntity) {
        // TODO: Implement town destination retrieval
        LOGGER.warn("getAllTownsForDestination not yet implemented for Forge");
        return new HashMap<>();
    }
    
    // @Override
    public Map<String, Boolean> getPlatformDestinations(Object blockEntity, String platformId) {
        // TODO: Implement platform destination state retrieval
        LOGGER.warn("getPlatformDestinations not yet implemented for Forge");
        return new HashMap<>();
    }
    
    // @Override
    public @Nullable Object getOriginTown(Object blockEntity) {
        // TODO: Implement origin town retrieval
        LOGGER.warn("getOriginTown not yet implemented for Forge");
        return null;
    }
    
    // @Override
    public @Nullable int[] getTownPosition(Object town) {
        try {
            if (!(town instanceof ITownDataProvider)) {
                LOGGER.warn("getTownPosition called with non-ITownDataProvider: {}", town.getClass());
                return null;
            }
            
            ITownDataProvider townData = (ITownDataProvider) town;
            ITownDataProvider.Position position = townData.getPosition();
            
            if (position == null) {
                LOGGER.warn("Town position is null for town: {}", townData.getTownName());
                return null;
            }
            
            return new int[]{position.getX(), position.getY(), position.getZ()};
            
        } catch (Exception e) {
            LOGGER.error("Exception in getTownPosition: {}", e.getMessage());
            return null;
        }
    }
    
    // @Override
    public @Nullable Object getTownById(Object player, String townId) {
        try {
            if (!(player instanceof ServerPlayer)) {
                LOGGER.warn("getTownById called with non-ServerPlayer: {}", player.getClass());
                return null;
            }
            
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel level = serverPlayer.serverLevel();
            
            // Convert string to UUID
            UUID townUUID;
            try {
                townUUID = UUID.fromString(townId);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Invalid town ID format in getTownById: {}", townId);
                return null;
            }
            
            // Get town using direct unified access
            try {
                com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager.get(level);
                com.quackers29.businesscraft.town.Town town = townManager.getTown(townUUID);
                
                if (town != null) {
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found town by ID {}: {}", townId, town.getClass().getSimpleName());
                    return town;
                } else {
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town not found by ID: {}", townId);
                    return null;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to access getAllTowns method: {}", e.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            LOGGER.error("Exception in getTownById for {}: {}", townId, e.getMessage());
            return null;
        }
    }
    
    // @Override
    public boolean setPlatformPath(Object blockEntity, String platformId, int startX, int startY, int startZ, int endX, int endY, int endZ) {
        // TODO: Implement platform path setting
        LOGGER.warn("setPlatformPath not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean resetPlatformPath(Object blockEntity, String platformId) {
        // TODO: Implement platform path reset
        LOGGER.warn("resetPlatformPath not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean setPlatformEnabledById(Object blockEntity, String platformId, boolean enabled) {
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Setting platform {} enabled state to {} on block entity {}", platformId, enabled, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                UUID platformUUID = UUID.fromString(platformId);
                
                Object platformObj = townInterface.getPlatform(platformUUID);
                Platform platform = platformObj instanceof Platform ? (Platform) platformObj : null;
                if (platform == null) {
                    LOGGER.warn("Platform not found with ID: {}", platformId);
                    return false;
                }
                
                platform.setEnabled(enabled);
                
                // Mark the block entity as changed to trigger NBT save
                townInterface.setChanged();
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Successfully set platform {} enabled state to {}", platformId, enabled);
                return true;
                
            } catch (IllegalArgumentException e) {
                LOGGER.error("FORGE BLOCK ENTITY HELPER: Invalid platform ID format: {}", platformId);
                return false;
            }
        }
        
        LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
        return false;
    }
    
    // @Override
    public boolean setPlatformCreationMode(Object blockEntity, boolean mode, String platformId) {
        LOGGER.info("FORGE BLOCK ENTITY HELPER: Setting platform creation mode to {} for platform {} on block entity {}", mode, platformId, blockEntity.getClass().getSimpleName());
        
        if (blockEntity instanceof TownInterfaceEntity townInterface) {
            try {
                UUID platformUUID = UUID.fromString(platformId);
                townInterface.setPlatformCreationMode(mode, platformUUID);
                LOGGER.info("FORGE BLOCK ENTITY HELPER: Successfully set platform creation mode to {} for platform {}", mode, platformId);
                return true;
            } catch (IllegalArgumentException e) {
                LOGGER.error("FORGE BLOCK ENTITY HELPER: Invalid platform ID format: {}", platformId);
                return false;
            }
        }
        
        LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", blockEntity.getClass().getSimpleName());
        return false;
    }
    
    // @Override
    public Object processResourceTrade(Object blockEntity, Object player, Object itemStack, int slotId) {
        // Cast parameters to proper types
        if (!(blockEntity instanceof TownInterfaceEntity townEntity)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", 
                blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
            return null;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return null;
        }
        
        if (!(itemStack instanceof ItemStack itemToTrade)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: ItemStack parameter is invalid: {}", 
                itemStack != null ? itemStack.getClass().getSimpleName() : "null");
            return null;
        }
        
        // Check if the item to trade is empty
        if (itemToTrade.isEmpty()) {
            LOGGER.warn("Received empty item in trade packet from player {}", serverPlayer.getName().getString());
            return null;
        }
        
        // Get the server level
        Level level = serverPlayer.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            LOGGER.warn("Player level is not a ServerLevel");
            return null;
        }
        
        // Get the town manager using the common module abstraction
        TownManager townManager = TownManager.get(serverLevel);
        if (townManager == null) {
            LOGGER.error("Town manager is null");
            return null;
        }
        
        // Get the town from the town manager
        Town town = townManager.getTown(townEntity.getTownId());
        if (town == null) {
            LOGGER.warn("No town found for town block at position {} for player {}", 
                townEntity.getBlockPos(), serverPlayer.getName().getString());
            return null;
        }
        
        // Add the item to the town's resources (main logic from main branch)
        int itemCount = itemToTrade.getCount();
        town.addResource(itemToTrade.getItem(), itemCount);
        
        // MARK DIRTY AFTER ADDING RESOURCES (critical for persistence)
        townManager.markDirty();
        
        DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "Player {} traded {} x{} to town {}", 
            serverPlayer.getName().getString(), 
            itemToTrade.getItem().getDescription().getString(), 
            itemCount,
            town.getName());
        
        // Get current emerald count before calculation
        int currentEmeralds = town.getResourceCount(Items.EMERALD);
        DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "BEFORE TRADE: Town {} has {} emeralds", 
            town.getName(), currentEmeralds);
        
        // Calculate emeralds to give based on the trade
        int emeraldsToGive = calculateEmeralds(itemToTrade, town);
        
        // Create payment item (emeralds)
        ItemStack payment = ItemStack.EMPTY;
        if (emeraldsToGive > 0) {
            payment = new ItemStack(Items.EMERALD, emeraldsToGive);
            
            // Deduct emeralds from town resources
            town.addResource(Items.EMERALD, -emeraldsToGive);
            
            // Explicitly mark the town manager as dirty to persist changes
            townManager.markDirty();
            
            // Force the townInterfaceEntity to update and sync
            townEntity.setChanged();
            
            // Ensure town data is properly synchronized
            townEntity.syncTownData();
            
            // Get updated emerald count after deduction
            int newEmeralds = town.getResourceCount(Items.EMERALD);
            
            DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "AFTER TRADE: Town {} now has {} emeralds (deducted {})", 
                town.getName(), newEmeralds, emeraldsToGive);
                
            // Send feedback to the player about the trade AND the deduction
            serverPlayer.sendSystemMessage(Component.literal("Traded " + itemCount + " " + 
                itemToTrade.getItem().getDescription().getString() + 
                " to " + town.getName() + " for " + emeraldsToGive + " emeralds."));
            
            // Add explicit deduction notification
            serverPlayer.sendSystemMessage(Component.literal("§6" + emeraldsToGive + " emeralds were deducted from town resources."));
        } else {
            // Send feedback that no payment was given
            serverPlayer.sendSystemMessage(Component.literal("Traded " + itemCount + " " + 
                itemToTrade.getItem().getDescription().getString() + 
                " to " + town.getName() + " but received no payment."));
        }
        
        // Sync the block entity to update client-side resource cache
        BlockPos pos = townEntity.getBlockPos();
        level.sendBlockUpdated(pos, townEntity.getBlockState(), townEntity.getBlockState(), 
            Block.UPDATE_ALL);
        
        // Ensure town data is properly synchronized with the block entity
        townEntity.syncTownData();
        
        // Force the TownManager to save changes
        townManager.markDirty();
        
        // Return the payment ItemStack for client synchronization
        return payment;
    }
    
    /**
     * Calculate how many emeralds to give based on the trade amount
     * Based on main branch implementation
     */
    private int calculateEmeralds(ItemStack itemToTrade, Town town) {
        // Number of items needed to receive 1 emerald (from main branch)
        final int ITEMS_PER_EMERALD = 10;
        
        int itemCount = itemToTrade.getCount();
        int emeraldCount = itemCount / ITEMS_PER_EMERALD;
        
        // Check if town has enough emeralds
        int availableEmeralds = town.getResourceCount(Items.EMERALD);
        DebugConfig.debug(LOGGER, DebugConfig.TRADE_OPERATIONS, "Trade calculation: {} items = {} emeralds, town has {} emeralds available", 
            itemCount, emeraldCount, availableEmeralds);
        
        // If no emeralds would be awarded, return early
        if (emeraldCount <= 0) {
            return 0;
        }
        
        // Ensure the town has enough emeralds to pay
        if (availableEmeralds < emeraldCount) {
            LOGGER.warn("Town {} doesn't have enough emeralds for trade! Requested: {}, Available: {}", 
                town.getName(), emeraldCount, availableEmeralds);
            // Cap the emerald payment to what's available
            return availableEmeralds;
        }
        
        return emeraldCount;
    }
    
    // @Override
    public List<Object> getUnclaimedRewards(Object blockEntity) {
        // Cast block entity to proper type
        if (!(blockEntity instanceof TownInterfaceEntity townEntity)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", 
                blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
            return new ArrayList<>();
        }
        
        // Get the server level
        Level level = townEntity.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            LOGGER.warn("Block entity level is not a ServerLevel");
            return new ArrayList<>();
        }
        
        // Get the town manager using the common module abstraction
        TownManager townManager = TownManager.get(serverLevel);
        if (townManager == null) {
            LOGGER.error("Town manager is null");
            return new ArrayList<>();
        }
        
        // Get the town from the town manager
        Town town = townManager.getTown(townEntity.getTownId());
        if (town == null) {
            LOGGER.warn("No town found for town block at position {}", townEntity.getBlockPos());
            return new ArrayList<>();
        }
        
        // Get unclaimed rewards from the town's real TownPaymentBoard system
        // Use the Enhanced MultiLoader platform service bridge to access the payment board
        TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
        List<Object> rewards = new ArrayList<>();
        
        if (paymentBoard != null) {
            // Get all unclaimed rewards from the sophisticated payment board system
            List<RewardEntry> unclaimedRewards = paymentBoard.getUnclaimedRewards();
            
            // UNIFIED ARCHITECTURE: Town names refreshed by ForgeNetworkHelper before sending to client
            
            // Convert RewardEntry objects to the format expected by the UI
            for (RewardEntry entry : unclaimedRewards) {
                rewards.add(entry);
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "Retrieved {} unclaimed rewards from TownPaymentBoard for town {} (with refreshed town names)", 
                unclaimedRewards.size(), town.getName());
        } else {
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, 
                "No TownPaymentBoard available for town {} at {}", 
                town.getName(), townEntity.getBlockPos());
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Retrieved {} unclaimed rewards for town {} at {}", 
            rewards.size(), town.getName(), townEntity.getBlockPos());
        
        return rewards;
    }
    
    /**
     * UNIFIED ARCHITECTURE: Refresh cached town names in reward entries with fresh server data
     * This eliminates stale names issue in payment board (like map view system)
     */
    
    // @Override
    public Object claimPaymentBoardReward(Object blockEntity, Object player, String rewardId, boolean toBuffer) {
        if (!(blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity townEntity)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not TownInterfaceEntity: {}", 
                blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
            return false;
        }
        
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Player is not ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return false;
        }
        
        try {
            // Get the town using Enhanced MultiLoader Template
            java.util.UUID townId = townEntity.getTownId();
            if (townId == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town interface has no town ID");
                return false;
            }
            
            // Get the town using direct unified access
            net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) townEntity.getLevel();
            com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager.get(serverLevel);
            com.quackers29.businesscraft.town.Town town = townManager.getTown(townId);
            
            if (town == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No town found for ID: {}", townId);
                return false;
            }
            
            // Get the real TownPaymentBoard from the town
            TownPaymentBoard paymentBoard = (TownPaymentBoard) town.getPaymentBoard();
            if (paymentBoard == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town has no payment board");
                return false;
            }
            
            // Use the real payment board claiming system
            java.util.UUID rewardUUID = java.util.UUID.fromString(rewardId);
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Claiming reward {} for player {} (toBuffer: {})", rewardUUID, serverPlayer.getName().getString(), toBuffer);
            
            // Claim the reward using the sophisticated TownPaymentBoard system
            TownPaymentBoard.ClaimResult claimResult = paymentBoard.claimReward(rewardUUID, "ALL", toBuffer);
            
            if (claimResult == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No claim result returned");
                return false;
            }
            
            // Check if the claim was successful using the real ClaimResult
            boolean success = claimResult.isSuccess();
            
            if (success) {
                // Get actual claimed items from the payment board system
                java.util.List<net.minecraft.world.item.ItemStack> claimedItems = claimResult.getClaimedItems();
                
                if (!toBuffer) {
                    // Try to give items to player inventory first
                    boolean inventoryFull = false;
                    for (net.minecraft.world.item.ItemStack stack : claimedItems) {
                        if (!serverPlayer.getInventory().add(stack.copy())) {
                            // Inventory full - need to handle overflow
                            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Player inventory full for item: {} x{}", stack.getItem(), stack.getCount());
                            inventoryFull = true;
                            break;
                        } else {
                            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Added {} x{} to player {}'s inventory", stack.getCount(), stack.getItem(), serverPlayer.getName().getString());
                        }
                    }
                    
                    if (inventoryFull) {
                        // For inventory full case, re-claim to buffer
                        claimResult = paymentBoard.claimReward(rewardUUID, "ALL", true);
                        if (claimResult != null && claimResult.isSuccess()) {
                            townEntity.onTownBufferChanged();
                            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "§e" + claimResult.getMessage() + " (Items sent to buffer due to full inventory)"));
                        }
                    }
                } else {
                    // Items were claimed directly to buffer
                    townEntity.onTownBufferChanged();
                }
                
                // Mark town as dirty for saving
                town.markDirty();
                
                // Notify town interface entity that buffer has changed
                townEntity.onTownBufferChanged();
                
                // Send updated payment board data to client using real payment board system
                java.util.List<Object> rewards = new java.util.ArrayList<>();
                
                // Get all unclaimed rewards after the claim operation (reuse existing paymentBoard variable)
                List<RewardEntry> unclaimedRewards = paymentBoard.getUnclaimedRewards();
                
                // UNIFIED ARCHITECTURE: Town names refreshed by ForgeNetworkHelper before sending to client
                
                for (RewardEntry entry : unclaimedRewards) {
                    rewards.add(entry);
                }
                
                com.quackers29.businesscraft.platform.PlatformServices.getNetworkHelper()
                    .sendPaymentBoardResponsePacket(serverPlayer, rewards);

                // Send updated buffer storage data to client
                var bufferSlots = town.getPaymentBoard().getBufferStorageSlots();
                com.quackers29.businesscraft.platform.PlatformServices.getNetworkHelper()
                    .sendBufferSlotStorageResponsePacket(serverPlayer, bufferSlots);
                
                // Send success message to player using real claim result
                String message = claimResult.getMessage();
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a" + message));
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Successfully claimed reward {} for player {}", rewardId, serverPlayer.getName().getString());
                return true;
                
            } else {
                // Send failure message to player using real claim result
                String message = claimResult.getMessage();
                serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c" + message));
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Failed to claim reward {} for player {}: {}", 
                    rewardId, serverPlayer.getName().getString(), message);
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error claiming payment board reward for player {}: {}", 
                serverPlayer.getName().getString(), e.getMessage(), e);
            serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "§cError: Failed to process claim request"));
            return false;
        }
    }
    
    // @Override
    public boolean openPaymentBoardUI(Object blockEntity, Object player) {
        // Cast parameters to proper types
        if (!(blockEntity instanceof TownInterfaceEntity townEntity)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Block entity is not a TownInterfaceEntity: {}", 
                blockEntity != null ? blockEntity.getClass().getSimpleName() : "null");
            return false;
        }
        
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return false;
        }
        
        DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Opening Payment Board for player {} at position {}", 
            serverPlayer.getName().getString(), townEntity.getBlockPos());
        
        try {
            // Create the Payment Board menu provider from the town interface entity
            Object menuProviderObj = townEntity.createPaymentBoardMenuProvider();
            MenuProvider menuProvider = menuProviderObj instanceof MenuProvider ? (MenuProvider) menuProviderObj : null;
            
            // Use NetworkHooks to properly open the container with server-client sync
            // This matches the main branch implementation exactly
            NetworkHooks.openScreen(serverPlayer, menuProvider, townEntity.getBlockPos());
            
            DebugConfig.debug(LOGGER, DebugConfig.UI_MANAGERS, "Successfully opened Payment Board for player {}", 
                serverPlayer.getName().getString());
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to open Payment Board UI for player {} at position {}: {}", 
                serverPlayer.getName().getString(), townEntity.getBlockPos(), e.getMessage(), e);
            return false;
        }
    }
    
    // @Override
    public boolean openTownInterfaceUI(Object blockEntity, Object player) {
        // TODO: Implement Town Interface UI opening
        LOGGER.warn("openTownInterfaceUI not yet implemented for Forge");
        return false;
    }
    
    // @Override
    public boolean processTownMapDataRequest(Object blockEntity, Object player, int zoomLevel, boolean includeStructures) {
        if (!(blockEntity instanceof TownInterfaceEntity) || !(player instanceof ServerPlayer)) {
            return false;
        }
        
        try {
            TownInterfaceEntity townEntity = (TownInterfaceEntity) blockEntity;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Processing town map data request for zoom level: {} at position: {}", 
                        zoomLevel, townEntity.getBlockPos());
            
            // Generate structured map data for sophisticated map features
            Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> structuredMapData = 
                generateStructuredMapData(townEntity, serverPlayer, zoomLevel, includeStructures);
            
            // Send structured town data to client using updated packet
            sendStructuredMapDataToClient(serverPlayer, townEntity, structuredMapData, zoomLevel);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to process town map data request", e);
            return false;
        }
    }
    
    public boolean processBoundarySyncRequest(Object townDataProvider, Object player, boolean enableVisualization, int renderDistance) {
        if (!(townDataProvider instanceof TownInterfaceEntity) || !(player instanceof ServerPlayer)) {
            return false;
        }
        
        try {
            TownInterfaceEntity townEntity = (TownInterfaceEntity) townDataProvider;
            ServerPlayer serverPlayer = (ServerPlayer) player;
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
                "Processing boundary sync request (enable: {}, distance: {}) at position: {}", 
                enableVisualization, renderDistance, townEntity.getBlockPos());
            
            // Get town information from the TownInterfaceEntity
            UUID townId = townEntity.getTownId();
            if (townId == null) {
                LOGGER.warn("No town ID found for boundary sync request at {}", townEntity.getBlockPos());
                return false;
            }
            
            // Get the server level
            ServerLevel serverLevel = (ServerLevel) serverPlayer.level();
            
            // Get town from TownManager (main branch approach)
            com.quackers29.businesscraft.town.TownManager townManager = 
                com.quackers29.businesscraft.town.TownManager.get(serverLevel);
            
            com.quackers29.businesscraft.town.Town town = townManager.getTown(townId);
            if (town == null) {
                LOGGER.warn("No town found with ID {} for boundary sync request", townId);
                return false;
            }
            
            // Calculate boundary radius from town (main branch approach)
            int currentBoundaryRadius = town.getBoundaryRadius();
            
            // Send boundary update back to client
            BlockPos pos = townEntity.getBlockPos();
            com.quackers29.businesscraft.network.packets.ui.BoundarySyncResponsePacket responsePacket = 
                new com.quackers29.businesscraft.network.packets.ui.BoundarySyncResponsePacket(
                    pos.getX(), pos.getY(), pos.getZ(), currentBoundaryRadius);
            
            PlatformServices.getNetworkHelper().sendToClient(responsePacket, serverPlayer);
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
                "Successfully sent boundary sync response for town {} at {}: radius={}", 
                town.getName(), pos, currentBoundaryRadius);
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to process boundary sync request", e);
            return false;
        }
    }
    
    public boolean updateTownMapUI(Object player, int x, int y, int z, String mapData, int zoomLevel) {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "updateTownMapUI called with mapData: {}", mapData);
            
            // Find the currently open map modal and update it
            Minecraft minecraft = Minecraft.getInstance();
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Current screen: {}", minecraft.screen != null ? minecraft.screen.getClass().getSimpleName() : "null");
            
            if (minecraft.screen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) {
                com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
                    (com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) minecraft.screen;
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found TownMapModal, parsing JSON data...");
                
                // Parse the JSON map data to structured format for sophisticated features
                Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> structuredData = 
                    parseJsonToStructuredMapData(mapData);
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Parsed {} towns from JSON data", structuredData.size());
                for (Map.Entry<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> entry : structuredData.entrySet()) {
                    TownMapDataResponsePacket.TownMapInfo town = entry.getValue();
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Parsed town: {} at ({}, {}, {})", town.name, town.x, town.y, town.z);
                }
                
                // Directly set the parsed town data on the sophisticated map
                mapModal.setTownData(structuredData);
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Set parsed town data directly on sophisticated map: {} towns", structuredData.size());
                
                return true;
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No town map modal currently open to update");
            return false;
            
        } catch (Exception e) {
            LOGGER.error("Failed to update town map UI", e);
            return false;
        }
    }
    
    /**
     * Parse JSON map data back to structured format for client-side sophisticated features.
     */
    private Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> parseJsonToStructuredMapData(String jsonData) {
        Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> result = new HashMap<>();
        
        try {
            // Basic JSON parsing to extract town data
            if (jsonData == null || !jsonData.contains("\"towns\":{")) {
                return result;
            }
            
            // Extract the towns section - need to find the matching closing brace
            int townsStart = jsonData.indexOf("\"towns\":{") + 9;
            int townsEnd = findMatchingClosingBrace(jsonData, townsStart - 1); // -1 to include the opening brace
            
            if (townsStart > 9 && townsEnd > townsStart) {
                String townsSection = jsonData.substring(townsStart, townsEnd);
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Extracted towns section: {}", townsSection);
                
                // Parse individual town entries
                String[] townEntries = townsSection.split("},");
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Split into {} town entries", townEntries.length);
                for (String townEntry : townEntries) {
                    try {
                        if (!townEntry.endsWith("}")) {
                            townEntry += "}";
                        }
                        
                        TownMapDataResponsePacket.TownMapInfo townInfo = parseTownEntry(townEntry);
                        if (townInfo != null) {
                            result.put(townInfo.townId, townInfo);
                        }
                        
                    } catch (Exception e) {
                        LOGGER.warn("Failed to parse town entry: {}", e.getMessage());
                    }
                }
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Parsed {} towns from JSON map data", result.size());
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse JSON to structured map data: {}", e.getMessage());
        }
        
        return result;
    }
    
    /**
     * Find the matching closing brace for a given opening brace position.
     */
    private int findMatchingClosingBrace(String json, int openBracePos) {
        int braceCount = 0;
        for (int i = openBracePos; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1; // No matching brace found
    }
    
    /**
     * Parse a single town entry from JSON format.
     */
    private TownMapDataResponsePacket.TownMapInfo parseTownEntry(String townEntry) {
        try {
            // Extract town UUID (the key)
            int colonIndex = townEntry.indexOf(":");
            if (colonIndex == -1) return null;
            
            String townIdStr = townEntry.substring(0, colonIndex).trim().replaceAll("\"", "");
            java.util.UUID townId = java.util.UUID.fromString(townIdStr);
            
            // Extract town data
            String townDataJson = townEntry.substring(colonIndex + 1).trim();
            if (!townDataJson.startsWith("{") || !townDataJson.endsWith("}")) {
                return null;
            }
            
            // Parse individual fields
            String name = extractJsonString(townDataJson, "name");
            int x = extractJsonInt(townDataJson, "x");
            int y = extractJsonInt(townDataJson, "y");
            int z = extractJsonInt(townDataJson, "z");
            int population = extractJsonInt(townDataJson, "population");
            int visitCount = extractJsonInt(townDataJson, "visitCount");
            long lastVisited = extractJsonLong(townDataJson, "lastVisited");
            boolean isCurrentTown = extractJsonBoolean(townDataJson, "isCurrentTown");
            
            return new TownMapDataResponsePacket.TownMapInfo(
                townId, name, x, y, z, population, visitCount, lastVisited, isCurrentTown
            );
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse town entry '{}': {}", townEntry, e.getMessage());
            return null;
        }
    }
    
    /**
     * Helper methods for JSON parsing.
     */
    private String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) return "";
        start += pattern.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    private int extractJsonInt(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Integer.parseInt(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    private long extractJsonLong(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return 0L;
        start += pattern.length();
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    private boolean extractJsonBoolean(String json, String key) {
        String pattern = "\"" + key + "\":";
        int start = json.indexOf(pattern);
        if (start == -1) return false;
        start += pattern.length();
        return json.substring(start).startsWith("true");
    }
    
    /**
     * Generate structured map data for sophisticated map features.
     * This creates TownMapInfo objects for all towns in the area.
     */
    private Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> generateStructuredMapData(
            TownInterfaceEntity townEntity, ServerPlayer player, int zoomLevel, boolean includeStructures) {
        
        Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> townMapData = new HashMap<>();
        
        try {
            ServerLevel level = player.serverLevel();
            BlockPos currentPos = townEntity.getBlockPos();
            
            // Get town manager and find all towns
            Object townManager = getTownManager(level);
            if (townManager == null) {
                LOGGER.warn("No town manager found for map data generation");
                return townMapData;
            }
            
            // Debug: Check total towns available before radius filtering
            try {
                com.quackers29.businesscraft.town.TownManager directTownManager = com.quackers29.businesscraft.town.TownManager.get((ServerLevel) townManager);
                Collection<com.quackers29.businesscraft.town.Town> debugAllTownsCollection = directTownManager.getAllTowns();
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
                    "TownManager.getAllTowns() returned {} total towns before radius filtering", debugAllTownsCollection.size());
                for (com.quackers29.businesscraft.town.Town town : debugAllTownsCollection) {
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, 
                        "Town found: '{}' at {}", town.getTownName(), getTownPosition(town));
                }
            } catch (Exception e) {
                LOGGER.error("DEBUG: Failed to check total towns: {}", e.getMessage());
            }
            
            // Calculate search radius based on zoom level
            int searchRadius = calculateMapSearchRadius(zoomLevel);
            
            // Find all towns within the search radius
            List<Object> nearbyTowns = findTownsInRadius(townManager, currentPos, searchRadius);
            
            for (Object townObj : nearbyTowns) {
                try {
                    ITownDataProvider townData = (ITownDataProvider) townObj;
                    
                    if (townData.getTownName() == null || townData.getTownId() == null) {
                        continue;
                    }
                    
                    // Get town position
                    BlockPos townPos = getTownPosition(townData);
                    if (townPos == null) {
                        continue;
                    }
                    
                    // Check if this is the current town
                    boolean isCurrentTown = townPos.equals(currentPos);
                    
                    // Create TownMapInfo object with structured data
                    TownMapDataResponsePacket.TownMapInfo townInfo = new TownMapDataResponsePacket.TownMapInfo(
                        townData.getTownId(),
                        townData.getTownName(),
                        townPos.getX(),
                        townPos.getY(), 
                        townPos.getZ(),
                        getTownPopulation(townData),
                        getTownVisitCount(townData),
                        getTownLastVisited(townData),
                        isCurrentTown
                    );
                    
                    townMapData.put(townData.getTownId(), townInfo);
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to process town for map data: {}", e.getMessage());
                }
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Generated structured map data for {} towns within radius {}", 
                        townMapData.size(), searchRadius);
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate structured map data: {}", e.getMessage());
        }
        
        return townMapData;
    }
    
    /**
     * Send structured map data to client using the sophisticated map modal system.
     */
    private void sendStructuredMapDataToClient(ServerPlayer player, TownInterfaceEntity townEntity, 
                                             Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> mapData, 
                                             int zoomLevel) {
        try {
            // For now, we'll convert the structured data to JSON format for compatibility
            // In the future, we could extend the packet system to send structured data directly
            String jsonMapData = convertStructuredDataToJson(mapData, zoomLevel);
            
            TownMapDataResponsePacket responsePacket = new TownMapDataResponsePacket(
                townEntity.getBlockPos().getX(),
                townEntity.getBlockPos().getY(), 
                townEntity.getBlockPos().getZ(),
                jsonMapData,
                zoomLevel
            );
            
            ModMessages.sendToPlayer(responsePacket, player);
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Sent structured map data for {} towns to client", mapData.size());
            
        } catch (Exception e) {
            LOGGER.error("Failed to send structured map data to client: {}", e.getMessage());
        }
    }
    
    /**
     * Convert structured map data to JSON format for packet transmission.
     */
    private String convertStructuredDataToJson(Map<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> mapData, int zoomLevel) {
        StringBuilder json = new StringBuilder();
        json.append("{\"towns\":{");
        
        boolean first = true;
        for (Map.Entry<java.util.UUID, TownMapDataResponsePacket.TownMapInfo> entry : mapData.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            TownMapDataResponsePacket.TownMapInfo town = entry.getValue();
            json.append("\"").append(entry.getKey().toString()).append("\":{");
            json.append("\"id\":\"").append(town.townId.toString()).append("\",");
            json.append("\"name\":\"").append(escapeJson(town.name)).append("\",");
            json.append("\"x\":").append(town.x).append(",");
            json.append("\"y\":").append(town.y).append(",");
            json.append("\"z\":").append(town.z).append(",");
            json.append("\"population\":").append(town.population).append(",");
            json.append("\"visitCount\":").append(town.visitCount).append(",");
            json.append("\"lastVisited\":").append(town.lastVisited).append(",");
            json.append("\"isCurrentTown\":").append(town.isCurrentTown);
            json.append("}");
        }
        
        json.append("},\"zoomLevel\":").append(zoomLevel);
        json.append(",\"generatedAt\":").append(System.currentTimeMillis());
        json.append("}");
        
        return json.toString();
    }
    
    /**
     * Calculate map search radius based on zoom level.
     */
    private int calculateMapSearchRadius(int zoomLevel) {
        // Larger radius for lower zoom levels to show more towns
        int baseRadius = 1000; // 1km base radius
        return baseRadius * Math.max(1, 5 - zoomLevel); // Increase radius for lower zoom
    }
    
    /**
     * Helper methods for accessing town data.
     */
    private Object getTownManager(ServerLevel level) {
        try {
            // The town manager service handles level access internally
            // Return the level so we can pass it to service methods
            return level;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town manager for level: {}", e.getMessage());
            return null;
        }
    }
    
    private List<Object> findTownsInRadius(Object townManager, BlockPos center, int radius) {
        List<Object> nearbyTowns = new ArrayList<>();
        
        try {
            if (townManager == null) {
                return nearbyTowns;
            }
            
            // townManager is actually the ServerLevel
            ServerLevel level = (ServerLevel) townManager;
            
            // Get all towns from the unified town manager
            com.quackers29.businesscraft.town.TownManager directTownManager = com.quackers29.businesscraft.town.TownManager.get(level);
            Collection<com.quackers29.businesscraft.town.Town> allTowns = directTownManager.getAllTowns();
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "findTownsInRadius: Retrieved {} total towns from TownManager", allTowns.size());
            
            for (com.quackers29.businesscraft.town.Town townObj : allTowns) {
                try {
                    com.quackers29.businesscraft.town.Town townData = townObj;
                    BlockPos townPos = getTownPosition(townData);
                    
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Processing town '{}' at position {}", townData.getTownName(), townPos);
                    
                    if (townPos != null) {
                        // Calculate distance
                        double distance = Math.sqrt(
                            Math.pow(townPos.getX() - center.getX(), 2) + 
                            Math.pow(townPos.getZ() - center.getZ(), 2)
                        );
                        
                        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town '{}' distance: {} (radius: {})", townData.getTownName(), distance, radius);
                        
                        // Include towns within radius
                        if (distance <= radius) {
                            nearbyTowns.add(townObj);
                            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town '{}' INCLUDED in radius", townData.getTownName());
                        } else {
                            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town '{}' EXCLUDED from radius", townData.getTownName());
                        }
                    } else {
                        LOGGER.warn("Town '{}' has null position", townData.getTownName());
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to process town for radius check: {}", e.getMessage());
                }
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found {} towns within radius {} of position {}", nearbyTowns.size(), radius, center);
            
        } catch (Exception e) {
            LOGGER.error("Failed to find towns in radius: {}", e.getMessage());
        }
        
        return nearbyTowns;
    }
    
    private BlockPos getTownPosition(ITownDataProvider townData) {
        try {
            // Get the town's position from the town data directly
            // The Town class should have position data
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                // Get position from the town - this may need to be implemented in Town class
                return new BlockPos(town.getX(), town.getY(), town.getZ());
            }
            return null;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town position: {}", e.getMessage());
            return null;
        }
    }
    
    private int getTownPopulation(ITownDataProvider townData) {
        try {
            // Get town population from town data
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                return town.getPopulation();
            }
            return 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town population: {}", e.getMessage());
            return 0;
        }
    }
    
    private int getTownVisitCount(ITownDataProvider townData) {
        try {
            // Get visit count from town data
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                return town.getVisitHistory().size(); // Assuming visit history is available
            }
            return 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town visit count: {}", e.getMessage());
            return 0;
        }
    }
    
    private long getTownLastVisited(ITownDataProvider townData) {
        try {
            // Get last visited timestamp from town data  
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                com.quackers29.businesscraft.town.Town town = (com.quackers29.businesscraft.town.Town) townData;
                // Get the most recent visit from visit history
                List<ITownDataProvider.VisitHistoryRecord> history = town.getVisitHistory();
                if (!history.isEmpty()) {
                    // Get the last visit record
                    ITownDataProvider.VisitHistoryRecord lastVisit = history.get(history.size() - 1);
                    return lastVisit.getTimestamp();
                }
                return 0; // No visits yet
            }
            return 0;
        } catch (Exception e) {
            LOGGER.warn("Failed to get town last visited: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * Simple data handle for tracking custom data attachments.
     */
    private static class DataHandle {
        private final BlockEntity blockEntity;
        private final String key;
        
        public DataHandle(BlockEntity blockEntity, String key) {
            this.blockEntity = blockEntity;
            this.key = key;
        }
        
        public BlockEntity getBlockEntity() {
            return blockEntity;
        }
        
        public String getKey() {
            return key;
        }
    }
    
    public boolean processPlatformDataRequest(Object player, int x, int y, int z, 
                                            boolean includePlatformConnections, 
                                            boolean includeDestinationTowns, 
                                            int maxRadius) {
        // Delegate to the overloaded method with null targetTownId for coordinate-based lookup
        return processPlatformDataRequest(player, x, y, z, includePlatformConnections, 
                                        includeDestinationTowns, maxRadius, null);
    }
    
    /**
     * Process platform data request with target town ID (UUID-based lookup).
     * This method handles UUID-based town lookups and uses the actual town coordinates.
     */
    public boolean processPlatformDataRequest(Object player, int x, int y, int z, 
                                            boolean includePlatformConnections, 
                                            boolean includeDestinationTowns, 
                                            int maxRadius, String targetTownId) {
        try {
            ServerPlayer serverPlayer = (ServerPlayer) player;
            ServerLevel level = serverPlayer.serverLevel();
            
            UUID townId;
            int actualX = x, actualY = y, actualZ = z;
            
            // Handle UUID-based lookup if targetTownId is provided
            if (targetTownId != null && !targetTownId.isEmpty()) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Processing UUID-based platform data request for town ID: {}", targetTownId);
                
                // Convert string to UUID
                try {
                    townId = UUID.fromString(targetTownId);
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Invalid town ID format: {}", targetTownId);
                    return false;
                }
                
                // Get town by UUID using existing method
                Object town = getTownById(player, targetTownId);
                if (town == null) {
                    LOGGER.warn("Could not find town with ID {} for platform data request", targetTownId);
                    return false;
                }
                
                // Get actual town coordinates
                int[] townPosition = getTownPosition(town);
                if (townPosition != null) {
                    actualX = townPosition[0];
                    actualY = townPosition[1];
                    actualZ = townPosition[2];
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found town {} at actual position ({}, {}, {})", targetTownId, actualX, actualY, actualZ);
                } else {
                    LOGGER.warn("Could not get position for town {}, using default coordinates", targetTownId);
                }
                
            } else {
                // Coordinate-based lookup (original behavior)
                townId = findTownIdByPosition(x, y, z);
                if (townId == null) {
                    LOGGER.warn("Could not find town at position ({}, {}, {}) for platform data request", x, y, z);
                    return false;
                }
            }
            
            // Create structured response packet with actual coordinates
            TownPlatformDataResponsePacket response = 
                new TownPlatformDataResponsePacket(actualX, actualY, actualZ, townId, maxRadius);
            
            // Add platform data if requested
            if (includePlatformConnections) {
                generateStructuredPlatformData(level, townId, response);
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Generated structured platform data for town {}", townId);
            }
            
            // Add town info if requested
            if (includeDestinationTowns) {
                generateStructuredTownInfo(level, townId, response);
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Generated structured town info for town {}", townId);
            }
            
            // Send packet using platform services
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "SERVER PLATFORM DATA SEND: Sending TownPlatformDataResponsePacket to client - townId: {}, platforms: {}, townInfo: {}", 
                       response.getTownId(), response.getPlatforms().size(), response.getTownInfo() != null ? response.getTownInfo().name : "null");
            
            try {
                PlatformServices.getNetworkHelper().sendToClient(response, serverPlayer);
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "SERVER PLATFORM DATA SEND: Successfully sent TownPlatformDataResponsePacket to client");
            } catch (Exception e) {
                LOGGER.error("SERVER PLATFORM DATA SEND: Failed to send TownPlatformDataResponsePacket to client: {}", e.getMessage());
                e.printStackTrace();
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to process platform data request (UUID-based) for town {}: {}", targetTownId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Process platform data request using UUID-based town lookup (simplified interface).
     * This is a convenience method that calls the main processPlatformDataRequest with default coordinates.
     */
    public boolean processPlatformDataRequestByTownId(Object player, String targetTownId,
                                                    boolean includePlatformConnections, 
                                                    boolean includeDestinationTowns, 
                                                    int maxRadius) {
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Processing UUID-based platform data request for town {}", targetTownId);
        // Call the main method with dummy coordinates since we're using UUID lookup
        return processPlatformDataRequest(player, 0, 64, 0, 
                                        includePlatformConnections, includeDestinationTowns, 
                                        maxRadius, targetTownId);
    }
    
    /**
     * Generate destination town data as JSON string for sophisticated map display.
     * This method finds all towns within the specified radius and formats them for the client cache.
     */
    private String generateDestinationTownData(ServerLevel level, BlockPos centerPos, int maxRadius) {
        try {
            // Use unified TownManager directly
            com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager.get(level);
            
            // Get all towns from TownManager
            Collection<com.quackers29.businesscraft.town.Town> allTowns = townManager.getAllTowns();
            
            if (allTowns == null || allTowns.isEmpty()) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "getAllTowns returned empty data");
                return "{}";
            }
            
            // Build JSON manually for simplicity (could use Gson if available)
            StringBuilder jsonBuilder = new StringBuilder();
            jsonBuilder.append("{");
            boolean first = true;
            
            for (com.quackers29.businesscraft.town.Town town : allTowns) {
                try {
                    // Town object is already the correct type
                    
                    com.quackers29.businesscraft.api.ITownDataProvider townData = 
                        (com.quackers29.businesscraft.api.ITownDataProvider) town;
                    
                    // Get town position through ITownDataProvider interface
                    com.quackers29.businesscraft.api.ITownDataProvider.Position townPos = townData.getPosition();
                    if (townPos == null) {
                        LOGGER.warn("Town position is null for town: {}", townData.getTownName());
                        continue;
                    }
                    
                    // Convert to BlockPos for distance calculation
                    BlockPos pos = new BlockPos(townPos.getX(), townPos.getY(), townPos.getZ());
                    
                    // Check distance
                    double distance = Math.sqrt(centerPos.distSqr(pos));
                    if (distance > maxRadius) {
                        continue;
                    }
                    
                    String townName = townData.getTownName();
                    java.util.UUID townId = townData.getTownId();
                    
                    if (townName == null || townId == null) {
                        LOGGER.warn("Town data missing: name={}, id={}", townName, townId);
                        continue;
                    }
                    
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Successfully processed town '{}' at ({}, {}, {}) - distance: {}", 
                        townName, pos.getX(), pos.getY(), pos.getZ(), (int)distance);
                    
                    if (!first) {
                        jsonBuilder.append(",");
                    }
                    first = false;
                    
                    // Add town data as JSON
                    jsonBuilder.append("\"").append(townId.toString()).append("\":{");
                    jsonBuilder.append("\"id\":\"").append(townId.toString()).append("\",");
                    jsonBuilder.append("\"name\":\"").append(escapeJson(townName)).append("\",");
                    jsonBuilder.append("\"x\":").append(pos.getX()).append(",");
                    jsonBuilder.append("\"y\":").append(pos.getY()).append(",");
                    jsonBuilder.append("\"z\":").append(pos.getZ()).append(",");
                    jsonBuilder.append("\"distance\":").append((int)distance);
                    jsonBuilder.append("}");
                    
                } catch (Exception e) {
                    LOGGER.warn("Failed to process town data for town: {}", 
                        town instanceof ITownDataProvider ? ((ITownDataProvider) town).getTownName() : "unknown", e);
                }
            }
            
            jsonBuilder.append("}");
            String result = jsonBuilder.toString();
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Generated destination data with {} towns within {}m radius", 
                        allTowns.size(), maxRadius);
            return result;
            
        } catch (Exception e) {
            LOGGER.warn("Failed to generate destination town data: {}", e.getMessage());
            return "{}";
        }
    }
    
    /**
     * Simple JSON string escaping for town names.
     */
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\"", "\\\"").replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }
    
    public boolean updateTownPlatformUI(Object player, int x, int y, int z, String platformData, String destinationData) {
        try {
            // Find the currently open map modal and update it with platform data
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) {
                com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
                    (com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) minecraft.screen;
                
                // Update the modal with platform and destination data
                // Parse the JSON data and update the sophisticated map modal
                try {
                    // Find the town ID that corresponds to this position
                    UUID townIdAtPosition = findTownIdByPosition(x, y, z);
                    if (townIdAtPosition != null) {
                        // Parse platform data into structured format
                        Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms = parsePlatformData(platformData);
                        
                        // Parse town info from destination data  
                        TownPlatformDataResponsePacket.TownInfo townInfo = parseTownInfo(destinationData, townIdAtPosition);
                        
                        // Update the modal with the parsed data
                        mapModal.refreshPlatformData(townIdAtPosition, platforms);
                        if (townInfo != null) {
                            mapModal.refreshTownData(townIdAtPosition, townInfo);
                        }
                        
                        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Updated town platform UI with {} platforms for town {} at ({}, {}, {})", 
                            platforms.size(), townIdAtPosition, x, y, z);
                        return true;
                    } else {
                        LOGGER.warn("Could not find town ID for position ({}, {}, {})", x, y, z);
                        return false;
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to parse platform data: {}", e.getMessage());
                    return false;
                }
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No town map modal open to update with platform data");
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to update town platform UI at ({}, {}, {}): {}", x, y, z, e.getMessage());
            return false;
        }
    }

    /**
     * Find town ID by position coordinates using client-side town map cache.
     */
    private UUID findTownIdByPosition(int x, int y, int z) {
        try {
            // Use client-side cache to find town at this position
            com.quackers29.businesscraft.client.cache.ClientTownMapCache cache = 
                com.quackers29.businesscraft.client.cache.ClientTownMapCache.getInstance();
            
            // Get all cached towns and find one at this position
            Map<UUID, com.quackers29.businesscraft.client.cache.ClientTownMapCache.CachedTownData> cachedTowns = 
                cache.getAllTowns();
            
            for (Map.Entry<UUID, com.quackers29.businesscraft.client.cache.ClientTownMapCache.CachedTownData> entry : cachedTowns.entrySet()) {
                com.quackers29.businesscraft.client.cache.ClientTownMapCache.CachedTownData town = entry.getValue();
                if (town.getX() == x && town.getY() == y && town.getZ() == z) {
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found town {} at position ({}, {}, {})", town.getName(), x, y, z);
                    return entry.getKey();
                }
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No cached town found at position ({}, {}, {})", x, y, z);
        } catch (Exception e) {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Failed to find town ID by position ({}, {}, {}): {}", x, y, z, e.getMessage());
        }
        return null;
    }

    /**
     * Parse platform data JSON into structured PlatformInfo objects.
     */
    private Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> parsePlatformData(String platformData) {
        Map<UUID, TownPlatformDataResponsePacket.PlatformInfo> platforms = new HashMap<>();
        
        if (platformData == null || platformData.trim().equals("{}") || platformData.trim().isEmpty()) {
            return platforms;
        }
        
        // For now, return empty map as the sophisticated map expects structured data
        // The current implementation uses JSON strings, but the sophisticated map 
        // expects PlatformInfo objects with specific fields
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Platform data parsing not yet implemented for JSON: {}", platformData);
        
        return platforms;
    }

    /**
     * Parse town info from destination data JSON.
     */
    private TownPlatformDataResponsePacket.TownInfo parseTownInfo(String destinationData, UUID townId) {
        if (destinationData == null || destinationData.trim().equals("{}") || destinationData.trim().isEmpty()) {
            return null;
        }
        
        // For now, return null as the sophisticated map expects structured data
        // The current implementation uses JSON strings, but the sophisticated map
        // expects TownInfo objects with specific fields  
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town info parsing not yet implemented for JSON: {}", destinationData);
        
        return null;
    }
    
    /**
     * Generate structured platform data for the response packet.
     */
    private void generateStructuredPlatformData(net.minecraft.server.level.ServerLevel level, UUID townId, TownPlatformDataResponsePacket response) {
        try {
            // Use unified TownManager directly
            com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager.get(level);
            
            // Get the town object
            com.quackers29.businesscraft.town.Town townData = townManager.getTown(townId);
            if (townData == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town {} not found for platform data generation", townId);
                return;
            }
            
            // Get the town's position to find the TownInterfaceEntity
            ITownDataProvider.Position townPos = townData.getPosition();
            if (townPos == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town {} has no position data", townId);
                return;
            }
            
            // Find the TownInterfaceEntity at the town's position
            BlockPos townBlockPos = new BlockPos(townPos.getX(), townPos.getY(), townPos.getZ());
            net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(townBlockPos);
            
            if (!(blockEntity instanceof com.quackers29.businesscraft.block.entity.TownInterfaceEntity)) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No TownInterfaceEntity found at town {} position {}", townId, townBlockPos);
                return;
            }
            
            com.quackers29.businesscraft.block.entity.TownInterfaceEntity townInterface = 
                (com.quackers29.businesscraft.block.entity.TownInterfaceEntity) blockEntity;
            
            // Get the platforms from the TownInterfaceEntity
            List<Object> platformsObj = townInterface.getPlatforms();
            List<com.quackers29.businesscraft.platform.Platform> platforms = new ArrayList<>();
            for (Object obj : platformsObj) {
                if (obj instanceof com.quackers29.businesscraft.platform.Platform platform) {
                    platforms.add(platform);
                }
            }
            
            if (platforms == null || platforms.isEmpty()) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town {} has no platforms created yet", townId);
                return;
            }
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found {} platforms for town {}", platforms.size(), townId);
            
            // Convert platforms to structured data
            for (com.quackers29.businesscraft.platform.Platform platform : platforms) {
                try {
                    UUID platformId = platform.getId();
                    String platformName = platform.getName() != null ? platform.getName() : "Platform " + platformId.toString().substring(0, 8);
                    boolean enabled = platform.isEnabled();
                    
                    // Get platform path coordinates
                    BlockPos startPos = platform.getStartPos();
                    BlockPos endPos = platform.getEndPos();
                    
                    if (startPos == null || endPos == null) {
                        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Platform {} has no path set (startPos={}, endPos={})", platformId, startPos, endPos);
                        continue;
                    }
                    
                    // Convert BlockPos to int arrays for Enhanced MultiLoader compatibility
                    int[] startCoords = new int[]{startPos.getX(), startPos.getY(), startPos.getZ()};
                    int[] endCoords = new int[]{endPos.getX(), endPos.getY(), endPos.getZ()};
                    
                    // Get enabled destinations (platform system should track these)
                    Set<UUID> enabledDestinations = new HashSet<>(); // TODO: Get actual destinations from platform
                    
                    // Add platform to response packet
                    response.addPlatform(platformId, platformName, enabled, startCoords, endCoords, enabledDestinations);
                    
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Added platform {} '{}' with path from {} to {}", 
                               platformId, platformName, startPos, endPos);
                    
                } catch (Exception e) {
                    LOGGER.error("Failed to process platform {}: {}", platform.getId(), e.getMessage());
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate structured platform data for town {}: {}", townId, e.getMessage());
        }
    }
    
    /**
     * Generate structured town info for the response packet.
     */
    private void generateStructuredTownInfo(net.minecraft.server.level.ServerLevel level, UUID townId, TownPlatformDataResponsePacket response) {
        try {
            // Use unified TownManager directly
            com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager.get(level);
            
            // Get the town object
            com.quackers29.businesscraft.town.Town townData = townManager.getTown(townId);
            if (townData == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Town {} not found for town info generation", townId);
                return;
            }
            
            // Extract town information
            String townName = townData.getTownName();
            int population = townData.getPopulation();
            int touristCount = townData.getTouristCount();
            
            // Get actual boundary radius from town (matches main branch behavior)
            int boundaryRadius = 10; // Default fallback
            if (townData instanceof com.quackers29.businesscraft.town.Town) {
                boundaryRadius = ((com.quackers29.businesscraft.town.Town) townData).getBoundaryRadius();
            }
            
            // Get town center coordinates
            BlockPos townPos = getTownPosition(townData);
            if (townPos != null) {
                int[] townPosition = new int[]{townPos.getX(), townPos.getY(), townPos.getZ()};
                // Set town info with actual center coordinates
                response.setTownInfo(townName, population, touristCount, boundaryRadius, 
                                   townPosition[0], townPosition[1], townPosition[2]);
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "TOWNINFO COORD DEBUG: Generated town info for {} at center ({},{},{}): population={}, tourists={}, boundary={} (calculated from town)", 
                           townName, townPosition[0], townPosition[1], townPosition[2], population, touristCount, boundaryRadius);
            } else {
                // Fallback to original method without coordinates (will use defaults)
                response.setTownInfo(townName, population, touristCount, boundaryRadius);
                LOGGER.warn("Could not get town center for {}, using default coordinates", townName);
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to generate structured town info for town {}: {}", townId, e.getMessage());
        }
    }
    
    /**
     * Update client-side town platform UI with structured data packet.
     * This method handles sophisticated map modal updates with structured PlatformInfo data.
     */
    public boolean updateTownPlatformUIStructured(Object player, int x, int y, int z, Object packet) {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: updateTownPlatformUIStructured called at ({}, {}, {})", x, y, z);
            
            // Cast to the structured packet type
            if (!(packet instanceof com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket)) {
                LOGGER.warn("FORGE BLOCK ENTITY HELPER: Invalid packet type for structured platform UI update: {}", packet.getClass());
                return false;
            }
            
            com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket structuredPacket = 
                (com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket) packet;
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: Structured packet received - townId: {}, platforms: {}", 
                       structuredPacket.getTownId(), structuredPacket.getPlatforms().size());
            
            // Get town map modal from current screen
            Minecraft mc = Minecraft.getInstance();
            Screen currentScreen = mc.screen;
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: Current screen type: {}", 
                       currentScreen != null ? currentScreen.getClass().getSimpleName() : "null");
            
            if (currentScreen instanceof com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: TownMapModal detected! Updating with platform data...");
                
                com.quackers29.businesscraft.ui.modal.specialized.TownMapModal mapModal = 
                    (com.quackers29.businesscraft.ui.modal.specialized.TownMapModal) currentScreen;
                
                // Update the modal with structured data directly
                UUID townId = structuredPacket.getTownId();
                Map<UUID, com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket.PlatformInfo> platforms = structuredPacket.getPlatforms();
                com.quackers29.businesscraft.network.packets.ui.TownPlatformDataResponsePacket.TownInfo townInfo = structuredPacket.getTownInfo();
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: Calling mapModal.refreshPlatformData with {} platforms for town {}", 
                           platforms.size(), townId);
                
                // The PlatformInfo class now has all the fields that TownMapModal expects
                // Pass the structured data directly to the sophisticated map
                mapModal.refreshPlatformData(townId, platforms);
                if (townInfo != null) {
                    DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: Calling mapModal.refreshTownData with townInfo: {}", townInfo.name);
                    mapModal.refreshTownData(townId, townInfo);
                }
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: Successfully updated sophisticated map with structured data: {} platforms for town {} at ({}, {}, {})", 
                    platforms.size(), townId, x, y, z);
                return true;
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "FORGE BLOCK ENTITY HELPER: Town map modal not currently open, structured update skipped. Current screen: {}", 
                           currentScreen != null ? currentScreen.getClass().getSimpleName() : "null");
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("FORGE BLOCK ENTITY HELPER: Failed to update platform UI with structured data at ({}, {}, {}): {}", 
                x, y, z, e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    
    /**
     * Open destinations UI on client side with provided town data.
     * Unified architecture approach - direct UI opening like main branch.
     */
    // Implementation of BlockEntityHelper.openDestinationsUI
    public void openDestinationsUI(int x, int y, int z, String platformId, String platformName, 
                           java.util.Map<java.util.UUID, String> townNames,
                           java.util.Map<java.util.UUID, Boolean> enabledState,
                           java.util.Map<java.util.UUID, Integer> townDistances,
                           java.util.Map<java.util.UUID, String> townDirections) {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Opening destinations UI for platform {} at [{}, {}, {}]", platformId, x, y, z);
            
            // Open the destinations UI directly on client side - unified architecture approach
            BlockPos blockPos = new BlockPos(x, y, z);
            java.util.UUID platformUUID = java.util.UUID.fromString(platformId);
            
            com.quackers29.businesscraft.ui.screens.platform.DestinationsScreenV2.open(
                blockPos, platformUUID, platformName, townNames, enabledState, townDistances, townDirections);
            
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Successfully opened destinations UI for platform {}", platformId);
            
        } catch (Exception e) {
            LOGGER.error("Failed to open destinations UI: {}", e.getMessage(), e);
        }
    }
    
    public boolean updateCommunalStorageUI(Object player, int x, int y, int z, java.util.Map<Integer, Object> storageItems) {
        // TODO: Implement communal storage UI updates for client-side
        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "updateCommunalStorageUI called - not yet implemented");
        return true; // Return true for now to not break the flow
    }
    
    public boolean updateBufferStorageUI(Object player, int x, int y, int z, java.util.Map<Integer, Object> bufferSlots) {
        try {
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "updateBufferStorageUI called with {} buffer slots", bufferSlots.size());
            
            // Get the current client screen
            Object currentScreen = PlatformServices.getPlatformHelper().getCurrentScreen();
            if (currentScreen == null) {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "No current screen to update with buffer storage data");
                return false;
            }
            
            String simpleClassName = currentScreen.getClass().getSimpleName();
            DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Current screen class: {}", simpleClassName);
            
            if ("PaymentBoardScreen".equals(simpleClassName)) {
                // Convert Map<Integer, Object> to SlotBasedStorage for slot-based update
                com.quackers29.businesscraft.town.data.SlotBasedStorage clientSlotStorage = 
                    new com.quackers29.businesscraft.town.data.SlotBasedStorage(18); // 18 slots for buffer
                
                for (java.util.Map.Entry<Integer, Object> entry : bufferSlots.entrySet()) {
                    int slotIndex = entry.getKey();
                    if (entry.getValue() instanceof net.minecraft.world.item.ItemStack stack && !stack.isEmpty()) {
                        clientSlotStorage.setSlot(slotIndex, stack);
                        DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Setting slot {}: {} x{}", slotIndex, stack.getItem(), stack.getCount());
                    }
                }
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Created SlotBasedStorage with {} slots", bufferSlots.size());
                
                // Use reflection to call updateBufferStorageSlots method (the new slot-based method)
                java.lang.reflect.Method updateMethod = currentScreen.getClass()
                    .getMethod("updateBufferStorageSlots", com.quackers29.businesscraft.town.data.SlotBasedStorage.class);
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Found updateBufferStorageSlots method: {}", updateMethod);
                
                updateMethod.invoke(currentScreen, clientSlotStorage);
                
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Successfully invoked PaymentBoardScreen.updateBufferStorageSlots() with {} slots", bufferSlots.size());
                return true;
            } else {
                DebugConfig.debug(LOGGER, DebugConfig.PLATFORM_SYSTEM, "Screen is not PaymentBoardScreen: {}", simpleClassName);
                return false;
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to update buffer storage UI: {}", e.getMessage(), e);
            return false;
        }
    }
    
    public boolean registerPlayerExitUI(Object blockEntity, Object player) {
        try {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                LOGGER.warn("registerPlayerExitUI called with non-ServerPlayer: {}", player.getClass());
                return false;
            }
            
            // Get the town interface entity
            if (!(blockEntity instanceof TownInterfaceEntity townEntity)) {
                LOGGER.warn("registerPlayerExitUI called with non-TownInterfaceEntity: {}", blockEntity.getClass());
                return false;
            }
            
            // Register player UI exit for visualization (same as main branch)
            townEntity.registerPlayerExitUI(serverPlayer.getUUID());
            
            // Send platform visualization packet to trigger client-side visualization
            UUID playerId = serverPlayer.getUUID();
            BlockPos pos = townEntity.getBlockPos();
            Level level = townEntity.getLevel();
            
            if (level != null) {
                // Create and send platform visualization packet through platform abstraction
                PlatformServices.getNetworkHelper().sendToClient(
                    new com.quackers29.businesscraft.network.packets.ui.PlatformVisualizationPacket(pos.getX(), pos.getY(), pos.getZ()),
                    serverPlayer
                );
                
                DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                    "Player {} exited UI, registered for visualization at {}", playerId, pos);
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.error("Failed to register player UI exit: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public void handleVisitorHistoryRequest(Object player, java.util.UUID townId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            LOGGER.warn("FORGE BLOCK ENTITY HELPER: Player is not a ServerPlayer: {}", 
                player != null ? player.getClass().getSimpleName() : "null");
            return;
        }
        
        ServerLevel level = serverPlayer.serverLevel();
        
        try {
            com.quackers29.businesscraft.town.TownManager townManager = com.quackers29.businesscraft.town.TownManager.get(level);
            com.quackers29.businesscraft.town.Town requestedTown = townManager.getTown(townId);
            
            // Debug: Log all available towns
            Collection<com.quackers29.businesscraft.town.Town> allTowns = townManager.getAllTowns();
            LOGGER.info("VISITOR HISTORY DEBUG: TownManager has {} towns loaded:", allTowns.size());
            for (com.quackers29.businesscraft.town.Town town : allTowns) {
                LOGGER.info("  - {} ({})", town.getName(), town.getId());
            }
            
            if (requestedTown == null) {
                LOGGER.warn("VISITOR HISTORY REQUEST: Town with UUID {} not found", townId);
                return;
            }
            
            // Get raw visitor history data directly from town
            List<com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord> rawHistory = requestedTown.getVisitHistory();
            
            LOGGER.info("VISITOR HISTORY DEBUG: Town {} has {} history records:", requestedTown.getName(), rawHistory.size());
            for (com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord record : rawHistory) {
                LOGGER.info("  - {} tourists from {} at {}", record.getCount(), record.getOriginTownId(), record.getTimestamp());
            }
            
            // Resolve town names fresh from server
            List<com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket.VisitorEntry> resolvedEntries = new ArrayList<>();
            
            for (com.quackers29.businesscraft.api.ITownDataProvider.VisitHistoryRecord record : rawHistory) {
                String resolvedName = "Unknown";
                if (record.getOriginTownId() != null) {
                    com.quackers29.businesscraft.town.Town originTown = townManager.getTown(record.getOriginTownId());
                    if (originTown != null) {
                        resolvedName = originTown.getName();
                        DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS,
                            "Resolved origin town {} -> '{}'", record.getOriginTownId(), resolvedName);
                    } else {
                        LOGGER.warn("Origin town {} not found for visitor history", record.getOriginTownId());
                        resolvedName = "Town-" + record.getOriginTownId().toString().substring(0, 8);
                    }
                }
                
                resolvedEntries.add(new com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket.VisitorEntry(
                    record.getTimestamp(),
                    record.getOriginTownId(),
                    record.getCount(),
                    record.getOriginPos().getX(),
                    record.getOriginPos().getY(), 
                    record.getOriginPos().getZ(),
                    resolvedName
                ));
            }
            
            // Send response packet with resolved names
            com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket responsePacket = 
                new com.quackers29.businesscraft.network.packets.ui.VisitorHistoryResponsePacket(resolvedEntries);
            
            PlatformServices.getNetworkHelper().sendVisitorHistoryResponsePacket(serverPlayer, responsePacket);
            
            DebugConfig.debug(LOGGER, DebugConfig.NETWORK_PACKETS, 
                "Processed visitor history request for town {} ({}): {} entries", 
                requestedTown.getName(), townId, resolvedEntries.size());
                
        } catch (Exception e) {
            LOGGER.error("Error handling visitor history request for town {}: {}", townId, e.getMessage(), e);
        }
    }
}