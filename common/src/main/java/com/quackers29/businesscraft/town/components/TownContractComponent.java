package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.contract.Contract;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.debug.DebugConfig;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class TownContractComponent implements TownComponent {
    private static final Logger LOGGER = LoggerFactory.getLogger(TownContractComponent.class);

    private final Town town;
    private long lastContractCheckTime = 0;
    private static final int CHECK_INTERVAL = 100; // Check every 100 ticks (5 seconds)
    private static final int MAX_ACTIVE_CONTRACTS = 3;

    public TownContractComponent(Town town) {
        this.town = town;
    }

    private java.util.Map<UUID, Long> pendingBids = new java.util.HashMap<>();

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();

        // Process pending bids every tick (or every few ticks)
        processPendingBids(currentTime);

        // Check every CHECK_INTERVAL ticks
        if (currentTime - lastContractCheckTime < CHECK_INTERVAL * 50) { // 50ms per tick approx
            return;
        }

        lastContractCheckTime = currentTime;

        tryCreateContract();
        scanForBids(currentTime);
    }

    private static final float MAX_BID_MULTIPLIER = 3.0f; // Don't pay more than 3x base price

    private void processPendingBids(long currentTime) {
        if (pendingBids.isEmpty())
            return;

        // Get the server level
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                level = manager.getLevel();
                break;
            }
        }

        if (level == null)
            return;
        ContractBoard board = ContractBoard.get(level);

        java.util.Iterator<java.util.Map.Entry<UUID, Long>> iterator = pendingBids.entrySet().iterator();
        while (iterator.hasNext()) {
            java.util.Map.Entry<UUID, Long> entry = iterator.next();
            if (currentTime >= entry.getValue()) {
                UUID contractId = entry.getKey();
                iterator.remove();

                // Re-evaluate and place bid
                Contract contract = board.getContract(contractId);
                if (contract instanceof SellContract sc && !sc.isExpired()) {
                    // Don't bid if auction is closed (courier phase)
                    if (sc.isAuctionClosed()) {
                        continue;
                    }

                    // Check if already the highest bidder (again, just in case)
                    if (town.getId().equals(sc.getHighestBidder())) {
                        continue;
                    }

                    // Calculate bid again to be fresh
                    float currentHighest = sc.getHighestBid();
                    float marketPrice = board.getMarketPrice(sc.getResourceId());
                    float basePrice = marketPrice * sc.getQuantity();

                    // Calculate courier cost
                    com.quackers29.businesscraft.town.Town sellerTown = com.quackers29.businesscraft.town.TownManager
                            .get(level).getTown(sc.getIssuerTownId());
                    int courierCost = ContractBoard.calculateCourierCost(town, sellerTown);

                    float bid = (float) Math.ceil((currentHighest > 0 ? currentHighest : basePrice) * 1.1f);

                    // Check max bid limit (Total budget = 3x base price)
                    float maxTotalBudget = basePrice * MAX_BID_MULTIPLIER;
                    float maxBid = maxTotalBudget - courierCost;

                    if (bid > maxBid) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Town {} skipping bid on {} - total cost too high (Bid {} + Courier {} > {})",
                                town.getName(), sc.getId(), bid, courierCost, maxTotalBudget);
                        continue;
                    }

                    // ESCROW: Check if town has enough emeralds before bidding (Bid + Courier)
                    int emeraldCount = town.getResourceCount(Items.EMERALD);
                    if (emeraldCount < (bid + courierCost)) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                                "Town {} cannot bid {} on contract {} - insufficient emeralds ({} available, need {})",
                                town.getName(), bid, sc.getId(), emeraldCount, (int) (bid + courierCost));
                        continue;
                    }

                    // Place bid
                    board.addBid(sc.getId(), town.getId(), bid, level);
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Town {} bid {} on contract {} (after delay)",
                            town.getName(), bid, sc.getId());
                }
            }
        }
    }

    private void scanForBids(long currentTime) {
        // Get the server level
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                level = manager.getLevel();
                break;
            }
        }

        if (level == null) {
            return;
        }

        ContractBoard board = ContractBoard.get(level);
        List<Contract> contracts = board.getContracts();

        for (Contract contract : contracts) {
            if (contract instanceof SellContract sc) {
                // Don't bid on own contracts
                if (sc.getIssuerTownId().equals(town.getId())) {
                    continue;
                }

                // Don't bid on expired contracts
                if (sc.isExpired()) {
                    continue;
                }

                // Don't bid if auction is closed (courier phase)
                if (sc.isAuctionClosed()) {
                    continue;
                }

                // Don't schedule if already pending
                if (pendingBids.containsKey(sc.getId())) {
                    continue;
                }

                // Check if town needs this resource
                String resourceId = sc.getResourceId();

                // Look up the resource type from ResourceRegistry
                com.quackers29.businesscraft.economy.ResourceType resourceType = com.quackers29.businesscraft.economy.ResourceRegistry
                        .get(resourceId);

                // Skip if resource not found in registry
                if (resourceType == null) {
                    continue;
                }

                // Get the canonical item for this resource
                net.minecraft.resources.ResourceLocation itemLoc = resourceType.getCanonicalItemId();
                Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(itemLoc);

                if (!(itemObj instanceof Item)) {
                    continue;
                }

                Item item = (Item) itemObj;
                int currentCount = town.getResourceCount(item);

                float cap = town.getTrading().getStorageCap(resourceId);
                float needThreshold = cap * (com.quackers29.businesscraft.config.ConfigLoader.minStockPercent / 100.0f);

                if (item != null && currentCount < needThreshold) {
                    // Check if already the highest bidder
                    if (town.getId().equals(sc.getHighestBidder())) {
                        continue;
                    }

                    // Pre-check max bid (approximate)
                    float currentHighest = sc.getHighestBid();
                    float marketPrice = board.getMarketPrice(sc.getResourceId());
                    float basePrice = marketPrice * sc.getQuantity();

                    // Calculate courier cost
                    com.quackers29.businesscraft.town.Town sellerTown = com.quackers29.businesscraft.town.TownManager
                            .get(level).getTown(sc.getIssuerTownId());
                    int courierCost = ContractBoard.calculateCourierCost(town, sellerTown);

                    float projectedBid = (float) Math.ceil((currentHighest > 0 ? currentHighest : basePrice) * 1.1f);

                    // Check max bid limit (Total budget = 3x base price)
                    float maxTotalBudget = basePrice * MAX_BID_MULTIPLIER;
                    float maxBid = maxTotalBudget - courierCost;

                    if (projectedBid > maxBid) {
                        continue;
                    }

                    // Schedule bid with 1s delay
                    pendingBids.put(sc.getId(), currentTime + 1000);
                }
            }
        }
    }

    private void tryCreateContract() {
        // Get the server level from any of the active TownManager instances
        // Since this town exists, we know its manager is loaded
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                // Found our manager, get its level
                level = getServerLevelFromManager(manager);
                break;
            }
        }

        if (level == null) {
            LOGGER.warn("Could not find ServerLevel for town {}", town.getName());
            return;
        }

        ContractBoard board = ContractBoard.get(level);
        long activeCount = board.getContracts().stream()
                .filter(c -> c instanceof SellContract sc &&
                        sc.getIssuerTownId().equals(town.getId()) &&
                        !sc.isExpired() && !sc.isCompleted())
                .count();

        if (activeCount >= MAX_ACTIVE_CONTRACTS) {
            return;
        }

        // Check all resources for excess (excluding currency)
        for (java.util.Map.Entry<Item, Integer> entry : town.getAllResources().entrySet()) {
            Item item = entry.getKey();

            // Skip emeralds - they are the currency
            if (item == Items.EMERALD) {
                continue;
            }

            // Look up the resource type from ResourceRegistry
            com.quackers29.businesscraft.economy.ResourceType resourceType = com.quackers29.businesscraft.economy.ResourceRegistry
                    .getFor(item);

            // Only create contracts for registered resources
            if (resourceType == null) {
                continue;
            }

            String resourceId = resourceType.getId();
            checkAndCreateContract(board, level, resourceId, item);
        }
    }

    private net.minecraft.server.level.ServerLevel getServerLevelFromManager(
            com.quackers29.businesscraft.town.TownManager manager) {
        return manager.getLevel();
    }

    private void checkAndCreateContract(ContractBoard board, net.minecraft.server.level.ServerLevel level,
            String resourceId, Item item) {
        // Check if already at max contracts
        long activeCount = board.getContracts().stream()
                .filter(c -> c instanceof SellContract sc &&
                        sc.getIssuerTownId().equals(town.getId()) &&
                        !sc.isExpired() && !sc.isCompleted())
                .count();

        if (activeCount >= MAX_ACTIVE_CONTRACTS) {
            return;
        }

        int resourceCount = town.getResourceCount(item);

        float cap = town.getTrading().getStorageCap(resourceId);
        float excessThreshold = cap * (com.quackers29.businesscraft.config.ConfigLoader.excessStockPercent / 100.0f);

        if (resourceCount > excessThreshold) {
            // Check if town already has an active contract for this resource
            boolean hasContract = board.getContracts().stream()
                    .anyMatch(c -> c instanceof SellContract sc &&
                            sc.getIssuerTownId().equals(town.getId()) &&
                            resourceId.equals(sc.getResourceId()) &&
                            !sc.isExpired());

            if (hasContract) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Town {} already has an active {} contract, skipping creation",
                        town.getName(), resourceId);
                return;
            }

            // Create a sell contract for excess resource
            int sellQuantity = (int) ((resourceCount - excessThreshold) / 2);
            if (sellQuantity <= 0) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Insufficient excess resources for {} ({} - {} = {}), skipping contract creation",
                        resourceId, resourceCount, excessThreshold, sellQuantity);
                return;
            }
            float marketPrice = board.getMarketPrice(resourceId);

            // Calculate excess ratio (how much above threshold)
            // Threshold = excessThreshold. "Vast excess" = excessThreshold * 3 (arbitrary
            // scaling? or relative to cap?)
            // User: "excess food is 80% max cap... check contract section"
            // Let's use relative to Cap for the multiplier scaling.
            // Old logic: (Count - Thresh) / (Thresh * 2). Thresh=200, Range=400.
            // New logic: (Count - Thresh) / (Cap - Thresh). Range is the remaining space.
            // If Thresh is 80% (800), max is 1000. Divisor = 200.
            // If count is 1000 (full), ratio = 200/200 = 1.0. Correct.

            float scalingRange = cap - excessThreshold;
            if (scalingRange < 1.0f)
                scalingRange = 1.0f;

            float excessRatio = (float) (resourceCount - excessThreshold) / scalingRange;
            if (excessRatio > 1.0f)
                excessRatio = 1.0f;

            // Interpolate modifier: +0.05 to -0.40
            float modifier = 0.05f + (excessRatio * (-0.40f - 0.05f));

            // Add randomness (+/- 5%)
            float randomness = (float) (Math.random() * 0.10f) - 0.05f;

            float pricePerUnit = marketPrice * (1.0f + modifier + randomness);

            // Ensure price is at least 0.1
            if (pricePerUnit < 0.1f)
                pricePerUnit = 0.1f;

            SellContract contract = new SellContract(
                    town.getId(),
                    town.getName(),
                    60000L,
                    resourceId,
                    sellQuantity,
                    pricePerUnit);

            board.addContract(contract);

            // ESCROW: Immediately deduct resources from seller
            town.addResource(item, -sellQuantity);

            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS, "Town {} created sell contract for {} {} (escrowed {} resources)",
                    town.getName(), sellQuantity, resourceId, sellQuantity);
        }
    }

    @Override
    public void save(CompoundTag tag) {
        tag.putLong("lastContractCheckTime", lastContractCheckTime);
    }

    @Override
    public void load(CompoundTag tag) {
        lastContractCheckTime = tag.getLong("lastContractCheckTime");
    }
}
