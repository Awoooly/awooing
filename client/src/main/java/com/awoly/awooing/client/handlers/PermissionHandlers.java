package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.common.Packet;
import com.awoly.awooing.common.PermissionType;
import java.util.Arrays;

public final class PermissionHandlers {

    public void handlePermission(Packet.PermissionPacket packet) {
        String receivedPermission = packet.permissionType() == null ? null : packet.permissionType().name();
        PermissionType permissionType = Arrays.stream(PermissionType.values())
            .filter(permission -> permission.name().equals(receivedPermission))
            .findFirst()
            .orElse(PermissionType.USER);

        Awooing.getInstance().permissionType = permissionType;

        String message = packet.message();
        if (message != null && !message.isBlank()) {
            renderMsg(INFO_COLOR, message);
        }
    }
}
