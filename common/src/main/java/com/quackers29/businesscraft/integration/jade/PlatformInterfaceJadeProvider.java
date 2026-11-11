package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.config.ConfigLoader;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

public enum PlatformInterfaceJadeProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!ConfigLoader.jadePlatformInterfaceTooltip || !config.get(BusinessCraftJadeIds.PLATFORM_INTERFACE)) {
            return;
        }
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(JadeTooltipNbt.PLATFORM)) {
            return;
        }
        if (data.contains(JadeTooltipNbt.NAME)) {
            tooltip.add(Component.translatable("businesscraft.jade.platform.name", data.getString(JadeTooltipNbt.NAME)));
        }

        DestinationMode mode = JadePlatformTooltipHelper.readMode(data);
        if (data.contains(JadeTooltipNbt.MODE)) {
            if (mode == DestinationMode.SPECIFIC) {
                tooltip.add(Component.translatable(JadePlatformTooltipHelper.modeLineKey(mode),
                        JadePlatformTooltipHelper.specificTownCount(data)));
            } else {
                tooltip.add(Component.translatable(JadePlatformTooltipHelper.modeLineKey(mode)));
            }
            if (mode == DestinationMode.AUTO) {
                String summary = JadePlatformTooltipHelper.learnedSummary(data);
                if (summary.isEmpty()) {
                    tooltip.add(Component.translatable("businesscraft.jade.platform.learned_empty"));
                } else {
                    tooltip.add(Component.translatable("businesscraft.jade.platform.learned_summary", summary));
                }
            }
        }

        if (!data.getBoolean(JadeTooltipNbt.LINK_ACTIVE)) {
            tooltip.add(Component.translatable("businesscraft.jade.platform.link_inactive"));
        } else {
            boolean enabled = data.getBoolean(JadeTooltipNbt.ENABLED);
            tooltip.add(Component.translatable(
                    enabled ? "businesscraft.jade.platform.spawn_enabled" : "businesscraft.jade.platform.spawn_disabled"));
        }
    }

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!ConfigLoader.jadePlatformInterfaceTooltip) {
            return;
        }
        BlockEntity be = accessor.getBlockEntity();
        if (!(be instanceof PlatformInterfaceEntity pie)) {
            return;
        }
        if (!(accessor.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        data.putBoolean(JadeTooltipNbt.PLATFORM, true);
        data.putBoolean(JadeTooltipNbt.LINK_ACTIVE, pie.isActive());
        data.putBoolean(JadeTooltipNbt.ENABLED, false);

        if (pie.getTownCentre() == null || pie.getPlatformId() == null) {
            return;
        }
        BlockEntity centre = serverLevel.getBlockEntity(pie.getTownCentre());
        if (!(centre instanceof TownInterfaceEntity townEntity)) {
            return;
        }
        Platform platform = townEntity.getPlatform(pie.getPlatformId());
        if (platform == null) {
            return;
        }

        data.putString(JadeTooltipNbt.NAME, platform.getName());
        DestinationMode mode = platform.getDestinationMode();
        data.putString(JadeTooltipNbt.MODE, JadeDestinationModes.configValue(mode));
        data.putBoolean(JadeTooltipNbt.ENABLED, platform.isEnabled() && pie.isActive());

        if (mode == DestinationMode.SPECIFIC) {
            data.putInt(JadeTooltipNbt.SPECIFIC_COUNT, platform.getEnabledDestinations().size());
        }

        if (mode == DestinationMode.AUTO) {
            Town originTown = townEntity.getTown();
            UUID originId = originTown != null ? originTown.getId() : null;
            long tick = serverLevel.getGameTime();
            platform.getLearnedRoutes().applyDecay(tick, ConfigLoader.touristAutoScoreHalfLifeTicks,
                    ConfigLoader.touristAutoFadeTripMultiplier, ConfigLoader.touristAutoPruneFloor);
            var valid = new java.util.HashSet<>(TownManager.get(serverLevel).getAllTowns().keySet());
            double total = platform.getLearnedRoutes().totalScoreFor(valid, originId);
            int limit = Math.max(1, ConfigLoader.jadeMaxLearnedRoutesInTooltip);
            List<Map.Entry<UUID, Double>> top = platform.getLearnedRoutes().topRoutes(limit, valid, originId);
            List<LearnedRoutesTooltipHelper.LearnedRouteLine> lines = LearnedRoutesTooltipHelper.buildLines(top, total,
                    id -> {
                        Town dest = TownManager.get(serverLevel).getTown(id);
                        return dest != null ? dest.getName() : "Unknown";
                    });
            data.putString(JadeTooltipNbt.LEARNED_SUMMARY,
                    LearnedRoutesTooltipHelper.formatLearnedRoutesSummary(lines));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return BusinessCraftJadeIds.PLATFORM_INTERFACE;
    }
}
