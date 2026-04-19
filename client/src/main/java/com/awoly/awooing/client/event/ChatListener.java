package com.awoly.awooing.client.event;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public final class ChatListener {

    public static void register() {
        ClientSendMessageEvents.ALLOW_CHAT.register(ChatListener::interceptMessage);
    }

    private static boolean interceptMessage(String message) {
        if (!Awooing.getInstance().isAwooing) {
            return true;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.msg(Awooing.getInstance().currentRoomId, message));
        return false;
    }
}
