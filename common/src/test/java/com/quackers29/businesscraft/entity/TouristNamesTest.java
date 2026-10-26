package com.quackers29.businesscraft.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TouristNamesTest {

    @Test
    void pool_isLargeAndMixed() {
        assertTrue(TouristNames.firstNames().size() >= 150);
        assertTrue(TouristNames.surnames().size() >= 40);
        assertTrue(TouristNames.firstNames().contains("Eleanor"));
        assertTrue(TouristNames.firstNames().contains("Arthur"));
        assertTrue(TouristNames.firstNames().contains("Quinn"));
        assertEquals(32.0, TouristNames.NEARBY_BLOCKS);
    }

    @Test
    void placeholder_isOnlyBlankOrTourist() {
        assertTrue(TouristNames.isPlaceholder(null));
        assertTrue(TouristNames.isPlaceholder(""));
        assertTrue(TouristNames.isPlaceholder("  Tourist "));
        assertFalse(TouristNames.isPlaceholder("Eleanor Hart"));
    }

    @Test
    void pick_skipsANearbyFirstName_thenAllowsASurname() {
        Deque<Integer> rolls = new ArrayDeque<>();
        rolls.add(0);
        rolls.add(1);
        rolls.add(1);
        rolls.add(0);
        rolls.add(0);
        String name = TouristNames.pick(bound -> rolls.removeFirst(), Set.of(), Set.of("Ada"));
        assertEquals("Agnes Adler", name);
        assertEquals("Ada", TouristNames.firstOf("Ada Adler"));
    }

    @Test
    void pick_doesNotRepeatTheSameFullNameWhileItHasOtherChoices() {
        Set<String> seen = new HashSet<>();
        int[] cursor = {0};
        for (int i = 0; i < 40; i++) {
            String name = TouristNames.pick(bound -> {
                cursor[0]++;
                return (cursor[0] * 17) % bound;
            }, seen, Set.of());
            assertFalse(seen.contains(name));
            seen.add(name);
        }
    }
}
