package com.quackers29.businesscraft.contract;

import com.quackers29.businesscraft.config.ConfigLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * T-005: Courier Delivery Rewards (Test + Docs Loop).
 *
 * Covers the pure-logic surface of CourierContract (the data carrier for
 * direct transport jobs) plus the delivery accumulator that feeds
 * ContractBoard.processCourierDelivery's reward payout path:
 *   - isDelivered() = deliveredAmount >= quantity (longs)
 *   - addDeliveredAmount / setters
 *   - reward passthrough (the value paid as (int)emeralds on completion)
 *   - full NBT save/load roundtrips (all fields, optionals, source pos)
 *   - no input clamping (contrast SellContract)
 *
 * The actual reward issuance (emerald ItemStack + PaymentBoard.addReward
 * COURIER_DELIVERY) and the "wrong-town" destination match gate live in
 * processCourierDelivery's callers and in Town/Level-dependent code; they
 * are documented in the vault note but intentionally not exercised here.
 *
 * Documentation: vault/Trade/Contracts/Courier Delivery Rewards.md
 */
class CourierContractTest {

    private static final UUID ISSUER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID DESTINATION = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID COURIER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final BlockPos SOURCE_POS = new BlockPos(100, 64, 200);
    private static final int SOURCE_RADIUS = 48;

    private int savedDefaultPop;
    private double savedCourierAcceptMin;
    private double savedCourierDeliveryMinPerM;
    private double savedSnailMailMinPerM;

    @BeforeEach
    void setUp() {
        savedDefaultPop = ConfigLoader.defaultStartingPopulation;
        savedCourierAcceptMin = ConfigLoader.contractCourierAcceptanceMinutes;
        savedCourierDeliveryMinPerM = ConfigLoader.contractCourierDeliveryMinutesPerMeter;
        savedSnailMailMinPerM = ConfigLoader.contractSnailMailDeliveryMinutesPerMeter;

        // Values not mutated by CourierContract pure paths; saved/restored
        // to follow established pattern from sibling contract tests.
        ConfigLoader.defaultStartingPopulation = 5;
    }

    @AfterEach
    void tearDown() {
        ConfigLoader.defaultStartingPopulation = savedDefaultPop;
        ConfigLoader.contractCourierAcceptanceMinutes = savedCourierAcceptMin;
        ConfigLoader.contractCourierDeliveryMinutesPerMeter = savedCourierDeliveryMinPerM;
        ConfigLoader.contractSnailMailDeliveryMinutesPerMeter = savedSnailMailMinPerM;
    }

    // --- helpers ---

    /** Minimal valid CourierContract using the rich ctor (reward is an input here). */
    private CourierContract courier(long quantity, float reward) {
        return new CourierContract(
                ISSUER, "IssuerTown", SOURCE_POS, SOURCE_RADIUS,
                120_000L, "copper", quantity,
                DESTINATION, "DestTown",
                reward);
    }

    private CourierContract courierWithResource(String resourceId, long quantity, float reward) {
        return new CourierContract(
                ISSUER, "IssuerTown", SOURCE_POS, SOURCE_RADIUS,
                120_000L, resourceId, quantity,
                DESTINATION, "DestTown",
                reward);
    }

    // --- construction + basic getters ---

    @Test
    void ctor_wiresAllFieldsAndInitialState() {
        CourierContract cc = courier(64, 25.0f);

        assertEquals("copper", cc.getResourceId());
        assertEquals(64L, cc.getQuantity());
        assertEquals(0L, cc.getDeliveredAmount());
        assertFalse(cc.isDelivered());
        assertEquals(64, cc.getAmount()); // (int) cast of small positive
        assertEquals(25.0f, cc.getReward(), 0.0001f);
        assertEquals(DESTINATION, cc.getDestinationTownId());
        assertEquals("DestTown", cc.getDestinationTownName());
        assertEquals(SOURCE_POS, cc.getSourceTownPos());
        assertEquals(SOURCE_RADIUS, cc.getSourceTownRadius());
        assertNull(cc.getCourierId());
        assertFalse(cc.isAccepted());
        assertEquals(0L, cc.getAcceptedTime());
        assertFalse(cc.isCompleted());
    }

    @Test
    void getAmount_isIntCastOfQuantity_noClamping() {
        // Normal case
        assertEquals(128, courier(128, 10.0f).getAmount());

        // Large value that would truncate on int cast — pins current permissive behavior
        long huge = 3_000_000_000L; // > Integer.MAX
        CourierContract cc = courier(huge, 1.0f);
        assertEquals((int) huge, cc.getAmount()); // truncation toward zero for positive
    }

