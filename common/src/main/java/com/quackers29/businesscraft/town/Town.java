package com.quackers29.businesscraft.town;

import net.minecraft.core.BlockPos;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import com.quackers29.businesscraft.config.ConfigLoader;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.components.TownEconomyComponent;
import com.quackers29.businesscraft.town.components.TownTradingComponent;
import com.quackers29.businesscraft.town.components.TownProductionComponent;
import com.quackers29.businesscraft.town.components.TownUpgradeComponent;
import com.quackers29.businesscraft.town.components.TownContractComponent;
import com.quackers29.businesscraft.api.ITownDataProvider;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.town.data.TownPaymentBoard;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import java.util.stream.Collectors;

public class Town implements ITownDataProvider, com.quackers29.businesscraft.town.ai.ITownState {
    private static final Logger LOGGER = LoggerFactory.getLogger(Town.class);
    private final UUID id;
    private final BlockPos position;
    private String name;
    private final TownEconomyComponent economy = new TownEconomyComponent();
    private final Map<UUID, Long> visitors = new HashMap<>();
    private int touristCount = 0; // Track tourists separately from population
    private String biome = "Unknown";
    private String biomeVariant = "Unknown";
    private boolean touristSpawningEnabled;
    private BlockPos pathStart;
    private BlockPos pathEnd;
    private int searchRadius = 10;
    // Pending tourist spawns buffer (accumulated from production)
    private int pendingTouristSpawns = 0;

    // Work Units (WU) - Special resource
    private long workUnits = 0;

    @Override
    public long getWorkUnits() {
        return workUnits;
    }

    public void setWorkUnits(long amount) {
        this.workUnits = Math.max(0, amount);
        markDirty();
    }

    /**
     * Adds work units to the town. Can be negative to remove.
     * Clamps to 0 and Cap.
     */
    public void addWorkUnits(long amount) {
        long cap = getWorkUnitCap();
        try {
            this.workUnits = Math.addExact(this.workUnits, amount);
        } catch (ArithmeticException e) {
            this.workUnits = amount > 0 ? Long.MAX_VALUE : 0;
        }

        if (this.workUnits < 0)
            this.workUnits = 0;
        if (cap > 0 && this.workUnits > cap)
            this.workUnits = cap;

        markDirty();
    }

    @Override
    public long getWorkUnitCap() {
        // Cap determined by upgrades
        return (long) upgrades.getModifier("wu_cap");
    }

    // Cumulative tourism stats
    private long totalTouristsArrived = 0;
    private double totalTouristDistance = 0.0;

    // Visit history storage - moved from TownBlockEntity
    private final List<VisitHistoryRecord> visitHistory = new ArrayList<>();
    private static final int MAX_HISTORY_SIZE = 50; // Maximum history entries to keep

    // Payment board system - replaces communal storage
    private final TownPaymentBoard paymentBoard = new TownPaymentBoard();

    // Personal storage - individual storage for each player (UUID -> items)
    private final Map<UUID, Map<Item, Long>> personalStorage = new HashMap<>();

    // ITownState specific fields
    private String biomeNamespace = "minecraft:plains"; // Default biome for ITownState

    // Escrow storage - resources locked in auctions
    private final Map<Item, Long> escrowedResources = new HashMap<>();

    // Components
    private final TownTradingComponent trading;
    private final TownProductionComponent production;
    private final TownUpgradeComponent upgrades;
    private final TownContractComponent contracts;

    public Town(UUID id, BlockPos pos, String name) {
        this.id = id;
        this.position = pos;
        this.name = name;
        this.touristSpawningEnabled = true;

        this.trading = new TownTradingComponent(this);
        this.production = new TownProductionComponent(this);
        this.upgrades = new TownUpgradeComponent(this);
        this.contracts = new TownContractComponent(this);

        // Initialize with default starting population
        economy.setPopulation(ConfigLoader.defaultStartingPopulation);
    }

