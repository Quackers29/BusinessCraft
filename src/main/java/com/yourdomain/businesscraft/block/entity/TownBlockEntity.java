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
import net.minecraft.world.entity.npc.VillagerProfession;

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
                case 3 -> touristSpawningEnabled ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> breadCount = value;
                case 1 -> {
                    population = value;
                    setChanged();
                }
                case 2 -> setTownNameIndex(value);
                case 3 -> touristSpawningEnabled = value > 0;
            }
        }

        @Override
        public int getCount() {
            return 4;
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
    private final Random random = new Random();
    private static final int MAX_TOURISTS = 5;
    private boolean touristSpawningEnabled = true;

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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("breadCount", breadCount);
        tag.putInt("population", population);
        tag.putString("townName", townName);
        tag.put("inventory", itemHandler.serializeNBT());
        
        CompoundTag visitorsTag = new CompoundTag();
        visitingPopulation.forEach(visitorsTag::putInt);
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
        tag.putBoolean("TouristSpawningEnabled", touristSpawningEnabled);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        breadCount = tag.getInt("breadCount");
        population = tag.getInt("population");
        townName = tag.getString("townName");
        
        if (tag.contains("inventory")) {
            itemHandler.deserializeNBT(tag.getCompound("inventory"));
        }
        
        CompoundTag visitorsTag = tag.getCompound("visitingPopulation");
        visitingPopulation.clear();
        visitorsTag.getAllKeys().forEach(town -> 
            visitingPopulation.put(town, visitorsTag.getInt(town)));
        
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
        
        // Ensure ContainerData is updated
        data.set(0, breadCount);
        data.set(1, population);
        data.set(2, getTownNameIndex());
        if (tag.contains("TouristSpawningEnabled")) {
            touristSpawningEnabled = tag.getBoolean("TouristSpawningEnabled");
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownBlockEntity blockEntity) {
        if (!level.isClientSide) {
            // Bread handling logic
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!stack.isEmpty() && stack.getItem() == Items.BREAD) {
                stack.shrink(1);
                breadCount++;
                if (breadCount >= ConfigLoader.breadPerPop) {
                    breadCount = 0;
                    population++;
                    setChanged();
                }
            }

            // Path-based villager spawning
            if (touristSpawningEnabled && population >= ConfigLoader.minPopForTourists && 
                pathStart != null && 
                pathEnd != null && 
                level.getGameTime() % 200 == 0) {
                
                // Count existing tourists in the path area
                AABB pathBounds = new AABB(
                    Math.min(pathStart.getX(), pathEnd.getX()) - 1,
                    pathStart.getY(),
                    Math.min(pathStart.getZ(), pathEnd.getZ()) - 1,
                    Math.max(pathStart.getX(), pathEnd.getX()) + 1,
                    pathStart.getY() + 2,
                    Math.max(pathStart.getZ(), pathEnd.getZ()) + 1
                );
                
                List<Villager> existingTourists = level.getEntitiesOfClass(Villager.class, pathBounds);
                
                if (existingTourists.size() < MAX_TOURISTS) {
                    // Try up to 3 times to find a valid spawn location
                    for (int attempt = 0; attempt < 3; attempt++) {
                        // Calculate a random position along the path
                        double progress = random.nextDouble();
                        double exactX = pathStart.getX() + (pathEnd.getX() - pathStart.getX()) * progress;
                        double exactZ = pathStart.getZ() + (pathEnd.getZ() - pathStart.getZ()) * progress;
                        int x = (int) Math.round(exactX);
                        int z = (int) Math.round(exactZ);
                        int y = pathStart.getY() + 1;
                        
                        BlockPos spawnPos = new BlockPos(x, y, z);
                        
                        // Check if the position is already occupied
                        boolean isOccupied = existingTourists.stream()
                            .anyMatch(v -> {
                                BlockPos vPos = v.blockPosition();
                                return vPos.getX() == spawnPos.getX() && 
                                       vPos.getZ() == spawnPos.getZ();
                            });
                        
                        if (!isOccupied && 
                            level.getBlockState(spawnPos).isAir() && 
                            level.getBlockState(spawnPos.above()).isAir()) {
                            
                            Villager villager = EntityType.VILLAGER.create(level);
                            if (villager != null) {
                                // Spawn in center of block and make extremely slow
                                villager.setPos(x + 0.5, y, z + 0.5);
                                villager.setCustomName(Component.literal(townName));
                                villager.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)
                                       .setBaseValue(0.000001);
                                
                                // Set random profession and level 6
                                VillagerProfession[] professions = {
                                    VillagerProfession.ARMORER,
                                    VillagerProfession.BUTCHER,
                                    VillagerProfession.CARTOGRAPHER,
                                    VillagerProfession.CLERIC,
                                    VillagerProfession.FARMER,
                                    VillagerProfession.FISHERMAN,
                                    VillagerProfession.FLETCHER,
                                    VillagerProfession.LEATHERWORKER,
                                    VillagerProfession.LIBRARIAN,
                                    VillagerProfession.MASON,
                                    VillagerProfession.SHEPHERD,
                                    VillagerProfession.TOOLSMITH,
                                    VillagerProfession.WEAPONSMITH
                                };
                                VillagerProfession randomProfession = professions[random.nextInt(professions.length)];
                                villager.setVillagerData(villager.getVillagerData()
                                    .setProfession(randomProfession)
                                    .setLevel(6));
                                
                                // Add tags when spawning villager
                                villager.addTag("type_tourist");
                                villager.addTag("from_" + townName);
                                villager.addTag("pos_" + getBlockPos().getX() + "_" + 
                                                getBlockPos().getY() + "_" + 
                                                getBlockPos().getZ());
                                
                                level.addFreshEntity(villager);
                                population--;
                                setChanged();
                            }
                            break; // Successfully spawned, exit the loop
                        }
                    }
                }
            }

            // Check for visitors
            if (level.getGameTime() % 40 == 0) {
                checkForVisitors(level, pos);
            }
        }
    }

    private void checkForVisitors(Level level, BlockPos pos) {
        List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                Villager.class,
                new AABB(pos).inflate(VISITOR_RADIUS));

        for (Villager villager : nearbyVillagers) {
            if (villager.getTags().contains("type_tourist")) {
                String originTown = null;
                BlockPos originPos = null;

                for (String tag : villager.getTags()) {
                    if (tag.startsWith("from_")) {
                        originTown = tag.substring(5);
                    } else if (tag.startsWith("pos_")) {
                        String[] coords = tag.substring(4).split("_");
                        originPos = new BlockPos(
                            Integer.parseInt(coords[0]),
                            Integer.parseInt(coords[1]),
                            Integer.parseInt(coords[2])
                        );
                    }
                }
                
                if (originTown != null && originPos != null && !originTown.equals(this.townName)) {
                    visitingPopulation.merge(originTown, 1, Integer::sum);

                    // Calculate distance and XP
                    double distance = Math.sqrt(originPos.distSqr(this.getBlockPos()));
                    int xpAmount = Math.max(1, (int)(distance / 10)); // 1 XP per 10 blocks, minimum 1

                    // Spawn XP orbs where the villager was
                    ExperienceOrb xpOrb = new ExperienceOrb(level,
                            villager.getX(), villager.getY(), villager.getZ(),
                            xpAmount);
                    level.addFreshEntity(xpOrb);

                    villager.remove(Entity.RemovalReason.DISCARDED);
                    setChanged();
                }
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