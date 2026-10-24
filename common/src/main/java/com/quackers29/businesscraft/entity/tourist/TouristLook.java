package com.quackers29.businesscraft.entity.tourist;

import com.quackers29.businesscraft.entity.TouristType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One named combination of skin, outfit, headwear, and accessory.
 * Parsed from flat JSON so a datapack can add a look without Java.
 */
public final class TouristLook {
    private static final Pattern ID_FIELD = Pattern.compile("\"id\"\\s*:\\s*\"([a-z0-9_]+)\"");
    private static final Pattern ID_VALUE = Pattern.compile("[a-z0-9_]+");

    private final String id;
    private final int order;
    private final int skin;
    private final int outfit;
    private final int headwear;
    private final int accessory;
    private final TouristType type;
    private final int packed;

    public TouristLook(String id, int order, int skin, int outfit, int headwear, int accessory) {
        this(id, order, skin, outfit, headwear, accessory, null);
    }

    public TouristLook(String id, int order, int skin, int outfit, int headwear, int accessory, TouristType type) {
        if (id == null || !ID_VALUE.matcher(id).matches()) {
            throw new IllegalArgumentException("tourist look id must match [a-z0-9_]+, was " + id);
        }
        this.id = id;
        this.order = order;
        this.skin = skin;
        this.outfit = outfit;
        this.headwear = headwear;
        this.accessory = accessory;
        this.type = type;
        this.packed = TouristAppearance.pack(skin, outfit, headwear, accessory);
    }

    public static TouristLook parse(String fallbackId, String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("empty tourist look json");
        }
        String id = stringField(json, ID_FIELD, fallbackId);
        int order = intField(json, "order", 1000);
        int skin = requiredInt(json, "skin");
        int outfit = requiredInt(json, "outfit");
        int headwear = requiredInt(json, "headwear");
        int accessory = requiredInt(json, "accessory");
        return new TouristLook(id, order, skin, outfit, headwear, accessory, typeField(json));
    }

    public String id() {
        return id;
    }

    public int order() {
        return order;
    }

    public int skin() {
        return skin;
    }

    public int outfit() {
        return outfit;
    }

    public int headwear() {
        return headwear;
    }

    public int accessory() {
        return accessory;
    }

    /** Flavour label that matches this costume. Null keeps a random type. */
    public TouristType type() {
        return type;
    }

    public int packed() {
        return packed;
    }

    private static String stringField(String json, Pattern pattern, String fallback) {
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (fallback == null || !ID_VALUE.matcher(fallback).matches()) {
            throw new IllegalArgumentException("tourist look is missing a valid id");
        }
        return fallback;
    }

    private static int requiredInt(String json, String key) {
        Integer value = findInt(json, key);
        if (value == null) {
            throw new IllegalArgumentException("tourist look is missing " + key);
        }
        return value;
    }

    private static int intField(String json, String key, int fallback) {
        Integer value = findInt(json, key);
        return value == null ? fallback : value;
    }

    private static TouristType typeField(String json) {
        Matcher matcher = Pattern.compile("\"type\"\\s*:\\s*\"([A-Za-z_]+)\"").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        try {
            return TouristType.valueOf(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("unknown tourist type " + matcher.group(1));
        }
    }

    private static Integer findInt(String json, String key) {
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*(-?\\d+)").matcher(json);
        if (!matcher.find()) {
            return null;
        }
        return Integer.parseInt(matcher.group(1));
    }
}