    // Stats
    private float happiness = 50.0f; // 0-100
    private float happinessModifier = 0f; // Calculated from upgrades

    public TownUpgradeComponent getUpgrades() {
        return upgrades;
    }

    public float getHappiness() {
        float modifier = upgrades.getModifier("happiness");
        return Math.max(0.0f, Math.min(100.0f, this.happiness + modifier));
    }

    public void setHappiness(float happiness) {
        this.happiness = happiness;
    }

    public void adjustHappiness(float delta) {
        this.happiness = Math.max(0.0f, Math.min(100.0f, this.happiness + delta));
    }

    public TownContractComponent getContracts() {
        return contracts;
    }

    private final Map<Item, Long> wantedResources = new HashMap<>();
    private int wantCalculationCooldown = 0;

    public void tick() {
        economy.tick();
        if (ConfigLoader.tradingEnabled) {
            trading.tick();
        }
        if (ConfigLoader.productionEnabled) {
            production.tick();
        }
        contracts.tick(); // Always tick contracts
        upgrades.tick(); // Always tick upgrades

        if (wantCalculationCooldown-- <= 0) {
            wantCalculationCooldown = 20; // Check every 1 second
            calculateWants();
        }
    }

    public void calculateWants() {
        wantedResources.clear();
        String bestNodeId = com.quackers29.businesscraft.town.ai.TownResearchAI.getBestUpgradeTarget(this);

        // If we are already researching the best target, we don't "want" resources for
        // it (we already paid)
        if (bestNodeId != null && bestNodeId.equals(upgrades.getCurrentResearchNode())) {
            bestNodeId = null;
        }

        if (bestNodeId != null) {
            List<com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount> costs = upgrades
                    .getUpgradeCost(bestNodeId);
            for (com.quackers29.businesscraft.data.parsers.DataParser.ResourceAmount ra : costs) {
                // Skip non-consumable stats like tourism
                if (ra.resourceId.startsWith("tourism_"))
                    continue;

                com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                        .get(ra.resourceId);
                if (type == null)
                    continue;

                net.minecraft.resources.ResourceLocation itemLoc = type.getCanonicalItemId();
                Object itemObj = PlatformAccess.getRegistry().getItem(itemLoc);
                if (itemObj instanceof Item item) {
                    float stored = trading.getStock(ra.resourceId);
                    long needed = (long) Math.ceil(ra.amount);
                    if (stored < needed) {
                        // Store as negative value representing the deficit (Want: -100 means we need
                        // 100 more)
                        long deficit = (long) (stored - needed);
                        wantedResources.put(item, deficit);
                    }
                }
            }
        }
    }

    public Map<Item, Long> getWantedResources() {
        return Collections.unmodifiableMap(wantedResources);
    }

    @Override
    public void addResource(Item item, long count) {
        economy.addResource(item, count);
    }

    @Override
    public long getResourceCount(Item item) {
        return economy.getResourceCount(item);
    }

    @Override
    public Map<Item, Long> getAllResources() {
        return economy.getResources().getAllResources();
    }

    public Map<Item, Long> getEscrowedResources() {
        return Collections.unmodifiableMap(escrowedResources);
    }

    public void addEscrowResource(Item item, long count) {
        if (count == 0)
            return;

        long current = escrowedResources.getOrDefault(item, 0L);

        if (count > 0) {
            try {
                long result = Math.addExact(current, count);
                escrowedResources.put(item, result);
            } catch (ArithmeticException e) {
                escrowedResources.put(item, Long.MAX_VALUE);
            }
        } else {
            // Subtraction / removing from escrow
            long newAmount = Math.max(0, current + count);
            if (newAmount == 0) {
                escrowedResources.remove(item);
            } else {
                escrowedResources.put(item, newAmount);
            }
        }
        markDirty();
    }

    public void removeEscrowResource(Item item, long count) {
        addEscrowResource(item, -count);
    }

