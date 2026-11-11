package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristDisplayFormat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public enum TouristEntityJadeProvider implements IEntityComponentProvider, IServerDataProvider<EntityAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        if (!ConfigLoader.jadeTouristTooltip || !config.get(BusinessCraftJadeIds.TOURIST)) {
            return;
        }
        if (!(accessor.getEntity() instanceof TouristEntity tourist)) {
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(JadeTooltipNbt.TOURIST)) {
            return;
        }

        String origin = JadeTouristTooltipHelper.resolveOrigin(data, tourist.getSyncedOriginLabel());
        if (!origin.isEmpty()) {
            tooltip.add(Component.translatable("businesscraft.jade.tourist.origin", origin));
        }

        String dest = JadeTouristTooltipHelper.resolveDestination(data, tourist);
        if (!dest.isEmpty()) {
            tooltip.add(Component.translatable("businesscraft.jade.tourist.destination", dest));
        }

        if (data.contains(JadeTooltipNbt.TRIP_STATUS)) {
            TouristTripStatus status = TouristTripStatus.parseNbt(data.getString(JadeTooltipNbt.TRIP_STATUS));
            tooltip.add(Component.translatable("businesscraft.jade.tourist.trip_status",
                    Component.translatable(status.translationKey())));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, EntityAccessor accessor) {
        if (!ConfigLoader.jadeTouristTooltip) {
            return;
        }
        if (!(accessor.getEntity() instanceof TouristEntity tourist)) {
            return;
        }
        data.putBoolean(JadeTooltipNbt.TOURIST, true);
        data.putString(JadeTooltipNbt.ORIGIN,
                TouristDisplayFormat.sanitize(tourist.getOriginTownName(), "Unknown"));
        data.putString(JadeTooltipNbt.DESTINATION,
                TouristDisplayFormat.sanitize(tourist.getDestinationTownName(), "Unknown"));
        TouristTripStatus status = TouristTripStatus.from(tourist.isPassenger(), tourist.isStationary());
        data.putString(JadeTooltipNbt.TRIP_STATUS, status.toNbt());
    }

    @Override
    public ResourceLocation getUid() {
        return BusinessCraftJadeIds.TOURIST;
    }
}
