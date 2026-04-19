package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;

public final class ForwardingHandlers {

    public void handleForwardMsg(Packet.ForwardMsgPacket packet) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        ClientPlayNetworkHandler networkHandler = client.getNetworkHandler();
        if (networkHandler != null) {
            networkHandler.sendChatCommand(packet.msg());
        }
    }

    public void handleForwardIsAllowed(Packet.ForwardIsAllowedPacket packet) {
        if (Boolean.TRUE.equals(packet.bool())) {
            Awooing.getInstance().forwardAs = packet.target();
            renderMsg(INFO_COLOR, "Successfully set sender for forwarded messages to " + packet.target());
        } else {
            renderMsg(INFO_COLOR, "You don't have the permission to forward as " + packet.target());
        }
    }
}