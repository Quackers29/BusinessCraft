package com.quackers29.businesscraft.testutil;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Test-only fixture that initializes vanilla Minecraft registries (items,
 * blocks, etc.) without launching the game, so unit tests can construct
 * ItemStacks and other registry-backed objects.
 *
 * Usage (Test + Docs Loop — see tasks/test_doc_loop.md):
 *
 *   @BeforeAll
 *   static void boot() { McBootstrap.init(); }
 *
 * Prefer pure-logic tests without this fixture where possible; use it only
 * when the target genuinely needs ItemStack/registry access. Things that need
 * a live Level/world, entities, or networking still cannot be tested this way
 * (those remain NEEDS-MC).
 */
public final class McBootstrap {

    private static volatile boolean initialized = false;

    private McBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        initialized = true;
    }
}
