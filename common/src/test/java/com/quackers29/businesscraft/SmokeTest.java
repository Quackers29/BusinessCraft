package com.quackers29.businesscraft;

import com.quackers29.businesscraft.debug.DebugConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the test infrastructure itself works (JUnit 5 on the :common test
 * source set under ForgeGradle). Real coverage is built incrementally by the
 * Test + Docs Loop — see tasks/test_doc_loop.md.
 */
class SmokeTest {

    @Test
    void testInfrastructure_runs() {
        assertTrue(true, "JUnit platform is wired up for :common");
    }

    @Test
    void testClasspath_seesProductionCode() {
        // Doubles as a release guard: FORCE_ALL_DEBUG must never ship enabled.
        assertFalse(DebugConfig.FORCE_ALL_DEBUG,
                "FORCE_ALL_DEBUG must be false in committed code");
    }
}
