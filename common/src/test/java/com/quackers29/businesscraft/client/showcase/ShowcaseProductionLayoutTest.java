package com.quackers29.businesscraft.client.showcase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ShowcaseProductionLayoutTest {
    @Test
    void townsAreAShortWalkApartAndDoNotOverlap() {
        assertEquals(20, ShowcaseProductionLayout.minDistance());
        assertTrue(ShowcaseProductionLayout.minDistance() >= ShowcaseProductionLayout.MIN_TOWN_DISTANCE);
        int radius = ShowcaseProductionLayout.BORDER_RADIUS;
        assertEquals(10, radius);
        assertTrue(ShowcaseProductionLayout.minDistance() >= radius * 2,
                "two showcase borders would overlap");
        var farm = ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.FARM);
        var high = ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.MIDGAME);
        var hungry = ShowcaseProductionLayout.TOWNS.get(ShowcaseProductionLayout.STARVING);
        assertEquals(20, farm.dx());
        assertEquals(0, farm.dz());
        assertEquals(0, high.dx());
        assertEquals(-20, high.dz());
        assertEquals(20, hungry.dx());
        assertEquals(-20, hungry.dz());
    }

    @Test
    void signsStayInsideTheirOwnBorder() {
        int radius = ShowcaseProductionLayout.BORDER_RADIUS;
        int signDx = ShowcaseProductionLayout.SIGN_DX;
        int signDz = ShowcaseProductionLayout.SIGN_DZ;
        assertTrue(signDx * signDx + signDz * signDz <= radius * radius, "sign is outside its town");
        for (var town : ShowcaseProductionLayout.TOWNS) {
            int sx = town.dx() + signDx;
            int sz = town.dz() + signDz;
            for (var other : ShowcaseProductionLayout.TOWNS) {
                if (other == town) {
                    continue;
                }
                int dx = sx - other.dx();
                int dz = sz - other.dz();
                assertTrue(dx * dx + dz * dz > radius * radius,
                        town.name() + " sign sits inside " + other.name());
            }
        }
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
    void productionConfigEnablesContractsAndShowcaseBorderOnly() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if (!Files.exists(root.resolve("tools/showcase-play.sh"))) {
            root = root.getParent();
        }
        assertTrue(Files.exists(root.resolve("tools/showcase-play.sh")), "repo root");
        Path dir = Files.createTempDirectory("bc-prod-config");
        Process process = new ProcessBuilder("bash", "-c",
                "source tools/showcase-play.sh && write_server_config production '" + dir + "'")
                .directory(root.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), output);
        assertEquals(0, process.exitValue(), output);

        String toml = Files.readString(dir.resolve("config/businesscraft.toml"));
        assertEquals("true", sectionValue(toml, "contracts", "enabled"));
        assertEquals("true", sectionValue(toml, "production", "enabled"));
        assertEquals("true", sectionValue(toml, "research", "enabled"));
        assertEquals("false", sectionValue(toml, "trading", "enabled"));
        assertEquals("10", sectionValue(toml, "general", "defaultBorderRadius"));

        String bundled = Files.readString(root.resolve(
                "common/src/main/resources/assets/businesscraft/config/businesscraft.toml"));
        assertEquals("false", sectionValue(bundled, "contracts", "enabled"));
        assertEquals("50", sectionValue(bundled, "general", "defaultBorderRadius"));
    }

    private static String sectionValue(String toml, String section, String key) {
        String current = "";
        for (String line : toml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("[") && trimmed.endsWith("]") && !trimmed.startsWith("[[")) {
                current = trimmed.substring(1, trimmed.length() - 1);
            }
            if (current.equals(section) && trimmed.startsWith(key + " ") || current.equals(section)
                    && trimmed.startsWith(key + "=")) {
                int eq = trimmed.indexOf('=');
                if (eq > 0) {
                    return trimmed.substring(eq + 1).trim();
                }
            }
        }
        return "";
    }

    @Test
    void signsFitTheSign() {
        for (var town : ShowcaseProductionLayout.TOWNS) {
            assertTrue(town.sign().size() <= 4, town.name());
            town.sign().forEach(line -> assertTrue(line.length() <= ShowcaseProductionLayout.SIGN_LINE_MAX, line));
        }
    }
}
