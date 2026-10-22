package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.init.CommonModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
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

import java.util.function.Consumer;

final class ShowcaseBuildHelper {
    private ShowcaseBuildHelper() {
    }

    static void buildSingleTown(MinecraftServer server, Consumer<BlockPos> onSuccess, Consumer<String> onFailure) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                    ? null
                    : server.getPlayerList().getPlayers().get(0);
            if (player == null) {
                onFailure.accept("no player joined");
                return;
            }
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(0, 0, false, false);
            player.setGameMode(GameType.CREATIVE);

            BlockPos feet = player.blockPosition();
            BlockPos placeAt = new BlockPos(feet.getX() + 8, feet.getY(), feet.getZ());
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
            ItemStack stack = new ItemStack(CommonModBlocks.TOWN_INTERFACE_BLOCK.get());
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                onFailure.accept("town interface item is not a block item");
                return;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
            blockItem.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
            BlockState placed = level.getBlockState(placeAt);
            if (!(placed.getBlock() instanceof TownInterfaceBlock)) {
                onFailure.accept("town interface was not placed at " + placeAt);
                return;
            }
            player.setGameMode(GameType.SPECTATOR);
            onSuccess.accept(placeAt.immutable());
        } catch (RuntimeException e) {
            onFailure.accept(e.toString());
        }
    }
}
