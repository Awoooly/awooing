package com.awoly.awooing.client.event;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.getActiveRoomId;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.setAwooing;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.client.ChatRoom;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

public class CommandListener {

    public static void register() {
        ClientSendMessageEvents.ALLOW_COMMAND.register(CommandListener::interceptCommand);
    }

    private static boolean interceptCommand(String command) {
        if (command.startsWith("chat") && !command.startsWith("chat awoo") && Awooing.getInstance().isAwooing) {
            setAwooing(false);
            return true;
        }

        if (command.startsWith("chat awoo")) {
            if (!isClientConnected()) {
                renderMsg(INFO_COLOR, "You are not connected to the server");
                return false;
            }

            String[] parts = command.trim().split("\\s+");
            if (parts.length == 2) {
                if (Awooing.getInstance().isAwooing) {
                    setAwooing(false);
                    return false;
                }

                String roomId = getActiveRoomId();
                if (roomId == null) {
                    renderMsg(INFO_COLOR, "You haven't joined any room");
                    return false;
                }

                setAwooing(true, roomId);
                return false;
            }

            if (parts.length < 3) {
                renderMsg(INFO_COLOR, "Usage: /chat awoo <room>");
                return false;
            }

            String resolvedRoomId = resolveJoinedRoomId(parts[2]);
            if (resolvedRoomId == null) {
                renderMsg(INFO_COLOR, "You haven't joined room " + parts[2]);
                return false;
            }

            setAwooing(true, resolvedRoomId);
            return false;
        }

        return true;
    }

    private static String resolveJoinedRoomId(String roomId) {
        if (roomId == null) {
            return null;
        }

        for (ChatRoom room : Awooing.getInstance().chatClient.getJoinedRooms().values()) {
            if (room.getName().equalsIgnoreCase(roomId)) {
                return room.getName();
            }
        }

        return null;
    }
}
