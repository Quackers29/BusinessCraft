package com.quackers29.businesscraft.forge.platform;

import com.quackers29.businesscraft.api.RegistryHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Forge implementation of RegistryHelper
 */
public class ForgeRegistryHelper implements RegistryHelper {
    private static final String MOD_ID = "businesscraft";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);

    public static void register(IEventBus modEventBus) {
        System.out.println("DEBUG: Registering BLOCKS DeferredRegister");
        BLOCKS.register(modEventBus);
        System.out.println("DEBUG: Registering ITEMS DeferredRegister");
        ITEMS.register(modEventBus);
        System.out.println("DEBUG: Registering ENTITY_TYPES DeferredRegister");
        ENTITY_TYPES.register(modEventBus);
        System.out.println("DEBUG: Registering BLOCK_ENTITY_TYPES DeferredRegister");
        BLOCK_ENTITY_TYPES.register(modEventBus);
        System.out.println("DEBUG: Registering MENU_TYPES DeferredRegister");
        MENU_TYPES.register(modEventBus);
        System.out.println("DEBUG: All DeferredRegisters registered");
    }

    @Override
    public void registerBlock(String name, Object block) {
        if (block instanceof net.minecraft.world.level.block.Block mcBlock) {
            // Register the block
            BLOCKS.register(name, () -> mcBlock);
            // Also register the block item
            ITEMS.register(name, () -> new net.minecraft.world.item.BlockItem(mcBlock, new net.minecraft.world.item.Item.Properties()));
        }
    }

    @Override
    public void registerBlockItem(String name, Object block) {
        if (block instanceof net.minecraft.world.level.block.Block mcBlock) {
            ITEMS.register(name, () -> new net.minecraft.world.item.BlockItem(mcBlock, new net.minecraft.world.item.Item.Properties()));
        }
    }

    @Override
    public void registerEntityType(String name, Object entityType) {
        if (entityType instanceof net.minecraft.world.entity.EntityType<?> mcEntityType) {
            ENTITY_TYPES.register(name, () -> mcEntityType);
        }
    }

    @Override
    public void registerBlockEntityType(String name, Object blockEntityType) {
        if (blockEntityType instanceof net.minecraft.world.level.block.entity.BlockEntityType<?> mcBlockEntityType) {
            BLOCK_ENTITY_TYPES.register(name, () -> mcBlockEntityType);
        }
    }

    @Override
    public void registerMenuType(String name, Object menuType) {
        if (menuType instanceof net.minecraft.world.inventory.MenuType<?> mcMenuType) {
            MENU_TYPES.register(name, () -> mcMenuType);
        }
    }

    @Override
    public Object getItem(Object location) {
        if (location instanceof net.minecraft.resources.ResourceLocation mcLocation) {
            return net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(mcLocation);
        }
        return null;
    }

    @Override
    public Object getItemKey(Object item) {
        if (item instanceof net.minecraft.world.item.Item mcItem) {
            return net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(mcItem);
        }
        return null;
    }
}
