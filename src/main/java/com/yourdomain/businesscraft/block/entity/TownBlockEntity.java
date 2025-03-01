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
import net.minecraft.world.item.Item;
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
import net.minecraft.world.entity.LivingEntity;

import java.util.Random;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.UUID;
import java.util.Iterator;
import org.slf4j.LoggerFactory;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.Connection;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import net.minecraft.world.phys.Vec3;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import com.yourdomain.businesscraft.service.TouristVehicleManager;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import com.yourdomain.businesscraft.api.ITownDataProvider.VisitHistoryRecord;
import com.yourdomain.businesscraft.platform.Platform;

public class TownBlockEntity extends BlockEntity implements MenuProvider, BlockEntityTicker<TownBlockEntity> {
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            // Accept any item as a resource
            return !stack.isEmpty();
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private static final int DATA_BREAD = 0;
    private static final int DATA_POPULATION = 1;
    private static final int DATA_SPAWN_ENABLED = 2;
    private static final int DATA_CAN_SPAWN = 3;
    private static final int DATA_SEARCH_RADIUS = 4;
    private final ContainerData data = new SimpleContainerData(5) {
        // Tracking time between logs to prevent spamming
        private long lastLogTime = 0;
        
        @Override
        public int get(int index) {
            if (level != null && level.isClientSide()) {
                return super.get(index);
            }
            
            if (townId == null) return 0;
            if (level instanceof ServerLevel sLevel) {
                Town town = TownManager.get(sLevel).getTown(townId);
                if (town == null) return 0;
                
                int value = switch (index) {
                    case DATA_BREAD -> town.getBreadCount(); // Legacy support for bread count
                    case DATA_POPULATION -> town.getPopulation();
                    case DATA_SPAWN_ENABLED -> town.isTouristSpawningEnabled() ? 1 : 0;
                    case DATA_CAN_SPAWN -> town.canSpawnTourists() ? 1 : 0;
                    case DATA_SEARCH_RADIUS -> town.getSearchRadius();
                    default -> 0;
                };
                
                super.set(index, value);
                return value;
            }
            return 0;
        }

        @Override
        public void set(int index, int value) {
            super.set(index, value);
        }
    };
    private static final Logger LOGGER = LogManager.getLogger("BusinessCraft/TownBlockEntity");
    private Map<String, Integer> visitingPopulation = new HashMap<>();
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private boolean isInPathCreationMode = false;
    private static final int MAX_PATH_DISTANCE = 50;
    private final Random random = new Random();
    private static final int MAX_TOURISTS = 5;
    private boolean touristSpawningEnabled = true;
    private UUID townId;
    private String name;
    private Town town;
    private static final ConfigLoader CONFIG = ConfigLoader.INSTANCE;
    private Map<UUID, Vec3> lastPositions = new HashMap<>();
    private Map<UUID, Vec3> lastVisitorPositions = new HashMap<>();
    private static final double VISITOR_POSITION_CHANGE_THRESHOLD = 0.001;
    private static final int DEFAULT_SEARCH_RADIUS = CONFIG.vehicleSearchRadius;
    private int searchRadius = DEFAULT_SEARCH_RADIUS;
    private final AABB searchBounds = new AABB(worldPosition).inflate(15);
    private List<LivingEntity> tourists = new ArrayList<>();
    private ITownDataProvider townDataProvider;
    
    /**
     * Rate limiting parameters for markDirty calls
     * Prevents excessive updates which can flood logs and impact performance
     */
    private long lastMarkDirtyTime = 0;
    private static final long MARK_DIRTY_COOLDOWN_MS = 2000; // 2 seconds between calls
    
    // Add a new TouristVehicleManager instance
    private final TouristVehicleManager touristVehicleManager = new TouristVehicleManager();

    // Client-side cache of resources for rendering
    private Map<Item, Integer> clientResources = new HashMap<>();

    // Visit buffer for grouping arrivals
    private static class VisitBuffer {
        // Single value object to store visitor count and origin position
        private static class VisitorInfo {
            final int count;
            final BlockPos originPos;
            
            public VisitorInfo(int count, BlockPos originPos) {
                this.count = count;
                this.originPos = originPos;
            }
            
            public VisitorInfo incrementCount() {
                return new VisitorInfo(count + 1, originPos);
            }
        }
        
        private final Map<UUID, VisitorInfo> visitors = new HashMap<>();
        private long lastVisitTime = 0;
        private static final long BUFFER_TIMEOUT_MS = 1000; // 1 second timeout

        public void addVisitor(UUID townId, BlockPos originPos) {
            // Update or insert a new VisitorInfo
            visitors.compute(townId, (id, info) -> {
                if (info == null) {
                    return new VisitorInfo(1, originPos);
                } else {
                    return info.incrementCount();
                }
            });
            
            lastVisitTime = System.currentTimeMillis();
        }

        public boolean shouldProcess() {
            return !visitors.isEmpty() && 
                   System.currentTimeMillis() - lastVisitTime > BUFFER_TIMEOUT_MS;
        }

