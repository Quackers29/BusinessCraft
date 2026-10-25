package com.quackers29.businesscraft.entity.tourist;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        assertTrue(looks.size() >= 6, "expected at least 6 builtin looks");
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
        assertTrue(headwear.size() >= 4, "headwear should vary");
        assertTrue(accessories.size() >= 3, "accessories should vary");
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
}
