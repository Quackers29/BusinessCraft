package com.quackers29.businesscraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.quackers29.businesscraft.town.TownManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Admin command for clearing all towns.
 * Unified architecture implementation for cross-platform compatibility.
 */
public class ClearTownsCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cleartowns")
            .requires(source -> source.hasPermission(2))
            .executes(ClearTownsCommand::execute));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        TownManager townManager = TownManager.get(context.getSource().getLevel());
        int count = townManager.getAllTowns().size();
        townManager.clearAllTowns();
        context.getSource().sendSuccess(() -> Component.literal("Cleared " + count + " towns."), true);
        return 1;
    }
}