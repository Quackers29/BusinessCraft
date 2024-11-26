package com.yourdomain.businesscraft.event;

import com.yourdomain.businesscraft.BusinessCraft;
import com.yourdomain.businesscraft.menu.ModMenuTypes;
import com.yourdomain.businesscraft.screen.CompanyBlockScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = BusinessCraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientModEvents {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.COMPANY_BLOCK_MENU.get(), CompanyBlockScreen::new);
        });
    }
}