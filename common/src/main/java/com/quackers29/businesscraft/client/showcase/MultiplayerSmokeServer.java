package com.quackers29.businesscraft.client.showcase;

import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristType;
import com.quackers29.businesscraft.init.CommonModBlocks;
import com.quackers29.businesscraft.init.CommonModEntityTypes;
import com.quackers29.businesscraft.init.CommonModItems;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.vehicle.Minecart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated-server half of {@code tools/multiplayer-smoke.sh}.
 *
 * <p>Builds a short line of towns and plain rails, seeds one AUTO learned table with
 * {@link com.quackers29.businesscraft.platform.PlatformLearnedRoutes#recordSuccess} (a real
 * minecart arrival is too slow and flaky for a headless smoke), spawns two named tourists,
 * and parks each joining player where both name tags are in range of the Oakvale platform.
 */
public final class MultiplayerSmokeServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/MpSmoke");
    static final String PROPERTY = "businesscraft.multiplayerSmoke";
    private static final boolean ENABLED = "setup".equals(System.getProperty(PROPERTY));

    /** Fresh towns use a border of 50, so centres must be at least 100 apart. */
    private static final int TOWN_SPACING = 120;
    private static final int READY_DELAY_TICKS = 100;

    private static int ticks;
    private static boolean started;
    private static volatile boolean ready;
    private static double alphaX;
    private static double betaX;
    private static double viewZ;
    private static double viewY;

    private MultiplayerSmokeServer() {
    }

    public static void initialize() {
        if (!ENABLED || PlatformAccess.getEvents() == null) {
            return;
        }
        PlatformAccess.getEvents().registerPlayerLoginCallback((player, level, pos) -> welcome(player));
    }

    public static void onLevelTick(ServerLevel level) {
        if (!ENABLED || started || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (++ticks < READY_DELAY_TICKS) {
            return;
        }
        started = true;
        try {
            build(level);
        } catch (RuntimeException exception) {
            LOGGER.error("MP_SMOKE_FAILED {}", exception.toString(), exception);
            writeReady(level.getServer(), false, exception.toString());
        }
    }

    private static void build(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ShowcaseWorldBuild.pinDaytime(level);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);

        level.getChunk(0, 0);
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 0, 0);
        if (surface < -64 || surface > 320) {
            throw new IllegalStateException("refusing to build at surface Y " + surface);
        }

        BlockPos oakPos = new BlockPos(0, surface, 0);
        BlockPos hillPos = new BlockPos(0, surface, TOWN_SPACING);
        BlockPos riverPos = new BlockPos(0, surface, TOWN_SPACING * 2);
        ShowcaseTownNames.reset();
        UUID oakId = placeTown(level, oakPos, ShowcaseTownNames.next());
        UUID hillId = placeTown(level, hillPos, ShowcaseTownNames.next());
        UUID riverId = placeTown(level, riverPos, ShowcaseTownNames.next());

        if (!ShowcasePlatformPlacement.ensure(level, oakPos)) {
            throw new IllegalStateException("Oakvale platform was not placed");
        }
        UUID seededPlatform = ShowcasePlatformPlacement.firstPlatformId();
        BlockPos platformPos = ShowcasePlatformPlacement.lastPlaced();
        if (seededPlatform == null || platformPos == null) {
            throw new IllegalStateException("Oakvale platform id was not recorded");
        }
        if (!ShowcasePlatformPlacement.ensure(level, hillPos) || !ShowcasePlatformPlacement.ensure(level, riverPos)) {
            throw new IllegalStateException("a down-line platform was not placed");
        }

        TownInterfaceEntity oak = townEntity(level, oakPos);
        tighten(level, oakPos);
        tighten(level, hillPos);
        tighten(level, riverPos);
        seedLearnedRoute(level, oak, seededPlatform, hillId, riverId);

        int rails = layRailZ(level, platformPos.getX() + 4, surface, oakPos.getZ(), riverPos.getZ());
        rails += layRailX(level, platformPos.getZ(), surface, platformPos.getX() + 1, platformPos.getX() + 3);
        rails += layRailX(level, hillPos.getZ(), surface, platformPos.getX() + 1, platformPos.getX() + 3);
        rails += layRailX(level, riverPos.getZ(), surface, platformPos.getX() + 1, platformPos.getX() + 3);
        Minecart cart = new Minecart(level, platformPos.getX() + 4.5, surface, 24.5);
        level.addFreshEntity(cart);

        spawnTourist(level, platformPos.getX() + 0.3, surface, platformPos.getZ() + 2.2,
                "Morgan Hale", "Oakvale", "Hillcrest");
        spawnTourist(level, platformPos.getX() + 1.2, surface, platformPos.getZ() + 2.2,
                "Eleanor Hart", "Oakvale", "Rivertown");

        alphaX = platformPos.getX() - 0.6;
        betaX = platformPos.getX() + 2.0;
        viewZ = platformPos.getZ() + 3.6;
        viewY = surface;
        level.setDefaultSpawnPos(BlockPos.containing(alphaX, viewY, viewZ), 180f);

        ready = true;
        String detail = "towns=3 platform=" + platformPos.getX() + "," + platformPos.getY() + ","
                + platformPos.getZ() + " rails=" + rails + " tourists=2 seeded=Oakvale->Hillcrest,Rivertown";
        LOGGER.info("MP_SMOKE_READY {}", detail);
        writeReady(server, true, detail);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            welcome(player);
        }
    }

    private static UUID placeTown(ServerLevel level, BlockPos placeAt, String name) {
        ShowcaseWorldBuild.preparePad(level, placeAt);
        level.setBlock(placeAt, CommonModBlocks.TOWN_INTERFACE_BLOCK.get().defaultBlockState(), 3);
        if (!(level.getBlockEntity(placeAt) instanceof TownInterfaceEntity townEntity)) {
            throw new IllegalStateException("no town block entity at " + placeAt);
        }
        UUID id = TownManager.get(level).registerTown(placeAt, name);
        if (id == null) {
            String error = TownManager.get(level).getTownPlacementError(placeAt);
            throw new IllegalStateException("town " + name + " refused at " + placeAt + ": " + error);
        }
        townEntity.setTownId(id);
        townEntity.setChanged();
        BlockState state = level.getBlockState(placeAt);
        level.sendBlockUpdated(placeAt, state, state, 3);
        LOGGER.info("MP_SMOKE_TOWN {} {} at {}", name, id, placeAt);
        return id;
    }

    private static TownInterfaceEntity townEntity(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof TownInterfaceEntity town) {
            return town;
        }
        throw new IllegalStateException("town entity missing at " + pos);
    }

    /** Keep scripted tourists out of the arrival capsule and stop live spawning. */
    private static void tighten(ServerLevel level, BlockPos townPos) {
        TownInterfaceEntity townEntity = townEntity(level, townPos);
        for (Platform platform : townEntity.getPlatforms()) {
            BlockPos pos = platform.getInterfacePos();
            if (pos == null) {
                continue;
            }
            platform.setSearchRadius(1);
            platform.setSpread(0);
            platform.setStartPos(pos.north(1));
            platform.setEndPos(pos.north(2));
        }
        townEntity.setTouristSpawningEnabled(false);
        Town town = townEntity.getTown();
        if (town != null) {
            int pending = town.getPendingTouristSpawns();
            if (pending != 0) {
                town.addPendingTouristSpawns(-pending);
            }
        }
        townEntity.setChanged();
        townEntity.syncPlatforms();
    }

    private static void seedLearnedRoute(ServerLevel level, TownInterfaceEntity oak, UUID platformId,
            UUID hillId, UUID riverId) {
        Platform platform = oak.getPlatform(platformId);
        if (platform == null) {
            throw new IllegalStateException("seeded platform " + platformId + " is missing");
        }
        platform.setEnabled(true);
        platform.setDestinationMode(DestinationMode.AUTO);
        platform.enableDestination(hillId);
        long tick = level.getGameTime();
        // Same trip length so the displayed strength ratio stays 5:3 while the server idles.
        platform.getLearnedRoutes().recordSuccess(hillId, tick, 5.0, 2400L, 12, 0.05);
        platform.getLearnedRoutes().recordSuccess(riverId, tick, 3.0, 2400L, 12, 0.05);
        oak.setChanged();
        oak.syncPlatforms();
        LOGGER.info("MP_SMOKE_SEEDED platform {} AUTO Hillcrest=5 Rivertown=3", platformId);
    }

    private static int layRailZ(ServerLevel level, int x, int y, int fromZ, int toZ) {
        int count = 0;
        BlockState rail = Blocks.RAIL.defaultBlockState();
        for (int z = Math.min(fromZ, toZ); z <= Math.max(fromZ, toZ); z++) {
            placeRail(level, new BlockPos(x, y, z), rail);
            count++;
        }
        return count;
    }

    private static int layRailX(ServerLevel level, int z, int y, int fromX, int toX) {
        int count = 0;
        BlockState rail = Blocks.RAIL.defaultBlockState();
        for (int x = Math.min(fromX, toX); x <= Math.max(fromX, toX); x++) {
            placeRail(level, new BlockPos(x, y, z), rail);
            count++;
        }
        return count;
    }

    private static void placeRail(ServerLevel level, BlockPos at, BlockState rail) {
        level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(at, rail, 3);
        if (!level.getBlockState(at.above()).isAir()) {
            level.setBlock(at.above(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static void spawnTourist(ServerLevel level, double x, double y, double z,
            String personalName, String origin, String destination) {
        TouristEntity tourist = new TouristEntity(CommonModEntityTypes.TOURIST.get(), level);
        tourist.applyDisplaySample(origin, destination, TouristType.SIGHTSEER, 900);
        tourist.setPersonalName(personalName);
        tourist.setCustomNameVisible(true);
        tourist.setNoAi(true);
        tourist.setNoGravity(true);
        tourist.setInvulnerable(true);
        tourist.setPersistenceRequired();
        tourist.setPos(x, y, z);
        tourist.setYRot(0f);
        tourist.setYHeadRot(0f);
        level.addFreshEntity(tourist);
        LOGGER.info("MP_SMOKE_TOURIST {} {} -> {}", personalName, origin, destination);
    }

    private static void welcome(ServerPlayer player) {
        if (!ready) {
            return;
        }
        boolean beta = "BC_Beta".equals(player.getGameProfile().getName());
        double x = beta ? betaX : alphaX;
        player.setGameMode(GameType.CREATIVE);
        player.teleportTo(player.serverLevel(), x, viewY, viewZ, 180f, 8f);
        player.setYHeadRot(180f);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        if (CommonModItems.CONDUCTOR_CLIPBOARD != null && CommonModItems.CONDUCTOR_CLIPBOARD.get() != null) {
            player.setItemInHand(InteractionHand.OFF_HAND,
                    new ItemStack(CommonModItems.CONDUCTOR_CLIPBOARD.get()));
        } else {
            LOGGER.error("MP_SMOKE_FAILED conductor clipboard item is not registered");
        }
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = false;
        player.getAbilities().invulnerable = true;
        player.onUpdateAbilities();
        LOGGER.info("MP_SMOKE_WELCOME {} at {}, {}, {}", player.getGameProfile().getName(), x, viewY, viewZ);
    }

    private static void writeReady(MinecraftServer server, boolean ok, String detail) {
        if (server == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok);
        body.put("detail", detail == null ? "" : detail);
        try {
            Files.writeString(server.getServerDirectory().toPath().resolve("mp-smoke-ready.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(body), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.error("MP_SMOKE_FAILED could not write mp-smoke-ready.json", exception);
        }
    }
}