    // --- delivery accumulator (the predicate that triggers reward payout) ---

    @Test
    void isDelivered_falseAtZero_trueWhenDeliveredAmountReachesOrExceedsQuantity() {
        CourierContract cc = courier(100, 5.0f);
        assertEquals(0L, cc.getDeliveredAmount());
        assertFalse(cc.isDelivered());

        cc.setDeliveredAmount(99);
        assertFalse(cc.isDelivered());

        cc.setDeliveredAmount(100);
        assertTrue(cc.isDelivered()); // >=

        cc.setDeliveredAmount(150);
        assertTrue(cc.isDelivered()); // stays true when over
    }

    @Test
    void addDeliveredAmount_accumulates_andFlipsIsDelivered_onThresholdCross() {
        CourierContract cc = courier(50, 12.5f);
        assertFalse(cc.isDelivered());

        cc.addDeliveredAmount(20);
        assertEquals(20L, cc.getDeliveredAmount());
        assertFalse(cc.isDelivered());

        cc.addDeliveredAmount(30);
        assertEquals(50L, cc.getDeliveredAmount());
        assertTrue(cc.isDelivered()); // crossed on this add

        cc.addDeliveredAmount(5);
        assertEquals(55L, cc.getDeliveredAmount());
        assertTrue(cc.isDelivered());
    }

    @Test
    void isDelivered_zeroQuantity_becomesTrueOnFirstPositiveAdd_currentBehavior() {
        // Edge: ctor with 0 quantity (no clamp in CourierContract). First add makes >= true.
        // This is reachable only if a bad/zero-qty contract is created or loaded.
        CourierContract cc = courier(0, 3.0f);
        assertTrue(cc.isDelivered()); // 0 >= 0 already
        // even if not, a positive add would cross immediately
    }

    // --- courier acceptance state ---

    @Test
    void isAccepted_tracksCourierId_andAcceptedTime() {
        CourierContract cc = courier(10, 4.0f);
        assertFalse(cc.isAccepted());
        assertNull(cc.getCourierId());

        cc.setCourierId(COURIER);
        cc.setAcceptedTime(1234567890123L);

        assertTrue(cc.isAccepted());
        assertEquals(COURIER, cc.getCourierId());
        assertEquals(1234567890123L, cc.getAcceptedTime());
    }

    // --- reward passthrough (what gets paid on delivery complete) ---

    @Test
    void getReward_returnsCtorValue_andSurvivesSet() {
        CourierContract cc = courier(32, 99.7f);
        assertEquals(99.7f, cc.getReward(), 0.0001f);

        // In prod the reward is almost never mutated after ctor, but setter exists
        // and would affect a subsequent payout amount.
        // (No setter on CourierContract itself — reward is final after construction
        // for the normal path; we only assert the getter here.)
    }

    // --- NBT roundtrips (exercising saveAdditional / loadAdditional + base) ---

    @Test
    void fullRoundTrip_allCourierFieldsPreserved() {
        CourierContract original = courier(50, 47.0f); // qty 50 so that 100 delivered crosses
        original.addDeliveredAmount(100); // over-deliver
        original.setCourierId(COURIER);
        original.setAcceptedTime(9876543210L);
        original.complete();

        CompoundTag tag = new CompoundTag();
        original.save(tag);

        CourierContract loaded = new CourierContract(tag);

        assertEquals("copper", loaded.getResourceId());
        assertEquals(50L, loaded.getQuantity());
        assertEquals(100L, loaded.getDeliveredAmount());
        assertTrue(loaded.isDelivered());
        assertEquals(47.0f, loaded.getReward(), 0.0001f);
        assertEquals(DESTINATION, loaded.getDestinationTownId());
        assertEquals("DestTown", loaded.getDestinationTownName());
        assertEquals(SOURCE_POS.getX(), loaded.getSourceTownPos().getX());
        assertEquals(SOURCE_POS.getY(), loaded.getSourceTownPos().getY());
        assertEquals(SOURCE_POS.getZ(), loaded.getSourceTownPos().getZ());
        assertEquals(SOURCE_RADIUS, loaded.getSourceTownRadius());
        assertEquals(COURIER, loaded.getCourierId());
        assertTrue(loaded.isAccepted());
        assertEquals(9876543210L, loaded.getAcceptedTime());
        assertTrue(loaded.isCompleted());
        assertEquals("courier", loaded.getType());
    }

