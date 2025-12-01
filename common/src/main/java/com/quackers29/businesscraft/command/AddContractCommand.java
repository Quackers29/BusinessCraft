package com.quackers29.businesscraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.quackers29.businesscraft.contract.ContractBoard;
import com.quackers29.businesscraft.contract.SellContract;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class AddContractCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bc")
                .then(Commands.literal("debug")
                        .then(Commands.literal("add_contract")
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .then(Commands.argument("resource", StringArgumentType.word())
                                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("price", FloatArgumentType.floatArg(0))
                                                                .executes(AddContractCommand::execute))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context) {
        String type = StringArgumentType.getString(context, "type");

        String resource = StringArgumentType.getString(context, "resource");
        int quantity = IntegerArgumentType.getInteger(context, "quantity");
        float price = FloatArgumentType.getFloat(context, "price");

        if ("sell".equalsIgnoreCase(type)) {
            UUID dummyTownId = UUID.randomUUID();
            long duration = 60000L; // 60 seconds in milliseconds

            SellContract contract = new SellContract(dummyTownId, duration, resource, quantity, price);

            ContractBoard.getInstance().addContract(contract);

            // Log for debugging
            System.out.println("[DEBUG] Created SellContract: " + contract.getId() + " for " + resource);

            context.getSource().sendSuccess(
                    () -> Component.literal("Added Sell Contract: " + quantity + " " + resource + " for " + price),
                    true);
        } else {
            context.getSource().sendFailure(Component.literal("Unknown contract type: " + type));
        }

        return 1;
    }
}
