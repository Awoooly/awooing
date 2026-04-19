package com.awoly.awooing.client.command;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.getActiveRoomId;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.showUsage;
import static com.awoly.awooing.client.Utils.suggestRooms;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class AwooChatCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, WrapperLookup registryAccess) {
        dispatcher.register(ClientCommandManager.literal("a")
            .executes(context -> showUsage("/a <message>"))
            .then(ClientCommandManager.argument("message", StringArgumentType.greedyString())
                .suggests(suggestRooms())
                .executes(context -> {
                    if (!isClientConnected()) {
                        renderMsg(INFO_COLOR, "You are not connected to the server");
                        return Command.SINGLE_SUCCESS;
                    }

                    String rawMessage = StringArgumentType.getString(context, "message");
                    String roomId = Awooing.getInstance().currentRoomId != null
                        ? Awooing.getInstance().currentRoomId
                        : getActiveRoomId();
                    String message = rawMessage;
                    java.util.List<String> joinedRoomIds = Awooing.getInstance().chatClient.getJoinedRooms().keySet().stream().toList();

                    if (joinedRoomIds.size() > 1) {
                        String[] parts = rawMessage.split("\\s+", 2);
                        if (parts.length > 1 && joinedRoomIds.contains(parts[0])) {
                            roomId = parts[0];
                            message = parts[1];
                        }
                    }

                    if (roomId == null) {
                        renderMsg(INFO_COLOR, "You haven't joined any room");
                        return Command.SINGLE_SUCCESS;
                    }

                    Awooing.getInstance().chatClient.sendPacket(Packet.msg(roomId, message));
                    return Command.SINGLE_SUCCESS;
                })));
    }
}