        public List<VisitHistoryRecord> processVisits() {
            if (visitors.isEmpty()) return Collections.emptyList();
            
            long now = System.currentTimeMillis();
            List<VisitHistoryRecord> records = visitors.entrySet().stream()
                .map(entry -> new VisitHistoryRecord(
                    now, 
                    entry.getKey(), 
                    entry.getValue().count,
                    entry.getValue().originPos
                ))
                .collect(Collectors.toList());
                
            // Clear the buffer
            visitors.clear();
            return records;
        }
    }

    // History buffer storage
    private final VisitBuffer visitBuffer = new VisitBuffer();
    
    // Client-side history cache
    private List<VisitHistoryRecord> clientVisitHistory = new ArrayList<>();

    // Add a client-side town name cache
    private final Map<UUID, String> townNameCache = new HashMap<>();

    // Add platform support
    private static final int MAX_PLATFORMS = 5;
    private List<Platform> platforms = new ArrayList<>();
    private List<Platform> clientPlatforms = new ArrayList<>();
    private boolean isInPlatformCreationMode = false;
    private UUID platformBeingEdited = null;

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
        
        // Update from provider when loaded
        if (!level.isClientSide()) {
            updateFromTownProvider();
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
        tag.put("inventory", itemHandler.serializeNBT());
        
        // Save local data to tag
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        
        tag.putString("name", name != null ? name : "");
        
        // Save legacy path data for backward compatibility
        if (pathStart != null) {
            CompoundTag startPos = new CompoundTag();
            startPos.putInt("x", pathStart.getX());
            startPos.putInt("y", pathStart.getY());
            startPos.putInt("z", pathStart.getZ());
            tag.put("PathStart", startPos);
        }
        
        if (pathEnd != null) {
            CompoundTag endPos = new CompoundTag();
            endPos.putInt("x", pathEnd.getX());
            endPos.putInt("y", pathEnd.getY());
            endPos.putInt("z", pathEnd.getZ());
            tag.put("PathEnd", endPos);
        }
        
        // Save platforms
        if (!platforms.isEmpty()) {
            ListTag platformsTag = new ListTag();
            for (Platform platform : platforms) {
                platformsTag.add(platform.toNBT());
            }
            tag.put("platforms", platformsTag);
        }
        
        tag.putInt("searchRadius", searchRadius);
        
        // Visit history is now saved in the Town object
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        // First load paths from the tag itself as a fallback
        if (tag.contains("PathStart")) {
            CompoundTag startPos = tag.getCompound("PathStart");
            pathStart = new BlockPos(
                startPos.getInt("x"),
                startPos.getInt("y"),
                startPos.getInt("z")
            );
        }
        
        if (tag.contains("PathEnd")) {
            CompoundTag endPos = tag.getCompound("PathEnd");
            pathEnd = new BlockPos(
                endPos.getInt("x"),
                endPos.getInt("y"),
                endPos.getInt("z")
            );
        }
        
        if (tag.contains("searchRadius")) {
            searchRadius = tag.getInt("searchRadius");
        }
        
        // Then try to get the town data
        if (tag.contains("TownId")) {
            townId = tag.getUUID("TownId");
            if (level instanceof ServerLevel sLevel1) {
                town = TownManager.get(sLevel1).getTown(townId);
                
                // Migrate any legacy visit history from block entity to Town
                if (town != null && tag.contains("visitHistory")) {
                    ITownDataProvider provider = town;
                    ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
                    LOGGER.info("Migrating {} visit history records to Town {}", historyTag.size(), town.getName());
                    
                    for (int i = 0; i < historyTag.size(); i++) {
                        CompoundTag visitTag = historyTag.getCompound(i);
                        
                        long timestamp = visitTag.getLong("timestamp");
                        int count = visitTag.getInt("count");
                        
                        // Get the town ID or generate one from the name
                        UUID originTownId = null;
                        if (visitTag.contains("townId")) {
                            originTownId = visitTag.getUUID("townId");
                        } else if (visitTag.contains("town")) {
                            // Legacy format - generate a UUID from the name
                            String townName = visitTag.getString("town");
                            originTownId = UUID.nameUUIDFromBytes(townName.getBytes());
                            LOGGER.info("Converted legacy town name '{}' to UUID: {}", townName, originTownId);
                        } else {
                            continue; // Skip if no town identifier
                        }
                        
                        BlockPos originPos = BlockPos.ZERO;
                        if (visitTag.contains("pos")) {
                            CompoundTag posTag = visitTag.getCompound("pos");
                            originPos = new BlockPos(
                                posTag.getInt("x"),
                                posTag.getInt("y"),
                                posTag.getInt("z")
                            );
                        }
                        
                        // Add the record to the Town using the provider
                        try {
                            // Use reflection to access the private list directly (to preserve exact timestamps)
                            java.lang.reflect.Field historyField = Town.class.getDeclaredField("visitHistory");
                            historyField.setAccessible(true);
                            @SuppressWarnings("unchecked")
                            List<VisitHistoryRecord> visitHistory = (List<VisitHistoryRecord>) historyField.get(town);
                            visitHistory.add(new VisitHistoryRecord(timestamp, originTownId, count, originPos));
                            
                            // Ensure list is sorted by timestamp (newest first)
                            visitHistory.sort((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                            
                            // Trim if needed
                            while (visitHistory.size() > 50) { // MAX_HISTORY_SIZE
                                visitHistory.remove(visitHistory.size() - 1);
                            }
                            
                            // Mark town as dirty
                            provider.markDirty();
                        } catch (Exception e) {
                            // Fallback to the standard method if reflection fails
                            LOGGER.error("Error migrating visit history: {}", e.getMessage());
                            provider.recordVisit(originTownId, count, originPos);
                        }
                    }
                }
                
                if (town != null) {
                    // Update local values from the Town object, but prefer the ones we already loaded
                    touristSpawningEnabled = town.isTouristSpawningEnabled();
                    
                    // Only use town paths if we don't have any
                    if (pathStart == null && town.getPathStart() != null) {
                        pathStart = town.getPathStart();
                    }
                    
                    if (pathEnd == null && town.getPathEnd() != null) {
                        pathEnd = town.getPathEnd();
                    }
                    
                    // Use town search radius if already set
                    if (searchRadius <= 0 && town.getSearchRadius() > 0) {
                        searchRadius = town.getSearchRadius();
                    }
                }
            }
        }
        
        if (tag.contains("name")) {
            name = tag.getString("name");
        }

        // Load platforms
        if (tag.contains("platforms")) {
            platforms.clear();
            ListTag platformsTag = tag.getList("platforms", Tag.TAG_COMPOUND);
            for (int i = 0; i < platformsTag.size(); i++) {
                CompoundTag platformTag = platformsTag.getCompound(i);
                platforms.add(Platform.fromNBT(platformTag));
            }
        } else if (pathStart != null && pathEnd != null) {
            // If no platforms but legacy paths exist, create a platform from them
            if (platforms.isEmpty()) {
                Platform legacyPlatform = new Platform("Main Platform", true, pathStart, pathEnd);
                platforms.add(legacyPlatform);
            }
        }
    }

    @Override
    public void tick(Level level, BlockPos pos, BlockState state, TownBlockEntity blockEntity) {
        if (!level.isClientSide()) {
            // Update from provider at regular intervals
            updateFromTownProvider();
            
            if (level instanceof ServerLevel sLevel1) {
                // Only sync data if town exists
                if (townId != null) {
                    Town town = TownManager.get(sLevel1).getTown(townId);
                    if (town != null) {
                        // Ensure paths are synchronized from Town to BlockEntity
                        if (town.getPathStart() != null && !town.getPathStart().equals(pathStart)) {
                            pathStart = town.getPathStart();
                        }
                        
                        if (town.getPathEnd() != null && !town.getPathEnd().equals(pathEnd)) {
                            pathEnd = town.getPathEnd();
                        }
                        
                        // Ensure search radius is synchronized
                        if (town.getSearchRadius() != searchRadius) {
                            searchRadius = town.getSearchRadius();
                        }
                        
                        syncTownData();
                        
                        // Resource handling - process any item in the slot
                        processResourcesInSlot();
                    }
                }
            }
        }
        
        if (!level.isClientSide && townId != null) {
            if (level instanceof ServerLevel sLevel1) {
                Town town = TownManager.get(sLevel1).getTown(townId);
                if (town != null) {
                    // Platform-based villager spawning
                    if (touristSpawningEnabled && town.canSpawnTourists() && 
                        !platforms.isEmpty() && 
                        level.getGameTime() % 200 == 0) {
                        
                        // Try to spawn from each enabled platform
                        for (Platform platform : platforms) {
                            if (platform.isEnabled() && platform.isComplete()) {
                                spawnTouristOnPlatform(level, town, platform);
                            }
                        }
                    }

                    // Check for visitors
                    if (level.getGameTime() % 40 == 0) {
                        checkForVisitors(level, pos);
                    }

                    // Add scoreboard update
                    if (level instanceof ServerLevel sLevel2) {
                        TownScoreboardManager.updateScoreboard(sLevel2);
                    }
                }
            }
        }
        
        // Handle tourist vehicles for all platforms
        if (level.getGameTime() % 20 == 0) { // Every 1 second
            if (touristSpawningEnabled && townId != null) {
                // Get town object safely
                Town currentTown = null;
                if (level instanceof ServerLevel sLevel) {
                    currentTown = TownManager.get(sLevel).getTown(townId);
                }
                
                if (currentTown != null && currentTown.canSpawnTourists()) {
                    // Process each enabled platform
                    for (Platform platform : platforms) {
                        if (platform.isEnabled() && platform.isComplete()) {
                            touristVehicleManager.mountTouristsToVehicles(
                                level, 
                                platform.getStartPos(), 
                                platform.getEndPos(), 
                                searchRadius, 
                                townId
                            );
                        }
                    }
                }
            }
        }
    }

    private void checkForVisitors(Level level, BlockPos pos) {
        if (townId == null || pathStart == null || pathEnd == null) return;
        
        if (level instanceof ServerLevel sLevel1) {
            Town thisTown = TownManager.get(sLevel1).getTown(townId);
            if (thisTown == null) return;
            
            // Get the town data provider
            ITownDataProvider provider = getTownDataProvider();
            if (provider == null) return;

            // Create AABB around the path
            AABB searchBounds = new AABB(
                Math.min(pathStart.getX(), pathEnd.getX()) - searchRadius,
                Math.min(pathStart.getY(), pathEnd.getY()) - 2,
                Math.min(pathStart.getZ(), pathEnd.getZ()) - searchRadius,
                Math.max(pathStart.getX(), pathEnd.getX()) + searchRadius,
                Math.max(pathStart.getY(), pathEnd.getY()) + 4,
                Math.max(pathStart.getZ(), pathEnd.getZ()) + searchRadius
            );

            List<Villager> nearbyVillagers = level.getEntitiesOfClass(
                    Villager.class,
                    searchBounds);

            // First, clean up positions of villagers that are no longer present
            cleanupVisitorPositions(nearbyVillagers);

            // Process visitors that are stationary
            for (Villager villager : nearbyVillagers) {
                Vec3 currentPos = villager.position();
                Vec3 lastPos = lastVisitorPositions.get(villager.getUUID());
                
                // Store current position for next check
                lastVisitorPositions.put(villager.getUUID(), currentPos);
                
                // Skip if this is the first time we've seen this villager
                if (lastPos == null) continue;
                
                // Calculate position change
                double positionChange = currentPos.distanceTo(lastPos);
                
                // Skip if villager has moved too much (likely on transport)
                if (positionChange > VISITOR_POSITION_CHANGE_THRESHOLD) {
                    continue;
                }

                if (villager.getTags().contains("type_tourist")) {
                    UUID originTownId = null;
                    BlockPos originPos = null;

                    // Extract tourist information from tags
                    try {
                    for (String tag : villager.getTags()) {
                        if (tag.startsWith("from_town_")) {
                            originTownId = UUID.fromString(tag.substring(10));
                            } else if (tag.startsWith("pos_")) {
                                String[] parts = tag.substring(4).split("_");
                                if (parts.length == 3) {
                                    int x = Integer.parseInt(parts[0]);
                                    int y = Integer.parseInt(parts[1]);
                                    int z = Integer.parseInt(parts[2]);
                                    originPos = new BlockPos(x, y, z);
                                }
                            }
                        }
                    } catch (IllegalArgumentException e) {
                        // Log the error and skip this villager
                        LOGGER.error("Error parsing tourist tags: {}", e.getMessage());
                        continue;
                    }
                    
                    if (originTownId != null && !originTownId.equals(this.townId)) {
                        // Add to visit buffer using UUID instead of name
                        visitBuffer.addVisitor(originTownId, originPos != null ? originPos : BlockPos.ZERO);

                        // Calculate distance and XP
                        double distance = Math.sqrt(villager.blockPosition().distSqr(this.getBlockPos()));
                        int xpAmount = Math.max(1, (int)(distance / 10));

                        ExperienceOrb xpOrb = new ExperienceOrb(level,
                                villager.getX(), villager.getY(), villager.getZ(),
                                xpAmount);
                        level.addFreshEntity(xpOrb);

                        villager.remove(Entity.RemovalReason.DISCARDED);
                        setChanged();
                    }
                }
            }
            
            // Process the visit buffer if it's ready
            if (visitBuffer.shouldProcess()) {
                List<VisitHistoryRecord> newVisits = visitBuffer.processVisits();
                if (!newVisits.isEmpty()) {
                    // Add to the town data provider (single source of truth)
                    for (VisitHistoryRecord record : newVisits) {
                        try {
                            // Record the visit in the Town
                            provider.recordVisit(record.getOriginTownId(), record.getCount(), record.getOriginPos());
                            
                            // Each visitor should be processed by adding them to the town's tally
                            for (int i = 0; i < record.getCount(); i++) {
                                thisTown.addVisitor(record.getOriginTownId());
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error processing visitor batch: {}", e.getMessage());
                        }
                    }
                    
                    // Ensure data is saved
                        if (level instanceof ServerLevel sLevel3) {
                            TownManager.get(sLevel3).markDirty();
                        }
                    
                    // Update clients
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    /**
     * Cleans up the lastVisitorPositions map by removing entries for villagers 
     * that no longer exist in the current list of nearby villagers.
     * This prevents memory leaks from accumulating over time.
     */
    private void cleanupVisitorPositions(List<Villager> currentVillagers) {
        // Skip if empty to avoid unnecessary work
        if (lastVisitorPositions.isEmpty()) return;
        
        // Create a set of current villager UUIDs for efficient lookups
        Set<UUID> currentVillagerIds = currentVillagers.stream()
            .map(Entity::getUUID)
            .collect(Collectors.toSet());
            
        // Keep track of how many entries we're removing for logging
        int removedCount = 0;
        
        // Remove entries that don't correspond to current villagers
        Iterator<UUID> iterator = lastVisitorPositions.keySet().iterator();
        while (iterator.hasNext()) {
            UUID id = iterator.next();
            if (!currentVillagerIds.contains(id)) {
                iterator.remove();
                removedCount++;
            }
        }
        
        // Log only if we actually removed something and at debug level only
        if (removedCount > 0 && level != null && !level.isClientSide() && LOGGER.isDebugEnabled()) {
            LOGGER.debug("Cleaned up {} stale visitor position entries", removedCount);
        }
    }

    public String getTownName() {
        if (townId != null) {
            if (level.isClientSide && name != null) {
                return name; // Use client-cached name
            }
            if (level instanceof ServerLevel sLevel1) {
                Town town = TownManager.get(sLevel1).getTown(townId);
                if (town != null) {
                    // Always update our local cached name with the latest town name
                    if (!town.getName().equals(name)) {
                        LOGGER.debug("[DEBUG] Updating cached name from {} to {}", name, town.getName());
                        name = town.getName();
                    }
                    return town.getName();
                }
                return "Loading...2";
            }
        }
        return "Initializing...";
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

    /**
     * Creates a standardized update tag with all necessary data for client rendering
     */
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        
        // Add town info
        if (townId != null) {
            tag.putUUID("TownId", townId);
        }
        
        // Add name for client display
        tag.putString("name", name != null ? name : "");
        
        // Add resource data for client rendering
        syncResourcesForClient(tag);
        
        // Add platforms
        if (!platforms.isEmpty()) {
            ListTag platformsTag = new ListTag();
            for (Platform platform : platforms) {
                platformsTag.add(platform.toNBT());
            }
            tag.put("platforms", platformsTag);
        }
        
        return tag;
    }
    
    /**
     * Adds resource data to the provided tag for client-side rendering
     * This centralizes our resource serialization logic in one place
     */
    private void syncResourcesForClient(CompoundTag tag) {
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            // Create a resources tag
            CompoundTag resourcesTag = new CompoundTag();
            
            // Add all resources to the tag
            provider.getAllResources().forEach((item, count) -> {
                String itemKey = ForgeRegistries.ITEMS.getKey(item).toString();
                resourcesTag.putInt(itemKey, count);
            });
            
            // Add resources tag to the update tag
            tag.put("clientResources", resourcesTag);
        }
        
        // Add visit history data
        syncVisitHistoryForClient(tag);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        
        // Handle name updates
        if (tag.contains("name")) {
            String newName = tag.getString("name");
            if (!newName.equals(name)) {
                LOGGER.debug("Updating town name from {} to {}", name, newName);
                name = newName;
            }
        }
        
        // Handle platform data updates
        if (tag.contains("platforms")) {
            // Platform update received from server
            clientPlatforms.clear();
            ListTag platformsTag = tag.getList("platforms", Tag.TAG_COMPOUND);
            for (int i = 0; i < platformsTag.size(); i++) {
                CompoundTag platformTag = platformsTag.getCompound(i);
                Platform platform = Platform.fromNBT(platformTag);
                clientPlatforms.add(platform);
            }
        }
        
        // Load client resources data
        loadResourcesFromTag(tag);
        
        // Load visit history data
        loadVisitHistoryFromTag(tag);
    }
    
    /**
     * Loads resources from the provided tag into the client-side cache
     * This centralizes our resource deserialization logic in one place
     */
    private void loadResourcesFromTag(CompoundTag tag) {
        if (tag.contains("clientResources")) {
            CompoundTag resourcesTag = tag.getCompound("clientResources");
            
            // Clear previous resources
            clientResources.clear();
            
            // Load all resources from the tag
            for (String key : resourcesTag.getAllKeys()) {
                try {
                    ResourceLocation resourceLocation = new ResourceLocation(key);
                    Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                    if (item != null && item != Items.AIR) {
                        int count = resourcesTag.getInt(key);
                        clientResources.put(item, count);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading client resource: {}", key, e);
                }
            }
        }
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
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
        
        // Update the town through the provider
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setPathStart(pos);
            markDirtyWithRateLimit(provider);
        }
        
        setChanged();
    }

    public void setPathEnd(BlockPos pos) {
        this.pathEnd = pos;
        
        // Update the town through the provider
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setPathEnd(pos);
            markDirtyWithRateLimit(provider);
        }
        
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
        return townId;
    }

    public int getBreadCount() {
        return data.get(DATA_BREAD);
    }

    public int getPopulation() {
        return data.get(DATA_POPULATION);
    }

    public void syncTownData() {
        if (level != null && !level.isClientSide()) {
            // Get latest name from provider if needed
            if (townId != null) {
                ITownDataProvider provider = getTownDataProvider();
                if (provider != null) {
                    String latestName = provider.getTownName();
                    if (!latestName.equals(name)) {
                        LOGGER.debug("Updating town name from '{}' to '{}'", name, latestName);
                        name = latestName;
                    }
                }
            }
            
            // Update container data
            data.set(DATA_BREAD, data.get(DATA_BREAD));
            data.set(DATA_POPULATION, data.get(DATA_POPULATION));
            data.set(DATA_SPAWN_ENABLED, data.get(DATA_SPAWN_ENABLED));
            data.set(DATA_CAN_SPAWN, data.get(DATA_CAN_SPAWN));
            data.set(DATA_SEARCH_RADIUS, data.get(DATA_SEARCH_RADIUS));
            
            // Force a block update to sync the latest data
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            setChanged();
        }
    }

    public void setTownId(UUID id) {
        this.townId = id;
        if (level instanceof ServerLevel sLevel1) {
            Town town = TownManager.get(sLevel1).getTown(id);
            this.name = town != null ? town.getName() : "Unnamed";
            syncTownData();
        }
    }

    // Replace the old mountTouristsToVehicles method with a simplified version that delegates to the manager
    private void mountTouristsToVehicles() {
        if (level == null || level.isClientSide || town == null) return;
        if (pathStart == null || pathEnd == null) return;

        int mounted = touristVehicleManager.mountTouristsToVehicles(level, pathStart, pathEnd, searchRadius, townId);
        if (mounted > 0) {
            LOGGER.debug("Mounted {} tourists to vehicles for town {}", mounted, name);
        }
    }

    private String formatVec3(Vec3 vec) {
        return String.format("[%.6f, %.6f, %.6f]", vec.x, vec.y, vec.z);
    }

    public int getSearchRadius() {
        return searchRadius;
    }

    public void setSearchRadius(int radius) {
        this.searchRadius = Math.max(1, Math.min(radius, 100)); // Limit between 1-100 blocks
        
        // Also update in the Town object
        if (townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                town.setSearchRadius(this.searchRadius);
                TownManager.get(sLevel).markDirty();
            }
        }
        
        // Make sure to update the container data
        if (level != null && !level.isClientSide()) {
            data.set(DATA_SEARCH_RADIUS, this.searchRadius);
        }
        
        setChanged();
    }

    public Town getTown() {
        return town;
    }

    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
        
        // Update the town through the provider
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            provider.setTouristSpawningEnabled(enabled);
            markDirtyWithRateLimit(provider);
        }
        
        // Update container data
        if (level != null && !level.isClientSide()) {
            data.set(DATA_SPAWN_ENABLED, enabled ? 1 : 0);
        }
        
        setChanged();
    }

    /**
     * Gets the town data provider, initializing it if needed
     */
    public ITownDataProvider getTownDataProvider() {
        if (townDataProvider == null && townId != null && level instanceof ServerLevel sLevel) {
            Town town = TownManager.get(sLevel).getTown(townId);
            if (town != null) {
                townDataProvider = town;
            }
        }
        return townDataProvider;
    }
    
    /**
     * Updates cached values from the town data provider
     */
    private void updateFromTownProvider() {
        ITownDataProvider provider = getTownDataProvider();
        if (provider != null) {
            // Sync data from the provider (the single source of truth)
            this.touristSpawningEnabled = provider.isTouristSpawningEnabled();
            this.pathStart = provider.getPathStart();
            this.pathEnd = provider.getPathEnd();
            this.searchRadius = provider.getSearchRadius();
            
            // If we made any local changes, we need to sync them back
            if (level != null && !level.isClientSide() && this.townId != null) {
                // Mark the provider as dirty to ensure changes are saved, but with rate limiting
                markDirtyWithRateLimit(provider);
            }
        }
    }
    
    /**
     * Marks the provider as dirty with rate limiting to reduce excessive updates
     * @param provider The provider to mark as dirty
     */
    private void markDirtyWithRateLimit(ITownDataProvider provider) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMarkDirtyTime > MARK_DIRTY_COOLDOWN_MS) {
            provider.markDirty();
            lastMarkDirtyTime = currentTime;
            
            // Only log at debug level when actually marking dirty
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Marking town data as dirty after cooldown period");
            }
        }
    }

    public void processResourcesInSlot() {
        if (level == null || level.isClientSide()) return;
        
            ItemStack stack = itemHandler.getStackInSlot(0);
        if (!stack.isEmpty() && townId != null) {
            if (level instanceof ServerLevel sLevel) {
                Town town = TownManager.get(sLevel).getTown(townId);
                if (town != null) {
                    Item item = stack.getItem();
                    // Process just 1 item per tick
                    stack.shrink(1);
                    town.addResource(item, 1);
                    setChanged();
                    
                    // Send update to clients when resources change
                    level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    // Ensure we clean up resources when the block entity is removed
    @Override
    public void setRemoved() {
        super.setRemoved();
        touristVehicleManager.clearTrackedVehicles();
        lastVisitorPositions.clear();
        LOGGER.debug("Cleared visitor position tracking on block removal");
    }

    /**
     * Gets the client-side resources map for rendering
     * @return Map of resources with quantities
     */
    public Map<Item, Integer> getClientResources() {
        return Collections.unmodifiableMap(clientResources);
    }

    /**
     * Creates the update packet for sending to clients
     */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Gets the visit history for client-side display
     */
    public List<VisitHistoryRecord> getVisitHistory() {
        if (level != null && level.isClientSide()) {
            return Collections.unmodifiableList(clientVisitHistory);
        } else {
                ITownDataProvider provider = getTownDataProvider();
                if (provider != null) {
                return provider.getVisitHistory();
            }
            return Collections.emptyList();
        }
    }
    
    /**
     * Adds visit history data to the provided tag for client-side rendering
     */
    private void syncVisitHistoryForClient(CompoundTag tag) {
        ITownDataProvider provider = getTownDataProvider();
        if (provider == null) return;
        
        List<VisitHistoryRecord> history = provider.getVisitHistory();
        if (history.isEmpty()) return;
        
        ListTag historyTag = new ListTag();
        for (VisitHistoryRecord record : history) {
            CompoundTag visitTag = new CompoundTag();
            visitTag.putLong("timestamp", record.getTimestamp());
            
            // Store both UUID and resolved name for client display
            if (record.getOriginTownId() != null) {
                UUID townId = record.getOriginTownId();
                visitTag.putUUID("townId", townId);
                
                // Resolve town name using the centralized method
                String townName = resolveTownName(townId, true);
                visitTag.putString("townName", townName);
            }
            
            visitTag.putInt("count", record.getCount());
            
            // Add origin position
            if (record.getOriginPos() != null && record.getOriginPos() != BlockPos.ZERO) {
                CompoundTag posTag = new CompoundTag();
                posTag.putInt("x", record.getOriginPos().getX());
                posTag.putInt("y", record.getOriginPos().getY());
                posTag.putInt("z", record.getOriginPos().getZ());
                visitTag.put("pos", posTag);
            }
            
            historyTag.add(visitTag);
        }
        tag.put("visitHistory", historyTag);
    }
    
    /**
     * Loads visit history from the provided tag into the client-side cache
     */
    private void loadVisitHistoryFromTag(CompoundTag tag) {
        if (tag.contains("visitHistory")) {
            ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
            
            // Clear previous history
            clientVisitHistory.clear();
            
            // Load all history entries
            for (int i = 0; i < historyTag.size(); i++) {
                CompoundTag visitTag = historyTag.getCompound(i);
                
                long timestamp = visitTag.getLong("timestamp");
                int count = visitTag.getInt("count");
                
                // Handle both old and new formats
                UUID townId = null;
                if (visitTag.contains("townId")) {
                    townId = visitTag.getUUID("townId");
                } else {
                    // Fallback to legacy format
                    LOGGER.warn("Found legacy visit history format without townId");
                    continue;
                }
                
                // Store the pre-resolved town name from the server in a client field
                if (visitTag.contains("townName")) {
                    String townName = visitTag.getString("townName");
                    // Only log when a town name is added for the first time
                    if (!townNameCache.containsKey(townId)) {
                        LOGGER.debug("Loaded town name for {}: {}", townId, townName);
                    }
                    // Store the name in a map for client-side lookup
                    townNameCache.put(townId, townName);
                } else {
                    LOGGER.warn("Missing town name for visit record with ID {}", townId);
                    // If no name is provided, use a fallback
                    townNameCache.put(townId, "Town-" + townId.toString().substring(0, 8));
                }
                
                BlockPos originPos = BlockPos.ZERO;
                if (visitTag.contains("pos")) {
                    CompoundTag posTag = visitTag.getCompound("pos");
                    originPos = new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z")
                    );
                }
                
                // Create the visit record
                clientVisitHistory.add(new VisitHistoryRecord(timestamp, townId, count, originPos));
            }
        }
    }

    /**
     * Resolves a town name from its UUID with flexible behavior for client/server contexts
     * @param townId The UUID of the town to resolve
     * @param logResolveFailure Whether to log when resolution fails
     * @return The resolved name or a fallback
     */
    private String resolveTownName(UUID townId, boolean logResolveFailure) {
        if (townId == null) return "Unknown";
        
        // For server-side or forced server-side lookup
        if (!level.isClientSide()) {
            if (level instanceof ServerLevel serverLevel) {
                Town town = TownManager.get(serverLevel).getTown(townId);
                if (town != null) {
                    return town.getName();
                } else if (logResolveFailure) {
                    LOGGER.warn("Could not resolve town name for {}", townId);
                }
            }
            return "Unknown Town";
        }
        
        // For client-side lookup
        if (townNameCache.containsKey(townId)) {
            String cachedName = townNameCache.get(townId);
            if (cachedName != null && !cachedName.isEmpty()) {
                return cachedName;
            }
        }
        
        // Fallback for client-side with no cache
        return "Town-" + townId.toString().substring(0, 8);
    }

    // Helper method to resolve town name from UUID - simplified version for backward compatibility
    private String resolveTownName(UUID townId) {
        return resolveTownName(townId, false);
    }

    // Helper method to get town name from client cache or resolve from server
    public String getTownNameFromId(UUID townId) {
        if (townId == null) return "Unknown";
        
        return resolveTownName(townId, level != null && !level.isClientSide());
    }

    /**
     * Client-side only: Updates the cached town name for immediate UI feedback
     * This is only meant to be used on the client side for visual updates
     * @param newName The new town name to display
     */
    public void setClientTownName(String newName) {
        if (level != null && level.isClientSide) {
            this.name = newName;
        }
    }

    /**
     * Try to spawn a tourist on the specified platform
     */
    private void spawnTouristOnPlatform(Level level, Town town, Platform platform) {
        BlockPos startPos = platform.getStartPos();
        BlockPos endPos = platform.getEndPos();
        
        if (startPos == null || endPos == null) return;
        
        // Count existing tourists in the platform area
        AABB pathBounds = new AABB(
            Math.min(startPos.getX(), endPos.getX()) - 1,
            startPos.getY(),
            Math.min(startPos.getZ(), endPos.getZ()) - 1,
            Math.max(startPos.getX(), endPos.getX()) + 1,
            startPos.getY() + 2,
            Math.max(startPos.getZ(), endPos.getZ()) + 1
        );
        
        List<Villager> existingTourists = level.getEntitiesOfClass(Villager.class, pathBounds);
        
        if (existingTourists.size() < MAX_TOURISTS) {
            // Try up to 3 times to find a valid spawn location
            for (int attempt = 0; attempt < 3; attempt++) {
                // Calculate a random position along the path
                double progress = random.nextDouble();
                double exactX = startPos.getX() + (endPos.getX() - startPos.getX()) * progress;
                double exactZ = startPos.getZ() + (endPos.getZ() - startPos.getZ()) * progress;
                int x = (int) Math.round(exactX);
                int z = (int) Math.round(exactZ);
                int y = startPos.getY() + 1;
                
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
                        LOGGER.info("[BusinessCraft] Spawning tourist for town {} with ID {}", town.getName(), townId);
                        villager.addTag("type_tourist");
                        villager.addTag("from_town_" + townId.toString());
                        villager.addTag("from_name_" + town.getName());
                        villager.addTag("pos_" + getBlockPos().getX() + "_" + 
                                        getBlockPos().getY() + "_" + 
                                        getBlockPos().getZ());
                        
                        // Add platform ID tag
                        villager.addTag("platform_" + platform.getId().toString());
                        
                        // Add debug to verify tags were added
                        LOGGER.info("[BusinessCraft] Tourist tags after spawning: {}", villager.getTags());
                        
                        level.addFreshEntity(villager);
                        town.removeTourist();
                setChanged();
            }
                    break; // Successfully spawned, exit the loop
                }
            }
        }
    }
    
    // Platform management methods
    
    /**
     * Gets the list of all platforms
     */
    public List<Platform> getPlatforms() {
        // If on client side, return the client cache
        if (level != null && level.isClientSide()) {
            return new ArrayList<>(clientPlatforms);
        }
        // Otherwise return the actual platforms
        return new ArrayList<>(platforms);
    }
    
    /**
     * Adds a new platform
     * @return true if added successfully, false if at max capacity
     */
    public boolean addPlatform() {
        if (platforms.size() >= MAX_PLATFORMS) {
            return false;
        }
        
        Platform platform = new Platform();
        platform.setName("Platform " + (platforms.size() + 1));
        platforms.add(platform);
        setChanged();
        return true;
    }
    
    /**
     * Removes a platform by ID
     */
    public boolean removePlatform(UUID platformId) {
        boolean removed = platforms.removeIf(p -> p.getId().equals(platformId));
        if (removed) {
            setChanged();
        }
        return removed;
    }
    
    /**
     * Gets a platform by ID
     */
    public Platform getPlatform(UUID platformId) {
        return platforms.stream()
            .filter(p -> p.getId().equals(platformId))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Sets the path start for a specific platform
     */
    public void setPlatformPathStart(UUID platformId, BlockPos pos) {
        Platform platform = getPlatform(platformId);
        if (platform != null) {
            platform.setStartPos(pos);
            setChanged();
        }
    }
    
    /**
     * Sets the path end for a specific platform
     */
    public void setPlatformPathEnd(UUID platformId, BlockPos pos) {
        Platform platform = getPlatform(platformId);
        if (platform != null) {
            platform.setEndPos(pos);
            setChanged();
        }
    }
    
    /**
     * Toggles a platform's enabled state
     */
    public void togglePlatformEnabled(UUID platformId) {
        Platform platform = getPlatform(platformId);
        if (platform != null) {
            platform.setEnabled(!platform.isEnabled());
            setChanged();
        }
    }
    
    /**
     * Sets whether we're in platform path creation mode
     */
    public void setPlatformCreationMode(boolean mode, UUID platformId) {
        this.isInPlatformCreationMode = mode;
        this.platformBeingEdited = mode ? platformId : null;
    }
    
    /**
     * Gets whether we're in platform path creation mode
     */
    public boolean isInPlatformCreationMode() {
        return isInPlatformCreationMode;
    }
    
    /**
     * Gets the ID of the platform currently being edited
     */
    public UUID getPlatformBeingEdited() {
        return platformBeingEdited;
    }
    
    /**
     * Checks if we can add more platforms
     */
    public boolean canAddMorePlatforms() {
        return platforms.size() < MAX_PLATFORMS;
    }
}