package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.text;

import com.awoly.awooing.common.Packet;
import java.net.URI;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;

public final class InfoHandlers {

    public void handleInfo(Packet.InfoPacket packet) {
        String prefix = (packet.roomId() == null) ? "Awoo" : packet.roomId();

        if (packet.clickUrl() != null) {
            renderMsg(prefix, INFO_COLOR, text(packet.msg()).styled(style -> style
                .withClickEvent(new ClickEvent.OpenUrl(URI.create(packet.clickUrl())))
                .withHoverEvent(new HoverEvent.ShowText(text(packet.clickUrl())))));
        } else if (packet.clickCommand() != null) {
            renderMsg(prefix, INFO_COLOR, text(packet.msg()).styled(style -> style
                .withClickEvent(new ClickEvent.RunCommand(packet.clickCommand()))
                .withHoverEvent(new HoverEvent.ShowText(text(packet.clickCommand())))));
        } else {
            renderMsg(prefix, INFO_COLOR, packet.msg());
        }
    }
}