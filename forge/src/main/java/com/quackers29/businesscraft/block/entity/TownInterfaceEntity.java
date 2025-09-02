package com.quackers29.businesscraft.block.entity;

import com.quackers29.businesscraft.init.ModBlockEntities;
import com.quackers29.businesscraft.town.TownInterfaceData;
import com.quackers29.businesscraft.town.data.PlatformManager;
import com.quackers29.businesscraft.town.data.TouristSpawningHelper;
import com.quackers29.businesscraft.town.data.VisitorProcessingHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * FORGE MODULE: Forge-specific TownInterfaceEntity implementation
 */
public class TownInterfaceEntity extends com.quackers29.businesscraft.block.entity.TownInterfaceEntityCommon {

    // Platform-specific fields that need to be initialized
    private PlatformManager platformManager;
    private Object visitBuffer;
    private TouristSpawningHelper touristSpawningHelper;
    private VisitorProcessingHelper visitorProcessingHelper;

    public TownInterfaceEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWN_INTERFACE_ENTITY.get(), pos, state);
        // Initialize platform-specific components
        initializePlatformComponents();
    }

    private void initializePlatformComponents() {
        // Initialize platform manager and other components
        // This should be done based on the original Forge implementation
        this.platformManager = new PlatformManager();
        this.visitBuffer = new com.quackers29.businesscraft.town.data.VisitBuffer();
        this.touristSpawningHelper = new TouristSpawningHelper();
        this.visitorProcessingHelper = new VisitorProcessingHelper();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return getItemHandlerCapability().cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public int getPlatformCount() {
        var platformManager = getPlatformManager();
        if (platformManager != null) {
            return platformManager.getEnabledPlatforms().size();
        }
        return 0; // Default fallback
    }

    // Platform-specific implementations of abstract methods
    @Override
    public java.util.List<Object> getPlatforms() {
        // Return platforms from the common list, platform manager will be handled by subclasses
        return super.getPlatforms();
    }

    @Override
    public Object getPlatform(UUID platformId) {
        // Default implementation - can be overridden by subclasses with specific platform manager logic
        return super.getPlatform(platformId);
    }

    @Override
    public boolean canAddMorePlatforms() {
        return super.canAddMorePlatforms();
    }

    @Override
    public Object addPlatform() {
        return super.addPlatform();
    }

    @Override
    public boolean removePlatform(UUID platformId) {
        return super.removePlatform(platformId);
    }

    @Override
    public void setPlatformCreationMode(boolean mode, UUID platformId) {
        super.setPlatformCreationMode(mode, platformId);
    }

    @Override
    public boolean isInPlatformCreationMode() {
        return super.isInPlatformCreationMode();
    }

    @Override
    public void setPlatformPathStart(UUID platformId, BlockPos pos) {
        super.setPlatformPathStart(platformId, pos);
    }

    @Override
    public void setPlatformPathEnd(UUID platformId, BlockPos pos) {
        super.setPlatformPathEnd(platformId, pos);
    }

    @Override
    public PlatformManager getPlatformManager() {
        // Return the platform manager instance - should be initialized by platform-specific setup
        return platformManager;
    }

    @Override
    public Object getVisitBuffer() {
        // Return visit buffer - platform-specific implementation
        return visitBuffer;
    }

    @Override
    public TouristSpawningHelper getTouristSpawningHelper() {
        // Return tourist spawning helper - should be initialized by platform-specific setup
        return touristSpawningHelper;
    }

    @Override
    public VisitorProcessingHelper getVisitorProcessingHelper() {
        // Return visitor processing helper - should be initialized by platform-specific setup
        return visitorProcessingHelper;
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        invalidateItemHandlerCapability();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return super.getUpdateTag();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }

    // Additional platform-specific method implementations
    @Override
    public Object getTownInterfaceData() {
        // Create and return a TownInterfaceData instance for UI operations
        TownInterfaceData data = new TownInterfaceData(
            this.worldPosition.getX(),
            this.worldPosition.getY(),
            this.worldPosition.getZ()
        );

        // Copy relevant data from the entity to the TownInterfaceData
        if (this.townId != null) {
            data.setTownId(this.townId);
        }
        if (this.name != null) {
            data.setTownName(this.name);
        }
        data.setTouristSpawningEnabled(this.touristSpawningEnabled);
        data.setSearchRadius(this.searchRadius);

        // Copy platforms if available
        var platformManager = getPlatformManager();
        if (platformManager != null) {
            com.quackers29.businesscraft.town.data.PlatformManager pm =
                (com.quackers29.businesscraft.town.data.PlatformManager) platformManager;
            // Convert platform manager platforms to TownInterfaceData platforms
            // Note: getPlatforms requires a boolean parameter (isClientSide)
            // For now, we'll skip platform copying until proper mapping is implemented
        }

        return data;
    }

    @Override
    public Object getBufferHandler() {
        // Get the buffer handler - platform-specific implementation
        // This could return a Forge-specific buffer handler implementation
        return super.getBufferHandler();
    }

    @Override
    public void onTownBufferChanged() {
        super.onTownBufferChanged();
        // Additional Forge-specific buffer change handling can be added here
    }

    @Override
    public Object getTownDataProvider() {
        return this;
    }

    @Override
    public void registerPlayerExitUI(UUID playerId) {
        // Register player exit from UI - Forge-specific implementation
        // This could track player interactions or update UI state
    }

    @Override
    public Object createPaymentBoardMenuProvider() {
        // Create payment board menu provider - Forge-specific implementation
        // This would typically return a MenuProvider for payment interactions
        return null; // Placeholder - needs implementation based on specific requirements
    }

    @Override
    public String getTownNameFromId(UUID townId) {
        if (townId != null && townId.equals(getTownId())) {
            return getName();
        }
        // Try to look up from town manager
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            var townManager = com.quackers29.businesscraft.town.TownManager.get(serverLevel);
            var town = townManager.getTown(townId);
            if (town != null) {
                return town.getName();
            }
        }
        return super.getTownNameFromId(townId);
    }

    private LazyOptional<IItemHandler> itemHandlerCapability;

    private LazyOptional<IItemHandler> getItemHandlerCapability() {
        if (itemHandlerCapability == null || !itemHandlerCapability.isPresent()) {
            itemHandlerCapability = LazyOptional.of(() -> new IItemHandler() {
    @Override
                public int getSlots() { return 1; }

    @Override
                public ItemStack getStackInSlot(int slot) {
                    return slot == 0 ? getStoredItem() : ItemStack.EMPTY;
    }

    @Override
                public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                    if (slot == 0 && canAcceptItem(stack)) {
                        return insertItemInternal(stack, simulate);
                    }
                    return stack;
                }
    
    @Override
                public ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot == 0) {
                        return extractItemInternal(amount, simulate);
                    }
                    return ItemStack.EMPTY;
    }
    
    @Override
                public int getSlotLimit(int slot) { return 64; }
    
    @Override
                public boolean isItemValid(int slot, ItemStack stack) {
                    return slot == 0 && canAcceptItem(stack);
                }
            });
        }
        return itemHandlerCapability;
    }

    private void invalidateItemHandlerCapability() {
        if (itemHandlerCapability != null) {
            itemHandlerCapability.invalidate();
            itemHandlerCapability = null;
        }
    }

    private ItemStack getStoredItem() {
        return ItemStack.EMPTY;
    }

    private boolean canAcceptItem(ItemStack stack) {
        return stack.getItem() == net.minecraft.world.item.Items.BREAD;
    }

    private ItemStack insertItemInternal(ItemStack stack, boolean simulate) {
        return stack;
    }

    private ItemStack extractItemInternal(int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }
}