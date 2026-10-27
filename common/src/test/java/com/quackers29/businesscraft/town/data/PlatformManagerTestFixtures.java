package com.quackers29.businesscraft.town.data;

import com.quackers29.businesscraft.platform.PlatformPlacement;
import net.minecraft.core.BlockPos;

/**
 * Test-only helpers for {@link PlatformManager}; not part of the player add-platform flow.
 */
public final class PlatformManagerTestFixtures {
    private PlatformManagerTestFixtures() {
    }

    public static boolean addSyntheticPlatform(PlatformManager manager) {
        BlockPos pos = new BlockPos(manager.getPlatformCount() * PlatformPlacement.MIN_SEPARATION, 0, 0);
        return manager.addInterfacePlatform(pos) != null;
    }
}
