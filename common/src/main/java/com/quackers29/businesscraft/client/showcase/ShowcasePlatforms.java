package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.init.CommonModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Places one Platform Interface next to a showcase town and opens its screen.
 */
public final class ShowcasePlatforms {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    public static final BlockPos OFFSET = new BlockPos(4, 0, 0);
    public static final BlockPos SECOND_OFFSET = new BlockPos(4, 0, 8);
    private static volatile BlockPos placedAt;
    private static volatile BlockPos secondAt;
    private static volatile UUID firstPlatformId;
    private static volatile UUID secondPlatformId;

    private ShowcasePlatforms() {
    }

    public static BlockPos interfacePos(BlockPos townPos) {
        if (townPos == null) {
            return null;
        }
        return townPos.offset(OFFSET);
    }

    public static BlockPos secondPos(BlockPos townPos) {
        if (townPos == null) {
            return null;
        }
        return townPos.offset(SECOND_OFFSET);
    }

    public static BlockPos lastPlaced() {
        return placedAt;
    }

    public static BlockPos lastSecond() {
        return secondAt;
    }

    public static UUID firstPlatformId() {
        return firstPlatformId;
    }

    public static UUID secondPlatformId() {
        return secondPlatformId;
    }

    public static boolean ensure(ServerLevel level, BlockPos townPos) {
        BlockPos pos = interfacePos(townPos);
        if (pos == null || level == null) {
            return false;
        }
        placedAt = pos.immutable();
        if (level.getBlockState(pos).getBlock() instanceof PlatformInterfaceBlock) {
            dress(level, pos, townPos);
            return true;
        }
        if (!(level.getBlockEntity(townPos) instanceof TownInterfaceEntity town) || town.getTownId() == null) {
            LOGGER.warn("Showcase platform: no town at {}", townPos);
            return false;
        }
        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        }
        level.setBlock(pos, CommonModBlocks.PLATFORM_INTERFACE_BLOCK.get().defaultBlockState()
                .setValue(PlatformInterfaceBlock.FACING, Direction.SOUTH), 3);
        if (!(level.getBlockEntity(pos) instanceof PlatformInterfaceEntity entity)) {
            LOGGER.warn("Showcase platform: block entity missing at {}", pos);
            return false;
        }
        UUID platformId = town.addInterfacePlatform(pos);
        if (platformId == null) {
            LOGGER.warn("Showcase platform: town refused a platform at {}", pos);
            level.removeBlock(pos, false);
            return false;
        }
        entity.link(town.getTownId(), platformId, townPos);
        firstPlatformId = platformId;
        town.setPlatformPathStart(platformId, pos.north(2));
        town.setPlatformPathEnd(platformId, pos.north(6));
        placeSecond(level, town, townPos);
        dress(level, pos, townPos);
        town.syncPlatforms();
        LOGGER.info("Showcase placed platform interface at {}", pos);
        return true;
    }

    private static void placeSecond(ServerLevel level, TownInterfaceEntity town, BlockPos townPos) {
        BlockPos pos = secondPos(townPos);
        secondAt = pos == null ? null : pos.immutable();
        if (pos == null || level.getBlockState(pos).getBlock() instanceof PlatformInterfaceBlock) {
            return;
        }
        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        }
        level.setBlock(pos, CommonModBlocks.PLATFORM_INTERFACE_BLOCK.get().defaultBlockState()
                .setValue(PlatformInterfaceBlock.FACING, Direction.SOUTH), 3);
        if (!(level.getBlockEntity(pos) instanceof PlatformInterfaceEntity entity)) {
            return;
        }
        UUID platformId = town.addInterfacePlatform(pos);
        if (platformId == null) {
            level.removeBlock(pos, false);
            return;
        }
        entity.link(town.getTownId(), platformId, townPos);
        secondPlatformId = platformId;
        town.setPlatformPathStart(platformId, pos.east(2));
        town.setPlatformPathEnd(platformId, pos.east(6));
    }

    /** Rails along the first platform's path, so the kiosk reads as a station. */
    private static void dress(ServerLevel level, BlockPos machine, BlockPos townPos) {
        for (int step = 1; step <= 7; step++) {
            BlockPos rail = machine.north(step);
            if (level.getBlockState(rail.below()).isAir()) {
                level.setBlock(rail.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            } else if (step <= 6) {
                level.setBlock(rail.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
            }
            level.setBlock(rail, Blocks.RAIL.defaultBlockState(), 3);
        }
        BlockPos second = secondPos(townPos);
        if (second == null) {
            return;
        }
        for (int step = 1; step <= 6; step++) {
            BlockPos rail = second.east(step);
            if (level.getBlockState(rail).isAir()) {
                level.setBlock(rail, Blocks.RAIL.defaultBlockState(), 3);
            }
        }
    }

    public static void openScreen(Minecraft minecraft, BlockPos townPos) {
        BlockPos pos = placedAt != null ? placedAt : interfacePos(townPos);
        if (pos == null || minecraft.getSingleplayerServer() == null || minecraft.player == null) {
            return;
        }
        MinecraftServer server = minecraft.getSingleplayerServer();
        UUID playerId = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof PlatformInterfaceBlock block) {
                block.use(state, level, pos, player, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            }
        });
    }
}
