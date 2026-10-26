package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.economy.JourneyProgress;
import com.quackers29.businesscraft.entity.TouristTradeCatalog.TradeDef;
import com.quackers29.businesscraft.menu.TouristScreenData;
import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Pins themed tourist deals, the minute clock, and the rule that no chain of
 * trades pays the player more than they spent.
 */
class TouristTradeCatalogTest {
    private static final int[] THRESHOLDS = JourneyProgress.DEFAULT_TOURIST_LEVELS;

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    private static TouristTradeCatalog.TradeContext brooksideAnyTown(double distance, int level) {
        return new TouristTradeCatalog.TradeContext(
                "Brookside",
                TouristEntity.ANY_TOWN_NAME,
                true,
                JourneyProgress.tierName(level),
                distance,
                "17m",
                "1h 51m",
                true);
    }

    @Test
    void desertGuide_levelOne_isSandAndCactus_notTheOldIronRow() {
        MerchantOffers offers = TouristTradeCatalog.offersFor(1, brooksideAnyTown(120, 1), "desert_guide");
        assertEquals(3, offers.size());
        assertTrue(offers.get(0).getBaseCostA().is(Items.PAPER));
        assertEquals("Travel Ticket", offers.get(0).getResult().getHoverName().getString());
        assertTrue(offers.get(1).getBaseCostA().is(Items.SAND));
        assertTrue(offers.get(1).getResult().is(Items.CACTUS));
        assertTrue(offers.get(2).getResult().is(Items.SAND));
        for (MerchantOffer offer : offers) {
            assertFalse(offer.getBaseCostA().is(Items.BARRIER));
            assertFalse(offer.getResult().is(Items.IRON_INGOT));
        }
    }

    @Test
    void desertGuide_gatesCompassAndClockByRank() {
        MerchantOffers voyager = TouristTradeCatalog.offersFor(3, brooksideAnyTown(1800, 3), "desert_guide");
        assertEquals(6, voyager.size());
        assertTrue(voyager.get(5).getResult().is(Items.COMPASS));
        MerchantOffers renowned = TouristTradeCatalog.offersFor(5, brooksideAnyTown(6000, 5), "desert_guide");
        assertEquals(8, renowned.size());
        assertTrue(renowned.get(7).getResult().is(Items.CLOCK));
        assertEquals(12, renowned.get(7).getBaseCostA().getCount());
        TradeDef clock = TouristTradeCatalog.findDef("desert_guide", "desert_clock");
        assertEquals("Locked · Renowned at 6.0 km", TouristTradeCatalog.lockLabel(clock, THRESHOLDS));
    }

    @Test
    void pilgrimAndScholar_askForBooksCandlesAndPaper() {
        List<TradeDef> pilgrim = TouristTradeCatalog.forLook("garden_pilgrim");
        assertTrue(pilgrim.stream().anyMatch(def -> def.resultItem() == Items.CANDLE));
        assertTrue(pilgrim.stream().anyMatch(def -> def.resultItem() == Items.BOOK));
        assertTrue(pilgrim.stream().anyMatch(def -> def.costItem() == Items.BREAD));
        List<TradeDef> scholar = TouristTradeCatalog.forLook("station_scholar");
        assertEquals(scholar, TouristTradeCatalog.forLook("library_clerk"));
        assertTrue(scholar.stream().anyMatch(def -> def.costItem() == Items.PAPER && def.resultItem() == Items.BOOK));
    }

    @Test
    void vanillaScreenshot_stillStartsWithTheBarrierRow() {
        MerchantOffers offers = TouristTradeCatalog.offersForVanillaScreenshot(1, brooksideAnyTown(120, 1), "desert_guide");
        assertTrue(offers.get(0).getBaseCostA().is(Items.BARRIER));
        assertEquals(4, offers.size());
    }

    @Test
    void ticketLore_explainsAnyTown_andDoesNotLectureAboutWalking() {
        String lore = TouristTradeCatalog.ticketLore(brooksideAnyTown(120, 1)).stream()
                .map(line -> line.getString())
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(lore.contains("From: Brookside"));
        assertTrue(lore.contains("To: Any Town"));
        assertTrue(lore.contains("souvenir"));
        assertTrue(lore.contains("first town they reach"));
        assertTrue(lore.contains("Wanderer"));
        assertFalse(lore.toLowerCase().contains("walk"));
        assertFalse(lore.toLowerCase().contains("minecart"));
        assertFalse(lore.toLowerCase().contains("not a ride"));
        assertFalse(lore.contains("Novice"));
    }

