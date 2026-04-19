package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.PRIVATE_COLOR;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.text;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import java.util.Objects;

public final class DirectMessageHandlers {

    public void handlePrivateMsg(Packet.PrivateMsgPacket packet) {
        String sender = Objects.requireNonNullElse(packet.sender(), "");
        String message = Objects.requireNonNullElse(packet.msg(), "");
        String username = getUsername();
        if (sender != null && username != null && !username.equalsIgnoreCase(sender)) {
            Awooing.getInstance().lastPrivateSender = sender;
        }

        String header = packet.isOutgoing() ? "[PM To] " : "[PM From] ";
        renderMsg("Awoo", PRIVATE_COLOR, text(header).append(text(sender, packet.color())).append(text(": " + message, PRIVATE_COLOR)));
    }
}