    public long getEscrowResourceCount(Item item) {
        return escrowedResources.getOrDefault(item, 0L);
    }

    public long getTotalResourceCount(Item item) {
        return getResourceCount(item) + getEscrowResourceCount(item);
    }

    public long getInTransitResourceCount(Item item) {
        com.quackers29.businesscraft.economy.ResourceType type = com.quackers29.businesscraft.economy.ResourceRegistry
                .getFor(item);
        if (type == null)
            return 0;
        return getContracts().getInTransitResourceCount(type.getId());
    }

    // ================================
    // Town Boundary System Methods
    // ================================

    /**
     * Gets the boundary radius for this town based on population (1:1 ratio)
     * 
     * @return The boundary radius in blocks
     */
    public int getBoundaryRadius() {
        // Use "border" modifier from upgrades/starting stats
        float borderMod = upgrades.getModifier("border");

        // Fallback for legacy towns that might not have the modifier yet
        if (borderMod <= 0) {
            // Fallback to population-based (legacy behavior) or a safe default
            // The user requested removing the tie, but for safety in existing worlds
            // without migration logic,
            // we might want to default to 50 if population is small, or keep population as
            // fallback?
            // Since we are decoupling, let's just make sure it's at least 50.
            // But if we return 50, old towns (pop 5) which expect 5 will suddenly jump to
            // 50.
            // That's acceptable as per "starting 'border:50'".
            borderMod = 50;
        }

        int radius = (int) borderMod;

        // Debugging rarely needed here unless issues arise
        // DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Town {} boundary
        // radius: {}", name, radius);
        return radius;
    }

    @Override
    public boolean isUnlocked(String nodeId) {
        return upgrades.getUnlockedNodes().contains(nodeId);
    }

    @Override
    public int getUpgradeLevel(String nodeId) {
        return upgrades.getUpgradeLevel(nodeId);
    }

    /**
     * Checks if a position is within the town's boundary
     * 
     * @param pos The position to check
     * @return true if the position is inside the town's radius
     */
    public boolean isPositionInside(BlockPos pos) {
        if (pos == null)
            return false;

        // Simple distance check (cylindrical or spherical? typically cylindrical for
        // towns)
        // Using cylindrical distance (ignoring Y) for now as it's more standard for
        // town claims,
        // but spherical (distSqr) is easier and matches `wouldOverlapWith`.
        // Let's match `wouldOverlapWith` logic which uses 3D distance by default
        // distSqr on BlockPos.

        // Actually, for player detection, 2D (horizontal) radius is often better so
        // players
        // strictly above/below are still included, but let's stick to simple 3D
        // distance
        // first as it's safe.
        // WAIT: `wouldOverlapWith` uses `this.position.distSqr(otherTown.position)`.

        double distanceSqr = this.position.distSqr(pos);
        int radius = getBoundaryRadius();

        return distanceSqr <= (radius * radius);
    }

    /**
     * Checks if this town's boundary would overlap with another town's boundary
     * 
     * @param otherTown The other town to check against
     * @return true if boundaries would overlap, false otherwise
     */
    public boolean wouldOverlapWith(Town otherTown) {
        if (otherTown == null)
            return false;

        double distance = Math.sqrt(this.position.distSqr(otherTown.position));
        double requiredDistance = this.getBoundaryRadius() + otherTown.getBoundaryRadius();

        boolean wouldOverlap = distance < requiredDistance;
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Boundary overlap check: {} vs {} - distance: {}, required: {}, overlap: {}",
                this.name, otherTown.name, distance, requiredDistance, wouldOverlap);

