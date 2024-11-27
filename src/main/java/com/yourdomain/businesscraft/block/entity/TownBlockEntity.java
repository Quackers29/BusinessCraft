package com.yourdomain.businesscraft.block.entity;

import com.yourdomain.businesscraft.menu.TownBlockMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class TownBlockEntity extends BlockEntity implements MenuProvider, BlockEntityTicker<TownBlockEntity> {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == Items.BREAD;
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private final ContainerData data = new SimpleContainerData(2) {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> breadCount;
                case 1 -> population;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> breadCount = value;
                case 1 -> population = value;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    };
    private int breadCount = 0;
    private int population = 0;
    private static final String[] TOWN_NAMES = {
            "Springfield", "Rivertown", "Maplewood", "Lakeside", "Greenfield"
    };
    private String townName = "Default Town";
    private String guiTownName = "Default Town";
    private static final Logger LOGGER = LogManager.getLogger();

    public TownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWN_BLOCK_ENTITY.get(), pos, state);
        if ("Default Town".equals(townName)) {
            this.townName = getRandomTownName();
            LOGGER.info("Assigned random town name: {}", this.townName);
        }
        this.guiTownName = this.townName;
        LOGGER.info("TownBlockEntity created with town name: {}", this.townName);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.businesscraft.town_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new TownBlockMenu(id, inventory, this, this.data);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putString("townName", townName);
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("breadCount", breadCount);
        tag.putInt("population", population);
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        String loadedTownName = tag.getString("townName");
        if (loadedTownName != null && !loadedTownName.isEmpty()) {
            this.townName = loadedTownName;
            LOGGER.info("Loaded town name: {}", this.townName);
        } else {
            LOGGER.warn("Loaded town name is null or empty, using default: {}", this.townName);
        }
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        breadCount = tag.getInt("breadCount");
        population = tag.getInt("population");
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownBlockEntity blockEntity) {
        if (!level.isClientSide) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!stack.isEmpty() && stack.getItem() == Items.BREAD) {
                stack.shrink(1); // Consume one bread
                breadCount++;
                if (breadCount >= 10) { // Example: 10 bread = 1 population
                    breadCount = 0;
                    population++;
                    setChanged();
                }
            }

            // Villager spawning logic
            if (population > 10 && level.getGameTime() % 200 == 0) { // Every 10 seconds
                spawnVillager(level, pos);
            }
        }
    }

    private void spawnVillager(Level level, BlockPos pos) {
        if (population > 0) {
            Villager villager = new Villager(EntityType.VILLAGER, level);
            villager.setPos(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            villager.setCustomName(Component.literal(townName));
            level.addFreshEntity(villager);
            population--; // Decrease population
            setChanged();
        }
    }

    public int getBreadCount() {
        return breadCount;
    }

    public int getPopulation() {
        return population;
    }

    public String getTownName() {
        return townName;
    }

    public String getGuiTownName() {
        return guiTownName;
    }

    private String getRandomTownName() {
        int index = new Random().nextInt(TOWN_NAMES.length);
        return TOWN_NAMES[index];
    }
}