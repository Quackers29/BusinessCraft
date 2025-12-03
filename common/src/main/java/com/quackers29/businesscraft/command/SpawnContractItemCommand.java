package com.quackers29.businesscraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.quackers29.businesscraft.util.ContractItemHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Debug command for spawning contract items for testing
 */
public class SpawnContractItemCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bc")
                .then(Commands.literal("debug")
                        .then(Commands.literal("spawnContractItem")
                                .then(Commands.argument("resource", StringArgumentType.word())
                                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                .executes(context -> execute(context, "Test Town"))
                                                .then(Commands.argument("destTown", StringArgumentType.greedyString())
                                                        .executes(context -> execute(context,
                                                                StringArgumentType.getString(context,
                                                                        "destTown")))))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, String destTown) {
        String resource = StringArgumentType.getString(context, "resource");
        int quantity = IntegerArgumentType.getInteger(context, "quantity");

        // Validate resource type
        if (!isValidResource(resource)) {
            context.getSource().sendFailure(
                    Component.literal("Invalid resource type. Valid types: wood, iron, coal"));
            return 0;
        }

        try {
            ServerPlayer player = context.getSource().getPlayerOrException();

            // Create contract item with dummy data for testing
            UUID dummyContractId = UUID.randomUUID();
            UUID dummyDestId = UUID.randomUUID();
            String sourceTown = "Debug Source";

            ItemStack contractItem = ContractItemHelper.createContractItem(
                    resource,
                    quantity,
                    dummyContractId,
                    dummyDestId,
                    destTown,
                    sourceTown);

            // Spawn item at player's feet
            ItemEntity itemEntity = new ItemEntity(
                    player.level(),
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    contractItem);
            player.level().addFreshEntity(itemEntity);

            context.getSource().sendSuccess(
                    () -> Component.literal("Spawned contract item: " + quantity + "x " + resource +
                            " → " + destTown),
                    true);

            System.out.println("[DEBUG] Spawned contract item with ID: " + dummyContractId);

            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("Error spawning contract item: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    private static boolean isValidResource(String resource) {
        return resource.equalsIgnoreCase("wood") ||
                resource.equalsIgnoreCase("iron") ||
                resource.equalsIgnoreCase("coal");
    }
}