    @Test
    void duration_isMinutesOnly() {
        assertEquals("<1m", TouristTradeCatalog.formatDuration(0));
        assertEquals("<1m", TouristTradeCatalog.formatDuration(59));
        assertEquals("17m", TouristTradeCatalog.formatDuration(17 * 60 + 31));
        assertEquals("1h 51m", TouristTradeCatalog.formatDuration(60 * 60 + 51 * 60));
        assertEquals("Out for 17m · 1h 51m left while they wait",
                TouristTradeCatalog.waitingLine(17 * 60 + 31, 60 * 60 + 51 * 60, true));
        assertFalse(TouristTradeCatalog.waitingLine(90, 90, true).toLowerCase().contains("walk"));
    }

    @Test
    void copyUses_survivesATicketWhoseLoreChanged() {
        TouristTradeCatalog.TradeContext first = brooksideAnyTown(120, 1);
        TouristTradeCatalog.TradeContext later = brooksideAnyTown(400, 1);
        MerchantOffers oldOffers = TouristTradeCatalog.offersFor(1, first, "desert_guide");
        oldOffers.get(0).increaseUses();
        MerchantOffers fresh = TouristTradeCatalog.offersFor(1, later, "desert_guide");
        assertFalse(ItemStack.isSameItemSameTags(oldOffers.get(0).getResult(), fresh.get(0).getResult()));
        TouristTradeCatalog.copyUses(oldOffers, fresh);
        assertTrue(fresh.get(0).isOutOfStock());
        assertFalse(fresh.get(1).isOutOfStock());
    }

    @Test
    void rowState_explainsLockedNeedAndTraded() {
        TradeDef clock = TouristTradeCatalog.findDef("desert_guide", "desert_clock");
        assertEquals(TouristTradeCatalog.RowState.LOCKED,
                TouristTradeCatalog.rowState(3, 16, false, clock));
        TradeDef sand = TouristTradeCatalog.findDef("desert_guide", "desert_sand");
        assertEquals(TouristTradeCatalog.RowState.NEED,
                TouristTradeCatalog.rowState(1, 3, false, sand));
        assertEquals("Needs 6 bread · you have 3", TouristTradeCatalog.needLabel(sand, 3));
        assertEquals(TouristTradeCatalog.RowState.TRADED,
                TouristTradeCatalog.rowState(1, 6, true, sand));
        assertEquals(TouristTradeCatalog.RowState.READY,
                TouristTradeCatalog.rowState(1, 6, false, sand));
    }

    @Test
    void progressAndIdentity_useRankNamesNotVillagerLevels() {
        assertEquals("120 m travelled · 380 m to Traveler",
                TouristTradeCatalog.progressLine(120, 1, THRESHOLDS));
        assertEquals("1.8 km travelled · 1.2 km to Veteran",
                TouristTradeCatalog.progressLine(1800, 3, THRESHOLDS));
        assertEquals("6.0 km travelled · Renowned",
                TouristTradeCatalog.progressLine(6000, 5, THRESHOLDS));
        assertEquals("Pilgrim · Desert Guide",
                TouristTradeCatalog.identityLine("Pilgrim", TouristTradeCatalog.lookLabel("desert_guide")));
        assertEquals("No town was chosen. They pay the first town they reach.",
                TouristTradeCatalog.destinationDetail("Any Town", true));
        assertEquals("They pay Hillcrest when they arrive.",
                TouristTradeCatalog.destinationDetail("Hillcrest", false));
        assertFalse(JourneyProgress.tierName(1).equals("Novice"));
    }

    @Test
    void trades_stayAtOrUnderValue_andCannotCycleIntoAProfit() {
        Map<Item, Integer> unit = valueTable();
        List<TradeDef> defs = TouristTradeCatalog.all();
        assertTrue(defs.size() > 20);
        Map<Item, List<Edge>> graph = new HashMap<>();
        Set<Item> nodes = new HashSet<>();
        for (TradeDef def : defs) {
            assertTrue(unit.containsKey(def.costItem()), "no value for cost of " + def.id());
            assertTrue(unit.containsKey(def.resultItem()), "no value for result of " + def.id());
            int spent = unit.get(def.costItem()) * def.costCount();
            int gained = unit.get(def.resultItem()) * def.resultCount();
            assertTrue(gained <= spent, def.id() + " returns " + gained + " for " + spent);
            assertNotEquals(Items.EMERALD, def.resultItem(), def.id());
            assertNotEquals(Items.COAL, def.resultItem(), def.id());
            assertNotEquals(Items.FIREWORK_ROCKET, def.resultItem(), def.id());
            nodes.add(def.costItem());
            nodes.add(def.resultItem());
            double weight = Math.log(spent) - Math.log(gained);
            graph.computeIfAbsent(def.costItem(), key -> new ArrayList<>()).add(new Edge(def.resultItem(), weight));
        }
        List<Item> order = new ArrayList<>(nodes);
        for (Item start : order) {
            Map<Item, Double> dist = new HashMap<>();
            for (Item node : order) {
                dist.put(node, Double.POSITIVE_INFINITY);
            }
            dist.put(start, 0.0);
            for (int pass = 0; pass < order.size(); pass++) {
                boolean updated = false;
                for (Item from : order) {
                    double fromDist = dist.get(from);
                    if (fromDist == Double.POSITIVE_INFINITY) {
                        continue;
                    }
                    for (Edge edge : graph.getOrDefault(from, List.of())) {
                        double next = fromDist + edge.weight;
                        if (next < dist.get(edge.to) - 1.0e-9) {
                            if (pass == order.size() - 1) {
                                fail("profitable trade cycle reaches " + edge.to);
                            }
                            dist.put(edge.to, next);
                            updated = true;
                        }
                    }
                }
                if (!updated) {
                    break;
                }
            }
        }
    }

