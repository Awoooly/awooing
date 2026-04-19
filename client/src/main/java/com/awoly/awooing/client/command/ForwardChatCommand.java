package com.awoly.awooing.client.command;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.showUsage;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class ForwardChatCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, WrapperLookup registryAccess) {
        dispatcher.register(ClientCommandManager.literal("f")
            .executes(context -> showUsage("/f <message>"))
            .then(ClientCommandManager.argument("message", StringArgumentType.greedyString()).executes(context -> {
                if (!isClientConnected()) {
                    renderMsg(INFO_COLOR, "You are not connected to the server");
                    return Command.SINGLE_SUCCESS;
                }

                if (Awooing.getInstance().forwardAs == null) {
                    renderMsg(INFO_COLOR, "You have to set a sender before using forwarding");
                    return Command.SINGLE_SUCCESS;
                }

                Awooing.getInstance().chatClient
                    .sendPacket(Packet.forwardMsg(Awooing.getInstance().forwardAs, StringArgumentType.getString(context, "message")));
                return Command.SINGLE_SUCCESS;
            })));
    }
}
