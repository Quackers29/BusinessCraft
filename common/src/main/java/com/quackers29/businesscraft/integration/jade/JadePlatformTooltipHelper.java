package com.quackers29.businesscraft.integration.jade;

import com.quackers29.businesscraft.platform.DestinationMode;
import net.minecraft.nbt.CompoundTag;

/**
 * Maps server-synced platform tooltip NBT to translation keys (testable, no Jade API).
 */
public final class JadePlatformTooltipHelper {

    private JadePlatformTooltipHelper() {
    }

    public static String modeLineKey(DestinationMode mode) {
        if (mode == null) {
            mode = DestinationMode.AUTO;
        }
        return switch (mode) {
            case ANY -> "businesscraft.jade.platform.mode_any";
            case AUTO -> "businesscraft.jade.platform.mode_auto";
            case SPECIFIC -> "businesscraft.jade.platform.mode_specific";
        };
    }

    public static DestinationMode readMode(CompoundTag data) {
        if (data == null || !data.contains(JadeTooltipNbt.MODE)) {
            return DestinationMode.AUTO;
        }
        return DestinationMode.fromConfig(data.getString(JadeTooltipNbt.MODE), DestinationMode.AUTO);
    }

    public static int specificTownCount(CompoundTag data) {
        if (data == null || !data.contains(JadeTooltipNbt.SPECIFIC_COUNT)) {
            return 0;
        }
        return Math.max(0, data.getInt(JadeTooltipNbt.SPECIFIC_COUNT));
    }

    public static String learnedSummary(CompoundTag data) {
        if (data == null || !data.contains(JadeTooltipNbt.LEARNED_SUMMARY)) {
            return "";
        }
        String summary = data.getString(JadeTooltipNbt.LEARNED_SUMMARY);
        return summary == null ? "" : summary.trim();
    }
}
