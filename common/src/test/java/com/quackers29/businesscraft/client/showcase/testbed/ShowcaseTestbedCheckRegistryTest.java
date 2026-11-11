package com.quackers29.businesscraft.client.showcase.testbed;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShowcaseTestbedCheckRegistryTest {

    @Test
    void resolveAll_isTradeContractOnly() {
        List<ShowcaseTestbedCheck> checks = ShowcaseTestbedCheckRegistry.resolve("all");
        assertEquals(1, checks.size());
        assertEquals("trade-contract", checks.get(0).name());
    }

    @Test
    void exampleDevItem_isOptIn() {
        List<ShowcaseTestbedCheck> checks = ShowcaseTestbedCheckRegistry.resolve("example-dev-item");
        assertEquals(1, checks.size());
        assertEquals("example-dev-item", checks.get(0).name());
        assertTrue(ShowcaseTestbedCheckRegistry.allNames().contains("example-dev-item"));
    }

    @Test
    void resolveSingleName() {
        List<ShowcaseTestbedCheck> checks = ShowcaseTestbedCheckRegistry.resolve("trade-contract");
        assertEquals(1, checks.size());
        assertEquals("trade-contract", checks.get(0).name());
    }

    @Test
    void unknownCheckThrows() {
        assertThrows(IllegalArgumentException.class, () -> ShowcaseTestbedCheckRegistry.resolve("not-a-check"));
    }
}
