package com.yourdomain.businesscraft.town;

import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import com.yourdomain.businesscraft.config.ConfigLoader;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yourdomain.businesscraft.town.components.TownEconomyComponent;
import com.yourdomain.businesscraft.api.ITownDataProvider;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.stream.Collectors;

public class Town implements ITownDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(Town.class);
    private final UUID id;
    private final BlockPos position;
    private String name;
    private final TownEconomyComponent economy = new TownEconomyComponent();
    private final Map<UUID, Integer> visitors = new HashMap<>();
    private int touristCount = 0; // Track tourists separately from population
    private boolean touristSpawningEnabled;
    private boolean cachedResult;
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private int searchRadius = 10;
    
    // Counter for tourists received since last population increase
    private int touristsReceivedCounter = 0;
    
    // Visit history storage - moved from TownBlockEntity
    private final List<VisitHistoryRecord> visitHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 50; // Maximum history entries to keep
    
    // Communal storage - accessible to all players
    private final Map<Item, Integer> communalStorage = new HashMap<>();
    
    // Personal storage - individual storage for each player (UUID -> items)
    private final Map<UUID, Map<Item, Integer>> personalStorage = new HashMap<>();
    
    public Town(UUID id, BlockPos pos, String name) {
        this.id = id;
        this.position = pos;
        this.name = name;
        this.touristSpawningEnabled = true;
        
        // Initialize with default starting population
        economy.setPopulation(ConfigLoader.defaultStartingPopulation);
    }
    
    public void addBread(int count) {
        economy.addBread(count);
    }
    
    @Override
    public void addResource(Item item, int count) {
        economy.addResource(item, count);
    }
    
    @Override
    public int getResourceCount(Item item) {
        return economy.getResourceCount(item);
    }
    
    @Override
    public Map<Item, Integer> getAllResources() {
        return economy.getResources().getAllResources();
    }
    
    public boolean canSpawnTourists() {
        boolean result = touristSpawningEnabled && economy.getPopulation() >= ConfigLoader.minPopForTourists;
        if (result != cachedResult) {
            LOGGER.info("SPAWN STATE CHANGE [{}] - Enabled: {}, Population: {}/{}, Tourists: {}, Result: {}",
                id, touristSpawningEnabled, economy.getPopulation(), ConfigLoader.minPopForTourists, touristCount, result);
            cachedResult = result;
        }
        return result;
    }
    
    /**
     * Checks if the town can support additional tourists based on current count and max limits
     * 
     * @return true if more tourists can be spawned, false otherwise
     */
    public boolean canAddMoreTourists() {
        // First check if tourist spawning is enabled at all
        if (!canSpawnTourists()) {
            return false;
        }
        
        // Check if we're already at the fixed maximum
        if (touristCount >= ConfigLoader.maxTouristsPerTown) {
            return false;
        }
        
        // Check population-based limit
        int populationBasedLimit = calculateMaxTouristsFromPopulation();
        return touristCount < populationBasedLimit;
    }
    
    /**
     * Calculate the maximum number of tourists this town can support based on population
     * 
     * @return the maximum number of tourists allowed
     */
    public int calculateMaxTouristsFromPopulation() {
        // Calculate based on population / populationPerTourist ratio
        int popBasedLimit = economy.getPopulation() / ConfigLoader.populationPerTourist;
        
        // Cap at the configured maximum
        return Math.min(popBasedLimit, ConfigLoader.maxPopBasedTourists);
    }
    
    /**
     * Get the maximum number of tourists this town can currently support
     * 
     * @return the maximum tourist capacity
     */
    public int getMaxTourists() {
        return Math.min(calculateMaxTouristsFromPopulation(), ConfigLoader.maxTouristsPerTown);
    }
    
    public void addTourist() {
        // Only add tourist if we haven't reached the limit
        if (canAddMoreTourists()) {
            touristCount++;
            LOGGER.debug("Tourist added to town {}, count now {}/{}", 
                name, touristCount, getMaxTourists());
        } else {
            LOGGER.debug("Cannot add tourist to town {}, at capacity {}/{}", 
                name, touristCount, getMaxTourists());
        }
    }
    
    public void removeTourist() {
        if (touristCount > 0) {
            touristCount--;
            LOGGER.debug("Tourist removed from town {}, count now {}/{}", 
                name, touristCount, getMaxTourists());
        }
    }
    
    public int getTouristCount() {
        return touristCount;
    }
    
    public void save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("posX", position.getX());
        tag.putInt("posY", position.getY());
        tag.putInt("posZ", position.getZ());
        tag.putInt("touristCount", touristCount);
        tag.putInt("touristsReceivedCounter", touristsReceivedCounter);
        CompoundTag visitorsTag = new CompoundTag();
        visitors.forEach((visitorId, count) -> {
            visitorsTag.putInt(visitorId.toString(), count);
        });
        tag.put("visitors", visitorsTag);
        CompoundTag economyTag = new CompoundTag();
        economy.save(economyTag);
        tag.put("economy", economyTag);
        
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
        
        tag.putInt("searchRadius", searchRadius);
        tag.putBoolean("touristSpawningEnabled", touristSpawningEnabled);
        
        // Save visit history
        if (!visitHistory.isEmpty()) {
            ListTag historyTag = new ListTag();
            for (VisitHistoryRecord record : visitHistory) {
                CompoundTag visitTag = new CompoundTag();
                visitTag.putLong("timestamp", record.getTimestamp());
                
                // Store UUID instead of name
                if (record.getOriginTownId() != null) {
                    visitTag.putUUID("townId", record.getOriginTownId());
                }
                
                visitTag.putInt("count", record.getCount());
                
                // Save origin position
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
        
        // Save communal storage
        if (!communalStorage.isEmpty()) {
            CompoundTag storageTag = new CompoundTag();
            communalStorage.forEach((item, count) -> {
                String itemKey = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString();
                storageTag.putInt(itemKey, count);
            });
            tag.put("communalStorage", storageTag);
        }
        
        // Save personal storage
        if (!personalStorage.isEmpty()) {
            CompoundTag personalTag = new CompoundTag();
            
            personalStorage.forEach((playerId, itemMap) -> {
                // Skip empty player inventories
                if (itemMap.isEmpty()) return;
                
                // Create tag for this player's items
                CompoundTag playerTag = new CompoundTag();
                
                // Save each item
                itemMap.forEach((item, count) -> {
                    String itemKey = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(item).toString();
                    playerTag.putInt(itemKey, count);
                });
                
                // Add to main personal storage tag with player UUID as key
                personalTag.put(playerId.toString(), playerTag);
            });
            
            tag.put("personalStorage", personalTag);
        }
    }
    
    public static Town load(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        String name = tag.getString("name");
        BlockPos pos = new BlockPos(
            tag.getInt("posX"),
            tag.getInt("posY"),
            tag.getInt("posZ")
        );
        
        Town town = new Town(id, pos, name);
        
        // Load tourist count
        if (tag.contains("touristCount")) {
            town.touristCount = tag.getInt("touristCount");
        }
        
        // Load tourists received counter
        if (tag.contains("touristsReceivedCounter")) {
            town.touristsReceivedCounter = tag.getInt("touristsReceivedCounter");
        }
        
        if (tag.contains("visitors")) {
            CompoundTag visitorsTag = tag.getCompound("visitors");
            visitorsTag.getAllKeys().forEach(key -> {
                town.visitors.put(UUID.fromString(key), visitorsTag.getInt(key));
            });
        }
        town.economy.load(tag.getCompound("economy"));
        
        if (tag.contains("PathStart")) {
            CompoundTag startPos = tag.getCompound("PathStart");
            town.pathStart = new BlockPos(
                startPos.getInt("x"),
                startPos.getInt("y"),
                startPos.getInt("z")
            );
        }
        
        if (tag.contains("PathEnd")) {
            CompoundTag endPos = tag.getCompound("PathEnd");
            town.pathEnd = new BlockPos(
                endPos.getInt("x"),
                endPos.getInt("y"),
                endPos.getInt("z")
            );
        }
        
        town.searchRadius = tag.contains("searchRadius") ? 
            tag.getInt("searchRadius") : 10;
        
        town.touristSpawningEnabled = !tag.contains("touristSpawningEnabled") || 
            tag.getBoolean("touristSpawningEnabled");
        
        // Load visit history
        if (tag.contains("visitHistory")) {
            ListTag historyTag = tag.getList("visitHistory", Tag.TAG_COMPOUND);
            
            for (int i = 0; i < historyTag.size(); i++) {
                CompoundTag visitTag = historyTag.getCompound(i);
                
                long timestamp = visitTag.getLong("timestamp");
                
                // Handle both legacy (name-based) and new (UUID-based) records
                UUID townId = null;
                if (visitTag.contains("townId")) {
                    townId = visitTag.getUUID("townId");
                } else if (visitTag.contains("town")) {
                    // Legacy record with only a town name - create a random UUID
                    // This is just a fallback for migration
                    townId = UUID.nameUUIDFromBytes(visitTag.getString("town").getBytes());
                    LOGGER.info("Converted legacy town name '{}' to UUID: {}", 
                        visitTag.getString("town"), townId);
                }
                
                int count = visitTag.getInt("count");
                
                BlockPos originPos = BlockPos.ZERO;
                if (visitTag.contains("pos")) {
                    CompoundTag posTag = visitTag.getCompound("pos");
                    originPos = new BlockPos(
                        posTag.getInt("x"),
                        posTag.getInt("y"),
                        posTag.getInt("z")
                    );
                }
                
                if (townId != null) {
                    town.visitHistory.add(new VisitHistoryRecord(timestamp, townId, count, originPos));
                }
            }
        }
        
        // Load communal storage
        if (tag.contains("communalStorage")) {
            CompoundTag storageTag = tag.getCompound("communalStorage");
            storageTag.getAllKeys().forEach(key -> {
                try {
                    net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(key);
                    Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                    if (item != null) {
                        int count = storageTag.getInt(key);
                        if (count > 0) {
                            town.communalStorage.put(item, count);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading communal storage item: {}", key, e);
                }
            });
        }
        
        // Load personal storage
        if (tag.contains("personalStorage")) {
            CompoundTag personalTag = tag.getCompound("personalStorage");
            
            // Iterate through each player's data
            personalTag.getAllKeys().forEach(playerKey -> {
                try {
                    // Parse player UUID
                    UUID playerId = UUID.fromString(playerKey);
                    
                    // Get player's items
                    CompoundTag playerTag = personalTag.getCompound(playerKey);
                    Map<Item, Integer> playerItems = new HashMap<>();
                    
                    // Load each item
                    playerTag.getAllKeys().forEach(itemKey -> {
                        try {
                            net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(itemKey);
                            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(itemId);
                            if (item != null) {
                                int count = playerTag.getInt(itemKey);
                                if (count > 0) {
                                    playerItems.put(item, count);
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error loading personal storage item for player {}: {}", playerKey, itemKey, e);
                        }
                    });
                    
                    // Add to town's personal storage if not empty
                    if (!playerItems.isEmpty()) {
                        town.personalStorage.put(playerId, playerItems);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error parsing player UUID for personal storage: {}", playerKey, e);
                }
            });
        }
        
        return town;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Sets a new name for this town
     * @param newName The new name for the town
     */
    public void setName(String newName) {
        this.name = newName;
    }
    
    public int getBreadCount() {
        return economy.getBreadCount();
    }
    
    public int getPopulation() {
        return economy.getPopulation();
    }
    
    public UUID getId() {
        return id;
    }
    
    public BlockPos getPosition() {
        return position;
    }
    
    public void setTouristSpawningEnabled(boolean enabled) {
        LOGGER.info("TOGGLE [{}] - Changing from {} to {}", 
            id, touristSpawningEnabled, enabled);
        this.touristSpawningEnabled = enabled;
    }
    
    public void addVisitor(UUID fromTownId) {
        visitors.merge(fromTownId, 1, Integer::sum);
        
        // Increment counter for tourists received
        touristsReceivedCounter++;
        
        // Check if population growth from tourism is enabled and if we should increase population
        if (ConfigLoader.touristsPerPopulationIncrease > 0 && 
            touristsReceivedCounter >= ConfigLoader.touristsPerPopulationIncrease) {
            // Increase population by 1
            economy.setPopulation(economy.getPopulation() + 1);
            
            // Reset counter, subtracting any excess tourists
            touristsReceivedCounter -= ConfigLoader.touristsPerPopulationIncrease;
            
            LOGGER.info("Town [{}] population increased to {} after receiving {} tourists", 
                name, economy.getPopulation(), ConfigLoader.touristsPerPopulationIncrease);
        }
    }
    
    public int getTotalVisitors() {
        return visitors.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public boolean isTouristSpawningEnabled() {
        return touristSpawningEnabled;
    }
    
    public void setPathStart(BlockPos pathStart) {
        this.pathStart = pathStart;
    }
    
    public void setPathEnd(BlockPos pathEnd) {
        this.pathEnd = pathEnd;
    }
    
    public BlockPos getPathStart() {
        return pathStart;
    }
    
    public BlockPos getPathEnd() {
        return pathEnd;
    }
    
    public int getSearchRadius() {
        return searchRadius;
    }
    
    public void setSearchRadius(int searchRadius) {
        this.searchRadius = searchRadius;
    }
    
    @Override
    public UUID getTownId() {
        return id;
    }
    
    @Override
    public void markDirty() {
        // Find the TownManager for all loaded levels and mark the town data as dirty
        net.minecraft.server.MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            boolean foundInAnyLevel = false;
            for (net.minecraft.world.level.Level level : server.getAllLevels()) {
                if (level instanceof net.minecraft.server.level.ServerLevel) {
                    net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) level;
                    TownManager manager = TownManager.get(serverLevel);
                    if (manager.getTown(id) == this) {
                        manager.markDirty();
                        foundInAnyLevel = true;
                        LOGGER.debug("Successfully marked town '{}' (id: {}) as dirty in level {}", 
                            this.name, this.id, serverLevel.dimension().location());
                    }
                }
            }
            
            if (!foundInAnyLevel) {
                LOGGER.warn("Failed to mark town '{}' (id: {}) as dirty - not found in any loaded level", 
                    this.name, this.id);
            }
        } else {
            LOGGER.warn("Failed to mark town '{}' (id: {}) as dirty - server is null", 
                this.name, this.id);
        }
    }
    
    @Override
    public String getTownName() {
        return getName();
    }
    
    // Visit history implementation
    @Override
    public void recordVisit(UUID originTownId, int count, BlockPos originPos) {
        long timestamp = System.currentTimeMillis();
        
        // Create the visit record
        VisitHistoryRecord record = new VisitHistoryRecord(timestamp, originTownId, count, originPos);
        
        // Add to the beginning of the list (newest first)
        visitHistory.add(0, record);
        
        // Trim if we exceed the maximum history size
        while (visitHistory.size() > MAX_HISTORY_SIZE) {
            visitHistory.remove(visitHistory.size() - 1);
        }
        
        // Mark as dirty to ensure it's saved
        markDirty();
    }
    
    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        return Collections.unmodifiableList(visitHistory);
    }
    
    /**
     * Add a resource to the town's communal storage
     * 
     * @param item The item to add
     * @param count The amount to add (can be negative to remove)
     * @return true if successful, false if there aren't enough items to remove
     */
    public boolean addToCommunalStorage(Item item, int count) {
        if (count == 0) return true;
        
        LOGGER.info("STORAGE UPDATE - Town '{}' - Attempting to add {} {} to communal storage", 
            this.name, count, item.getDescription().getString());
        
        // Get current amount
        int currentAmount = communalStorage.getOrDefault(item, 0);
        int newAmount = currentAmount + count;
        
        // Check if removing more than available
        if (newAmount < 0) {
            LOGGER.warn("Attempted to remove {} {} from communal storage but only {} available", 
                Math.abs(count), item.getDescription().getString(), currentAmount);
            return false;
        }
        
        // Update storage
        if (newAmount > 0) {
            communalStorage.put(item, newAmount);
            LOGGER.info("STORAGE UPDATE - Town '{}' - Updated communal storage: {} {} (now {})", 
                this.name,
                count > 0 ? "Added" : "Removed", 
                Math.abs(count) + " " + item.getDescription().getString(),
                newAmount);
                
            // Print current storage contents
            LOGGER.info("STORAGE UPDATE - Town '{}' - Current communal storage contents: {}", 
                this.name,
                communalStorage.entrySet().stream()
                    .map(e -> e.getKey().getDescription().getString() + ": " + e.getValue())
                    .collect(Collectors.joining(", ")));
        } else {
            // Remove the entry if amount is zero
            communalStorage.remove(item);
            LOGGER.info("STORAGE UPDATE - Town '{}' - Removed {} from communal storage (empty)", 
                this.name, item.getDescription().getString());
        }
        
        // Mark town as dirty to save changes
        markDirty();
        return true;
    }
    
    /**
     * Get the count of a specific item in the communal storage
     * 
     * @param item The item to check
     * @return The amount stored
     */
    public int getCommunalStorageCount(Item item) {
        return communalStorage.getOrDefault(item, 0);
    }
    
    /**
     * Get all items in the communal storage
     * 
     * @return Map of all items and their counts
     */
    public Map<Item, Integer> getAllCommunalStorageItems() {
        return Collections.unmodifiableMap(communalStorage);
    }
    
    /**
     * Add a resource to a player's personal storage
     * 
     * @param playerId The UUID of the player
     * @param item The item to add
     * @param count The amount to add (can be negative to remove)
     * @return true if successful, false if there aren't enough items to remove
     */
    public boolean addToPersonalStorage(UUID playerId, Item item, int count) {
        if (count == 0 || playerId == null) return true;
        
        // Get or create the player's storage map
        Map<Item, Integer> playerStorage = personalStorage.computeIfAbsent(playerId, k -> new HashMap<>());
        
        // Get current amount
        int currentAmount = playerStorage.getOrDefault(item, 0);
        int newAmount = currentAmount + count;
        
        // Check if removing more than available
        if (newAmount < 0) {
            LOGGER.warn("Attempted to remove {} {} from personal storage of player {} but only {} available", 
                Math.abs(count), item.getDescription().getString(), playerId, currentAmount);
            return false;
        }
        
        // Update storage
        if (newAmount > 0) {
            playerStorage.put(item, newAmount);
            LOGGER.debug("Updated personal storage for player {}: {} {} (now {})", 
                playerId,
                count > 0 ? "Added" : "Removed", 
                Math.abs(count) + " " + item.getDescription().getString(),
                newAmount);
        } else {
            // Remove the entry if amount is zero
            playerStorage.remove(item);
            LOGGER.debug("Removed {} from personal storage of player {} (empty)", item.getDescription().getString(), playerId);
        }
        
        // Mark town as dirty to save changes
        markDirty();
        return true;
    }
    
    /**
     * Get the count of a specific item in a player's personal storage
     * 
     * @param playerId The UUID of the player
     * @param item The item to check
     * @return The amount stored
     */
    public int getPersonalStorageCount(UUID playerId, Item item) {
        if (playerId == null) return 0;
        
        Map<Item, Integer> playerStorage = personalStorage.get(playerId);
        if (playerStorage == null) return 0;
        
        return playerStorage.getOrDefault(item, 0);
    }
    
    /**
     * Get all items in a player's personal storage
     * 
     * @param playerId The UUID of the player
     * @return Map of all items and their counts
     */
    public Map<Item, Integer> getPersonalStorageItems(UUID playerId) {
        if (playerId == null) return Collections.emptyMap();
        
        Map<Item, Integer> playerStorage = personalStorage.get(playerId);
        if (playerStorage == null) return Collections.emptyMap();
        
        return Collections.unmodifiableMap(playerStorage);
    }
} 