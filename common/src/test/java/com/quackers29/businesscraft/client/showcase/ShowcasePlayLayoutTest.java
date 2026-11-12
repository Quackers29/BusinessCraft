package com.quackers29.businesscraft.client.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShowcasePlayLayoutTest {
    @Test
    void townsAreLegalDistanceApart() {
        assertTrue(ShowcasePlayLayout.minDistance() >= ShowcasePlayLayout.MIN_TOWN_DISTANCE,
                "closest pair is " + ShowcasePlayLayout.minDistance());
    }

    @Test
    void townNamesAreUnique() {
        Set<String> names = new HashSet<>();
        ShowcasePlayLayout.TOWNS.forEach(town -> assertTrue(names.add(town.name()), town.name()));
        assertTrue(ShowcasePlayLayout.TOWNS.size() >= 3);
    }

    @Test
    void signsFitTheSign() {
        for (ShowcasePlayLayout.SignSpec sign : ShowcasePlayLayout.SIGNS) {
            assertTrue(sign.lines().size() <= 4, "too many lines: " + sign.lines());
            assertTrue(sign.rotation() >= 0 && sign.rotation() < 16);
            assertTrue(sign.town() >= 0 && sign.town() < ShowcasePlayLayout.TOWNS.size());
            for (String line : sign.lines()) {
                assertTrue(line.length() <= ShowcasePlayLayout.SIGN_LINE_MAX, "sign line too long: " + line);
            }
        }
    }

    @Test
    void signsDoNotShareABlock() {
        Set<String> cells = new HashSet<>();
        for (ShowcasePlayLayout.SignSpec sign : ShowcasePlayLayout.SIGNS) {
            var town = ShowcasePlayLayout.TOWNS.get(sign.town());
            assertTrue(cells.add((town.dx() + sign.dx()) + "," + (town.dz() + sign.dz())), "overlap " + sign);
        }
    }

    @Test
    void segmentsRunNorthBetweenTownStopsInsideTheirPlatformBoxes() {
        for (ShowcasePlayLayout.Segment segment : ShowcasePlayLayout.SEGMENTS) {
            int depart = ShowcasePlayLayout.departZ(segment);
            int arrive = ShowcasePlayLayout.arriveZ(segment);
            assertTrue(arrive < depart, "cart drives toward negative z");
            assertEquals(ShowcasePlayLayout.trackX(segment.from()), ShowcasePlayLayout.trackX(segment.to()));
            int fromZ = ShowcasePlayLayout.TOWNS.get(segment.from()).dz();
            int toZ = ShowcasePlayLayout.TOWNS.get(segment.to()).dz();
            assertTrue(Math.abs(depart - fromZ) <= ShowcasePlayLayout.LINE_HALF, "departure inside origin platform box");
            assertTrue(Math.abs(arrive - toZ) <= ShowcasePlayLayout.LINE_HALF, "arrival inside destination platform box");
            // The track must stay clear of both town pads (half width 5).
            assertTrue(depart < fromZ - 5 && arrive > toZ + 5);
        }
    }

    @Test
    void guidePagesAreShortEnoughForABook() {
        assertFalse(ShowcasePlayLayout.guidePages().isEmpty());
        ShowcasePlayLayout.guidePages().forEach(page -> assertTrue(page.length() <= 256, page));
    }
}
