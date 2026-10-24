package com.quackers29.businesscraft.client.showcase;

import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.TownInterfaceBlock;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.init.CommonModBlocks;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.ui.screens.platform.PlatformInterfaceScreen;
import com.quackers29.businesscraft.ui.screens.town.TownInterfaceScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Development-only client harness. It does nothing unless the system property
 * {@code businesscraft.showcase} is set (Gradle {@code -Pshowcase}).
 *
 * <p>A run joins the quick-play world, builds a throwaway pad, places a town
 * interface the way a player would, photographs the block and the town tabs,
 * then quits. Screenshots are {@code screenshots/showcase_<shot>.png} under
 * the game directory. The world is rewritten, so always give it a copy.
 *
 * <pre>
 * bash ./gradlew :forge:runClient -PquickPlay=zz_showcase -Pshowcase=all
 * bash tools/showcase.sh town
 * </pre>
 *
 * <p>Shot names: {@code warmup}, {@code town_interface}, {@code town_interface_back},
 * {@code platform_interface}, {@code platform_interface_ui},
 * {@code town_overview}, {@code town_resources}, {@code town_production},
 * {@code town_settings}. Scenes: {@code block}, {@code town}, {@code all}.
 * {@code warmup} waits for chunks to draw but is not written to disk. {@code before}
 * actions that change the world must run on the server thread.
 */
public final class Showcase {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Showcase");
    private static final String PROPERTY = "businesscraft.showcase";
    private static final int JOIN_DELAY_TICKS = 40;
    private static final int FIRST_SHOT_TICKS = 100;
    private static final int QUIT_DELAY_TICKS = 40;

    private static int processExitCode;

    private static boolean configured;
    private static int joinTicks;
    private static boolean buildQueued;
    private static volatile boolean buildDone;
    private static volatile String failure;
    private static volatile BlockPos townPos = BlockPos.ZERO;
    private static volatile BlockPos kioskPos = BlockPos.ZERO;
    private static volatile BlockPos activeTownBlockPos = BlockPos.ZERO;
    private static boolean twoTownsConfigured;
    private static boolean testbedConfigured;
    private static boolean mapSweepConfigured;
    private static List<PlannedShot> plan = List.of();
    private static int shotIndex;
    private static int ticksOnShot;
    private static boolean enteredShot;
    private static int saved;
    private static boolean quitting;
    private static int quitTicks;

    private Showcase() {
    }

