package com.example;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.item.ItemStack;

public class TestMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemStack stack = ItemStack.EMPTY;
        System.out.println("Test mod works: " + stack);
    }
}