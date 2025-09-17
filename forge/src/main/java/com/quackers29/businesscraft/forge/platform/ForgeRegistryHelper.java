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
    public void registerBlock(String name, Block block) {
        // Register the block
        BLOCKS.register(name, () -> block);
        // Also register the block item
        ITEMS.register(name, () -> new BlockItem(block, new Item.Properties()));
    }

    @Override
    public void registerBlockItem(String name, Block block) {
        ITEMS.register(name, () -> new BlockItem(block, new Item.Properties()));
    }

    @Override
    public void registerEntityType(String name, EntityType<?> entityType) {
        ENTITY_TYPES.register(name, () -> entityType);
    }

    @Override
    public void registerBlockEntityType(String name, BlockEntityType<?> blockEntityType) {
        BLOCK_ENTITY_TYPES.register(name, () -> blockEntityType);
    }

    @Override
    public void registerMenuType(String name, MenuType<?> menuType) {
        MENU_TYPES.register(name, () -> menuType);
    }

    @Override
    public Item getItem(ResourceLocation location) {
        return ForgeRegistries.ITEMS.getValue(location);
    }

    @Override
    public ResourceLocation getItemKey(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }
}
