package com.zazow.shipmentbox.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zazow.shipmentbox.Economy;
import com.zazow.shipmentbox.SBMod;
import com.zazow.shipmentbox.config.SBConfig;
import net.minecraft.commands.CommandRuntimeException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;

public class SBCommand {
    public static void init() {

    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("sb")
                .then(Commands.literal("economy")
                        .then(Commands.literal("history")
                                .then(Commands.argument("Item", StringArgumentType.greedyString())
                                        .suggests(((context, builder) -> SharedSuggestionProvider.suggest(SBConfig.GENERAL.getPriceMap().keySet(), builder)))
                                        .executes(SBCommand::executeHistoryCommand)
                                )
                        )
                )
                .then(Commands.literal("reload")
                        .requires(player -> player.hasPermission(2))
                        .executes(SBCommand::reloadCommand)
                )
        );
    }

    public static int executeHistoryCommand(CommandContext<CommandSourceStack> context) throws CommandRuntimeException, CommandSyntaxException {
        Player sender = context.getSource().getPlayerOrException();
        String item = context.getArgument("Item", String.class);

        long day = sender.level.getDayTime() / 24000;
        Economy.ItemHistory history = Economy.get().getItemHistory(item, day);

        String histString = "[";
        for (long i = day + 1; i <= day + Economy.MOVING_AVERAGE_SIZE; ++i) {
            histString += history.history[(int)(i % Economy.MOVING_AVERAGE_SIZE)] + ", ";
        }
        histString += "]";
        sender.sendMessage(new TranslatableComponent("shipmentbox:history_command", item, histString), sender.getUUID());
        return 1;
    }

    public static int reloadCommand(CommandContext<CommandSourceStack> context) throws CommandRuntimeException, CommandSyntaxException {
        SBConfig.GENERAL.computeCachedPrices();
        return 1;
    }
}