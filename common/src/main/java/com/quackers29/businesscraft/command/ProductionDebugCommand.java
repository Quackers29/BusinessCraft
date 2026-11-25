package com.quackers29.businesscraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.quackers29.businesscraft.debug.production.ProductionEventLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Op-only: {@code /bc debug production [town]} writes one state-snapshot row
 * per town into the open production event log.
 */
public final class ProductionDebugCommand {
    private ProductionDebugCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bc")
                .then(Commands.literal("debug")
                        .then(Commands.literal("production")
                                .requires(source -> source.hasPermission(2))
                                .executes(ctx -> snapshot(ctx, ""))
                                .then(Commands.argument("town", StringArgumentType.greedyString())
                                        .executes(ctx -> snapshot(ctx,
                                                StringArgumentType.getString(ctx, "town")))))));
    }

    private static int snapshot(CommandContext<CommandSourceStack> context, String town) {
        CommandSourceStack source = context.getSource();
        if (!ProductionEventLog.isOpen()) {
            source.sendFailure(Component.literal(
                    "Production event log is off. Set [debug] productionEventLog in businesscraft.toml."));
            return 0;
        }
        int written = ProductionEventLog.snapshot(source.getServer(), town);
        if (written <= 0) {
            source.sendFailure(Component.literal("No matching town for '" + town + "'."));
            return 0;
        }
        String path = ProductionEventLog.eventsFile() == null
                ? ""
                : ProductionEventLog.eventsFile().toAbsolutePath().toString();
        source.sendSuccess(() -> Component.literal(
                "Wrote " + written + " production snapshot row" + (written == 1 ? "" : "s") + " to " + path),
                true);
        return written;
    }
}
