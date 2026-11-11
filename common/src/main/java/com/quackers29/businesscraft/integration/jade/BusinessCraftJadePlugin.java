package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class BusinessCraftJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(PlatformInterfaceJadeProvider.INSTANCE, PlatformInterfaceEntity.class);
        registration.registerBlockDataProvider(TownInterfaceJadeProvider.INSTANCE, TownInterfaceEntity.class);
        registration.registerEntityDataProvider(TouristEntityJadeProvider.INSTANCE, TouristEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(PlatformInterfaceJadeProvider.INSTANCE, PlatformInterfaceBlock.class);
        registration.registerBlockComponent(TownInterfaceJadeProvider.INSTANCE, TownInterfaceBlock.class);
        registration.registerEntityComponent(TouristEntityJadeProvider.INSTANCE, TouristEntity.class);
    }
}
