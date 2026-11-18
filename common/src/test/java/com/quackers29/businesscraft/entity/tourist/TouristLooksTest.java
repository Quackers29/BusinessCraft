package com.quackers29.businesscraft.entity.tourist;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TouristLooksTest {

    @Test
    void packRoundTrip_keepsEachComponent() {
        int packed = TouristAppearance.pack(7, 3, 5, 4);
        assertEquals(7, TouristAppearance.skin(packed));
        assertEquals(3, TouristAppearance.outfit(packed));
        assertEquals(5, TouristAppearance.headwear(packed));
        assertEquals(4, TouristAppearance.accessory(packed));
    }

    @Test
    void pack_rejectsSentinelCollision() {
        assertThrows(IllegalArgumentException.class, () -> TouristAppearance.pack(255, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> TouristAppearance.pack(-1, 0, 0, 0));
    }

    @Test
    void builtinLooks_areDistinctAndEnough() {
        List<TouristLook> looks = TouristLooks.loadFromClasspath();
        assertTrue(looks.size() >= 16, "expected at least 16 builtin looks");
        Set<String> ids = new HashSet<>();
        Set<Integer> packed = new HashSet<>();
        Set<Integer> headwear = new HashSet<>();
        Set<Integer> accessories = new HashSet<>();
        for (TouristLook look : looks) {
            assertTrue(ids.add(look.id()), "duplicate id " + look.id());
            assertTrue(packed.add(look.packed()), "duplicate appearance " + look.id());
            assertTrue(TouristParts.knownHeadwear(look.headwear()), look.id());
            assertTrue(TouristParts.knownAccessory(look.accessory()), look.id());
            headwear.add(look.headwear());
            accessories.add(look.accessory());
            assertTrue(look.type() != null, look.id() + " should name the matching tourist type");
            assertEquals(look.packed(), TouristAppearance.pack(look.skin(), look.outfit(), look.headwear(), look.accessory()));
        }
        assertTrue(headwear.size() >= 14, "headwear should vary");
        assertTrue(accessories.size() >= 12, "accessories should vary");
        assertFalse(accessories.contains(5), "retired umbrella index");
        assertEquals(TouristAppearance.pack(1, 0, 1, 1), packedOf(looks, "safari_sightseer"));
        assertEquals(TouristAppearance.pack(0, 1, 0, 2), packedOf(looks, "market_shopper"));
        assertEquals(TouristAppearance.pack(2, 2, 3, 4), packedOf(looks, "station_scholar"));
        assertEquals(TouristAppearance.pack(3, 3, 2, 3), packedOf(looks, "trail_hiker"));
        assertEquals(TouristAppearance.pack(4, 4, 4, 0), packedOf(looks, "coast_visitor"));
        assertEquals(TouristAppearance.pack(5, 5, 7, 1), packedOf(looks, "night_luxury"));
        assertEquals(TouristAppearance.pack(6, 6, 6, 4), packedOf(looks, "desert_guide"));
        assertEquals(TouristAppearance.pack(7, 7, 5, 0), packedOf(looks, "festival_guest"));
        assertEquals(TouristAppearance.pack(8, 8, 11, 9), packedOf(looks, "alpine_walker"));
        assertEquals(TouristAppearance.pack(9, 9, 8, 0), packedOf(looks, "rain_caller"));
        assertEquals(TouristAppearance.pack(10, 10, 10, 7), packedOf(looks, "garden_pilgrim"));
        assertEquals(TouristAppearance.pack(11, 11, 12, 6), packedOf(looks, "library_clerk"));
        assertEquals(TouristAppearance.pack(12, 12, 9, 4), packedOf(looks, "harbour_skipper"));
        assertEquals(TouristAppearance.pack(13, 13, 15, 8), packedOf(looks, "bazaar_merchant"));
        assertEquals(TouristAppearance.pack(14, 14, 14, 11), packedOf(looks, "canyon_scout"));
        assertEquals(TouristAppearance.pack(15, 15, 13, 10), packedOf(looks, "station_conductor"));
        assertEquals(TouristAppearance.pack(16, 16, 16, 0), packedOf(looks, "beach_daytripper"));
        assertEquals(TouristAppearance.pack(17, 17, 20, 13), packedOf(looks, "business_traveller"));
        assertEquals(TouristAppearance.pack(18, 18, 23, 17), packedOf(looks, "campus_student"));
        assertEquals(TouristAppearance.pack(19, 19, 17, 15), packedOf(looks, "park_retiree"));
        assertEquals(TouristAppearance.pack(20, 20, 21, 12), packedOf(looks, "field_photographer"));
        assertEquals(TouristAppearance.pack(21, 21, 19, 14), packedOf(looks, "city_cyclist"));
        assertEquals(TouristAppearance.pack(22, 22, 30, 18), packedOf(looks, "summit_hiker"));
        assertEquals(TouristAppearance.pack(23, 23, 24, 4), packedOf(looks, "museum_visitor"));
        assertEquals(TouristAppearance.pack(24, 24, 22, 19), packedOf(looks, "orchard_picker"));
        assertEquals(TouristAppearance.pack(25, 25, 26, 16), packedOf(looks, "rain_commuter"));
        assertEquals(TouristAppearance.pack(26, 26, 25, 6), packedOf(looks, "tea_tourist"));
        assertEquals(TouristAppearance.pack(27, 27, 31, 11), packedOf(looks, "night_painter"));
        assertEquals(TouristAppearance.pack(28, 28, 18, 20), packedOf(looks, "souk_traveller"));
        assertEquals(TouristAppearance.pack(29, 29, 29, 4), packedOf(looks, "rail_surveyor"));
        assertEquals(TouristAppearance.pack(30, 30, 28, 7), packedOf(looks, "spa_guest"));
        assertEquals(TouristAppearance.pack(31, 31, 27, 6), packedOf(looks, "winter_scholar"));
    }

    private static int packedOf(List<TouristLook> looks, String id) {
        for (TouristLook look : looks) {
            if (look.id().equals(id)) {
                return look.packed();
            }
        }
        throw new AssertionError("missing look " + id);
    }

    @Test
    void parse_readsFlatJson() {
        TouristLook look = TouristLook.parse("ignored",
                "{ \"id\": \"market_shopper\", \"order\": 4, \"skin\": 1, \"outfit\": 2, \"headwear\": 0, \"accessory\": 2 }");
        assertEquals("market_shopper", look.id());
        assertEquals(4, look.order());
        assertEquals(TouristAppearance.pack(1, 2, 0, 2), look.packed());
    }

    @Test
    void parse_rejectsMissingPart() {
        assertThrows(IllegalArgumentException.class,
                () -> TouristLook.parse("plain", "{ \"id\": \"plain\", \"skin\": 0 }"));
    }

    @Test
    void pick_usesTheGivenIndex() {
        List<TouristLook> looks = TouristLooks.all();
        TouristLook first = TouristLooks.pick(bound -> 0);
        TouristLook last = TouristLooks.pick(bound -> looks.size() - 1);
        assertEquals(looks.get(0).id(), first.id());
        assertEquals(looks.get(looks.size() - 1).id(), last.id());
        assertEquals(first.id(), TouristLooks.pick(bound -> 0).id());
    }

    @Test
    void pick_reachesEveryBuiltinLookOnce() {
        TouristLooks.replace(TouristLooks.loadFromClasspath());
        List<TouristLook> looks = TouristLooks.all();
        Set<String> picked = new HashSet<>();
        for (int i = 0; i < looks.size(); i++) {
            int index = i;
            TouristLook look = TouristLooks.pick(bound -> index);
            assertTrue(picked.add(look.id()), "duplicate pick " + look.id());
            assertEquals(looks.get(index).id(), look.id());
        }
        assertEquals(looks.size(), picked.size());
        assertTrue(picked.contains("beach_daytripper"));
        assertTrue(picked.contains("winter_scholar"));
    }
}
