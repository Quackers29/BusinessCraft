package com.quackers29.businesscraft.client.showcase.testbed;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registry of dev testbed checks, addressable by name from {@code tools/showcase.sh testbed}.
 */
public final class ShowcaseTestbedCheckRegistry {
    private static final Map<String, Supplier<ShowcaseTestbedCheck>> CHECKS = new LinkedHashMap<>();

    static {
        register("trade-contract", TradeContractCheck::new);
        register("example-dev-item", ExampleDevItemCheck::new);
    }

    private ShowcaseTestbedCheckRegistry() {
    }

    public static void register(String name, Supplier<ShowcaseTestbedCheck> factory) {
        CHECKS.put(name.toLowerCase(Locale.ROOT), factory);
    }

    public static List<String> allNames() {
        return List.copyOf(CHECKS.keySet());
    }

    public static List<ShowcaseTestbedCheck> resolve(String property) {
        List<String> names = parseNames(property);
        List<ShowcaseTestbedCheck> resolved = new ArrayList<>();
        for (String name : names) {
            Supplier<ShowcaseTestbedCheck> factory = CHECKS.get(name.toLowerCase(Locale.ROOT));
            if (factory == null) {
                throw new IllegalArgumentException("Unknown testbed check: " + name
                        + " (known: " + allNames() + ")");
            }
            resolved.add(factory.get());
        }
        return resolved;
    }

    private static List<String> parseNames(String property) {
        if (property == null || property.isBlank() || "all".equalsIgnoreCase(property.trim())) {
            return allNames();
        }
        List<String> names = new ArrayList<>();
        for (String raw : property.split(",")) {
            String token = raw.trim().toLowerCase(Locale.ROOT);
            if (!token.isEmpty()) {
                names.add(token);
            }
        }
        return names.isEmpty() ? allNames() : List.copyOf(names);
    }

    public static Map<String, String> descriptions() {
        Map<String, String> out = new LinkedHashMap<>();
        out.put("trade-contract", "Sell auction between town A and B: list, detail UI, bid, time-skip completion.");
        out.put("example-dev-item", "Template check for new dev features (always passes; copy to add real asserts).");
        return Collections.unmodifiableMap(out);
    }
}
