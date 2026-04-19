package com.awoly.awooing.client.command;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.showUsage;
import static com.awoly.awooing.client.Utils.suggestUsers;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class AwooMessageCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, WrapperLookup registryAccess) {
        dispatcher.register(ClientCommandManager.literal("amsg")
            .executes(context -> showUsage("/amsg <user> <message>"))
            .then(ClientCommandManager.argument("user", word()).suggests(suggestUsers())
                .then(ClientCommandManager.argument("message", greedyString()).executes(context -> {
                    if (!isClientConnected()) {
                        renderMsg(INFO_COLOR, "You are not connected to the server");
                        return Command.SINGLE_SUCCESS;
                    }

                    Awooing.getInstance().chatClient.sendPacket(
                        Packet.privateMsg(getString(context, "user"), getString(context, "message")));
                    return Command.SINGLE_SUCCESS;
                }))));
    }
}
