package com.quackers29.businesscraft.data.parsers;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataParser.class);

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
                if (part.contains("*")) {
                    String[] kv = part.split("\\*");
                    if (kv.length == 2) {
                        String key = kv[0].trim();
                        try {
                            float val = Float.parseFloat(kv[1].trim());
                            effects.add(new Effect(key, val, false));
                            continue;
                        } catch (NumberFormatException e) {
                            LOGGER.warn("Invalid effect modifier value: {}", part);
                        }
                    }
                }

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
                effects.add(new Effect(key, val, isPct));
            } catch (NumberFormatException e) {
                LOGGER.warn("Invalid effect value: {}", part);
            }
        }
        return effects;
    }

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
                operator = "=";
            }

            if (!operator.isEmpty() && !operator.equals("=")) {
                valueStr = expr.substring(operator.length()).trim();
            }

            boolean isPct = valueStr.endsWith("%");

            conditions.add(new Condition(key, operator, valueStr, isPct));
        }
        return conditions;
    }

    public static class ResourceAmount {
        public String resourceId;
        public float amount;
        public String amountExpression; // Stores dynamic expression

        public ResourceAmount(String r, float a) {
            this.resourceId = r;
            this.amount = a;
            this.amountExpression = String.valueOf(a);
        }

        public ResourceAmount(String r, String expr) {
            this.resourceId = r;
            this.amountExpression = expr;
            try {
                this.amount = Float.parseFloat(expr);
            } catch (NumberFormatException e) {
                this.amount = 0f; // Dynamic
            }
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
                String key = kv[0].trim();
                if (key.equals("population"))
                    key = "pop";
                else if (key.equals("population_cap"))
                    key = "pop_cap";
                else if (key.equals("tourist_count"))
                    key = "tourist";
                list.add(new ResourceAmount(key, kv[1].trim()));
            }
        }
        return list;
    }
}
