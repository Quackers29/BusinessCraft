package com.quackers29.businesscraft.debug.production;

import java.util.Map;
import java.util.TreeMap;

/** {@code key=value;key=value} details. Keys are sorted so a seeded run repeats. */
public final class Detail {
    private Detail() {
    }

    public static String of(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }
        TreeMap<String, String> sorted = new TreeMap<>();
        for (Map.Entry<String, String> entry : pairs.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty()) {
                continue;
            }
            sorted.put(sanitize(entry.getKey()), sanitize(entry.getValue()));
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            if (out.length() > 0) {
                out.append(';');
            }
            out.append(entry.getKey()).append('=').append(entry.getValue());
        }
        return out.toString();
    }

    /** {@code id:amount,id:amount} with ids sorted. */
    public static String amounts(Map<String, Long> amounts) {
        if (amounts == null || amounts.isEmpty()) {
            return "";
        }
        TreeMap<String, Long> sorted = new TreeMap<>();
        for (Map.Entry<String, Long> entry : amounts.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isEmpty() || entry.getValue() == null) {
                continue;
            }
            sorted.merge(sanitize(entry.getKey()), entry.getValue(), Long::sum);
        }
        StringBuilder out = new StringBuilder();
        for (Map.Entry<String, Long> entry : sorted.entrySet()) {
            if (out.length() > 0) {
                out.append(',');
            }
            out.append(entry.getKey()).append(':').append(entry.getValue());
        }
        return out.toString();
    }

    public static String number(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "";
        }
        if (value == Math.rint(value) && Math.abs(value) < 1.0e15) {
            return Long.toString((long) value);
        }
        return Float.toString((float) value);
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace(';', ' ').replace('\n', ' ').replace('\r', ' ');
    }
}
