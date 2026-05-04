package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;

import com.awoly.awooing.common.Packet;

public final class ServerInfoHandler {

    public void handleInfo(Packet.ServerInfoPacket packet) {
        if (packet.usercount() == null) {
            renderMsg(INFO_COLOR, "Online count unavailable");
            return;
        }

        String message = "Players online: " + packet.usercount();
        if (packet.roomCount() != null) {
            message += " | Rooms: " + packet.roomCount();
        }
        renderMsg(INFO_COLOR, message);
    }
}