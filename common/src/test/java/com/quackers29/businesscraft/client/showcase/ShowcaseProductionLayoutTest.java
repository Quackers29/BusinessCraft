package com.quackers29.businesscraft.client.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShowcaseProductionLayoutTest {
    @Test
    void townsAreLegalDistanceApart() {
        assertTrue(ShowcaseProductionLayout.minDistance() >= ShowcaseProductionLayout.MIN_TOWN_DISTANCE,
                "closest pair is " + ShowcaseProductionLayout.minDistance());
    }

    @Test
    void fourUniquelyNamedTownsInTheFourStates() {
        Set<String> names = new HashSet<>();
        ShowcaseProductionLayout.TOWNS.forEach(town -> assertTrue(names.add(town.name()), town.name()));
        assertEquals(4, ShowcaseProductionLayout.TOWNS.size());
        var starter = ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.STARTER);
        assertTrue(starter.unlocks().isEmpty() && starter.stock().isEmpty(), "starter keeps its biome kit");
        var starving = ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.STARVING);
        assertTrue(starving.stock().stream().filter(s -> s.resource().equals("food"))
                .allMatch(s -> s.percentOfCap() < 20), "starving town is short of food");
        assertTrue(ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.MIDGAME).unlocks().size()
                > ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.FARM).unlocks().size());
    }

    @Test
    void stockIsAPercentageOfTheCap() {
        for (var town : ShowcaseProductionLayout.TOWNS) {
            town.stock().forEach(s -> assertTrue(s.percentOfCap() >= 0 && s.percentOfCap() <= 100, town.name()));
            assertTrue(town.workUnitPercent() <= 100, town.name());
            town.unlocks().forEach(u -> assertTrue(u.level() >= 1, town.name()));
        }
    }

    @Test
    void seedNodesExistInBundledUpgrades() throws Exception {
        Set<String> ids = bundledUpgradeIds();
        assertTrue(ids.contains("farming_advanced"), "bundled upgrades.csv should name farming_advanced");
        for (var town : ShowcaseProductionLayout.TOWNS) {
            for (var unlock : town.unlocks()) {
                assertTrue(ids.contains(unlock.node()),
                        town.name() + " seeds unknown upgrade node " + unlock.node());
            }
        }
    }

    /** First column of the shipped upgrades.csv. This is the file a fresh install copies. */
    private static Set<String> bundledUpgradeIds() throws Exception {
        try (InputStream in = ShowcaseProductionLayoutTest.class
                .getResourceAsStream("/assets/businesscraft/config/upgrades.csv")) {
            assertNotNull(in, "bundled upgrades.csv is missing from the test classpath");
            Set<String> ids = new HashSet<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                reader.readLine();
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    String id = trimmed.split(",", 2)[0].trim();
                    if (!id.isEmpty()) {
                        ids.add(id);
                    }
                }
            }
            return ids;
        }
    }

    @Test
    void signsFitTheSign() {
        for (var town : ShowcaseProductionLayout.TOWNS) {
            assertTrue(town.sign().size() <= 4, town.name());
            town.sign().forEach(line -> assertTrue(line.length() <= ShowcaseProductionLayout.SIGN_LINE_MAX, line));
        }
    }
}
