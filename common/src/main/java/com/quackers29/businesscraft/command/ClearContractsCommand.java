package com.quackers29.businesscraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.data.ContractSavedData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;

public class ClearContractsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bc")
                .then(Commands.literal("debug")
                        .then(Commands.literal("clear_contracts")
                                .requires(source -> source.hasPermission(2))
                                .executes(ClearContractsCommand::execute))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        // Get all ContractBoard instances and clear their contracts
        try {
            // Access the private INSTANCES map via reflection
            java.lang.reflect.Field instancesField = ContractBoard.class.getDeclaredField("INSTANCES");
            instancesField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ServerLevel, ContractBoard> instances = (Map<ServerLevel, ContractBoard>) instancesField.get(null);

            int totalCleared = 0;
            for (ContractBoard instanceBoard : instances.values()) {
                // Access the savedData field via reflection
                java.lang.reflect.Field savedDataField = ContractBoard.class.getDeclaredField("savedData");
                savedDataField.setAccessible(true);
                ContractSavedData savedData = (ContractSavedData) savedDataField.get(instanceBoard);

                // Clear contracts and mark as dirty
                totalCleared += savedData.getContracts().size();
                savedData.getContracts().clear();
                savedData.setDirty();
            }

            final int finalTotalCleared = totalCleared;
            context.getSource().sendSuccess(
                    () -> Component.literal("Cleared " + finalTotalCleared + " contracts from all levels."),
                    true);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to clear contracts: " + e.getMessage()));
            return 0;
        }
    }
}