        return wouldOverlap;
    }

    /**
     * Calculates the minimum distance required between this town and another
     * 
     * @param otherTown The other town
     * @return The minimum required distance in blocks
     */
    public double getMinimumDistanceRequired(Town otherTown) {
        if (otherTown == null)
            return this.getBoundaryRadius();

        double minDistance = this.getBoundaryRadius() + otherTown.getBoundaryRadius();
        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "Minimum distance between {} and {}: {} ({}+{})",
                this.name, otherTown.name, minDistance, this.getBoundaryRadius(), otherTown.getBoundaryRadius());

        return minDistance;
    }

    /**
     * @deprecated Use TownService.canSpawnTourists() instead
     */
    @Deprecated
    public boolean canSpawnTourists() {
        // Delegate to service layer for business logic
        com.quackers29.businesscraft.town.service.TownService service = new com.quackers29.businesscraft.town.service.TownService(
                new com.quackers29.businesscraft.town.service.TownValidationService());

        return service.canSpawnTourists(this).getOrElse(false);
    }

    /**
     * Checks if the town can support additional tourists based on current count and
     * max limits
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
     * @deprecated Use TownService.calculateMaxTourists() instead
     */
    @Deprecated
    public int calculateMaxTouristsFromPopulation() {
        // Delegate to service layer
        com.quackers29.businesscraft.town.service.TownService service = new com.quackers29.businesscraft.town.service.TownService(
                new com.quackers29.businesscraft.town.service.TownValidationService());

        return service.calculateMaxTourists(this).getOrElse(0);
    }

    /**
     * @deprecated Use TownService.calculateMaxTourists() instead
     */
    @Deprecated
    public long getMaxTourists() {
        return calculateMaxTouristsFromPopulation();
    }

    /**
     * @deprecated Use TownService.addTourist() instead
     */
    @Deprecated
    public void addTourist() {
        // Delegate to service layer for business logic
        com.quackers29.businesscraft.town.service.TownService service = new com.quackers29.businesscraft.town.service.TownService(
                new com.quackers29.businesscraft.town.service.TownValidationService());

        com.quackers29.businesscraft.util.Result<Void, com.quackers29.businesscraft.util.BCError.TownError> result = service
                .addTourist(this);

        if (result.isFailure()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Cannot add tourist to town {}: {}", name,
                    result.getError().getMessage());
        }
    }

    /**
     * @deprecated Use TownService.removeTourist() instead
     */
    @Deprecated
    public void removeTourist() {
        // Delegate to service layer for business logic
        com.quackers29.businesscraft.town.service.TownService service = new com.quackers29.businesscraft.town.service.TownService(
                new com.quackers29.businesscraft.town.service.TownValidationService());

        com.quackers29.businesscraft.util.Result<Void, com.quackers29.businesscraft.util.BCError.TownError> result = service
                .removeTourist(this);

        if (result.isFailure()) {
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Cannot remove tourist from town {}: {}", name,
                    result.getError().getMessage());
        }
    }

    public long getTouristCount() {
        return touristCount;
    }

    /**
     * Sets the tourist count directly. Used by TownService.
     * 
     * @param count The new tourist count
     */
    public void setTouristCount(int count) {
        this.touristCount = count;
        markDirty();
    }

    public void save(CompoundTag tag) {
        tag.putUUID("id", id);
        tag.putString("name", name);
        tag.putInt("posX", position.getX());
        tag.putInt("posY", position.getY());
        tag.putInt("posZ", position.getZ());
        tag.putInt("touristCount", touristCount);
        tag.putInt("touristCount", touristCount);
        tag.putString("biome", biome);
        tag.putString("biomeVariant", biomeVariant);

        tag.putLong("workUnits", workUnits);

        tag.putLong("totalTouristsArrived", totalTouristsArrived);
        tag.putDouble("totalTouristDistance", totalTouristDistance);
        CompoundTag visitorsTag = new CompoundTag();
        visitors.forEach((visitorId, count) -> {
            visitorsTag.putLong(visitorId.toString(), count);
        });
        tag.put("visitors", visitorsTag);
        CompoundTag economyTag = new CompoundTag();
        economy.save(economyTag);
        tag.put("economy", economyTag);

        CompoundTag tradingTag = new CompoundTag();
        trading.save(tradingTag);
        tag.put("trading", tradingTag);

        CompoundTag productionTag = new CompoundTag();
        production.save(productionTag);
        tag.put("production", productionTag);

        CompoundTag upgradesTag = new CompoundTag();
        upgrades.save(upgradesTag);
        tag.put("upgrades", upgradesTag);

        CompoundTag contractsTag = new CompoundTag();
        contracts.save(contractsTag);
        tag.put("contracts", contractsTag);

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
        tag.putInt("pendingTouristSpawns", pendingTouristSpawns);
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

        // Save payment board (replaces communal storage)
        tag.put("paymentBoard", paymentBoard.toNBT());

        // Save personal storage
        if (!personalStorage.isEmpty()) {
            CompoundTag personalTag = new CompoundTag();

            personalStorage.forEach((playerId, itemMap) -> {
                // Skip empty player inventories
                if (itemMap.isEmpty())
                    return;

                // Create tag for this player's items
                CompoundTag playerTag = new CompoundTag();

                // Save each item
                itemMap.forEach((item, count) -> {
                    String itemKey = PlatformAccess.getRegistry().getItemKey(item).toString();
                    playerTag.putLong(itemKey, count);
                });

                // Add to main personal storage tag with player UUID as key
                personalTag.put(playerId.toString(), playerTag);
            });

            tag.put("personalStorage", personalTag);
        }

        // Save wanted resources
        if (!wantedResources.isEmpty()) {
            CompoundTag wantsTag = new CompoundTag();
            wantedResources.forEach((item, amount) -> {
                String itemKey = PlatformAccess.getRegistry().getItemKey(item).toString();
                wantsTag.putLong(itemKey, amount);
            });
            tag.put("wantedResources", wantsTag);
        }

        // Save escrowed resources
        if (!escrowedResources.isEmpty()) {
            CompoundTag escrowTag = new CompoundTag();
            escrowedResources.forEach((item, count) -> {
                String itemKey = PlatformAccess.getRegistry().getItemKey(item).toString();
                escrowTag.putLong(itemKey, count);
            });
            tag.put("escrowedResources", escrowTag);
        }
    }

    public static Town load(CompoundTag tag) {
        UUID id = tag.getUUID("id");
        String name = tag.getString("name");
        BlockPos pos = new BlockPos(
                tag.getInt("posX"),
                tag.getInt("posY"),
                tag.getInt("posZ"));

        Town town = new Town(id, pos, name);

        // Load tourist count
        if (tag.contains("touristCount")) {
            town.touristCount = tag.getInt("touristCount");
        }

        if (tag.contains("totalTouristsArrived")) {
            town.totalTouristsArrived = tag.getLong("totalTouristsArrived");
        }

        if (tag.contains("totalTouristDistance")) {
            town.totalTouristDistance = tag.getDouble("totalTouristDistance");
        }

        if (tag.contains("biome")) {
            town.biome = tag.getString("biome");
        }
        if (tag.contains("biomeVariant")) {
            town.biomeVariant = tag.getString("biomeVariant");
        }

        if (tag.contains("workUnits")) {
            long val = tag.getLong("workUnits");
            town.workUnits = Math.max(0, val); // Sanitize negative values
        }

        if (tag.contains("visitors")) {
            CompoundTag visitorsTag = tag.getCompound("visitors");
            visitorsTag.getAllKeys().forEach(key -> {
                town.visitors.put(UUID.fromString(key), visitorsTag.getLong(key));
            });
        }
        town.economy.load(tag.getCompound("economy"));
        if (tag.contains("trading")) {
            town.trading.load(tag.getCompound("trading"));
        }
        if (tag.contains("production")) {
            town.production.load(tag.getCompound("production"));
        }
        if (tag.contains("upgrades")) {
            town.upgrades.load(tag.getCompound("upgrades"));
        }
        if (tag.contains("contracts")) {
            town.contracts.load(tag.getCompound("contracts"));
        }
        if (tag.contains("happiness")) {
            town.happiness = tag.getFloat("happiness");
        }

        if (tag.contains("PathStart")) {
            CompoundTag startPos = tag.getCompound("PathStart");
            town.pathStart = new BlockPos(
                    startPos.getInt("x"),
                    startPos.getInt("y"),
                    startPos.getInt("z"));
        }

        if (tag.contains("PathEnd")) {
            CompoundTag endPos = tag.getCompound("PathEnd");
            town.pathEnd = new BlockPos(
                    endPos.getInt("x"),
                    endPos.getInt("y"),
                    endPos.getInt("z"));
        }

        town.searchRadius = tag.contains("searchRadius") ? tag.getInt("searchRadius") : 10;

        if (tag.contains("pendingTouristSpawns")) {
            town.pendingTouristSpawns = tag.getInt("pendingTouristSpawns");
        }

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
                }

                int count = visitTag.getInt("count");

                BlockPos originPos = BlockPos.ZERO;
                if (visitTag.contains("pos")) {
                    CompoundTag posTag = visitTag.getCompound("pos");
                    originPos = new BlockPos(
                            posTag.getInt("x"),
                            posTag.getInt("y"),
                            posTag.getInt("z"));
                }

                if (townId != null) {
                    town.visitHistory.add(new VisitHistoryRecord(timestamp, townId, count, originPos));
                }
            }
        }

        // Load payment board (replaces communal storage)
        if (tag.contains("paymentBoard")) {
            town.paymentBoard.fromNBT(tag.getCompound("paymentBoard"));
        } else if (tag.contains("communalStorage")) {
            // Migration: convert old communal storage to payment buffer
            CompoundTag storageTag = tag.getCompound("communalStorage");
            storageTag.getAllKeys().forEach(key -> {
                try {
                    net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(key);
                    Object itemObj = PlatformAccess.getRegistry().getItem(itemId);
                    if (itemObj instanceof net.minecraft.world.item.Item item) {
                        if (item != null) {
                            int count = storageTag.getInt(key);
                            if (count > 0) {
                                town.paymentBoard.addToBuffer(item, count);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error migrating communal storage item: {}", key, e);
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
                    Map<Item, Long> playerItems = new HashMap<>();

                    // Load each item
                    playerTag.getAllKeys().forEach(itemKey -> {
                        try {
                            net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(
                                    itemKey);
                            Object itemObj = PlatformAccess.getRegistry().getItem(itemId);
                            if (itemObj instanceof net.minecraft.world.item.Item item) {
                                if (item != null) {
                                    long count = playerTag.getLong(itemKey);
                                    if (count > 0) {
                                        playerItems.put(item, count);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LOGGER.error("Error loading personal storage item for player {}: {}", playerKey, itemKey,
                                    e);
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
        if (tag.contains("wantedResources")) {
            CompoundTag wantsTag = tag.getCompound("wantedResources");
            wantsTag.getAllKeys().forEach(key -> {
                try {
                    net.minecraft.resources.ResourceLocation itemId = new net.minecraft.resources.ResourceLocation(key);
                    Object itemObj = PlatformAccess.getRegistry().getItem(itemId);
                    if (itemObj instanceof Item item) {
                        long amount = wantsTag.getLong(key);
                        town.wantedResources.put(item, amount);
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading wanted resource: {}", key, e);
                }
            });
        }

        if (tag.contains("escrow")) {
            CompoundTag escrowTag = tag.getCompound("escrow");
            escrowTag.getAllKeys().forEach(key -> {
                try {
                    net.minecraft.resources.ResourceLocation resourceLocation = new net.minecraft.resources.ResourceLocation(
                            key);
                    Object itemObj = PlatformAccess.getRegistry().getItem(resourceLocation);
                    if (itemObj instanceof net.minecraft.world.item.Item item) {
                        long amount = escrowTag.getLong(key);
                        // Sanitize negative values
                        if (amount < 0)
                            amount = 0;
                        if (amount > 0) {
                            town.escrowedResources.put(item, amount);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Error loading escrowed resource: {}", key, e);
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
     * 
     * @param newName The new name for the town
     */
    public void setName(String newName) {
        this.name = newName;
    }

    public long getPopulation() {
        return economy.getPopulation();
    }

    public void setPopulation(int population) {
        economy.setPopulation(population);
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getPosition() {
        return position;
    }

    public void setTouristSpawningEnabled(boolean enabled) {
        this.touristSpawningEnabled = enabled;
    }

    public void addVisitor(UUID fromTownId) {
        visitors.merge(fromTownId, 1L, Long::sum);
    }

    public int getPendingTouristSpawns() {
        return pendingTouristSpawns;
    }

    public void setPendingTouristSpawns(int count) {
        this.pendingTouristSpawns = count;
        markDirty();
    }

    public void addPendingTouristSpawns(int count) {
        this.pendingTouristSpawns += count;
        markDirty();
    }

    public int getTotalVisitors() {
        return (int) Math.min(Integer.MAX_VALUE, visitors.values().stream().mapToLong(Long::longValue).sum());
    }

    public long getTotalTouristsArrived() {
        return totalTouristsArrived;
    }

    public double getTotalTouristDistance() {
        return totalTouristDistance;
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

    public String getBiome() {
        return biome;
    }

    public void setBiome(String biome) {
        this.biome = biome;
        markDirty();
    }

    public String getBiomeVariant() {
        return biomeVariant;
    }

    public void setBiomeVariant(String biomeVariant) {
        this.biomeVariant = biomeVariant;
        markDirty();
    }

    @Override
    public UUID getTownId() {
        return id;
    }

    @Override
    public void markDirty() {
        // Find the TownManager for all loaded levels and mark the town data as dirty
        // Platform-agnostic: iterate through all TownManager instances
        boolean foundInAnyLevel = false;
        for (TownManager manager : TownManager.getAllInstances()) {
            if (manager.getTown(id) == this) {
                manager.markDirty();
                foundInAnyLevel = true;
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                        "Successfully marked town '{}' (id: {}) as dirty",
                        this.name, this.id);
            }
        }

        if (!foundInAnyLevel) {
            LOGGER.warn("Failed to mark town '{}' (id: {}) as dirty - not found in any loaded level",
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

        // Update cumulative stats
        this.totalTouristsArrived += count;

        if (originPos != null && originPos != BlockPos.ZERO) {
            double distance = Math.sqrt(this.position.distSqr(originPos));
            this.totalTouristDistance += distance * count; // Weighted by visitor count
        }

        // Mark as dirty to ensure it's saved
        markDirty();
    }

    @Override
    public List<VisitHistoryRecord> getVisitHistory() {
        return Collections.unmodifiableList(visitHistory);
    }

    /**
     * Add items to the payment buffer (replaces communal storage for direct item
     * management)
     * 
     * @param item  The item to add
     * @param count The amount to add (can be negative to remove)
     * @return true if successful, false if there aren't enough items to remove
     */
    public boolean addToCommunalStorage(Item item, long count) {
        if (count == 0)
            return true;

        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                "BUFFER UPDATE - Town '{}' - Attempting to add {} {} to payment buffer",
                this.name, count, item.getDescription().getString());

        if (count > 0) {
            // Adding items to buffer
            boolean success = paymentBoard.addToBuffer(item, count);
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                        "BUFFER UPDATE - Town '{}' - Added {} {} to payment buffer",
                        this.name, count, item.getDescription().getString());
                markDirty();
            }
            return success;
        } else {
            // Removing items from buffer
            boolean success = paymentBoard.removeFromBuffer(item, Math.abs(count));
            if (success) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                        "BUFFER UPDATE - Town '{}' - Removed {} {} from payment buffer",
                        this.name, Math.abs(count), item.getDescription().getString());
                markDirty();
            }
            return success;
        }
    }

    /**
     * Get the count of a specific item in the payment buffer
     *
     * @param item The item to check
     * @return The amount stored
     */
    public long getCommunalStorageCount(Item item) {
        return paymentBoard.getBufferStorage().getOrDefault(item, 0L);
    }

    /**
     * Get all items in the payment buffer
     *
     * @return Map of all items and their counts
     */
    public Map<Item, Long> getAllCommunalStorageItems() {
        return paymentBoard.getBufferStorage();
    }

    /**
     * Get the payment board for this town
     * 
     * @return The TownPaymentBoard instance
     */
    public TownPaymentBoard getPaymentBoard() {
        return paymentBoard;
    }

    /**
     * Add a resource to a player's personal storage
     * 
     * @param playerId The UUID of the player
     * @param item     The item to add
     * @param count    The amount to add (can be negative to remove)
     * @return true if successful, false if there aren't enough items to remove
     */
    public boolean addToPersonalStorage(UUID playerId, Item item, long count) {
        if (count == 0 || playerId == null)
            return true;

        // Get or create the player's storage map
        Map<Item, Long> playerStorage = personalStorage.computeIfAbsent(playerId, k -> new HashMap<>());

        // Get current amount
        long currentAmount = playerStorage.getOrDefault(item, 0L);
        long newAmount = currentAmount + count;

        // Check if removing more than available
        if (newAmount < 0) {
            LOGGER.warn("Attempted to remove {} {} from personal storage of player {} but only {} available",
                    Math.abs(count), item.getDescription().getString(), playerId, currentAmount);
            return false;
        }

        // Update storage
        if (newAmount > 0) {
            playerStorage.put(item, newAmount);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                    "Updated personal storage for player {}: {} {} (now {})",
                    playerId,
                    count > 0 ? "Added" : "Removed",
                    Math.abs(count) + " " + item.getDescription().getString(),
                    newAmount);
        } else {
            // Remove the entry if amount is zero
            playerStorage.remove(item);
            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                    "Removed {} from personal storage of player {} (empty)", item.getDescription().getString(),
                    playerId);
        }

        // Mark town as dirty to save changes
        markDirty();
        return true;
    }

    /**
     * Get the count of a specific item in a player's personal storage
     *
     * @param playerId The UUID of the player
     * @param item     The item to check
     * @return The amount stored
     */
    public long getPersonalStorageCount(UUID playerId, Item item) {
        if (playerId == null)
            return 0;

        Map<Item, Long> playerStorage = personalStorage.get(playerId);
        if (playerStorage == null)
            return 0;

        return playerStorage.getOrDefault(item, 0L);
    }

    /**
     * Get all items in a player's personal storage
     *
     * @param playerId The UUID of the player
     * @return Map of all items and their counts
     */
    public Map<Item, Long> getPersonalStorageItems(UUID playerId) {
        if (playerId == null)
            return Collections.emptyMap();

        Map<Item, Long> playerStorage = personalStorage.get(playerId);
        if (playerStorage == null)
            return Collections.emptyMap();

        return Collections.unmodifiableMap(playerStorage);
    }

    public com.quackers29.businesscraft.town.components.TownTradingComponent getTrading() {
        return trading;
    }

    public com.quackers29.businesscraft.town.components.TownProductionComponent getProduction() {
        return production;
    }

    public com.quackers29.businesscraft.town.components.TownEconomyComponent getEconomy() {
        return economy;
    }

    // --- ITownState Implementation ---
    @Override
    public float getStock(String resourceId) {
        return trading.getStock(resourceId);
    }

    @Override
    public float getStorageCap(String resourceId) {
        return trading.getStorageCap(resourceId);
    }

    @Override
    public float getProductionRate(String resourceId) {
        return production.getProductionRate(resourceId);
    }

    @Override
    public float getConsumptionRate(String resourceId) {
        return production.getConsumptionRate(resourceId);
    }

}
