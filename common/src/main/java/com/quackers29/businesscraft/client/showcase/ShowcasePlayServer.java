package com.quackers29.businesscraft.client.showcase;

import com.google.gson.GsonBuilder;
import com.quackers29.businesscraft.api.PlatformAccess;
import com.quackers29.businesscraft.block.PlatformInterfaceBlock;
import com.quackers29.businesscraft.block.entity.PlatformInterfaceEntity;
import com.quackers29.businesscraft.block.entity.TownInterfaceEntity;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import com.quackers29.businesscraft.entity.TouristEntity;
import com.quackers29.businesscraft.entity.TouristType;
import com.quackers29.businesscraft.init.CommonModBlocks;
import com.quackers29.businesscraft.init.CommonModEntityTypes;
import com.quackers29.businesscraft.init.CommonModItems;
import com.quackers29.businesscraft.platform.DestinationMode;
import com.quackers29.businesscraft.platform.Platform;
import com.quackers29.businesscraft.town.Town;
import com.quackers29.businesscraft.town.TownManager;
import com.quackers29.businesscraft.town.data.RewardSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.entity.SignText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Dedicated-server half of {@code tools/showcase-play.sh}: a world to walk around in.
 *
 * <p>Selected with {@code -PmpSmoke=play}. Builds the towns in {@link ShowcasePlayLayout}, ticket
 * machines in every destination mode, a rail line with a scripted cart dispatcher so tourists really
 * travel, contracts, payment-board rewards, signs and a guide chest. Unlike the smoke it never quits.
 */
public final class ShowcasePlayServer {
    private static final Logger LOGGER = LoggerFactory.getLogger("BusinessCraft/Play");
    private static final boolean ENABLED = ShowcasePlayLayout.MODE.equals(System.getProperty(MultiplayerSmokeServer.PROPERTY));
    private static final int READY_DELAY_TICKS = 100;
    private static final String SEEN_TAG = "bc_play_seen";
    /** Blocks per tick. Vanilla's own limit on plain rail is 0.4. */
    private static final double CART_SPEED = 0.3;
    private static final int DWELL_TICKS = 500;
    private static final int ARRIVAL_TICKS = 160;
    private static final int RESUPPLY_TICKS = 400;
    private static final String[] NAMES = {"Morgan Hale", "Eleanor Hart", "Tobias Reed", "Ingrid Vale",
            "Callum Frost", "Priya Nair"};

    private static int ticks;
    private static boolean started;
    private static volatile boolean ready;
    private static int surface;
    private static double viewX;
    private static double viewZ;
    private static final List<Line> LINES = new ArrayList<>();
    private static final List<UUID> TOWN_IDS = new ArrayList<>();
    private static final List<BlockPos> TOWN_POS = new ArrayList<>();

    private ShowcasePlayServer() {
    }

    static boolean enabled() {
        return ENABLED;
    }

    public static void initialize() {
        if (!ENABLED || PlatformAccess.getEvents() == null) {
            return;
        }
        PlatformAccess.getEvents().registerPlayerLoginCallback((player, level, pos) -> welcome(player));
    }

    public static void onLevelTick(ServerLevel level) {
        if (!ENABLED || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!started) {
            if (++ticks < READY_DELAY_TICKS) {
                return;
            }
            started = true;
            try {
                build(level);
            } catch (RuntimeException exception) {
                LOGGER.error("PLAY_FAILED {}", exception.toString(), exception);
                writeReady(level.getServer(), false, exception.toString());
            }
            return;
        }
        if (ready) {
            dispatch(level);
        }
    }

    // ---- build -----------------------------------------------------------------------------

    private static void build(ServerLevel level) {
        MinecraftServer server = level.getServer();
        ShowcaseWorldBuild.pinDaytime(level);
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, server);
        level.getGameRules().getRule(GameRules.RULE_KEEPINVENTORY).set(true, server);

