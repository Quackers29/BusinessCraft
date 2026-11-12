package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.init.CommonModBlocks;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;

/**
 * Shared world setup for showcase scenarios (pads, player-like town interface placement).
 */
public final class ShowcaseWorldBuild {
    /** Noon. Every showcase and smoke scene is lit the same way. */
    public static final long SHOWCASE_DAY_TIME = 6000L;

    /**
     * Fixes the clock at noon with clear weather and no daylight or weather cycle, so shots
     * from different runs differ only by what changed in the mod.
     */
    public static void pinDaytime(ServerLevel level) {
        net.minecraft.server.MinecraftServer server = level.getServer();
        level.setDayTime(SHOWCASE_DAY_TIME);
        level.setWeatherParameters(0, 0, false, false);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
    }

    public record PlaceAttemptResult(
            boolean blockRemains,
            int townCountAfter,
            String placementErrorMessage,
            UUID townIdAtPos) {
    }

    private ShowcaseWorldBuild() {
    }

    public static void preparePad(ServerLevel level, BlockPos placeAt) {
        BlockPos support = placeAt.below();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                BlockPos floor = support.offset(dx, 0, dz);
                level.setBlock(floor, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                for (int dy = 1; dy <= 8; dy++) {
                    level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    public static PlaceAttemptResult placeTownInterfaceLikePlayer(ServerPlayer player, ServerLevel level, BlockPos placeAt) {
        preparePad(level, placeAt);
        BlockPos support = placeAt.below();
        ItemStack stack = new ItemStack(CommonModBlocks.TOWN_INTERFACE_BLOCK.get());
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return new PlaceAttemptResult(false, countTowns(level), "town interface item is not a block item", null);
        }
        player.setGameMode(GameType.CREATIVE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
        blockItem.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));

        BlockState placed = level.getBlockState(placeAt);
        boolean blockRemains = placed.getBlock() instanceof TownInterfaceBlock;
        TownManager manager = TownManager.get(level);
        String err = manager.getTownPlacementError(placeAt);
        UUID townId = null;
        if (blockRemains) {
            for (Map.Entry<UUID, Town> entry : manager.getAllTowns().entrySet()) {
                if (entry.getValue().getPosition().equals(placeAt)) {
                    townId = entry.getKey();
                    break;
                }
            }
        }
        return new PlaceAttemptResult(blockRemains, countTowns(level), err, townId);
    }

    public static int countTowns(ServerLevel level) {
        return TownManager.get(level).getAllTowns().size();
    }
}
