package com.quackers29.businesscraft.debug.production;

import com.quackers29.businesscraft.client.showcase.ShowcaseProductionLayout;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless run of the four showcase towns. No Minecraft server.
 */
class ProductionSimulationTest {
    @Test
    void seededRunIsSaneAndRepeatable() throws Exception {
        Path dir = Path.of(System.getProperty(ProductionSimulation.DIR_PROPERTY,
                Path.of("build", "production-sim").toString()));
        int ticks = Integer.parseInt(System.getProperty(ProductionSimulation.TICKS_PROPERTY,
                Integer.toString(ProductionSimulation.DEFAULT_TICKS)));
        long seed = Long.parseLong(System.getProperty(ProductionSimulation.SEED_PROPERTY,
                Long.toString(ProductionSimulation.DEFAULT_SEED)));
        ProductionSimulation.Request request = new ProductionSimulation.Request(ticks, seed, dir);

        ProductionSimulation.Result first = ProductionSimulation.run(request);
        List<List<String>> rows = dataRows(first.eventsCsv());
        List<List<String>> summary = dataRows(first.summaryCsv());
        ProductionSimulation.Result second = ProductionSimulation.run(request);
        assertFalse(rows.isEmpty(), "events csv has data rows");
        for (ShowcaseProductionLayout.TownSpec spec : ShowcaseProductionLayout.TOWNS) {
            assertTrue(rows.stream().anyMatch(row -> spec.name().equals(row.get(2))
                    && ProductionEvent.TOWN_CREATED.equals(row.get(4))), spec.name());
        }
        assertTrue(rows.stream().anyMatch(row -> ProductionEvent.RECIPE_COMPLETED.equals(row.get(4))));
        assertNoDuplicateTickRows(rows);
        assertSummaryMatches(rows, summary);
        assertEquals(identity(rows), identity(dataRows(second.eventsCsv())));
        assertTrue(Files.exists(first.summaryMarkdown()));
    }

    private static List<List<String>> dataRows(Path file) throws Exception {
        List<String> lines = Files.readAllLines(file);
        List<List<String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            if (!lines.get(i).isBlank()) {
                rows.add(Csv.parse(lines.get(i)));
            }
        }
        return rows;
    }

    private static void assertNoDuplicateTickRows(List<List<String>> rows) {
        Set<String> seen = new LinkedHashSet<>();
        for (List<String> row : rows) {
            String key = row.get(1) + "|" + row.get(3) + "|" + row.get(4) + "|" + row.get(5) + "|" + row.get(7);
            assertTrue(seen.add(key), "duplicate row " + key);
        }
    }

    private static void assertSummaryMatches(List<List<String>> events, List<List<String>> summary) {
        Map<String, Double> produced = new LinkedHashMap<>();
        Map<String, Double> consumed = new LinkedHashMap<>();
        Map<String, Long> stalls = new LinkedHashMap<>();
        Map<String, Long> research = new LinkedHashMap<>();
        Map<String, Double> researchCost = new LinkedHashMap<>();
        Map<String, Long> contracts = new LinkedHashMap<>();
        for (List<String> row : events) {
            String type = row.get(4);
            String town = row.get(3);
            Map<String, String> details = details(row.get(7));
            if (ProductionEvent.RECIPE_COMPLETED.equals(type)) {
                add(produced, town, details.get("outputs"));
                add(consumed, town, details.get("inputs"));
            } else if (ProductionEvent.RECIPE_STALLED.equals(type)) {
                stalls.merge(town + "|stall|" + details.getOrDefault("reason", ""), 1L, Long::sum);
            } else if (ProductionEvent.RESEARCH_STARTED.equals(type)) {
                research.merge(town + "|research_bought|" + row.get(5), 1L, Long::sum);
                add(researchCost, town + "|research_cost", details.get("cost"));
            } else if (type.startsWith("contract_")) {
                contracts.merge(town + "|contract|" + type.substring("contract_".length()), 1L, Long::sum);
            }
        }
        for (List<String> row : summary) {
            String town = row.get(1);
            String metric = row.get(2);
            String key = row.get(3);
            String total = row.get(4);
            switch (metric) {
                case "produced" -> assertEquals(total, number(produced.get(town + "|" + key)), row.toString());
                case "consumed" -> assertEquals(total, number(consumed.get(town + "|" + key)), row.toString());
                case "research_cost" -> assertEquals(total, number(researchCost.get(town + "|research_cost|" + key)),
                        row.toString());
                case "stall" -> assertEquals(total, Long.toString(stalls.get(town + "|stall|" + key)), row.toString());
                case "research_bought" -> assertEquals(total,
                        Long.toString(research.get(town + "|research_bought|" + key)), row.toString());
                case "contract" -> assertEquals(total, Long.toString(contracts.get(town + "|contract|" + key)),
                        row.toString());
                default -> throw new AssertionError(metric);
            }
        }
    }

    private static void add(Map<String, Double> into, String prefix, String packed) {
        if (packed == null || packed.isEmpty()) {
            return;
        }
        for (String part : packed.split(",")) {
            int colon = part.indexOf(':');
            if (colon > 0) {
                into.merge(prefix + "|" + part.substring(0, colon),
                        Double.parseDouble(part.substring(colon + 1)), Double::sum);
            }
        }
    }

    private static Map<String, String> details(String packed) {
        Map<String, String> pairs = new LinkedHashMap<>();
        if (packed == null || packed.isEmpty()) {
            return pairs;
        }
        for (String part : packed.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) {
                pairs.put(part.substring(0, eq), part.substring(eq + 1));
            }
        }
        return pairs;
    }

    private static String number(Double value) {
        if (value == null) {
            return null;
        }
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return Long.toString(value.longValue());
        }
        return Float.toString(value.floatValue());
    }

    private static List<String> identity(List<List<String>> rows) {
        List<String> out = new ArrayList<>();
        for (List<String> row : rows) {
            StringBuilder line = new StringBuilder();
            for (int i = 1; i < row.size(); i++) {
                if (i > 1) {
                    line.append('|');
                }
                line.append(row.get(i));
            }
            out.add(line.toString());
        }
        return out;
    }
}
