package com.quackers29.businesscraft.network;

import com.quackers29.businesscraft.testutil.McBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerItemExchangeTest {
    @BeforeAll
    static void boot() {
        McBootstrap.init();
    }

    @Test
    void tryTakeFrom_removesOnlyWhenTheFullCountIsPresent() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Items.EMERALD, 10));
        stacks.add(new ItemStack(Items.DIAMOND, 4));

        assertFalse(PlayerItemExchange.tryTakeFrom(stacks, Items.EMERALD, 11));
        assertEquals(10, stacks.get(0).getCount());

        assertTrue(PlayerItemExchange.tryTakeFrom(stacks, Items.EMERALD, 6));
        assertEquals(4, stacks.get(0).getCount());
        assertEquals(4, stacks.get(1).getCount());
    }

    @Test
    void tryTakeFrom_splitsAcrossStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(new ItemStack(Items.BREAD, 2));
        stacks.add(new ItemStack(Items.BREAD, 3));

        assertTrue(PlayerItemExchange.tryTakeFrom(stacks, Items.BREAD, 4));
        assertTrue(stacks.get(0).isEmpty());
        assertEquals(1, stacks.get(1).getCount());
    }
}
