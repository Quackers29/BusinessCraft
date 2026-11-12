package com.quackers29.businesscraft.client.showcase;

import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristDisplayFormat;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristNames;
import com.quackers29.businesscraft.item.ConductorClipboardItem;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformDestinationModePacket;
import com.quackers29.businesscraft.network.packets.platform.SetPlatformEnabledPacket;
import com.quackers29.businesscraft.network.packets.ui.ClientTownMapCache;
import com.quackers29.businesscraft.network.packets.ui.RequestTownMapDataPacket;
import com.quackers29.businesscraft.network.packets.ui.TownMapDataResponsePacket;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.ui.screens.platform.PlatformInterfaceScreen;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client half of {@code tools/multiplayer-smoke.sh}. Joins the dedicated server, photographs
 * the tourists and the platform screen, and writes {@code multiplayer-smoke-observation.json}
 * from the data those screens are actually showing.
 *
 * <p>With {@code businesscraft.multiplayerSmoke.reach=true} (BC_Beta) the client then walks
 * {@link MultiplayerSmokePlan#REACH_TEST_DISTANCE} blocks away, sends two platform packets for
 * the Oakvale platform, walks back, and checks that the platform screen still shows the seeded
 * state. The server must have refused both.
 */
public final class MultiplayerSmokeClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/MpSmoke");
    private static final int JOIN_TIMEOUT_TICKS = 20 * 60 * 8;
    private static final int SCENE_TIMEOUT_TICKS = 20 * 90;

    private enum Phase {
        CONNECT,
        WAIT_SCENE,
        SHOOT_TAGS,
        SHOOT_TRACK,
        OPEN_PLATFORM,
        REACH_AWAY,
        REACH_SEND,
        REACH_BACK,
        REACH_VERIFY,
        WRITE,
        QUIT
    }

    private static Phase phase = Phase.CONNECT;
    private static int ticks;
    private static int phaseTicks;
    private static boolean connectStarted;
    private static String failure;
    private static int exitCode;
    private static int quitTicks;
    private static int stableTowns;
    private static int stablePlatform;
    private static boolean wrote;
    private static boolean shotsBegun;
    private static String lastScreen = "";
    private static int overlayTicks;

    private static final boolean REACH_STAGE = Boolean.getBoolean(MultiplayerSmokePlan.REACH_PROPERTY);
    /** Small enough that several moves landing in one server tick stay under the vanilla speed check. */
    private static final double REACH_STEP = 4.0;
    private static Vec3 reachHome;
    private static BlockPos reachTown;
    private static UUID reachPlatform;
    private static double reachSentFrom;
    private static boolean reachSent;
    private static boolean reachHeld;

    private MultiplayerSmokeClient() {
    }

    /** @return true when this client is the multiplayer smoke observer and the showcase should stay idle */
    public static boolean tick() {
        if (!"observe".equals(System.getProperty(MultiplayerSmokeServer.PROPERTY))) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = false;
        if (!shotsBegun) {
            shotsBegun = true;
            ShowcaseShotManifest.begin(minecraft, "mp-smoke", MultiplayerSmokePlan.SHOTS);
        }
        if (minecraft.getOverlay() != null) {
            if (overlayTicks++ % 100 == 0) {
                LOGGER.info("MP_SMOKE_OVERLAY {}", minecraft.getOverlay().getClass().getName());
            }
            return true;
        }
        if (minecraft.screen instanceof AccessibilityOnboardingScreen) {
            minecraft.options.onboardAccessibility = false;
            minecraft.options.save();
            minecraft.setScreen(new TitleScreen());
        }
        if (minecraft.screen instanceof PauseScreen) {
            minecraft.setScreen(null);
        }
        String screenName = minecraft.screen == null ? "null" : minecraft.screen.getClass().getName();
        if (!screenName.equals(lastScreen)) {
            lastScreen = screenName;
            LOGGER.info("MP_SMOKE_SCREEN {}", screenName);
        }
        ticks++;
        phaseTicks++;
        if (phase == Phase.QUIT) {
            shutdown(minecraft);
            return true;
        }
        if (failure != null) {
            phase = Phase.QUIT;
            phaseTicks = 0;
            return true;
        }
        if (minecraft.screen instanceof DisconnectedScreen screen) {
            fail(minecraft, "disconnected: " + screen.getTitle().getString());
            return true;
        }
        switch (phase) {
            case CONNECT -> connect(minecraft);
            case WAIT_SCENE -> waitScene(minecraft);
            case SHOOT_TAGS -> shootTags(minecraft);
            case SHOOT_TRACK -> shootTrack(minecraft);
            case OPEN_PLATFORM -> openPlatform(minecraft);
            case REACH_AWAY -> reachAway(minecraft);
            case REACH_SEND -> reachSend(minecraft);
            case REACH_BACK -> reachBack(minecraft);
            case REACH_VERIFY -> reachVerify(minecraft);
            case WRITE -> write(minecraft);
            case QUIT -> shutdown(minecraft);
        }
        return true;
    }

    private static void connect(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.level != null) {
            enter(Phase.WAIT_SCENE);
            minecraft.options.hideGui = true;
            minecraft.options.setCameraType(CameraType.FIRST_PERSON);
            return;
        }
        if (!connectStarted && minecraft.screen instanceof TitleScreen && ticks > 20) {
            connectStarted = true;
            int port = port();
            String address = "127.0.0.1:" + port;
            LOGGER.info("MP_SMOKE_CONNECT {}", address);
            ConnectScreen.startConnecting(minecraft.screen, minecraft, ServerAddress.parseString(address),
                    new ServerData("BusinessCraft mp smoke", address, false), false);
        }
        if (ticks > JOIN_TIMEOUT_TICKS) {
            fail(minecraft, "timed out before joining the server");
        }
    }

    private static void waitScene(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            if (phaseTicks > JOIN_TIMEOUT_TICKS) {
                fail(minecraft, "left the world before the scene was visible");
            }
            return;
        }
        if (phaseTicks % 40 == 1) {
            requestTowns();
        }
        boolean townsReady = townCount() >= 3;
        stableTowns = townsReady ? stableTowns + 1 : 0;
        List<Map<String, Object>> tourists = visibleTourists(minecraft);
        boolean clipboard = ConductorClipboardItem.clientPlayerHoldingForRouteView();
        if (townsReady && stableTowns >= 10 && tourists.size() >= 2 && clipboard) {
            LOGGER.info("MP_SMOKE_SCENE towns={} tourists={}", townCount(), tourists.size());
            enter(Phase.SHOOT_TAGS);
            return;
        }
        if (phaseTicks > SCENE_TIMEOUT_TICKS) {
            fail(minecraft, "scene was not ready (towns=" + townCount() + ", tourists=" + tourists.size()
                    + ", clipboard=" + clipboard + ")");
        }
    }

    private static void shootTags(Minecraft minecraft) {
        aimAtTourists(minecraft);
        if (phaseTicks < 40) {
            return;
        }
        ShowcaseShotManifest.grab(minecraft, "mp_nametags");
        enter(Phase.SHOOT_TRACK);
    }

    private static void shootTrack(Minecraft minecraft) {
        if (minecraft.player != null) {
            minecraft.player.setYRot(0f);
            minecraft.player.setXRot(12f);
            minecraft.player.setYHeadRot(0f);
        }
        if (phaseTicks < 20) {
            return;
        }
        ShowcaseShotManifest.grab(minecraft, "mp_track");
        enter(Phase.OPEN_PLATFORM);
    }

    private static void openPlatform(Minecraft minecraft) {
        PlatformInterfaceScreen screen = settledPlatformScreen(minecraft);
        if (screen == null) {
            return;
        }
        ShowcaseShotManifest.grab(minecraft, "mp_platform");
        if (!REACH_STAGE) {
            enter(Phase.WRITE);
            return;
        }
        BlockPos kiosk = screen.getMenu().getBlockPos();
        if (kiosk == null || !(minecraft.level.getBlockEntity(kiosk) instanceof PlatformInterfaceEntity entity)
                || entity.getTownCentre() == null || entity.getPlatformId() == null) {
            fail(minecraft, "reach stage could not read the platform kiosk");
            return;
        }
        reachTown = entity.getTownCentre().immutable();
        reachPlatform = entity.getPlatformId();
        reachHome = minecraft.player.position();
        minecraft.player.closeContainer();
        LOGGER.info("MP_SMOKE_REACH_LEAVING town={} platform={}", reachTown, reachPlatform);
        enter(Phase.REACH_AWAY);
    }

    /**
     * Opens the nearest platform kiosk and returns its screen once the server data has been
     * steady for 15 ticks. Returns null while waiting, and after reporting a failure.
     */
    private static PlatformInterfaceScreen settledPlatformScreen(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null || minecraft.gameMode == null) {
            fail(minecraft, "lost the world while opening the platform");
            return null;
        }
        aimAtTourists(minecraft);
        if (!(minecraft.screen instanceof PlatformInterfaceScreen screen)) {
            stablePlatform = 0;
            if (phaseTicks % 40 == 0) {
                BlockPos pos = nearestPlatform(minecraft);
                if (pos == null) {
                    if (phaseTicks > SCENE_TIMEOUT_TICKS) {
                        fail(minecraft, "no platform interface block near the player");
                    }
                    return null;
                }
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), net.minecraft.core.Direction.UP, pos, false);
                minecraft.gameMode.useItemOn(minecraft.player, InteractionHand.MAIN_HAND, hit);
                LOGGER.info("MP_SMOKE_OPEN_PLATFORM {}", pos);
            }
            if (phaseTicks > SCENE_TIMEOUT_TICKS) {
                fail(minecraft, "platform screen did not open");
            }
            return null;
        }
        boolean ready = screen.smokeDataReceived()
                && !screen.smokeLearnedRoutes().isEmpty()
                && screen.smokePlatformEnabled() != null
                && !screen.smokeDestinationRows().isEmpty();
        stablePlatform = ready ? stablePlatform + 1 : 0;
        if (ready && stablePlatform >= 15) {
            return screen;
        }
        if (phaseTicks > SCENE_TIMEOUT_TICKS) {
            minecraft.setScreen(null);
            fail(minecraft, "platform screen did not receive destination data");
        }
        return null;
    }

    private static void reachAway(Minecraft minecraft) {
        Vec3 far = reachHome.add(MultiplayerSmokePlan.REACH_TEST_DISTANCE, 0, 0);
        if (stepToward(minecraft, far) && phaseTicks > 20) {
            enter(Phase.REACH_SEND);
            return;
        }
        if (phaseTicks > SCENE_TIMEOUT_TICKS) {
            fail(minecraft, "reach stage did not get away from the town");
        }
    }

    private static void reachSend(Minecraft minecraft) {
        if (minecraft.player == null) {
            fail(minecraft, "lost the player during the reach stage");
            return;
        }
        if (!reachSent) {
            reachSent = true;
            reachSentFrom = Math.sqrt(minecraft.player.distanceToSqr(Vec3.atCenterOf(reachTown)));
            PlatformAccess.getNetworkMessages().sendToServer(
                    new SetPlatformEnabledPacket(reachTown, reachPlatform, false));
            PlatformAccess.getNetworkMessages().sendToServer(
                    new SetPlatformDestinationModePacket(reachTown, reachPlatform, DestinationMode.SPECIFIC));
            LOGGER.info("MP_SMOKE_REACH_SENT distance={}", Math.round(reachSentFrom));
        }
        // Long enough for the server to have handled both packets before the walk back.
        if (phaseTicks > 40) {
            enter(Phase.REACH_BACK);
        }
    }

    private static void reachBack(Minecraft minecraft) {
        if (stepToward(minecraft, reachHome) && phaseTicks > 20) {
            minecraft.player.setYRot(180f);
            minecraft.player.setYHeadRot(180f);
            enter(Phase.REACH_VERIFY);
            return;
        }
        if (phaseTicks > SCENE_TIMEOUT_TICKS) {
            fail(minecraft, "reach stage did not get back to the platform");
        }
    }

    private static void reachVerify(Minecraft minecraft) {
        PlatformInterfaceScreen screen = settledPlatformScreen(minecraft);
        if (screen == null) {
            return;
        }
        // The first observation needs both tourists back in view, same as before the walk.
        if (visibleTourists(minecraft).size() < 2) {
            if (phaseTicks > SCENE_TIMEOUT_TICKS) {
                fail(minecraft, "tourists were not visible after the reach stage");
            }
            return;
        }
        boolean stillEnabled = Boolean.TRUE.equals(screen.smokePlatformEnabled());
        boolean stillAuto = screen.smokeDestinationMode() == DestinationMode.AUTO;
        reachHeld = stillEnabled && stillAuto;
        LOGGER.info("MP_SMOKE_REACH_RESULT enabled={} mode={}", stillEnabled, screen.smokeDestinationMode());
        if (!reachHeld) {
            fail(minecraft, "a platform packet sent from " + Math.round(reachSentFrom)
                    + " blocks away was applied (enabled=" + stillEnabled
                    + ", mode=" + screen.smokeDestinationMode().toConfigValue() + ")");
            return;
        }
        enter(Phase.WRITE);
    }

    /**
     * Moves the player up to {@link #REACH_STEP} blocks toward {@code target} and reports whether
     * it has arrived. Waits when the next chunk has not reached this client yet, because a local
     * player outside loaded chunks stops sending its position.
     */
    private static boolean stepToward(Minecraft minecraft, Vec3 target) {
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        Vec3 here = minecraft.player.position();
        Vec3 gap = new Vec3(target.x - here.x, 0, target.z - here.z);
        double left = gap.length();
        if (left < 0.01) {
            return true;
        }
        Vec3 next = left <= REACH_STEP ? target : here.add(gap.scale(REACH_STEP / left));
        if (!minecraft.level.hasChunkAt(BlockPos.containing(next))) {
            return false;
        }
        minecraft.player.setDeltaMovement(Vec3.ZERO);
        minecraft.player.setPos(next.x, target.y, next.z);
        return false;
    }

    private static void write(Minecraft minecraft) {
        if (ShowcaseShotManifest.pending() > 0) {
            if (phaseTicks > 200) {
                fail(minecraft, "screenshot writes did not finish");
            }
            return;
        }
        if (ShowcaseShotManifest.hasFailure()) {
            fail(minecraft, "one or more screenshots were not saved");
            return;
        }
        writeObservation(minecraft, true);
        LOGGER.info("MP_SMOKE_OBSERVATION_WRITTEN {}", observationPath(minecraft));
        exitCode = 0;
        enter(Phase.QUIT);
    }

    private static void enter(Phase next) {
        phase = next;
        phaseTicks = 0;
    }

    private static void fail(Minecraft minecraft, String reason) {
        if (failure != null) {
            return;
        }
        failure = reason;
        exitCode = 1;
        LOGGER.error("MP_SMOKE_CLIENT_FAILED {}", reason);
        writeObservation(minecraft, false);
        enter(Phase.QUIT);
    }

    private static void shutdown(Minecraft minecraft) {
        int wait = exitCode == 0 ? 60 : 10;
        if (++quitTicks < wait) {
            return;
        }
        minecraft.stop();
        System.exit(exitCode);
    }

    private static void requestTowns() {
        if (PlatformAccess.getNetworkMessages() != null) {
            PlatformAccess.getNetworkMessages().sendToServer(new RequestTownMapDataPacket());
        }
    }

    private static int townCount() {
        return ClientTownMapCache.getInstance().getAllTowns().size();
    }

    private static int port() {
        String raw = System.getProperty("businesscraft.multiplayerSmoke.port",
                Integer.toString(MultiplayerSmokePlan.FORGE_PORT));
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException exception) {
            return MultiplayerSmokePlan.FORGE_PORT;
        }
    }

    private static List<Map<String, Object>> visibleTourists(Minecraft minecraft) {
        List<Map<String, Object>> rows = new ArrayList<>();
        if (minecraft.player == null || minecraft.level == null) {
            return rows;
        }
        boolean clipboard = ConductorClipboardItem.clientPlayerHoldingForRouteView();
        AABB box = minecraft.player.getBoundingBox().inflate(8.0);
        for (TouristEntity tourist : minecraft.level.getEntitiesOfClass(TouristEntity.class, box)) {
            if (!tourist.isCustomNameVisible()) {
                continue;
            }
            if (!TouristDisplayFormat.withinNameTagRange(minecraft.player.distanceToSqr(tourist))) {
                continue;
            }
            String personal = tourist.getPersonalName();
            if (TouristNames.isPlaceholder(personal)) {
                continue;
            }
            String first = TouristNames.firstOf(personal);
            if (first.isBlank()) {
                continue;
            }
            String line = "";
            if (clipboard) {
                line = TouristDisplayFormat.conductorRouteLine(
                        tourist.getSyncedOriginLabel(), tourist.getSyncedDestLabel());
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nameTag", first);
            row.put("clipboardLine", line);
            rows.add(row);
        }
        rows.sort(Comparator.comparing((Map<String, Object> row) -> String.valueOf(row.get("nameTag")))
                .thenComparing(row -> String.valueOf(row.get("clipboardLine"))));
        return rows;
    }

    private static void aimAtTourists(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }
        AABB box = minecraft.player.getBoundingBox().inflate(8.0);
        List<TouristEntity> tourists = minecraft.level.getEntitiesOfClass(TouristEntity.class, box);
        if (tourists.isEmpty()) {
            return;
        }
        double x = 0;
        double y = 0;
        double z = 0;
        for (TouristEntity tourist : tourists) {
            x += tourist.getX();
            y += tourist.getY() + tourist.getBbHeight() + 0.4;
            z += tourist.getZ();
        }
        x /= tourists.size();
        y /= tourists.size();
        z /= tourists.size();
        Vec3 eye = minecraft.player.getEyePosition();
        float[] facing = ShowcaseSelection.facing(eye.x, eye.y, eye.z, x, y, z);
        minecraft.player.setYRot(facing[0]);
        minecraft.player.setXRot(facing[1]);
        minecraft.player.setYHeadRot(facing[0]);
    }

    private static BlockPos nearestPlatform(Minecraft minecraft) {
        BlockPos origin = minecraft.player.blockPosition();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!(minecraft.level.getBlockState(pos).getBlock() instanceof PlatformInterfaceBlock)) {
                        continue;
                    }
                    double distance = minecraft.player.distanceToSqr(Vec3.atCenterOf(pos));
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos.immutable();
                    }
                }
            }
        }
        return best;
    }

    private static void writeObservation(Minecraft minecraft, boolean ok) {
        if (wrote && ok) {
            return;
        }
        wrote = true;
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok);
        body.put("failure", failure == null ? "" : failure);
        String viewer = "";
        if (minecraft.player != null) {
            viewer = minecraft.player.getGameProfile().getName();
        }
        body.put("viewer", viewer);
        body.put("holdingClipboard", ConductorClipboardItem.clientPlayerHoldingForRouteView());
        body.put("ui", ShowcaseUiSnapshot.capture(minecraft));
        body.put("shots", ShowcaseShotManifest.rows());
        body.put("towns", townRows());
        body.put("platform", platformRow(minecraft));
        body.put("tourists", visibleTourists(minecraft));
        if (REACH_STAGE) {
            Map<String, Object> reach = new LinkedHashMap<>();
            reach.put("sent", reachSent);
            reach.put("distance", Math.round(reachSentFrom));
            reach.put("packets", List.of("SetPlatformEnabledPacket", "SetPlatformDestinationModePacket"));
            reach.put("stateUnchanged", reachHeld);
            body.put("reach", reach);
        }
        try {
            Files.writeString(observationPath(minecraft),
                    new GsonBuilder().setPrettyPrinting().create().toJson(body), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.error("MP_SMOKE_CLIENT_FAILED could not write observation", exception);
            exitCode = 1;
        }
    }

    private static java.nio.file.Path observationPath(Minecraft minecraft) {
        return minecraft.gameDirectory.toPath().resolve("multiplayer-smoke-observation.json");
    }

    private static List<Map<String, Object>> townRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TownMapDataResponsePacket.TownMapInfo town : ClientTownMapCache.getInstance().getAllTowns().values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", town.name);
            row.put("x", town.position.getX());
            row.put("y", town.position.getY());
            row.put("z", town.position.getZ());
            row.put("population", town.population);
            row.put("touristCount", town.touristCount);
            rows.add(row);
        }
        rows.sort(Comparator.comparing(row -> String.valueOf(row.get("name"))));
        return rows;
    }

    private static Map<String, Object> platformRow(Minecraft minecraft) {
        Map<String, Object> row = new LinkedHashMap<>();
        if (!(minecraft.screen instanceof PlatformInterfaceScreen screen)) {
            row.put("name", "");
            row.put("mode", "");
            row.put("enabled", false);
            row.put("enabledTowns", List.of());
            row.put("towns", List.of());
            row.put("learnedRoutes", List.of());
            return row;
        }
        BlockPos pos = screen.getMenu().getBlockPos();
        row.put("name", screen.smokePlatformName());
        if (pos != null) {
            Map<String, Object> at = new LinkedHashMap<>();
            at.put("x", pos.getX());
            at.put("y", pos.getY());
            at.put("z", pos.getZ());
            row.put("pos", at);
        }
        row.put("mode", screen.smokeDestinationMode().toConfigValue());
        Boolean enabled = screen.smokePlatformEnabled();
        row.put("enabled", enabled != null && enabled);
        List<Map<String, Object>> towns = new ArrayList<>();
        List<String> enabledTowns = new ArrayList<>();
        for (PlatformInterfaceScreen.SmokeTownRow destination : screen.smokeDestinationRows()) {
            Map<String, Object> town = new LinkedHashMap<>();
            town.put("name", destination.name());
            town.put("enabled", destination.enabled());
            town.put("distance", destination.distance());
            town.put("direction", destination.direction());
            towns.add(town);
            if (destination.enabled()) {
                enabledTowns.add(destination.name());
            }
        }
        towns.sort(Comparator.comparing(town -> String.valueOf(town.get("name"))));
        enabledTowns.sort(Comparator.naturalOrder());
        row.put("enabledTowns", enabledTowns);
        row.put("towns", towns);
        List<Map<String, Object>> learned = new ArrayList<>();
        for (var route : screen.smokeLearnedRoutes()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", route.name());
            entry.put("strength", Math.round(route.strength() * 10000.0) / 10000.0);
            learned.add(entry);
        }
        learned.sort(Comparator.comparing(entry -> String.valueOf(entry.get("name"))));
        row.put("learnedRoutes", learned);
        return row;
    }
}
