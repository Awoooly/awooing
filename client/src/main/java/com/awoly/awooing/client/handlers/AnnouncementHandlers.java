package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;

import com.awoly.awooing.common.Packet;

public final class AnnouncementHandlers {

    public void handleAnnouncement(Packet.AnnouncementPacket packet) {
        int color = packet.color() != null ? packet.color() : INFO_COLOR;
        renderMsg("Awoo", color, packet.msg());
    }
}
