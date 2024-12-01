package com.yourdomain.businesscraft.block.entity;

import com.yourdomain.businesscraft.config.ConfigLoader;
import com.yourdomain.businesscraft.menu.TownBlockMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ExperienceOrb;
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
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;

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
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> breadCount;
                case 1 -> population;
                case 2 -> getTownNameIndex();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> breadCount = value;
                case 1 -> population = value;
                case 2 -> setTownNameIndex(value);
            }
        }

        @Override
        public int getCount() {
            return 3;
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
    private Map<String, Integer> visitingPopulation = new HashMap<>();
    private static final int VISITOR_RADIUS = 5; // blocks
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private boolean isInPathCreationMode = false;
    private static final int MAX_PATH_DISTANCE = 50;

    public TownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWN_BLOCK_ENTITY.get(), pos, state);
        if ("Default Town".equals(townName)) {
            this.townName = getRandomTownName();
            LOGGER.info("Assigned random town name: {}", this.townName);
        }
        this.guiTownName = this.townName;
        LOGGER.info("TownBlockEntity created with town name: {}", this.townName);

        // Log the stack trace to identify where the constructor is being called from
        for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
            LOGGER.debug("at " + element);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.businesscraft.town_block");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeBlockPos(this.getBlockPos());
        return new TownBlockMenu(id, inventory, buffer);
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
        CompoundTag visitorsTag = new CompoundTag();
        visitingPopulation.forEach((town, count) -> visitorsTag.putInt(town, count));
        tag.put("visitingPopulation", visitorsTag);
        if (pathStart != null) {
            tag.putInt("pathStartX", pathStart.getX());
            tag.putInt("pathStartY", pathStart.getY());
            tag.putInt("pathStartZ", pathStart.getZ());
        }
        if (pathEnd != null) {
            tag.putInt("pathEndX", pathEnd.getX());
            tag.putInt("pathEndY", pathEnd.getY());
            tag.putInt("pathEndZ", pathEnd.getZ());
        }
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
        CompoundTag visitorsTag = tag.getCompound("visitingPopulation");
        visitingPopulation.clear();
        visitorsTag.getAllKeys().forEach(town -> visitingPopulation.put(town, visitorsTag.getInt(town)));
        if (tag.contains("pathStartX")) {
            pathStart = new BlockPos(
                tag.getInt("pathStartX"),
                tag.getInt("pathStartY"),
                tag.getInt("pathStartZ")
            );
        }
        if (tag.contains("pathEndX")) {
            pathEnd = new BlockPos(
                tag.getInt("pathEndX"),
                tag.getInt("pathEndY"),
                tag.getInt("pathEndZ")
            );
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownBlockEntity blockEntity) {
        if (!level.isClientSide) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!stack.isEmpty() && stack.getItem() == Items.BREAD) {
                stack.shrink(1); // Consume one bread
                breadCount++;
                if (breadCount >= ConfigLoader.breadPerPop) {
                    breadCount = 0;
                    population++;
                    setChanged();
                }
            }

            // Villager spawning logic
            if (population > ConfigLoader.minPopForTourists && level.getGameTime() % 200 == 0) {
                spawnVillager(level, pos);
            }

            // Every 2 seconds, check for visitors
            if (level.getGameTime() % 40 == 0) {
                checkForVisitors(level, pos);
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

    private void checkForVisitors(Level level, BlockPos pos) {
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                new AABB(pos).inflate(VISITOR_RADIUS));

        for (Villager villager : nearbyVillagers) {
            Component customName = villager.getCustomName();
            if (customName != null && !customName.getString().equals(this.townName)) {
                String originTown = customName.getString();
                visitingPopulation.merge(originTown, 1, Integer::sum);

                // Spawn XP bottle where the villager was
                ExperienceOrb xpOrb = new ExperienceOrb(level,
                        villager.getX(), villager.getY(), villager.getZ(),
                        1);
                level.addFreshEntity(xpOrb);

                villager.remove(Entity.RemovalReason.DISCARDED);
                setChanged();
            }
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
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    public void setGuiTownName(String guiTownName) {
        this.guiTownName = guiTownName;
    }

    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        this.saveAdditional(tag);
        return tag;
    }

    public ContainerData getContainerData() {
        return data;
    }

    private int getTownNameIndex() {
        for (int i = 0; i < TOWN_NAMES.length; i++) {
            if (TOWN_NAMES[i].equals(townName)) {
                return i;
            }
        }
        return 0; // Default to first index if not found
    }

    private void setTownNameIndex(int index) {
        if (index >= 0 && index < TOWN_NAMES.length) {
            townName = TOWN_NAMES[index];
        }
    }

    public static String[] getTownNames() {
        return TOWN_NAMES;
    }

    public Map<String, Integer> getVisitingPopulation() {
        return Collections.unmodifiableMap(visitingPopulation);
    }

    public int getVisitingPopulationFrom(String townName) {
        return visitingPopulation.getOrDefault(townName, 0);
    }

    public BlockPos getPathStart() {
        return pathStart;
    }

    public BlockPos getPathEnd() {
        return pathEnd;
    }

    public void setPathStart(BlockPos pos) {
        this.pathStart = pos;
        setChanged();
    }

    public void setPathEnd(BlockPos pos) {
        this.pathEnd = pos;
        setChanged();
    }

    public boolean isInPathCreationMode() {
        return isInPathCreationMode;
    }

    public void setPathCreationMode(boolean mode) {
        this.isInPathCreationMode = mode;
    }

    public boolean isValidPathDistance(BlockPos pos) {
        return pos.distManhattan(this.getBlockPos()) <= MAX_PATH_DISTANCE;
    }
}