package com.quackers29.businesscraft.economy;

import com.quackers29.businesscraft.config.ConfigLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Default linear fare table (1 tourist, exponent 1.0, metersPerEmerald = 100).
 */
class TouristPaymentCalculatorTest {

    private int savedMetersPerEmerald;
    private double savedExponent;
    private int savedReferenceBlocks;
    private double savedEmeraldsAtReference;

    @BeforeEach
    void setDefaults() {
        savedMetersPerEmerald = ConfigLoader.metersPerEmerald;
        savedExponent = ConfigLoader.payoutCurveExponent;
        savedReferenceBlocks = ConfigLoader.payoutReferenceBlocks;
        savedEmeraldsAtReference = ConfigLoader.payoutEmeraldsPerTouristAtReference;

        ConfigLoader.metersPerEmerald = 100;
        ConfigLoader.payoutCurveExponent = 1.0;
        ConfigLoader.payoutReferenceBlocks = 1000;
        ConfigLoader.payoutEmeraldsPerTouristAtReference = 10.0;
    }

    @AfterEach
    void restore() {
        ConfigLoader.metersPerEmerald = savedMetersPerEmerald;
        ConfigLoader.payoutCurveExponent = savedExponent;
        ConfigLoader.payoutReferenceBlocks = savedReferenceBlocks;
        ConfigLoader.payoutEmeraldsPerTouristAtReference = savedEmeraldsAtReference;
    }

    @ParameterizedTest(name = "{0} blocks → {1} emeralds (1 tourist)")
    @CsvSource({
            "100, 1",
            "300, 3",
            "500, 5",
            "1000, 10",
            "1500, 15",
            "1999, 19",
            "2000, 20",
    })
    void defaultLinearCurve_singleTourist(int distanceBlocks, int expectedEmeralds) {
        assertEquals(expectedEmeralds, TouristPaymentCalculator.computeBatchPayment(distanceBlocks, 1));
    }

    @ParameterizedTest(name = "batch {0} blocks × {1} tourists → {2} emeralds")
    @CsvSource({
            "500, 2, 10",
            "1000, 3, 30",
            "150, 5, 7",
    })
    void defaultLinearCurve_multiTourist(int distanceBlocks, int tourists, int expectedEmeralds) {
        assertEquals(expectedEmeralds, TouristPaymentCalculator.computeBatchPayment(distanceBlocks, tourists));
    }

    @org.junit.jupiter.api.Test
    void powerCurve_matchesReferencePoint() {
        ConfigLoader.payoutCurveExponent = 2.0;
        ConfigLoader.payoutReferenceBlocks = 1000;
        ConfigLoader.payoutEmeraldsPerTouristAtReference = 10.0;
        // 1000^2 / 1000^2 * 10 = 10
        assertEquals(10, TouristPaymentCalculator.computeBatchPayment(1000, 1));
        // 500 → (0.5)^2 * 10 = 2.5 → batch max(1, 2.5) = 2
        assertEquals(2, TouristPaymentCalculator.computeBatchPayment(500, 1));
    }
}
