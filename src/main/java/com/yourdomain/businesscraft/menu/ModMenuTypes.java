package com.yourdomain.businesscraft.menu;

import com.yourdomain.businesscraft.BusinessCraft;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES,
            BusinessCraft.MOD_ID);

    public static final RegistryObject<MenuType<CompanyBlockMenu>> COMPANY_BLOCK_MENU = MENUS.register("company_block",
            () -> IForgeMenuType.create(CompanyBlockMenu::new));
}