    public static void tick() {
        String property = System.getProperty(PROPERTY);
        if (!ShowcaseSelection.enabled(property)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!configured) {
            configured = true;
            minecraft.options.pauseOnLostFocus = false;
            minecraft.options.hideGui = true;
            if (ShowcaseSelection.twoTownsMode(property)) {
                plan = List.of();
                LOGGER.info("Showcase two-towns scenario enabled");
            } else if (ShowcaseSelection.testbedMode(property)) {
                plan = List.of();
                LOGGER.info("Showcase testbed scenario enabled");
            } else if (ShowcaseSelection.mapSweepMode(property)) {
                plan = List.of();
                LOGGER.info("Showcase map-sweep enabled");
            } else if (ShowcaseSelection.allViewsMode(property)) {
                plan = List.of();
                LOGGER.info("Showcase all-views enabled: {} routes", ShowcaseAllViews.expectedRouteCount());
            } else {
                plan = planFor(property);
                LOGGER.info("Showcase enabled ({}): {}", property.isBlank() ? "all" : property,
                        plan.stream().map(s -> s.name).toList());
            }
        }
        if (failure != null) {
            finish(minecraft);
            return;
        }
        if (ShowcaseSelection.twoTownsMode(property)) {
            if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                return;
            }
            if (!twoTownsConfigured) {
                twoTownsConfigured = true;
                ShowcaseTwoTowns.reset(minecraft);
            }
            ShowcaseTwoTowns.tick(minecraft);
            return;
        }
        if (ShowcaseSelection.testbedMode(property)) {
            if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                return;
            }
            if (!testbedConfigured) {
                testbedConfigured = true;
                ShowcaseTestbed.reset(minecraft);
            }
            ShowcaseTestbed.tick(minecraft);
            return;
        }
        if (ShowcaseSelection.mapSweepMode(property)) {
            if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                return;
            }
            if (!mapSweepConfigured) {
                mapSweepConfigured = true;
                ShowcaseMapSweep.reset(minecraft);
            }
            ShowcaseMapSweep.tick(minecraft);
            return;
        }
        if (ShowcaseTourists.isMode(property)) {
            if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
                return;
            }
            if (!configured) {
                configured = true;
                ShowcaseTourists.reset(minecraft);
            }
            ShowcaseTourists.tick(minecraft);
            return;
        }
        if (minecraft.player == null || minecraft.level == null || minecraft.getSingleplayerServer() == null) {
            return;
        }
        if (quitting) {
            shutdown(minecraft);
            return;
        }
        if (!buildDone) {
            if (!buildQueued && ++joinTicks >= JOIN_DELAY_TICKS) {
                buildQueued = true;
                MinecraftServer server = minecraft.getSingleplayerServer();
                server.execute(() -> build(server));
            }
            return;
        }
        if (ShowcaseSelection.allViewsMode(property)) {
            if (buildDone && minecraft.getSingleplayerServer() != null) {
                ShowcaseSetup.prepareOnce(minecraft.getSingleplayerServer(), townPos);
            }
            ShowcaseAllViews.tick(minecraft, townPos);
            return;
        }
        if (plan.isEmpty()) {
            failure = "no shots selected";
            return;
        }
        PlannedShot shot = plan.get(shotIndex);
        if (!enteredShot) {
            enteredShot = true;
            ticksOnShot = 0;
            aim(minecraft, shot);
            if (shot.before != null) {
                shot.before.run();
            }
        }
        ticksOnShot++;
        if (shot.screen && !tabReady(minecraft, shot)) {
            if (ticksOnShot == 40 || ticksOnShot == 70) {
                if ("platform_interface_ui".equals(shot.name)) {
                    openPlatformInterface(minecraft);
                } else {
                    showTab(minecraft, tabFor(shot.name));
                }
            }
            if (ticksOnShot > 100) {
                failure = "screen did not show " + shot.name;
            }
            return;
        }
        int wait = shotIndex == 0 ? FIRST_SHOT_TICKS : shot.pause;
        if (ticksOnShot < wait) {
            return;
        }
        if (!"warmup".equals(shot.name)) {
            grab(minecraft, shot.name);
        }
        boolean last = shotIndex + 1 >= plan.size();
        if (!last && !plan.get(shotIndex + 1).screen && minecraft.screen != null) {
            minecraft.setScreen(null);
        }
        shotIndex++;
        enteredShot = false;
        if (last) {
            quitting = true;
        }
    }

    private static List<PlannedShot> planFor(String property) {
        List<PlannedShot> shots = new ArrayList<>();
        for (String name : ShowcaseSelection.shotsFor(property)) {
            boolean screen = screenShot(name);
            String tab = tabFor(name);
            Runnable before;
            if ("platform_interface_ui".equals(name)) {
                before = () -> openPlatformInterface(Minecraft.getInstance());
            } else if (screen) {
                before = () -> showTab(Minecraft.getInstance(), tab);
            } else {
                before = null;
            }
            shots.add(new PlannedShot(name, pauseFor(name), screen, before));
        }
        return List.copyOf(shots);
    }

    private static int pauseFor(String name) {
        if ("town_overview".equals(name)) {
            return 50;
        }
        if ("platform_interface_ui".equals(name)) {
            return 30;
        }
        return name.startsWith("town_") && !name.startsWith("town_interface") ? 30 : 20;
    }

    private static boolean screenShot(String name) {
        return "platform_interface_ui".equals(name)
                || (name.startsWith("town_") && !name.startsWith("town_interface"));
    }

    private static void build(MinecraftServer server) {
        try {
            ServerPlayer player = server.getPlayerList().getPlayers().isEmpty()
                    ? null
                    : server.getPlayerList().getPlayers().get(0);
            if (player == null) {
                failure = "no player joined";
                return;
            }
            ServerLevel level = player.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(0, 0, false, false);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, server);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, server);
            level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false, server);
            player.setGameMode(GameType.CREATIVE);

            BlockPos feet = player.blockPosition();
            BlockPos placeAt = new BlockPos(feet.getX() + 8, feet.getY(), feet.getZ());
            BlockPos support = placeAt.below();
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos floor = support.offset(dx, 0, dz);
                    level.setBlock(floor, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                    for (int dy = 1; dy <= 8; dy++) {
                        level.setBlock(floor.above(dy), Blocks.AIR.defaultBlockState(), 3);
                    }
                }
            }
            player.teleportTo(level, placeAt.getX() + 0.5, placeAt.getY(), placeAt.getZ() + 3.5, 180f, 0f);
            player.setYHeadRot(180f);

            ItemStack stack = new ItemStack(CommonModBlocks.TOWN_INTERFACE_BLOCK.get());
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                failure = "town interface item is not a block item";
                LOGGER.error("Showcase failed: {}", failure);
                return;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
            var result = blockItem.place(
                    new BlockPlaceContext(player, InteractionHand.MAIN_HAND, stack, hit));
            BlockState placed = level.getBlockState(placeAt);
            if (!(placed.getBlock() instanceof TownInterfaceBlock)) {
                failure = "town interface was not placed (" + result + ") at " + placeAt;
                LOGGER.error("Showcase failed: {}", failure);
                return;
            }
            townPos = placeAt.immutable();
            if (!(level.getBlockEntity(townPos) instanceof TownInterfaceEntity centreEntity)
                    || centreEntity.getPlatforms().isEmpty()
                    || !centreEntity.getPlatforms().get(0).isComplete()) {
                failure = "town centre has no platform stop";
                LOGGER.error("Showcase failed: {}", failure);
                return;
            }
            Platform linked = centreEntity.getPlatforms().get(0);
            BlockPos kioskAt = linked.getStartPos().above();
            level.setBlock(kioskAt, Blocks.AIR.defaultBlockState(), 3);
            player.teleportTo(level, kioskAt.getX() + 0.5, kioskAt.getY(), kioskAt.getZ() + 2.5, 180f, 0f);
            player.setYHeadRot(180f);
            ItemStack kioskStack = new ItemStack(CommonModBlocks.PLATFORM_INTERFACE_BLOCK.get());
            if (!(kioskStack.getItem() instanceof BlockItem kioskItem)) {
                failure = "platform interface item is not a block item";
                LOGGER.error("Showcase failed: {}", failure);
                return;
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, kioskStack);
            BlockHitResult kioskHit = new BlockHitResult(
                    Vec3.atCenterOf(linked.getStartPos()).add(0, 0.5, 0), Direction.UP, linked.getStartPos(), false);
            kioskItem.place(new BlockPlaceContext(player, InteractionHand.MAIN_HAND, kioskStack, kioskHit));
            if (!(level.getBlockState(kioskAt).getBlock() instanceof PlatformInterfaceBlock)) {
                failure = "platform interface was not placed at " + kioskAt + " beside stop " + linked.getStartPos();
                LOGGER.error("Showcase failed: {}", failure);
                return;
            }
            kioskPos = kioskAt.immutable();
            ShowcaseTourismSeed.seed(level, townPos);
            player.setGameMode(GameType.SPECTATOR);
            LOGGER.info("Showcase placed town centre at {} and platform interface at {}", townPos, kioskPos);
            buildDone = true;
        } catch (RuntimeException exception) {
            failure = exception.toString();
            LOGGER.error("Showcase failed", exception);
        }
    }

    private static void aim(Minecraft minecraft, PlannedShot shot) {
        boolean kioskShot = "platform_interface".equals(shot.name) && !kioskPos.equals(BlockPos.ZERO);
        Vec3 centre = Vec3.atCenterOf(townPos);
        Vec3 target = kioskShot ? Vec3.atCenterOf(kioskPos) : centre;
        Vec3 eye = switch (shot.name) {
            case "town_interface_back" -> centre.add(-2.6, 3.0, -7.2);
            case "town_interface", "warmup" -> centre.add(3.0, 3.0, 6.4);
            case "platform_interface" -> target.add(4.6, 2.4, 2.2);
            default -> centre.add(0.0, 1.2, 2.4);
        };
        double lookUp = kioskShot ? 0.4 : 1.0;
        float[] facing = ShowcaseSelection.facing(eye.x, eye.y, eye.z, target.x, target.y + lookUp, target.z);
        Player player = minecraft.player;
        double feetY = eye.y - player.getEyeHeight();
        player.setYRot(facing[0]);
        player.setXRot(facing[1]);
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server != null) {
            server.execute(() -> {
                ServerPlayer serverPlayer = server.getPlayerList().getPlayer(player.getUUID());
                if (serverPlayer != null) {
                    serverPlayer.teleportTo(serverPlayer.serverLevel(), eye.x, feetY, eye.z, facing[0], facing[1]);
                    serverPlayer.setYHeadRot(facing[0]);
                }
            });
        }
    }

    /** True once the town screen is open on this shot's tab. A tab change waits one tick so it draws. */
    private static boolean tabReady(Minecraft minecraft, PlannedShot shot) {
        if ("platform_interface_ui".equals(shot.name)) {
            return minecraft.screen instanceof PlatformInterfaceScreen;
        }
        if (!(minecraft.screen instanceof TownInterfaceScreen screen) || screen.getTabPanel() == null) {
            return false;
        }
        String want = tabFor(shot.name);
        if (!want.equals(screen.getTabPanel().getActiveTabId())) {
            screen.getTabPanel().setActiveTab(want);
            return false;
        }
        return true;
    }

    private static String tabFor(String shot) {
        return switch (shot) {
            case "town_resources" -> "resources";
            case "town_production" -> "production";
            case "town_settings" -> "settings";
            default -> "overview";
        };
    }

    private static void openPlatformInterface(Minecraft minecraft) {
        BlockPos pos = kioskPos;
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null || pos.equals(BlockPos.ZERO)) {
            return;
        }
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
            if (serverPlayer == null) {
                return;
            }
            ServerLevel level = serverPlayer.serverLevel();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof PlatformInterfaceBlock block) {
                block.use(state, level, pos, serverPlayer, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            }
        });
    }

    private static void showTab(Minecraft minecraft, String tab) {
        if (minecraft.screen instanceof TownInterfaceScreen screen && screen.getTabPanel() != null) {
            screen.getTabPanel().setActiveTab(tab);
            return;
        }
        BlockPos pos = townPos;
        MinecraftServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        server.execute(() -> {
            ServerPlayer serverPlayer = server.getPlayerList().getPlayer(minecraft.player.getUUID());
            if (serverPlayer == null) {
                return;
            }
            ServerLevel level = serverPlayer.serverLevel();
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof TownInterfaceBlock block) {
                block.use(state, level, pos, serverPlayer, InteractionHand.MAIN_HAND,
                        new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            }
        });
    }

    private static void grab(Minecraft minecraft, String name) {
        String file = "showcase_" + name + ".png";
        Screenshot.grab(minecraft.gameDirectory, file, minecraft.getMainRenderTarget(), message ->
                LOGGER.info("Showcase shot {}: {}", file, message.getString()));
        saved++;
    }

    private static void finish(Minecraft minecraft) {
        if (!quitting) {
            LOGGER.error("Showcase failed: {}", failure);
            processExitCode = 1;
            quitting = true;
            quitTicks = 0;
        }
        shutdown(minecraft);
    }

    private static void shutdown(Minecraft minecraft) {
        if (++quitTicks >= QUIT_DELAY_TICKS) {
            if (processExitCode == 0) {
                LOGGER.info("Showcase finished: {} screenshots", saved);
            }
            minecraft.stop();
            System.exit(processExitCode);
        }
    }

    public static void setActiveTownBlockPos(BlockPos pos) {
        activeTownBlockPos = pos != null ? pos.immutable() : BlockPos.ZERO;
        townPos = activeTownBlockPos;
    }

    public static BlockPos getActiveTownBlockPos() {
        return activeTownBlockPos;
    }

    public static void setProcessExitCode(int code) {
        processExitCode = code;
    }

    private static final class PlannedShot {
        final String name;
        final int pause;
        final boolean screen;
        final Runnable before;

        PlannedShot(String name, int pause, boolean screen, Runnable before) {
            this.name = name;
            this.pause = pause;
            this.screen = screen;
            this.before = before;
        }
    }
}
