package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;

import com.awoly.awooing.common.Packet;

public final class OnlineCountHandler {

    public void handleStatus(Packet.OnlineCountPacket packet) {
        if (packet.count() == null) {
            renderMsg(INFO_COLOR, "Online count unavailable");
            return;
        }

        renderMsg(INFO_COLOR, "Players online: " + packet.count());
    }
}