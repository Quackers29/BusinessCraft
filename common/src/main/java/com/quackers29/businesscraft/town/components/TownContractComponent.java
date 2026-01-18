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

    // Helper to check if we are currently selling a resource (Active Sell Contract)
    private boolean isSellingResource(String resourceId, ContractBoard board) {
        return board.getContracts().stream()
                .anyMatch(c -> c instanceof SellContract sc &&
                        sc.getIssuerTownId().equals(town.getId()) &&
                        resourceId.equals(sc.getResourceId()) &&
                        !sc.isExpired() && !sc.isCompleted());
    }

    // Helper to check if we are currently buying a resource (Highest Bidder)
    private boolean isBuyingResource(String resourceId, ContractBoard board) {
        return board.getContracts().stream()
                .anyMatch(c -> c instanceof SellContract sc &&
                        town.getId().equals(sc.getHighestBidder()) &&
                        resourceId.equals(sc.getResourceId()) &&
                        !sc.isExpired() && !sc.isCompleted());
    }

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

                    // NEED-BASED BID MODIFIER: Calculate current need level
                    String resourceId = sc.getResourceId();
                    com.quackers29.businesscraft.economy.ResourceType resourceType =
                            com.quackers29.businesscraft.economy.ResourceRegistry.get(resourceId);

                    float needRatio = 1.0f; // Default to desperate if can't determine
                    boolean isWanted = false;

                    if (resourceType != null) {
                        net.minecraft.resources.ResourceLocation itemLoc = resourceType.getCanonicalItemId();
                        Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(itemLoc);

                        if (itemObj instanceof Item item) {
                            int currentCount = town.getResourceCount(item);
                            float cap = town.getTrading().getStorageCap(resourceId);
                            float needThreshold = cap * (com.quackers29.businesscraft.config.ConfigLoader.minStockPercent / 100.0f);

                            needRatio = needThreshold > 0 ? (needThreshold - currentCount) / needThreshold : 1.0f;
                            needRatio = Math.max(0f, Math.min(1f, needRatio));

                            isWanted = town.getWantedResources().containsKey(item);
                            if (isWanted) {
                                needRatio = 1.0f; // Treat wanted items as desperate
                            }
                        }
                    }

                    // Bid modifier: 0.80 (reluctant) to 1.20 (desperate)
                    float bidModifier = 0.80f + (needRatio * 0.40f);

                    // Add randomness ±5%
                    float randomness = (float)(Math.random() * 0.10f) - 0.05f;
                    bidModifier += randomness;

                    // Calculate bid - if there's a current highest, we must beat it
                    float bid;
                    if (currentHighest > 0) {
                        // Must beat current bid by at least 1 emerald (or 10%, whichever is higher)
                        float minIncrement = Math.max(1.0f, currentHighest * 0.10f);
                        bid = (float) Math.ceil(currentHighest + minIncrement);
                    } else {
                        // First bid: use need-based modifier on base price
                        bid = (float) Math.ceil(basePrice * bidModifier);
                    }

                    // Floor: minimum bid is 1 emerald
                    if (bid < 1.0f) {
                        bid = 1.0f;
                    }

                    // Check max bid limit (Total budget = 3x base price normally, 5x if wanted)
                    // For very cheap items, ensure budget is at least (1 emerald + courier cost)
                    float multiplier = isWanted ? 5.0f : MAX_BID_MULTIPLIER;
                    float calculatedBudget = basePrice * multiplier;
                    float minBudget = 1.0f + courierCost;
                    float maxTotalBudget = Math.max(calculatedBudget, minBudget);
                    float maxBid = maxTotalBudget - courierCost;

                    if (bid > maxBid) {
                        DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                                "Town {} skipping bid on {} - total cost too high (Bid {} + Courier {} > {})",
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
                    DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                            "Town {} bid {} on contract {} (after delay)",
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

                // Avoid conflict: Don't BID if we are currently SELLING this resource
                if (isSellingResource(resourceId, board)) {
                    continue;
                }

                // Skip if resource not found in registry
                if (resourceType == null) {
                    continue;
                }

                // Get the canonical item for this resource
                net.minecraft.resources.ResourceLocation itemLoc = resourceType.getCanonicalItemId();
                Object itemObj = com.quackers29.businesscraft.api.PlatformAccess.getRegistry().getItem(itemLoc);

                if (!(itemObj instanceof Item item)) {
                    continue;
                }

                int currentCount = town.getResourceCount(item);
                float cap = town.getTrading().getStorageCap(resourceId);
                float needThreshold = cap * (com.quackers29.businesscraft.config.ConfigLoader.minStockPercent / 100.0f);

                boolean isWanted = town.getWantedResources().containsKey(item);
                int wantedDeficit = town.getWantedResources().getOrDefault(item, 0); // Negative value

                // Bid if stock is low OR if we explicitly want it for an upgrade
                if (item != null && (currentCount < needThreshold || isWanted)) {
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

                    // NEED-BASED BID MODIFIER: Mirrors seller's excess-based pricing
                    // - Desperate (empty stock, needRatio=1): bid at GPI * 1.20
                    // - Reluctant (at threshold, needRatio=0): bid at GPI * 0.80
                    // This enables natural price discovery through trades
                    float needRatio = needThreshold > 0 ? (needThreshold - currentCount) / needThreshold : 1.0f;
                    needRatio = Math.max(0f, Math.min(1f, needRatio)); // Clamp 0-1

                    // If wanted for upgrade, treat as desperate (high modifier)
                    if (isWanted) {
                        needRatio = 1.0f;
                    }

                    // Bid modifier: 0.80 (reluctant) to 1.20 (desperate)
                    float bidModifier = 0.80f + (needRatio * 0.40f);

                    // Add randomness ±5%
                    float randomness = (float)(Math.random() * 0.10f) - 0.05f;
                    bidModifier += randomness;

                    // Calculate bid - if there's a current highest, we must beat it
                    float projectedBid;
                    if (currentHighest > 0) {
                        // Must beat current bid by at least 1 emerald (or 10%, whichever is higher)
                        float minIncrement = Math.max(1.0f, currentHighest * 0.10f);
                        projectedBid = (float) Math.ceil(currentHighest + minIncrement);
                    } else {
                        // First bid: use need-based modifier on base price
                        projectedBid = (float) Math.ceil(basePrice * bidModifier);
                    }

                    // Floor: minimum bid is 1 emerald
                    if (projectedBid < 1.0f) {
                        projectedBid = 1.0f;
                    }

                    // Check max bid limit (Total budget = 3x base price normally, 5x if wanted)
                    // For very cheap items, ensure budget is at least (1 emerald + courier cost)
                    // so the town can still participate in auctions
                    float multiplier = isWanted ? 5.0f : MAX_BID_MULTIPLIER;
                    float calculatedBudget = basePrice * multiplier;
                    float minBudget = 1.0f + courierCost; // At least 1 emerald bid + courier
                    float maxTotalBudget = Math.max(calculatedBudget, minBudget);

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

        // Use TOTAL count (Available + Escrow) for decision making
        int resourceCount = town.getTotalResourceCount(item);

        float cap = town.getTrading().getStorageCap(resourceId);
        float excessThreshold = cap * (com.quackers29.businesscraft.config.ConfigLoader.excessStockPercent / 100.0f);

        if (resourceCount > excessThreshold) {
            // Check if town already has an active contract for this resource
            if (isSellingResource(resourceId, board)) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                        "Town {} already has an active {} contract, skipping creation",
                        town.getName(), resourceId);
                return;
            }

            // Avoid conflict: Don't SELL if we are currently BUYING this resource (Highest
            // Bidder)
            if (isBuyingResource(resourceId, board)) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                        "Town {} is currently buying {}, skipping contract creation (Mutual Exclusion)",
                        town.getName(), resourceId);
                return;
            }

            // Create a sell contract for excess resource
            // Target stock is halfway between Minimum Required and Excess Threshold
            float minThreshold = cap * (com.quackers29.businesscraft.config.ConfigLoader.minStockPercent / 100.0f);
            float targetStock = minThreshold + ((excessThreshold - minThreshold) / 2);

            int sellQuantity = (int) (resourceCount - targetStock);
            // Cap sell quantity to prevent massive dumps and potential overflow issues
            if (sellQuantity > 10000) {
                sellQuantity = 10000;
            }

            // CRITICAL FIX: Ensure we actually have these items AVAILABLE (not just in
            // Total/Escrow)
            // If we assume we have them but they are all in escrow, addResource will clamp
            // to 0
            // but addEscrow will add them, creating infinite resources.
            int availableCount = town.getResourceCount(item);
            if (sellQuantity > availableCount) {
                sellQuantity = availableCount;
            }

            if (sellQuantity <= 0) {
                DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                        "Insufficient available resources for {} (Count: {} - Target: {} = {}), skipping contract creation",
                        resourceId, resourceCount, targetStock, sellQuantity);
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
                    (long) (com.quackers29.businesscraft.config.ConfigLoader.contractAuctionDurationMinutes * 60000L),
                    resourceId,
                    sellQuantity,
                    pricePerUnit);

            board.addContract(contract);

            // ESCROW: Move items from Available to Escrow
            town.addResource(item, -sellQuantity);
            town.addEscrowResource(item, sellQuantity);

            DebugConfig.debug(LOGGER, DebugConfig.TOWN_DATA_SYSTEMS,
                    "Town {} created sell contract for {} {} (moved to escrow)",
                    town.getName(), sellQuantity, resourceId);
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

    public int getInTransitResourceCount(String resourceId) {
        // Need to find the server level to get the board
        net.minecraft.server.level.ServerLevel level = null;
        for (com.quackers29.businesscraft.town.TownManager manager : com.quackers29.businesscraft.town.TownManager
                .getAllInstances()) {
            if (manager.getTown(town.getId()) != null) {
                level = manager.getLevel();
                break;
            }
        }

        if (level == null) {
            return 0;
        }

        ContractBoard board = ContractBoard.get(level);
        // Sum up quantities of SellContracts where:
        // 1. We are the winner (active or delivered-but-not-fully)
        // 2. !isDelivered (completely) OR deliveredAmount < quantity
        // Actually simplest is: SellContract where winningTownId == town.id AND
        // !isDelivered
        // Logic:
        // - Auction Closed (We won)
        // - Courier Assigned (or not yet)
        // - Delivered = false (Not fully delivered)

        return board.getContracts().stream()
                .filter(c -> c instanceof SellContract sc &&
                        town.getId().equals(sc.getWinningTownId()) &&
                        resourceId.equals(sc.getResourceId()) &&
                        !sc.isExpired() &&
                        !sc.isDelivered()) // isDelivered check covers "fully delivered"
                .mapToInt(c -> ((SellContract) c).getQuantity() - ((SellContract) c).getDeliveredAmount())
                .sum();
    }
}
