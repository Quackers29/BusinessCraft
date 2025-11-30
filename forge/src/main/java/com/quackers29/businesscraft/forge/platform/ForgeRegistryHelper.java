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
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Forge implementation of RegistryHelper using strong types.
 */
public class ForgeRegistryHelper implements RegistryHelper {
    private static final String MOD_ID = "businesscraft";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister
            .create(ForgeRegistries.ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister
            .create(ForgeRegistries.BLOCK_ENTITY_TYPES, MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            MOD_ID);

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
    public <T extends Block> Supplier<T> registerBlock(String name, Supplier<T> block) {
        return BLOCKS.register(name, block);
    }

    @Override
    public <T extends Item> Supplier<T> registerBlockItem(String name, Supplier<? extends Block> block) {
        RegistryObject<Item> registered = ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return () -> (T) registered.get();
    }

    @Override
    public <T extends EntityType<?>> Supplier<T> registerEntityType(String name, Supplier<T> entityType) {
        return ENTITY_TYPES.register(name, entityType);
    }

    @Override
    public <T extends BlockEntityType<?>> Supplier<T> registerBlockEntityType(String name,
            Supplier<T> blockEntityType) {
        return BLOCK_ENTITY_TYPES.register(name, blockEntityType);
    }

    @Override
    public <T extends MenuType<?>> Supplier<T> registerMenuType(String name, Supplier<T> menuType) {
        return MENU_TYPES.register(name, menuType);
    }

    @Override
    public <T extends net.minecraft.world.inventory.AbstractContainerMenu> Supplier<MenuType<T>> registerExtendedMenuType(
            String name, MenuFactory<T> factory) {
        return MENU_TYPES.register(name, () -> net.minecraftforge.common.extensions.IForgeMenuType.create(
                (windowId, inv, data) -> factory.create(windowId, inv, data)));
    }

    @Override
    public Item getItem(ResourceLocation location) {
        return ForgeRegistries.ITEMS.getValue(location);
    }

    @Override
    public ResourceLocation getItemKey(Item item) {
        return ForgeRegistries.ITEMS.getKey(item);
    }

    @Override
    public Iterable<Item> getItems() {
        return ForgeRegistries.ITEMS;
    }
}