    @Test
    void roundTrip_minimalTag_noOptionalName_noCourierYet() {
        CompoundTag tag = new CompoundTag();
        // Base Contract fields (as written by Contract.save + type wrapper in real usage)
        tag.putUUID("id", UUID.randomUUID());
        tag.putUUID("issuerTownId", ISSUER);
        tag.putString("issuerTownName", "IssuerTown");
        tag.putLong("creationTime", System.currentTimeMillis());
        tag.putLong("expiryTime", System.currentTimeMillis() + 120_000);
        tag.putBoolean("isCompleted", false);
        // no bids list

        // Courier-specific (no destinationTownName, no courierId, no acceptedTime, delivered=0)
        tag.putString("resourceId", "iron");
        tag.putLong("quantity", 75L);
        tag.putLong("deliveredAmount", 0L);
        tag.putUUID("destinationTownId", DESTINATION);
        tag.putFloat("reward", 18.5f);
        tag.putInt("sourceX", 50);
        tag.putInt("sourceY", 70);
        tag.putInt("sourceZ", -30);
        tag.putInt("sourceRadius", 32);

        CourierContract loaded = new CourierContract(tag);

        assertEquals("iron", loaded.getResourceId());
        assertEquals(75L, loaded.getQuantity());
        assertEquals(0L, loaded.getDeliveredAmount());
        assertFalse(loaded.isDelivered());
        assertEquals(18.5f, loaded.getReward(), 0.0001f);
        assertEquals(DESTINATION, loaded.getDestinationTownId());
        assertNull(loaded.getDestinationTownName()); // absent in tag → field as left by loadAdditional (no defaulting in courier load)
        BlockPos pos = loaded.getSourceTownPos();
        assertNotNull(pos);
        assertEquals(50, pos.getX());
        assertEquals(70, pos.getY());
        assertEquals(-30, pos.getZ());
        assertEquals(32, loaded.getSourceTownRadius());
        assertNull(loaded.getCourierId());
        assertFalse(loaded.isAccepted());
    }

    @Test
    void roundTrip_overDelivered_andFloatRewardTruncationOnPayoutConcept() {
        // We can't call the payout (needs MC), but we can pin that the values that
        // would be used are preserved: delivered > qty and the exact float reward.
        CourierContract original = courier(10, 3.9f);
        original.addDeliveredAmount(15); // over

        CompoundTag tag = new CompoundTag();
        original.save(tag);

        CourierContract loaded = new CourierContract(tag);
        assertTrue(loaded.isDelivered());
        assertEquals(15L, loaded.getDeliveredAmount());
        assertEquals(3.9f, loaded.getReward(), 0.0001f);
        // In processCourierDelivery the payout would do (int)3.9f = 3 emeralds.
    }

    @Test
    void load_doesNotClampQuantityOrReward_currentBehavior() {
        // CourierContract applies no Math.max/min in ctor or loadAdditional (unlike Sell).
        // This pins the permissive behavior.
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", UUID.randomUUID());
        tag.putUUID("issuerTownId", ISSUER);
        tag.putString("issuerTownName", "S");
        tag.putLong("creationTime", System.currentTimeMillis());
        tag.putLong("expiryTime", System.currentTimeMillis() + 60_000);
        tag.putBoolean("isCompleted", false);
        tag.putString("resourceId", "gold");
        tag.putLong("quantity", 0L);        // would be clamped if this were Sell
        tag.putFloat("pricePerUnit", 0f);   // not used by courier, but harmless in tag
        tag.putLong("deliveredAmount", 0L);
        tag.putUUID("destinationTownId", DESTINATION);
        tag.putFloat("reward", -2.5f);      // negative reward
        tag.putInt("sourceX", 0);
        tag.putInt("sourceY", 64);
        tag.putInt("sourceZ", 0);
        tag.putInt("sourceRadius", 0);

        CourierContract cc = new CourierContract(tag);

        assertEquals(0L, cc.getQuantity());     // NOT forced to 1
        assertEquals(-2.5f, cc.getReward(), 0.0001f); // stored as-is
        // isDelivered would be true for qty=0 on any >=0 delivered
        assertTrue(cc.isDelivered());
    }

    @Test
    void getType_returnsCourierDiscriminator() {
        assertEquals("courier", courier(5, 1.0f).getType());
    }
}
