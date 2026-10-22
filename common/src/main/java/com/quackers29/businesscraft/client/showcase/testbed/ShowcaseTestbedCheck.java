package com.quackers29.businesscraft.client.showcase.testbed;

import net.minecraft.client.Minecraft;

import java.util.Map;

/**
 * Pluggable showcase testbed check. Checks run sequentially after the two-town world is built.
 */
public interface ShowcaseTestbedCheck {

    /** Stable CLI name (e.g. {@code trade-contract}). */
    String name();

    /** Reset state before {@link #tick(Minecraft, ShowcaseTestbedContext)}. */
    void begin(Minecraft minecraft, ShowcaseTestbedContext context);

    /**
     * Advance one client tick. Return {@code true} when finished (pass or fail recorded via {@link #passed()}).
     */
    boolean tick(Minecraft minecraft, ShowcaseTestbedContext context);

    boolean passed();

    Map<String, Object> details();

    default String failureMessage() {
        return null;
    }
}
