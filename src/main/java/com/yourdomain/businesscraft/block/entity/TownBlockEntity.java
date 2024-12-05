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
import com.yourdomain.businesscraft.town.Town;
import com.yourdomain.businesscraft.town.TownManager;
import com.yourdomain.businesscraft.scoreboard.TownScoreboardManager;
import net.minecraft.server.level.ServerLevel;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import org.slf4j.LoggerFactory;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.world.level.block.Block;

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
            if (townId == null) return 0;
            Town town = TownManager.getInstance().getTown(townId);
            if (town == null) return 0;
            
            return switch (index) {
                case 0 -> town.getBreadCount();
                case 1 -> town.getPopulation();
                case 2 -> town.canSpawnTourists() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // Data is read-only as it's managed by Town class
        }

        @Override
        public int getCount() {
            return 3;
        }
    };
    private static final Logger LOGGER = LogManager.getLogger("BusinessCraft/TownBlockEntity");
    private Map<String, Integer> visitingPopulation = new HashMap<>();
    private static final int VISITOR_RADIUS = 5; // blocks
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private boolean isInPathCreationMode = false;
    private static final int MAX_PATH_DISTANCE = 50;
    private final Random random = new Random();
    private static final int MAX_TOURISTS = 5;
    private boolean touristSpawningEnabled = true;
    private UUID townId;

    public TownBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOWN_BLOCK_ENTITY.get(), pos, state);
        LOGGER.debug("TownBlockEntity created at position: {}", pos);
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
        if (!level.isClientSide()) {
            if (level instanceof ServerLevel serverLevel) {
                TownManager.init(serverLevel);
                level.scheduleTick(getBlockPos(), getBlockState().getBlock(), 1);
            }
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (townId != null) {
            tag.putUUID("townId", townId);
            LOGGER.info("[TownBlockEntity] Saved town ID to NBT: {} at pos: {}", townId, this.getBlockPos());
        } else {
            LOGGER.info("[TownBlockEntity] No town ID to save to NBT at pos: {}", this.getBlockPos());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("townId")) {
            townId = tag.getUUID("townId");
            LOGGER.info("[TownBlockEntity] Loaded town ID from NBT: {} at pos: {}", townId, this.getBlockPos());
        } else {
            LOGGER.info("[TownBlockEntity] No town ID found in NBT at pos: {}", this.getBlockPos());
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            if (level instanceof ServerLevel serverLevel) {
                TownManager.init(serverLevel);
                
                // Only sync data if town exists
                if (townId != null) {
                    Town town = TownManager.getInstance().getTown(townId);
                    if (town != null) {
                        syncTownData();
                    }
                }
            }
        }
        
        if (!level.isClientSide && townId != null) {
            Town town = TownManager.getInstance().getTown(townId);
            if (town != null) {
                // Bread handling logic
                ItemStack stack = itemHandler.getStackInSlot(0);
                if (!stack.isEmpty() && stack.getItem() == Items.BREAD) {
                    stack.shrink(1);
                    town.addBread(1);
                    setChanged();
                }

                // Path-based villager spawning
                if (touristSpawningEnabled && town.canSpawnTourists() && pathStart != null && 
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
                                    villager.setCustomName(Component.literal(town.getName()));
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
                                    villager.addTag("from_" + town.getName());
                                    villager.addTag("pos_" + getBlockPos().getX() + "_" + 
                                                    getBlockPos().getY() + "_" + 
                                                    getBlockPos().getZ());
                                    
                                    level.addFreshEntity(villager);
                                    town.removeTourist();
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

                // Add scoreboard update
                if (level instanceof ServerLevel serverLevel) {
                    TownScoreboardManager.updateScoreboard(serverLevel);
                }
            }
        }
    }

    private void checkForVisitors(Level level, BlockPos pos) {
        if (townId == null) return;
        
        Town thisTown = TownManager.getInstance().getTown(townId);
        if (thisTown == null) return;

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
                
                if (originTown != null && originPos != null && !originTown.equals(thisTown.getName())) {
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

    public String getTownName() {
        if (townId != null) {
            Town town = TownManager.getInstance().getTown(townId);
            if (town != null) {
                return town.getName();
            }
            return "Loading...";  // Town ID exists but town not loaded yet
        }
        return "Initializing...";  // No town ID yet
    }

    private String getRandomTownName() {
        if (ConfigLoader.townNames == null || ConfigLoader.townNames.isEmpty()) {
            return "DefaultTown"; // Fallback name
        }
        int index = new Random().nextInt(ConfigLoader.townNames.size());
        return ConfigLoader.townNames.get(index);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    public ContainerData getContainerData() {
        return data;
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

    public UUID getTownId() {
        LOGGER.info("[TownBlockEntity] Getting town ID: {} at pos: {}", townId, this.getBlockPos());
        return townId;
    }

    public int getBreadCount() {
        return data.get(0);
    }

    public int getPopulation() {
        return data.get(1);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    public void syncTownData() {
        if (level != null && !level.isClientSide()) {
            Town town = TownManager.getInstance().getTown(townId);
            if (town != null) {
                data.set(0, town.getBreadCount());
                data.set(1, town.getPopulation());
                setChanged();
            }
        }
    }

    public void setTownId(UUID id) {
        LOGGER.info("[TownBlockEntity] Setting town ID to: {} at pos: {}", id, this.getBlockPos());
        this.townId = id;
        syncTownData();
    }
}