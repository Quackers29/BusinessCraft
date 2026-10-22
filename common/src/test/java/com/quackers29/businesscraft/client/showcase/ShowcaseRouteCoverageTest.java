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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lists BusinessCraft {@code Screen} subclasses under {@code ui/} that no showcase route targets.
 */
class ShowcaseRouteCoverageTest {
    private static final Pattern CLASS_LINE = Pattern.compile(
            "public\\s+(?:abstract\\s+)?class\\s+(\\w+)\\s+extends\\s+(\\w+(?:<[^>]+>)?)");

    @Test
    void documentsUncoveredScreens() throws IOException {
        Path uiRoot = Path.of("src/main/java/com/quackers29/businesscraft/ui");
        List<String> uncovered = findUncoveredScreenClasses(uiRoot);
        System.out.println("Showcase routes: " + ShowcaseRouteRegistry.allRoutes().stream().map(ShowcaseRoute::name).toList());
        System.out.println("Covered screen classes: " + ShowcaseRouteRegistry.coveredScreenClassNames());
        System.out.println("Uncovered Screen subclasses: " + uncovered);
        assertFalse(ShowcaseRouteRegistry.allRoutes().isEmpty());
    }

    static List<String> findUncoveredScreenClasses(Path uiRoot) throws IOException {
        Set<String> covered = ShowcaseRouteRegistry.coveredScreenClassNames();
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
}
