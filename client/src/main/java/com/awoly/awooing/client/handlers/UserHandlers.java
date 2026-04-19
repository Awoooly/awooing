package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.text;
import static com.awoly.awooing.client.config.ConfigManager.config;
import static com.awoly.awooing.client.config.ConfigManager.save;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;

public final class UserHandlers {

    public void handleChangeColor(Packet.ChangeColorPacket packet) {
        Awooing.getInstance().chatClient.getJoinedRooms().values().forEach(room -> {
            if (room.hasUser(packet.username())) {
                room.setUserColor(packet.username(), packet.color());
            }
        });

        if (packet.username().equals(getUsername())) {
            renderMsg(
                INFO_COLOR,
                text("Color " + String.format("#%06X", packet.color()) + " changed successfully", packet.color()));
            config.userColor = packet.color();
            save();
        }
    }
}