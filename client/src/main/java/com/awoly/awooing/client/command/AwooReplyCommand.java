package com.awoly.awooing.client.command;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.showUsage;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class AwooReplyCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, WrapperLookup registryAccess) {
        dispatcher.register(ClientCommandManager.literal("ar")
            .executes(context -> showUsage("/ar <message>"))
            .then(ClientCommandManager.argument("message", greedyString()).executes(context -> {
                if (!isClientConnected()) {
                    renderMsg(INFO_COLOR, "You are not connected to the server");
                    return Command.SINGLE_SUCCESS;
                }

                if (Awooing.getInstance().lastPrivateSender == null) {
                    renderMsg(INFO_COLOR, "Noone to reply to");
                    return Command.SINGLE_SUCCESS;
                }

                Awooing.getInstance().chatClient.sendPacket(
                    Packet.privateMsg(Awooing.getInstance().lastPrivateSender, getString(context, "message")));
                return Command.SINGLE_SUCCESS;
            })));
    }
}
