package com.quackers29.businesscraft.data.parsers;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataParser.class);

    // Parses string like "storage_cap_all:200;happiness:50%"
    public static List<Effect> parseEffects(String packed) {
        List<Effect> effects = new ArrayList<>();
        if (packed == null || packed.isEmpty())
            return effects;

        String[] parts = packed.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty())
                continue;

            if (!part.contains(":")) {
                // Special case: prod_id unlock (no value)
                // Treat as value 1 (boolean true)
                effects.add(new Effect(part, 1.0f, false));
                continue;
            }

            String[] kv = part.split(":");
            if (kv.length != 2) {
                LOGGER.warn("Invalid effect format: {}", part);
                continue;
            }

            String key = kv[0].trim();
            String valStr = kv[1].trim();
            boolean isPct = valStr.endsWith("%");

            try {
                if (isPct) {
                    valStr = valStr.substring(0, valStr.length() - 1);
                }
                float val = Float.parseFloat(valStr);
                // If it's percentage, we might want to store it as 1.20 (for +20%) or keep as
                // 20?
                // The plan says: "pop_cap:15% -> x1.15".
                // Let's store raw value here (15) and handle logic in Component.
                // Wait, logic says "basic_farming-time:-30%"
                effects.add(new Effect(key, val, isPct));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid effect value: {}", part);
            }
        }
        return effects;
    }

    // Parses string like "happiness:>60;pop:<pop_cap"
    public static List<Condition> parseConditions(String packed) {
        List<Condition> conditions = new ArrayList<>();
        if (packed == null || packed.isEmpty())
            return conditions;

        String[] parts = packed.split(";");
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty())
                continue;

            String[] kv = part.split(":");
            if (kv.length != 2) {
                LOGGER.warn("Invalid condition format: {}", part);
                continue;
            }

            String key = kv[0].trim();
            String expr = kv[1].trim();

            String operator = "";
            String valueStr = expr;

            if (expr.startsWith(">="))
                operator = ">=";
            else if (expr.startsWith("<="))
                operator = "<=";
            else if (expr.startsWith(">"))
                operator = ">";
            else if (expr.startsWith("<"))
                operator = "<";
            else if (expr.startsWith("="))
                operator = "=";
            else {
                // Default or error? Assuming usage like "pop*food" isn't a condition but input.
                // For conditions, assume generic equals if no op?
                operator = "=";
            }

            if (!operator.isEmpty() && !operator.equals("=")) {
                valueStr = expr.substring(operator.length()).trim();
            }

            // Check for percentage logic in value (e.g. 95%pop_cap)
            // But here we likely just want to parse the structure.
            // Complex parsing might be needed for "95%pop_cap"
            // Let's just store the string value for now and let the engine interpret it.
            // But wait, the Condition class asks for isPercentage.
            boolean isPct = valueStr.endsWith("%");
            // This % flag might be misleading if the value is "pop_cap", not a number.
            // Actually, plan example: "pop:<=95%pop_cap".
            // So value is "95%pop_cap".

            conditions.add(new Condition(key, operator, valueStr, isPct));
        }
        return conditions;
    }

    // Parses "wood:4;iron:2"
    public static class ResourceAmount {
        public String resourceId;
        public float amount;

        public ResourceAmount(String r, float a) {
            this.resourceId = r;
            this.amount = a;
        }
    }

    public static List<ResourceAmount> parseResources(String packed) {
        List<ResourceAmount> list = new ArrayList<>();
        if (packed == null || packed.isEmpty())
            return list;

        String[] parts = packed.split(";");
        for (String part : parts) {
            String[] kv = part.split(":");
            if (kv.length == 2) {
                try {
                    list.add(new ResourceAmount(kv[0].trim(), Float.parseFloat(kv[1].trim())));
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid resource amount: {}", part);
                }
            }
        }
        return list;
    }
}
