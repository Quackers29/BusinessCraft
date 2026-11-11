package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.town.Town;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum TownInterfaceJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!ConfigLoader.jadeTownInterfaceTooltip || !config.get(BusinessCraftJadeIds.TOWN_INTERFACE)) {
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(JadeTooltipNbt.TOWN)) {
            return;
        }
        if (data.contains(JadeTooltipNbt.NAME)) {
            tooltip.add(Component.translatable("businesscraft.jade.town.name", data.getString(JadeTooltipNbt.NAME)));
        }
        if (data.contains(JadeTooltipNbt.PLATFORM_COUNT) && data.contains(JadeTooltipNbt.TOURIST_COUNT)) {
            tooltip.add(Component.translatable("businesscraft.jade.town.summary",
                    data.getInt(JadeTooltipNbt.PLATFORM_COUNT),
                    data.getLong(JadeTooltipNbt.TOURIST_COUNT)));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!ConfigLoader.jadeTownInterfaceTooltip) {
            return;
        }
        BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof TownInterfaceEntity townEntity)) {
            return;
        }
        if (!(accessor.getLevel() instanceof ServerLevel)) {
            return;
        }
        data.putBoolean(JadeTooltipNbt.TOWN, true);
        String name = townEntity.getTownName();
        if (name != null && !name.isBlank()) {
            data.putString(JadeTooltipNbt.NAME, name);
        }
        data.putInt(JadeTooltipNbt.PLATFORM_COUNT, townEntity.getPlatformCount());
        Town town = townEntity.getTown();
        long tourists = town != null ? town.getTouristCount() : 0L;
        data.putLong(JadeTooltipNbt.TOURIST_COUNT, tourists);
    }

    @Override
    public ResourceLocation getUid() {
        return BusinessCraftJadeIds.TOWN_INTERFACE;
    }
}
