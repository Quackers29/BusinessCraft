package com.quackers29.businesscraft.client.showcase;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ensures every BusinessCraft {@code Screen} subclass under {@code ui/} is either
 * targeted by a showcase route or listed as an explicit coverage exclusion.
 */
class ShowcaseRouteCoverageTest {
    private static final Pattern CLASS_LINE = Pattern.compile(
            "public\\s+(?:abstract\\s+)?class\\s+(\\w+)\\s+extends\\s+(\\w+(?:<[^>]+>)?)");

    @Test
    void allScreensCoveredOrExplicitlyExcluded() throws IOException {
        Path uiRoot = Path.of("src/main/java/com/quackers29/businesscraft/ui");
        List<String> uncovered = findUncoveredScreenClasses(uiRoot);
        System.out.println("Showcase routes (" + ShowcaseRouteRegistry.allRoutes().size() + "): "
                + ShowcaseRouteRegistry.allRoutes().stream().map(ShowcaseRoute::name).toList());
        System.out.println("Covered screen classes: " + ShowcaseRouteRegistry.coveredScreenClassNames());
        System.out.println("Explicit exclusions: " + ShowcaseRouteRegistry.coverageExclusions());
        System.out.println("Uncovered Screen subclasses: " + uncovered);
        assertEquals(List.of(), uncovered, "Every Screen subclass must have a route or explicit exclusion");
    }

    static List<String> findUncoveredScreenClasses(Path uiRoot) throws IOException {
        Set<String> covered = ShowcaseRouteRegistry.coveredScreenClassNames();
        Set<String> excluded = ShowcaseRouteRegistry.exclusionReasonsByClass().keySet();
        List<String> screens = new ArrayList<>();
        if (!Files.isDirectory(uiRoot)) {
            return screens;
        }
        try (Stream<Path> paths = Files.walk(uiRoot)) {
            paths.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String simple = readScreenClassName(p);
                    if (simple != null) {
                        screens.add(simple);
                    }
                } catch (IOException ignored) {
                }
            });
        }
        return screens.stream()
                .filter(name -> !covered.contains(name))
                .filter(name -> !excluded.contains(name))
                .filter(name -> !name.equals("BaseTownScreen"))
                .sorted()
                .toList();
    }

    private static String readScreenClassName(Path file) throws IOException {
        for (String line : Files.readAllLines(file)) {
            Matcher matcher = CLASS_LINE.matcher(line.trim());
            if (!matcher.find()) {
                continue;
            }
            String simple = matcher.group(1);
            String parent = matcher.group(2);
            if (parent.startsWith("Screen")
                    || parent.startsWith("AbstractContainerScreen")
                    || parent.startsWith("BCModalGridScreen")
                    || parent.startsWith("BCModalInventoryScreen")
                    || parent.startsWith("BCModalScreen")
                    || parent.startsWith("BCPopupScreen")
                    || parent.startsWith("BaseTownScreen")) {
                return simple;
            }
        }
        return null;
    }

    @Test
    void navigatorWaitSteps_parseTimeouts() {
        assertTrue(ShowcaseNavigator.waitTimeoutTicks("wait-for-screen:TownMapModal:120") >= 100);
    }

    @Test
    void exclusionsAreNonEmptyForLegacyScreens() {
        assertFalse(ShowcaseRouteRegistry.coverageExclusions().isEmpty());
    }
}
