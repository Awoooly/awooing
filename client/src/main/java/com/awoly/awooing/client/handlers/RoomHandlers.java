package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.CHAT_COLOR;
import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.setAwooing;
import static com.awoly.awooing.client.Utils.text;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.client.ChatRoom;
import com.awoly.awooing.client.Utils;
import com.awoly.awooing.common.Packet;
import java.util.List;

public final class RoomHandlers {

    public void handleRoomList(Packet.RoomListPacket packet) {
        String roomNames = String.join(", ", packet.roomNames());
        String suffix = Boolean.TRUE.equals(packet.truncated()) ? " (showing first 10)" : "";
        renderMsg(INFO_COLOR, "Public rooms: " + roomNames + suffix);
    }

    public void handleUserJoinedRoom(Packet.RoomJoinPacket packet) {
        String roomId = packet.roomId();
        String sender = packet.sender();

        renderMsg(roomId, INFO_COLOR, sender + " joined the room");

        if (roomId != null && sender != null && packet.color() != null) {
            Awooing.getInstance().chatClient.getOrCreateRoom(roomId).setUserColor(sender, packet.color());
        }

        if (sender != null && sender.equals(getUsername()) && Awooing.getInstance().currentRoomId == null) {
            Awooing.getInstance().currentRoomId = roomId;
        }
    }

    public void handleUserLeftRoom(Packet.RoomLeavePacket packet) {
        boolean wasKicked = Boolean.TRUE.equals(packet.wasKicked());
        String sender = packet.sender();
        String roomId = packet.roomId();
        boolean isSelf = getUsername().equals(sender);

        if (!isSelf) {
            renderMsg(roomId, INFO_COLOR, sender + (wasKicked ? " was kicked from the room" : " left the room"));
            ChatRoom room = Awooing.getInstance().chatClient.getRoom(roomId);
            if (room != null) {
                room.removeUser(sender);
                if (room.hasNoUsers()) {
                    Awooing.getInstance().chatClient.removeRoom(roomId);
                }
            }
            return;
        }

        renderMsg(roomId, INFO_COLOR, wasKicked ? "You have been removed from the room" : "You left the room");
        Awooing.getInstance().chatClient.removeRoom(roomId);

        if (roomId.equals(Awooing.getInstance().currentRoomId)) {
            List<String> joinedRoomIds = Awooing.getInstance().chatClient.getJoinedRooms().keySet().stream().toList();
            if (!joinedRoomIds.isEmpty()) {
                Utils.setAwooing(Awooing.getInstance().isAwooing, joinedRoomIds.getFirst());
            } else {
                Awooing.getInstance().currentRoomId = null;
                setAwooing(false);
            }
        }
    }

    public void handleHostChanged(Packet.HostChangePacket packet) {
        if (packet.roomId() == null || packet.target() == null) {
            return;
        }

        String target = packet.target();
        ChatRoom room = Awooing.getInstance().chatClient.getRoom(packet.roomId());
        if (room == null) {
            return;
        }

        room.setLeader(target.equals(getUsername()));

        String message = target.equals(getUsername())
            ? "You are now the leader"
            : target + " is the new leader";

        renderMsg(packet.roomId(), INFO_COLOR, message);
    }

    public void handleMsg(Packet.MsgPacket packet) {
        String roomId = packet.roomId();
        if (roomId == null) {
            return;
        }

        String sender = packet.sender() == null ? "" : packet.sender();
        String message = packet.msg() == null ? "" : packet.msg();
        ChatRoom room = Awooing.getInstance().chatClient.getJoinedRooms().get(roomId);
        if (room == null) {
            return;
        }
        renderMsg(roomId, Utils.CHAT_COLOR, text(sender, room.getUserColor(sender)).append(text(": " + message, CHAT_COLOR)));
    }

    public void handleUserList(Packet.UserListPacket packet) {
        if (packet.roomId() == null) {
            return;
        }

        ChatRoom room = Awooing.getInstance().chatClient.getOrCreateRoom(packet.roomId());
        room.replaceUsers(packet.users());
        room.setLeader(packet.users() != null && packet.users().size() == 1 && packet.users().containsKey(getUsername()));
    }
}