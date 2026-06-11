package com.quackers29.businesscraft.town.components;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.testutil.McBootstrap;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-034: Autonomous Bidding and Sell Contract Creation (Test + Docs Loop).
 *
 * Covers the TownContractComponent (autonomous excess detection, sell contract
 * creation with excessRatio pricing, need-based bidding with courier budget,
 * mutual-exclusion guards, in-transit accounting, and the 3-contract cap).
 *
 * The core decision formulas live in private methods (isSellingResource,
 * isBuyingResource, checkAndCreateContract, scanForBids/processPendingBids)
 * that are tightly coupled to ContractBoard, TownManager statics, ServerLevel,
 * ResourceRegistry, and live Town stock. Only construction, persistence of the
 * single scalar (lastContractCheckTime), and safe no-manager return paths are
 * exercised here.
 *
 * Full formulas + worked examples + edge rules are in the vault note:
 * vault/Town/Contracts/Autonomous Bidding and Sell Contract Creation.md
 *
 * McBootstrap is used only to allow the Town(UUID, BlockPos, String) ctor.
 * No registry doubles or ItemStacks are required for the smoke paths.
 */
class TownContractComponentTest {

    private static final UUID TOWN_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final BlockPos TOWN_POS = new BlockPos(100, 64, 200);
    private static final String TOWN_NAME = "ContractTestTown";

    private Town town;
    private TownContractComponent comp;

    private int savedDefaultPop;
    private int savedExcess;
    private int savedMinStock;
    private double savedAuctionMin;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @BeforeEach
    void setUp() {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedExcess = ConfigLoader.excessStockPercent;
        savedMinStock = ConfigLoader.minStockPercent;
        savedAuctionMin = ConfigLoader.contractAuctionDurationMinutes;

        ConfigLoader.defaultStartingPopulation = 10;
        ConfigLoader.excessStockPercent = 80;
        ConfigLoader.minStockPercent = 20;
        // auction duration left at default; not mutated by these tests

        town = new Town(TOWN_ID, TOWN_POS, TOWN_NAME);
        comp = town.getContracts();
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        ConfigLoader.excessStockPercent = savedExcess;
        ConfigLoader.minStockPercent = savedMinStock;
        ConfigLoader.contractAuctionDurationMinutes = savedAuctionMin;
    }

    @Test
    void construction_viaTown_succeeds() {
        assertNotNull(comp);
    }

    @Test
    void saveLoad_lastContractCheckTime_roundtrips() {
        // Initial value is 0 (from field initializer)
        CompoundTag tag1 = new CompoundTag();
        comp.save(tag1);
        // The component only persists this one long
        assertTrue(tag1.contains("lastContractCheckTime"));

        // Load a synthetic value and verify it survives a subsequent save
        CompoundTag loadTag = new CompoundTag();
        loadTag.putLong("lastContractCheckTime", 1234567890123L);
        comp.load(loadTag);

        CompoundTag tag2 = new CompoundTag();
        comp.save(tag2);
        assertEquals(1234567890123L, tag2.getLong("lastContractCheckTime"));
    }

    @Test
    void getInTransitResourceCount_noManagers_returnsZero() {
        // No TownManager instances registered in this test harness → early null guard
        assertEquals(0L, comp.getInTransitResourceCount("iron"));
        assertEquals(0L, comp.getInTransitResourceCount("food"));
    }

    @Test
    void getInTransitResourceCount_nullResource_returnsZero() {
        assertEquals(0L, comp.getInTransitResourceCount(null));
    }
}
