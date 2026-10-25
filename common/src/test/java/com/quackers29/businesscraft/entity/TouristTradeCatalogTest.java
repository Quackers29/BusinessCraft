package com.quackers29.businesscraft.entity;

import com.quackers29.businesscraft.economy.JourneyProgress;
import com.quackers29.businesscraft.menu.TouristScreenData;
import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the deals the tourist screen still offers, and the words that replaced
 * the old barrier row and the villager rank "Novice".
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
                "2m 0s",
                "118m 0s",
                true);
    }

    @Test
    void levelOne_keepsTicketAndEightBread_andDropsTheBarrierRow() {
        MerchantOffers offers = TouristTradeCatalog.offersFor(1, brooksideAnyTown(120, 1));
        assertEquals(2, offers.size());
        assertTrue(offers.get(0).getBaseCostA().is(Items.PAPER));
        assertEquals(1, offers.get(0).getBaseCostA().getCount());
        assertTrue(offers.get(0).getResult().is(Items.PAPER));
        assertEquals("Travel Ticket", offers.get(0).getResult().getHoverName().getString());
        assertTrue(offers.get(1).getBaseCostA().is(Items.BREAD));
        assertEquals(8, offers.get(1).getBaseCostA().getCount());
        assertEquals(2, offers.get(1).getResult().getCount());
        assertTrue(offers.get(1).getResult().is(Items.IRON_INGOT));
        for (MerchantOffer offer : offers) {
            assertFalse(offer.getBaseCostA().is(Items.BARRIER));
        }
    }

    @Test
    void higherRanks_keepTheSameBreadRates() {
        MerchantOffers voyager = TouristTradeCatalog.offersFor(3, brooksideAnyTown(1800, 3));
        assertEquals(4, voyager.size());
        assertEquals(6, voyager.get(2).getBaseCostA().getCount());
        assertEquals(4, voyager.get(3).getBaseCostA().getCount());
        MerchantOffers renowned = TouristTradeCatalog.offersFor(5, brooksideAnyTown(6000, 5));
        assertEquals(6, renowned.size());
        assertTrue(renowned.get(4).getResult().is(Items.COMPASS));
        assertTrue(renowned.get(5).getResult().is(Items.CLOCK));
        assertEquals(2, renowned.get(5).getBaseCostA().getCount());
    }

    @Test
    void vanillaScreenshot_stillStartsWithTheBarrierRow() {
        MerchantOffers offers = TouristTradeCatalog.offersForVanillaScreenshot(1, brooksideAnyTown(120, 1));
        assertTrue(offers.get(0).getBaseCostA().is(Items.BARRIER));
        assertEquals(3, offers.size());
    }

    @Test
    void ticketLore_explainsAnyTownAndThatItIsNotARide() {
        String lore = TouristTradeCatalog.ticketLore(brooksideAnyTown(120, 1)).stream()
                .map(line -> line.getString())
                .reduce("", (a, b) -> a + "\n" + b);
        assertTrue(lore.contains("From: Brookside"));
        assertTrue(lore.contains("To: Any Town"));
        assertTrue(lore.contains("not a ride"));
        assertTrue(lore.contains("never walk"));
        assertTrue(lore.contains("first town they reach"));
        assertTrue(lore.contains("Wanderer"));
        assertFalse(lore.contains("Novice"));
    }

    @Test
    void copyUses_survivesATicketWhoseLoreChanged() {
        TouristTradeCatalog.TradeContext first = brooksideAnyTown(120, 1);
        TouristTradeCatalog.TradeContext later = brooksideAnyTown(400, 1);
        MerchantOffers oldOffers = TouristTradeCatalog.offersFor(1, first);
        oldOffers.get(0).increaseUses();
        MerchantOffers fresh = TouristTradeCatalog.offersFor(1, later);
        assertFalse(ItemStack.isSameItemSameTags(oldOffers.get(0).getResult(), fresh.get(0).getResult()));
        TouristTradeCatalog.copyUses(oldOffers, fresh);
        assertTrue(fresh.get(0).isOutOfStock());
        assertFalse(fresh.get(1).isOutOfStock());
    }

    @Test
    void rowState_explainsLockedNeedAndTraded() {
        TouristTradeCatalog.TradeDef compass = TouristTradeCatalog.all().get(4);
        assertEquals(TouristTradeCatalog.RowState.LOCKED,
                TouristTradeCatalog.rowState(3, 8, false, compass));
        assertEquals("Locked · Veteran at 3.0 km", TouristTradeCatalog.lockLabel(compass, THRESHOLDS));
        TouristTradeCatalog.TradeDef iron = TouristTradeCatalog.all().get(1);
        assertEquals(TouristTradeCatalog.RowState.NEED,
                TouristTradeCatalog.rowState(1, 3, false, iron));
        assertEquals("Needs 8 bread · you have 3", TouristTradeCatalog.needLabel(iron, 3));
        assertEquals(TouristTradeCatalog.RowState.TRADED,
                TouristTradeCatalog.rowState(1, 8, true, iron));
        assertEquals(TouristTradeCatalog.RowState.READY,
                TouristTradeCatalog.rowState(1, 8, false, iron));
    }

    @Test
    void progressAndIdentity_useRankNamesNotVillagerLevels() {
        assertEquals("120 m travelled · 380 m to Traveler",
                TouristTradeCatalog.progressLine(120, 1, THRESHOLDS));
        assertEquals("1.8 km travelled · 1.2 km to Veteran",
                TouristTradeCatalog.progressLine(1800, 3, THRESHOLDS));
        assertEquals("6.0 km travelled · Renowned",
                TouristTradeCatalog.progressLine(6000, 5, THRESHOLDS));
        assertEquals("Safari Sightseer",
                TouristTradeCatalog.identityLine("Sightseer", TouristTradeCatalog.lookLabel("safari_sightseer")));
        assertEquals("No town was chosen. They pay the first town they reach.",
                TouristTradeCatalog.destinationDetail("Any Town", true));
        assertEquals("They pay Hillcrest when they arrive.",
                TouristTradeCatalog.destinationDetail("Hillcrest", false));
        assertFalse(JourneyProgress.tierName(1).equals("Novice"));
    }

    @Test
    void screenData_roundTripsAndRebuildsWideDistances() {
        TouristScreenData data = new TouristScreenData(
                "Brookside", "Hillcrest", false, "Sightseer", "Safari Sightseer",
                THRESHOLDS, 3, 40_000, 1 << 2, 90, 120, false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        data.write(buf);
        TouristScreenData read = TouristScreenData.read(buf);
        assertEquals("Brookside", read.origin());
        assertEquals("Hillcrest", read.destination());
        assertFalse(read.anyTown());
        assertEquals("Safari Sightseer", read.lookLabel());
        assertEquals(3, read.level());
        assertEquals(40_000, read.distanceMeters());
        assertEquals(40_000, TouristScreenData.combineDistance(
                TouristScreenData.distanceLo(40_000), TouristScreenData.distanceHi(40_000)));
        // A signed short would turn 40000 into a negative value. Masking restores it.
        int signedLo = (short) TouristScreenData.distanceLo(40_000);
        assertTrue(signedLo < 0);
        assertEquals(40_000, TouristScreenData.combineDistance(signedLo, TouristScreenData.distanceHi(40_000)));
        assertEquals(List.of(500, 1500, 3000, 6000), java.util.Arrays.stream(read.thresholds()).boxed().toList());
    }
}
