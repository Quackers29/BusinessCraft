package com.quackers29.businesscraft.testutil;

import com.quackers29.businesscraft.town.data.ClaimStatus;
import com.quackers29.businesscraft.town.data.RewardEntry;
import com.quackers29.businesscraft.town.data.RewardSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the McBootstrap fixture against a previously NEEDS-MC target:
 * ItemStack construction and RewardEntry claim-eligibility logic (T-012).
 * Full T-012 coverage is delivered by the Test + Docs Loop; this class only
 * proves the fixture unblocks that work.
 */
class McBootstrapValidationTest {

    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @Test
    void itemStack_canBeConstructedAfterBootstrap() {
        ItemStack stack = new ItemStack(Items.EMERALD, 5);
        assertEquals(5, stack.getCount());
        assertFalse(stack.isEmpty());
    }

    @Test
    void rewardEntry_allEligibility_claimableByAnyone() {
        RewardEntry entry = new RewardEntry(
                RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 3)), "ALL");
        assertTrue(entry.canBeClaimed("any-player-uuid"));
    }

    @Test
    void rewardEntry_personalEligibility_onlyExactMatchCanClaim() {
        RewardEntry entry = new RewardEntry(
                RewardSource.COURIER_DELIVERY, List.of(new ItemStack(Items.IRON_INGOT, 4)), "p-uuid-123");
        assertTrue(entry.canBeClaimed("p-uuid-123"));
        assertFalse(entry.canBeClaimed("someone-else"));
    }

    @Test
    void rewardEntry_claimedOrExpired_notClaimable() {
        RewardEntry claimed = new RewardEntry(
                RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        claimed.setStatus(ClaimStatus.CLAIMED);
        assertFalse(claimed.canBeClaimed("anyone"));

        RewardEntry expired = new RewardEntry(
                RewardSource.TOURIST_ARRIVAL, List.of(new ItemStack(Items.EMERALD, 1)), "ALL");
        expired.setExpirationTime(System.currentTimeMillis() - 1);
        assertFalse(expired.canBeClaimed("anyone"));
    }
}