        // The template world can carry a built-up spawn at (0, 0); sample open ground beside it.
        level.getChunk(4, 4);
        surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, 64, 64);
        if (surface < -64 || surface > 320) {
            throw new IllegalStateException("refusing to build at surface Y " + surface);
        }

        ShowcaseTownNames.reset();
        for (ShowcasePlayLayout.TownSpec spec : ShowcasePlayLayout.TOWNS) {
            BlockPos pos = new BlockPos(spec.dx(), surface, spec.dz());
            TOWN_POS.add(pos);
            TOWN_IDS.add(MultiplayerSmokeServer.placeTown(level, pos, spec.name()));
        }
        for (int i = 0; i < TOWN_POS.size(); i++) {
            Town town = MultiplayerSmokeServer.townEntity(level, TOWN_POS.get(i)).getTown();
            if (town != null) {
                town.setPopulation(ShowcasePlayLayout.TOWNS.get(i).population());
            }
        }

        int platforms = 0;
        platforms += buildOakvale(level);
        platforms += buildLineStop(level, ShowcasePlayLayout.HILLCREST, DestinationMode.SPECIFIC, ShowcasePlayLayout.RIVERTOWN);
        platforms += buildLineStop(level, ShowcasePlayLayout.RIVERTOWN, DestinationMode.ANY, -1);
        platforms += buildLineStop(level, ShowcasePlayLayout.PINEWOOD, DestinationMode.SPECIFIC, ShowcasePlayLayout.HILLCREST);

        int rails = 0;
        for (ShowcasePlayLayout.Segment segment : ShowcasePlayLayout.SEGMENTS) {
            rails += layTrack(level, segment);
            LINES.add(new Line(segment));
        }
        rails += layStub(level, ShowcasePlayLayout.PINEWOOD);

        seedTowns(level);
        int contracts = seedContracts(level);
        int signs = 0;
        for (ShowcasePlayLayout.SignSpec spec : ShowcasePlayLayout.SIGNS) {
            BlockPos town = TOWN_POS.get(spec.town());
            placeSign(level, town.offset(spec.dx(), 0, spec.dz()), spec.rotation(), spec.lines());
            signs++;
        }
        placeGuideChest(level);
        int tourists = spawnLineup(level);

        viewX = ShowcasePlayLayout.TOWNS.get(0).dx() + ShowcasePlayLayout.VIEW_DX + 0.5;
        viewZ = ShowcasePlayLayout.TOWNS.get(0).dz() + ShowcasePlayLayout.VIEW_DZ + 0.5;
        level.setDefaultSpawnPos(BlockPos.containing(viewX, surface, viewZ), 180f);

        ready = true;
        String detail = "towns=" + TOWN_IDS.size() + " platforms=" + platforms + " rails=" + rails
                + " signs=" + signs + " contracts=" + contracts + " tourists=" + tourists
                + " lines=" + LINES.size() + " layout=" + ShowcasePlayLayout.summary();
        LOGGER.info("PLAY_READY {}", detail);
        writeReady(server, true, detail);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            welcome(player);
        }
    }

    /** Oakvale: the line platform plus one ticket machine per other mode, and one switched off. */
    private static int buildOakvale(ServerLevel level) {
        BlockPos townPos = TOWN_POS.get(ShowcasePlayLayout.OAKVALE);
        TownInterfaceEntity town = MultiplayerSmokeServer.townEntity(level, townPos);
        int y = surface;
        int x = townPos.getX();
        int z = townPos.getZ();
        UUID hill = TOWN_IDS.get(ShowcasePlayLayout.HILLCREST);
        UUID river = TOWN_IDS.get(ShowcasePlayLayout.RIVERTOWN);

        placeLinePlatform(level, town, townPos, DestinationMode.SPECIFIC, hill);
        // AUTO: wide spawn area, with learned routes already weighted 5:3.
        UUID auto = placePlatform(level, town, townPos, new BlockPos(x + 8, y, z + 6), Direction.WEST,
                new BlockPos(x + 10, y, z + 6), new BlockPos(x + 18, y, z + 6), 5, 4, DestinationMode.AUTO, true);
        Platform autoPlatform = town.getPlatform(auto);
        long tick = level.getGameTime();
        autoPlatform.getLearnedRoutes().recordSuccess(hill, tick, 5.0, 2400L, 12, 0.05);
        autoPlatform.getLearnedRoutes().recordSuccess(river, tick, 3.0, 2400L, 12, 0.05);
        // ANY: narrow path on the line.
        placePlatform(level, town, townPos, new BlockPos(x + 8, y, z + 12), Direction.WEST,
                new BlockPos(x + 10, y, z + 12), new BlockPos(x + 18, y, z + 12), 3, 0, DestinationMode.ANY, true);
        // Off: the sign lamp stays dark.
        placePlatform(level, town, townPos, new BlockPos(x + 8, y, z + 18), Direction.WEST,
                new BlockPos(x + 10, y, z + 18), new BlockPos(x + 18, y, z + 18), 3, 0, DestinationMode.ANY, false);
        for (int row : new int[] {6, 12, 18}) {
            layRow(level, x + 10, x + 18, z + row);
        }
        finish(town);
        return 4;
    }

    private static int buildLineStop(ServerLevel level, int index, DestinationMode mode, int destination) {
        BlockPos townPos = TOWN_POS.get(index);
        TownInterfaceEntity town = MultiplayerSmokeServer.townEntity(level, townPos);
        placeLinePlatform(level, town, townPos, mode, destination < 0 ? null : TOWN_IDS.get(destination));
        finish(town);
        return 1;
    }

    private static void placeLinePlatform(ServerLevel level, TownInterfaceEntity town, BlockPos townPos,
            DestinationMode mode, UUID destination) {
        int tx = townPos.getX() + ShowcasePlayLayout.TRACK_DX;
        int z = townPos.getZ();
        placePlatform(level, town, townPos,
                new BlockPos(tx + 2, surface, z), Direction.WEST,
                new BlockPos(tx, surface, z + ShowcasePlayLayout.LINE_HALF),
                new BlockPos(tx, surface, z - ShowcasePlayLayout.LINE_HALF),
                ShowcasePlayLayout.LINE_RADIUS, 0, mode, true, destination);
    }

    private static UUID placePlatform(ServerLevel level, TownInterfaceEntity town, BlockPos townPos, BlockPos at,
            Direction facing, BlockPos start, BlockPos end, int radius, int spread, DestinationMode mode,
            boolean enabled, UUID... destinations) {
        level.setBlock(at.below(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        level.setBlock(at, CommonModBlocks.PLATFORM_INTERFACE_BLOCK.get().defaultBlockState()
                .setValue(PlatformInterfaceBlock.FACING, facing), 3);
        if (!(level.getBlockEntity(at) instanceof PlatformInterfaceEntity entity)) {
            throw new IllegalStateException("platform block entity missing at " + at);
        }
        UUID id = town.addInterfacePlatform(at);
        if (id == null) {
            throw new IllegalStateException("town refused a platform at " + at);
        }
        entity.link(town.getTownId(), id, townPos);
        town.setPlatformPathStart(id, start);
        town.setPlatformPathEnd(id, end);
        Platform platform = town.getPlatform(id);
        platform.setSearchRadius(radius);
        platform.setSpread(spread);
        platform.setDestinationMode(mode);
        for (UUID destination : destinations) {
            if (destination != null) {
                platform.enableDestination(destination);
            }
        }
        platform.setEnabled(enabled);
        return id;
    }

    private static void finish(TownInterfaceEntity town) {
        town.setTouristSpawningEnabled(true);
        Town data = town.getTown();
        if (data != null) {
            data.addPendingTouristSpawns(3);
        }
        town.setChanged();
        town.syncPlatforms();
    }

    // ---- track -----------------------------------------------------------------------------

    private static int layTrack(ServerLevel level, ShowcasePlayLayout.Segment segment) {
        int x = ShowcasePlayLayout.trackX(segment.from());
        int from = ShowcasePlayLayout.departZ(segment);
        int to = ShowcasePlayLayout.arriveZ(segment);
        int count = 0;
        for (int z = Math.min(from, to); z <= Math.max(from, to); z++) {
            rail(level, new BlockPos(x, surface, z));
            count++;
        }
        for (int cx = x >> 4; cx <= x >> 4; cx++) {
            for (int cz = Math.min(from, to) >> 4; cz <= Math.max(from, to) >> 4; cz++) {
                level.setChunkForced(cx, cz, true);
            }
        }
        // Buffer behind the arrival stop and a shelter post with a lantern at each stop.
        level.setBlock(new BlockPos(x, surface, to - 1), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        station(level, x, from);
        station(level, x, to);
        return count;
    }

    /** A short stub of rail so a platform with no cart still reads as a station. */
    private static int layStub(ServerLevel level, int town) {
        BlockPos pos = TOWN_POS.get(town);
        int x = pos.getX() + ShowcasePlayLayout.TRACK_DX;
        int count = 0;
        for (int dz = -ShowcasePlayLayout.LINE_HALF; dz <= ShowcasePlayLayout.LINE_HALF; dz++) {
            if (Math.abs(dz) > 5) {
                rail(level, new BlockPos(x, surface, pos.getZ() + dz));
                count++;
            }
        }
        return count;
    }

    private static int layRow(ServerLevel level, int fromX, int toX, int z) {
        int count = 0;
        for (int x = fromX; x <= toX; x++) {
            rail(level, new BlockPos(x, surface, z));
            count++;
        }
        return count;
    }

    private static void rail(ServerLevel level, BlockPos at) {
        level.setBlock(at.below(), Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(at, Blocks.RAIL.defaultBlockState(), 3);
    }

    private static void station(ServerLevel level, int x, int z) {
        BlockPos post = new BlockPos(x + 1, surface, z);
        level.setBlock(post, Blocks.OAK_FENCE.defaultBlockState(), 3);
        level.setBlock(post.above(), Blocks.OAK_FENCE.defaultBlockState(), 3);
        level.setBlock(post.above(2), Blocks.LANTERN.defaultBlockState(), 3);
    }

    // ---- content ---------------------------------------------------------------------------

    private static final Item[][] STOCK = {
            {Items.OAK_LOG, Items.COBBLESTONE, Items.IRON_INGOT, Items.BREAD, Items.EMERALD, Items.WHEAT},
            {Items.IRON_INGOT, Items.COAL, Items.STONE, Items.COPPER_INGOT, Items.GOLD_INGOT, Items.EMERALD},
            {Items.COD, Items.SALMON, Items.SAND, Items.CLAY_BALL, Items.SUGAR_CANE, Items.EMERALD},
            {Items.SPRUCE_LOG, Items.DIAMOND, Items.GOLD_INGOT, Items.EMERALD, Items.APPLE, Items.LAPIS_LAZULI}};
    private static final int[][] AMOUNT = {
            {320, 500, 120, 90, 40, 200}, {300, 400, 600, 150, 60, 25},
            {150, 80, 200, 120, 90, 15}, {600, 12, 80, 200, 64, 90}};
    /** Visits recorded for each town: {town index, origin index, count}. */
    private static final int[][] VISITS = {
            {0, 1, 4}, {0, 2, 2}, {1, 0, 6}, {1, 3, 1}, {2, 1, 3}, {2, 0, 1}, {3, 1, 2}};

    private static void seedTowns(ServerLevel level) {
        for (int i = 0; i < TOWN_IDS.size(); i++) {
            Town town = TownManager.get(level).getTown(TOWN_IDS.get(i));
            if (town == null) {
                continue;
            }
            // The random biome variant may skip the starting node that grants the tourist cap.
            if (!town.getUpgrades().isUnlocked("basic_settlement")) {
                town.getUpgrades().unlockNode("basic_settlement");
            }
            if (town.getUpgrades().getModifier("tourist_cap") < 1f) {
                town.getUpgrades().addFlatModifier("tourist_cap", 6f);
            }
            for (int r = 0; r < STOCK[i].length; r++) {
                town.addResource(STOCK[i][r], AMOUNT[i][r]);
            }
            town.getPaymentBoard().addReward(RewardSource.TOURIST_PAYMENT,
                    List.of(new ItemStack(Items.EMERALD, 6)), "ALL");
            town.getPaymentBoard().addReward(RewardSource.MILESTONE,
                    List.of(new ItemStack(Items.DIAMOND, 1), new ItemStack(Items.EMERALD, 4)), "ALL");
            town.getPaymentBoard().addReward(RewardSource.TRADE,
                    List.of(new ItemStack(Items.GOLD_INGOT, 5)), "ALL");
        }
        for (int[] visit : VISITS) {
            Town town = TownManager.get(level).getTown(TOWN_IDS.get(visit[0]));
            if (town != null) {
                town.recordVisit(TOWN_IDS.get(visit[1]), visit[2], TOWN_POS.get(visit[1]));
            }
        }
        for (BlockPos pos : TOWN_POS) {
            TownInterfaceEntity entity = MultiplayerSmokeServer.townEntity(level, pos);
            entity.setChanged();
            entity.updateInterfaceVM();
            entity.syncAllDirtyTownVMsToPlayers();
        }
    }

    private static int seedContracts(ServerLevel level) {
        ContractBoard board = ContractBoard.get(level);
        TownManager towns = TownManager.get(level);
        int count = 0;
        try {
            count += openAuction(board, towns.getTown(TOWN_IDS.get(ShowcasePlayLayout.PINEWOOD)), Items.DIAMOND, "diamond", 2, 6f);
            count += openAuction(board, towns.getTown(TOWN_IDS.get(ShowcasePlayLayout.HILLCREST)), Items.COAL, "coal", 32, 1f);
            count += openAuction(board, towns.getTown(TOWN_IDS.get(ShowcasePlayLayout.OAKVALE)), Items.BREAD, "bread", 16, 1f);

            // One finished trade, so the list shows history: Rivertown wins Oakvale's emeralds.
            Town seller = towns.getTown(TOWN_IDS.get(ShowcasePlayLayout.OAKVALE));
            Town buyer = towns.getTown(TOWN_IDS.get(ShowcasePlayLayout.RIVERTOWN));
            seller.addResource(Items.EMERALD, 4);
            SellContract sold = new SellContract(seller.getId(), seller.getName(), 60_000L, "emerald", 2, 1.0f);
            board.addContract(sold);
            seller.addResource(Items.EMERALD, -2);
            seller.addEscrowResource(Items.EMERALD, 2);
            buyer.addResource(Items.EMERALD, 200);
            board.addBid(sold.getId(), buyer.getId(), 12f, level);
            for (int pass = 0; pass < 3; pass++) {
                sold.expireNow();
                board.tick(level);
            }
            LOGGER.info("PLAY_TRADE completed={} closed={}", sold.isCompleted(), sold.isAuctionClosed());
        } catch (RuntimeException exception) {
            LOGGER.warn("PLAY_CONTRACTS partial: {}", exception.toString());
        }
        return count + 1;
    }

    private static int openAuction(ContractBoard board, Town seller, Item item, String id, long quantity, float min) {
        if (seller == null) {
            return 0;
        }
        seller.addResource(item, quantity);
        SellContract contract = new SellContract(seller.getId(), seller.getName(), 86_400_000L, id, quantity, min);
        board.addContract(contract);
        seller.addResource(item, -quantity);
        seller.addEscrowResource(item, quantity);
        return 1;
    }

    private static void placeSign(ServerLevel level, BlockPos at, int rotation, List<String> lines) {
        BlockPos pos = new BlockPos(at.getX(), surface, at.getZ());
        if (level.getBlockState(pos.below()).isAir()) {
            level.setBlock(pos.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        }
        level.setBlock(pos, Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rotation), 3);
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof SignBlockEntity sign) {
            SignText text = new SignText();
            for (int i = 0; i < Math.min(4, lines.size()); i++) {
                text = text.setMessage(i, Component.literal(lines.get(i)));
            }
            sign.setText(text, true);
            sign.setChanged();
            BlockState state = level.getBlockState(pos);
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static void placeGuideChest(ServerLevel level) {
        BlockPos oak = TOWN_POS.get(ShowcasePlayLayout.OAKVALE);
        BlockPos pos = new BlockPos(oak.getX() + ShowcasePlayLayout.VIEW_DX + 8, surface, oak.getZ() + ShowcasePlayLayout.VIEW_DZ - 2);
        level.setBlock(pos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH), 3);
        if (!(level.getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            throw new IllegalStateException("guide chest missing at " + pos);
        }
        int slot = 0;
        if (CommonModItems.CONDUCTOR_CLIPBOARD != null && CommonModItems.CONDUCTOR_CLIPBOARD.get() != null) {
            chest.setItem(slot++, new ItemStack(CommonModItems.CONDUCTOR_CLIPBOARD.get()));
        } else {
            LOGGER.error("PLAY_FAILED conductor clipboard item is not registered");
        }
        chest.setItem(slot++, guideBook());
        chest.setItem(slot++, new ItemStack(Items.MINECART, 4));
        chest.setItem(slot++, new ItemStack(Items.RAIL, 64));
        chest.setItem(slot++, new ItemStack(Items.POWERED_RAIL, 16));
        chest.setItem(slot, new ItemStack(Items.REDSTONE_BLOCK, 8));
        chest.setChanged();
    }

    private static ItemStack guideBook() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = book.getOrCreateTag();
        tag.putString("title", "BusinessCraft Tour");
        tag.putString("author", "Showcase");
        ListTag pages = new ListTag();
        for (String page : ShowcasePlayLayout.guidePages()) {
            pages.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(page))));
        }
        tag.put("pages", pages);
        return book;
    }

    /** Tourists with name tags standing in front of the spawn point, one of each type. */
    private static int spawnLineup(ServerLevel level) {
        BlockPos oak = TOWN_POS.get(ShowcasePlayLayout.OAKVALE);
        TouristType[] types = TouristType.values();
        String[] towns = {"Hillcrest", "Rivertown", "Pinewood", "Hillcrest", "Oakvale"};
        int count = 0;
        for (int i = 0; i < 5; i++) {
            TouristEntity tourist = new TouristEntity(CommonModEntityTypes.TOURIST.get(), level);
            tourist.applyDisplaySample("Oakvale", towns[i], types[i % types.length], 300 + 150 * i);
            tourist.setPersonalName(NAMES[i % NAMES.length]);
            tourist.setCustomNameVisible(true);
            tourist.setNoAi(true);
            tourist.setNoGravity(true);
            tourist.setInvulnerable(true);
            tourist.setPersistenceRequired();
            tourist.setPos(oak.getX() + ShowcasePlayLayout.VIEW_DX - 4 + i * 2.0 + 0.5, surface,
                    oak.getZ() + ShowcasePlayLayout.VIEW_DZ - 4.5);
            tourist.setYRot(0f);
            tourist.setYHeadRot(0f);
            level.addFreshEntity(tourist);
            count++;
        }
        return count;
    }

    // ---- cart dispatcher -------------------------------------------------------------------

    private enum Phase {
        WAITING,
        MOVING,
        ARRIVED
    }

    /** One scripted cart on one segment: wait for boarding, run to the next town, drop it, repeat. */
    private static final class Line {
        final ShowcasePlayLayout.Segment segment;
        UUID cart;
        Phase phase = Phase.WAITING;
        int timer;

        Line(ShowcasePlayLayout.Segment segment) {
            this.segment = segment;
        }
    }

    private static void dispatch(ServerLevel level) {
        for (Line line : LINES) {
            step(level, line);
        }
        if (level.getGameTime() % 1200 == 0) {
            StringBuilder report = new StringBuilder();
            for (int i = 0; i < TOWN_IDS.size(); i++) {
                Town town = TownManager.get(level).getTown(TOWN_IDS.get(i));
                if (town != null) {
                    report.append(ShowcasePlayLayout.TOWNS.get(i).name()).append(" can=").append(town.canSpawnTourists())
                            .append(" pending=").append(town.getPendingTouristSpawns())
                            .append(" tourists=").append(town.getTouristCount())
                            .append('/').append(town.getMaxTourists()).append("; ");
                }
            }
            LOGGER.info("PLAY_STATUS {}", report);
        }
        if (level.getGameTime() % RESUPPLY_TICKS == 0) {
            for (BlockPos pos : TOWN_POS) {
                if (level.getBlockEntity(pos) instanceof TownInterfaceEntity entity && entity.getTown() != null
                        && entity.getTown().getPendingTouristSpawns() < 2) {
                    entity.getTown().addPendingTouristSpawns(2);
                }
            }
        }
    }

    private static void step(ServerLevel level, Line line) {
        double x = ShowcasePlayLayout.trackX(line.segment.from()) + 0.5;
        double departZ = ShowcasePlayLayout.departZ(line.segment) + 0.5;
        double arriveZ = ShowcasePlayLayout.arriveZ(line.segment) + 0.5;
        Entity entity = line.cart == null ? null : level.getEntity(line.cart);
        Minecart cart = entity instanceof Minecart found && found.isAlive() ? found : null;
        switch (line.phase) {
            case WAITING -> {
                if (cart == null) {
                    cart = new Minecart(level, x, surface, departZ);
                    level.addFreshEntity(cart);
                    line.cart = cart.getUUID();
                    line.timer = 0;
                }
                if (++line.timer >= DWELL_TICKS) {
                    LOGGER.info("PLAY_DEPART {}->{} passengers={}", ShowcasePlayLayout.TOWNS.get(line.segment.from()).name(),
                            ShowcasePlayLayout.TOWNS.get(line.segment.to()).name(), cart.getPassengers().size());
                    cart.setDeltaMovement(0, 0, -CART_SPEED);
                    line.phase = Phase.MOVING;
                }
            }
            case MOVING -> {
                if (cart == null) {
                    line.phase = Phase.WAITING;
                    line.timer = 0;
                } else if (cart.getZ() <= arriveZ) {
                    cart.setPos(x, cart.getY(), arriveZ);
                    cart.setDeltaMovement(0, 0, 0);
                    line.phase = Phase.ARRIVED;
                    line.timer = 0;
                    LOGGER.info("PLAY_ARRIVE at {} passengers={}", ShowcasePlayLayout.TOWNS.get(line.segment.to()).name(),
                            cart.getPassengers().size());
                } else {
                    cart.setDeltaMovement(0, cart.getDeltaMovement().y, -CART_SPEED);
                }
            }
            case ARRIVED -> {
                boolean empty = cart == null || cart.getPassengers().isEmpty();
                if (++line.timer >= ARRIVAL_TICKS || (empty && line.timer >= 60)) {
                    if (cart != null) {
                        cart.ejectPassengers();
                        cart.discard();
                    }
                    line.cart = null;
                    line.phase = Phase.WAITING;
                    line.timer = 0;
                }
            }
        }
    }

    // ---- players ---------------------------------------------------------------------------

    private static void welcome(ServerPlayer player) {
        if (!ready) {
            return;
        }
        player.setGameMode(GameType.CREATIVE);
        if (!player.getTags().contains(SEEN_TAG)) {
            player.addTag(SEEN_TAG);
            player.teleportTo(player.serverLevel(), viewX, surface, viewZ, 180f, 8f);
            player.setYHeadRot(180f);
            if (CommonModItems.CONDUCTOR_CLIPBOARD != null && CommonModItems.CONDUCTOR_CLIPBOARD.get() != null) {
                player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(CommonModItems.CONDUCTOR_CLIPBOARD.get()));
            }
        }
        player.getAbilities().mayfly = true;
        player.onUpdateAbilities();
        LOGGER.info("PLAY_WELCOME {}", player.getGameProfile().getName());
    }

    private static void writeReady(MinecraftServer server, boolean ok, String detail) {
        if (server == null) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", ok);
        body.put("detail", detail == null ? "" : detail);
        try {
            Files.writeString(server.getServerDirectory().toPath().resolve("showcase-play-ready.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(body), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOGGER.error("PLAY_FAILED could not write showcase-play-ready.json", exception);
        }
    }
}