    @Test
    void screenData_roundTripsNameLookAndLongJourneys() {
        TouristScreenData data = new TouristScreenData(
                "Brookside", "Hillcrest", false, "Pilgrim", "Desert Guide",
                "Eleanor Hart", "desert_guide",
                THRESHOLDS, 3, 40_000, 1 << 2, 90_000, 6_660, false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        data.write(buf);
        TouristScreenData read = TouristScreenData.read(buf);
        assertEquals("Brookside", read.origin());
        assertEquals("Hillcrest", read.destination());
        assertFalse(read.anyTown());
        assertEquals("Desert Guide", read.lookLabel());
        assertEquals("Eleanor Hart", read.personalName());
        assertEquals("desert_guide", read.lookId());
        assertEquals(3, read.level());
        assertEquals(40_000, read.distanceMeters());
        assertEquals(90_000, read.journeySeconds());
        assertEquals(6_660, read.remainingSeconds());
        assertEquals(40_000, TouristScreenData.combineDistance(
                TouristScreenData.distanceLo(40_000), TouristScreenData.distanceHi(40_000)));
        int signedLo = (short) TouristScreenData.distanceLo(40_000);
        assertTrue(signedLo < 0);
        assertEquals(40_000, TouristScreenData.combineDistance(signedLo, TouristScreenData.distanceHi(40_000)));
        assertEquals(List.of(500, 1500, 3000, 6000), java.util.Arrays.stream(read.thresholds()).boxed().toList());
    }

    private static Map<Item, Integer> valueTable() {
        Map<Item, Integer> unit = new HashMap<>();
        unit.put(Items.BREAD, 2);
        unit.put(Items.WHEAT, 1);
        unit.put(Items.PAPER, 1);
        unit.put(Items.SAND, 1);
        unit.put(Items.RED_SAND, 1);
        unit.put(Items.CACTUS, 2);
        unit.put(Items.SANDSTONE, 2);
        unit.put(Items.DEAD_BUSH, 1);
        unit.put(Items.CANDLE, 4);
        unit.put(Items.BOOK, 6);
        unit.put(Items.WRITABLE_BOOK, 8);
        unit.put(Items.INK_SAC, 2);
        unit.put(Items.MAP, 8);
        unit.put(Items.COMPASS, 12);
        unit.put(Items.CLOCK, 24);
        unit.put(Items.SPYGLASS, 10);
        unit.put(Items.LANTERN, 8);
        unit.put(Items.IRON_INGOT, 8);
        unit.put(Items.COPPER_INGOT, 3);
        unit.put(Items.GOLD_NUGGET, 3);
        unit.put(Items.LEATHER, 4);
        unit.put(Items.NAME_TAG, 14);
        unit.put(Items.PAINTING, 6);
        unit.put(Items.TORCH, 1);
        unit.put(Items.DRIED_KELP, 1);
        unit.put(Items.KELP, 1);
        unit.put(Items.PRISMARINE_CRYSTALS, 4);
        unit.put(Items.PRISMARINE_SHARD, 2);
        unit.put(Items.NAUTILUS_SHELL, 12);
        unit.put(Items.EMERALD, 16);
        unit.put(Items.OXEYE_DAISY, 1);
        unit.put(Items.CORNFLOWER, 1);
        unit.put(Items.COCOA_BEANS, 2);
        unit.put(Items.BAMBOO, 1);
        unit.put(Items.COAL, 3);
        unit.put(Items.GLOW_BERRIES, 3);
        unit.put(Items.COOKIE, 2);
        unit.put(Items.PUMPKIN_PIE, 5);
        unit.put(Items.TERRACOTTA, 2);
        unit.put(Items.SNOWBALL, 1);
        unit.put(Items.CAMPFIRE, 6);
        return unit;
    }

    private record Edge(Item to, double weight) {
    }
}
