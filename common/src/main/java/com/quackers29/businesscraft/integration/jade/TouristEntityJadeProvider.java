package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.entity.TouristEntity;
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
        String origin = tourist.getSyncedOriginLabel();
        String dest = tourist.getSyncedDestLabel();
        if (origin != null && !origin.isBlank()) {
            tooltip.add(Component.translatable("businesscraft.jade.tourist.origin", origin));
        }
        if (dest != null && !dest.isBlank()) {
            tooltip.add(Component.translatable("businesscraft.jade.tourist.destination", dest));
        } else if (tourist.isAnyTownDestination()) {
            tooltip.add(Component.translatable("businesscraft.jade.tourist.destination",
                    TouristEntity.ANY_TOWN_NAME));
        }

        CompoundTag data = accessor.getServerData();
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
        TouristTripStatus status = TouristTripStatus.from(tourist.isPassenger(), tourist.isStationary());
        data.putString(JadeTooltipNbt.TRIP_STATUS, status.toNbt());
    }

    @Override
    public ResourceLocation getUid() {
        return BusinessCraftJadeIds.TOURIST;
    }
}
