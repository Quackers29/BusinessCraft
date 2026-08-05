package com.quackers29.businesscraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.quackers29.businesscraft.debug.TownDataSnapshotBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Op-only command: {@code /bc debug dump} — writes a full-picture SNBT snapshot
 * of all town / contract / market / platform data under
 * {@code <world>/businesscraft/dumps/}.
 */
public final class DumpTownDataCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(DumpTownDataCommand.class);

    private static final DateTimeFormatter FILENAME_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private DumpTownDataCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bc")
                .then(Commands.literal("debug")
                        .then(Commands.literal("dump")
                                .requires(source -> source.hasPermission(2))
                                .executes(DumpTownDataCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        if (server == null) {
            source.sendFailure(Component.literal("No server available for dump."));
            return 0;
        }

        try {
            TownDataSnapshotBuilder.DumpResult result = TownDataSnapshotBuilder.build(server);
            Path file = writeSnapshot(server, result.root());

            int towns = result.townCount();
            int contracts = result.contractCount();
            boolean market = result.marketPresent();
            String pathStr = file.toAbsolutePath().toString();

            source.sendSuccess(
                    () -> Component.literal(String.format(
                            "Wrote BusinessCraft dump (%d towns, %d contracts, market=%s) to %s. "
                                    + "Reflects save() output (escrow/happiness asymmetries included).",
                            towns, contracts, market, pathStr)),
                    true);
            LOGGER.info("Town data dump written to {} (towns={}, contracts={}, market={})",
                    pathStr, towns, contracts, market);
            return 1;
        } catch (IOException e) {
            LOGGER.error("Failed to write town data dump", e);
            source.sendFailure(Component.literal("Failed to write dump: " + e.getMessage()));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Failed to build town data dump", e);
            source.sendFailure(Component.literal("Failed to build dump: " + e.getMessage()));
            return 0;
        }
    }

    private static Path writeSnapshot(MinecraftServer server, CompoundTag root) throws IOException {
        long timestamp = root.contains("dumpTimestamp")
                ? root.getLong("dumpTimestamp")
                : System.currentTimeMillis();
        String stamp = FILENAME_UTC.format(Instant.ofEpochMilli(timestamp));

        Path dumpsDir = server.getWorldPath(LevelResource.ROOT)
                .resolve("businesscraft")
                .resolve("dumps");
        Files.createDirectories(dumpsDir);

        Path file = dumpsDir.resolve("bc-dump-" + stamp + ".snbt");
        String snbt = NbtUtils.structureToSnbt(root);
        Files.writeString(file, snbt, StandardCharsets.UTF_8);
        return file;
    }
}
