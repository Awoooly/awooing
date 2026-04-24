package com.awoly.awooing.common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Packet {

    PacketType type();

    default String toJson() {
        return CODEC.toJson(this);
    }

    static Packet fromJson(String json) {
        return CODEC.fromJson(json);
    }

    static Packet msg(String roomId, String sender, String msg) {
        return new MsgPacket(roomId, sender, msg);
    }

    static Packet msg(String roomId, String msg) {
        return new MsgPacket(roomId, null, msg);
    }

    static Packet sessionChallenge(String serverId) {
        return new SessionChallengePacket(serverId);
    }

    static Packet permissions(PermissionType permissionType) {
        return new PermissionPacket(permissionType, null);
    }

    static Packet permissions(PermissionType permissionType, String message) {
        return new PermissionPacket(permissionType, message);
    }

    static Packet authResponse(String username, int color, int protocolVersion, int clientVersion) {
        return new AuthResponsePacket(username, color, protocolVersion, clientVersion);
    }

    // roomPassword may be null for public rooms
    static Packet requestRoomCreate(String roomId, String roomName, RoomAccessMode roomAccessMode, String roomPassword) {
        return new RoomCreatePacket(roomId, roomName, roomAccessMode == null ? RoomAccessMode.PUBLIC : roomAccessMode, roomPassword);
    }

    // roomPassword may be null for public rooms
    static Packet requestChangePrivacy(String roomId, RoomAccessMode roomAccessMode, String roomPassword) {
        return new ChangePrivacyPacket(roomId, roomAccessMode == null ? RoomAccessMode.PUBLIC : roomAccessMode, roomPassword);
    }

    static Packet requestChangePassword(String roomId, String roomPassword) {
        return new ChangePasswordPacket(roomId, roomPassword);
    }

    // roomPassword may be null for public rooms
    static Packet requestRoomJoin(String roomId, String roomPassword) {
        return new RoomJoinPacket(roomId, null, null, roomPassword);
    }

    static Packet roomJoined(String roomId, String sender, int color) {
        return new RoomJoinPacket(roomId, sender, color, null);
    }

    static Packet requestRoomList() {
        return new RoomListPacket(List.of(), null);
    }

    static Packet roomList(List<String> roomNames, boolean truncated) {
        return new RoomListPacket(roomNames, truncated);
    }

    static Packet invite(String roomId, String target) {
        return new RoomInvitePacket(roomId, target);
    }

    static Packet leaveRoom(String roomId) {
        return new RoomLeavePacket(roomId, null, null);
    }

    static Packet roomLeft(String roomId, String sender, boolean wasKicked) {
        return new RoomLeavePacket(roomId, sender, wasKicked);
    }

    static Packet setRoomHost(String roomId, String target) {
        return new HostChangePacket(roomId, target, null);
    }

    static Packet hostChanged(String roomId, String target, String sender) {
        return new HostChangePacket(roomId, target, sender);
    }

    static Packet listUsers(String roomId, Map<String, Integer> userColors) {
        return new UserListPacket(roomId, userColors == null ? Map.of() : userColors);
    }

    static Packet changeColor(int color) {
        return new ChangeColorPacket(null, color);
    }

    static Packet changeColor(String user, int color) {
        return new ChangeColorPacket(user, color);
    }

    static Packet forwardMsg(String target, String msg) {
        return new ForwardMsgPacket(target, msg);
    }

    static Packet forwardAllow(String target) {
        return new ForwardAllowPacket(target);
    }

    static Packet forwardRevoke(String target) {
        return new ForwardRevokePacket(target);
    }

    static Packet forwardRequest(String target) {
        return new ForwardRequestPacket(target);
    }

    static Packet reqForwardList() {
        return new ForwardListPacket();
    }

    static Packet reqForwardAllowance(String target) {
        return new ForwardIsAllowedPacket(target, null);
    }

    static Packet isForwardAllowed(String target, Boolean allowed) {
        return new ForwardIsAllowedPacket(target, allowed);
    }

    static Packet kick(String target, String roomId) {
        return new KickPacket(target, roomId);
    }

    static Packet info(String msg) {
        return new InfoPacket(msg, null, null, null);
    }

    static Packet info(String roomId, String msg) {
        return new InfoPacket(msg, roomId, null, null);
    }

    static Packet clickableInfo(String msg, String command) {
        return new InfoPacket(msg, null, command, null);
    }

    static Packet urlInfo(String msg, String url) {
        return new InfoPacket(msg, null, null, url);
    }

    static Packet privateMsg(String target, String msg) {
        return new PrivateMsgPacket(target, null, msg, null, null);
    }

    static Packet privateMsg(String sender, String msg, int color, boolean isOutgoing) {
        return new PrivateMsgPacket(null, sender, msg, color, isOutgoing);
    }

    record MsgPacket(String roomId, String sender, String msg) implements Packet {
        public PacketType type() {
            return PacketType.MSG;
        }
    }

    // roomId, clickCommand, and clickUrl are nullable
    record InfoPacket(String msg, String roomId, String clickCommand, String clickUrl) implements Packet {
        public PacketType type() {
            return PacketType.INFO;
        }
    }

    record SessionChallengePacket(String serverId) implements Packet {
        public PacketType type() {
            return PacketType.SESSION_CHALLENGE;
        }
    }

    // message is optional and can be displayed by the client when present
    record PermissionPacket(PermissionType permissionType, String message) implements Packet {
        public PacketType type() {
            return PacketType.PERMISSION;
        }
    }

    record AuthResponsePacket(String username, Integer color, Integer protocolVersion, Integer clientVersion) implements Packet {
        public PacketType type() {
            return PacketType.AUTH_RESPONSE;
        }
    }

    record RoomCreatePacket(String roomId, String roomName, RoomAccessMode roomAccessMode, String roomPassword) implements Packet {
        public PacketType type() {
            return PacketType.ROOM_CREATE;
        }
    }

    // username/color present on response, null on request; roomPassword null for public rooms
    record RoomJoinPacket(String roomId, String sender, Integer color, String roomPassword) implements Packet {
        public PacketType type() {
            return PacketType.ROOM_JOIN;
        }
    }

    // username/wasKicked present on server->client, null on client->server
    record RoomLeavePacket(String roomId, String sender, Boolean wasKicked) implements Packet {
        public PacketType type() {
            return PacketType.ROOM_LEAVE;
        }
    }

    record RoomListPacket(List<String> roomNames, Boolean truncated) implements Packet {
        public PacketType type() {
            return PacketType.ROOM_LIST;
        }

        public RoomListPacket {
            roomNames = roomNames == null ? List.of() : List.copyOf(roomNames);
        }

        public List<String> roomNames() {
            return List.copyOf(roomNames);
        }
    }

    record HostChangePacket(String roomId, String target, String sender) implements Packet {
        public PacketType type() {
            return PacketType.HOST_CHANGE;
        }
    }

    record UserListPacket(String roomId, Map<String, Integer> users) implements Packet {
        public PacketType type() {
            return PacketType.USER_LIST;
        }

        public UserListPacket {
            users = users == null ? null : new HashMap<>(users);
        }

        public Map<String, Integer> users() {
            return users == null ? null : Map.copyOf(users);
        }
    }

    record ForwardMsgPacket(String target, String msg) implements Packet {
        public PacketType type() {
            return PacketType.FORWARD_MSG;
        }
    }

    record ForwardAllowPacket(String target) implements Packet {
        public PacketType type() {
            return PacketType.FORWARD_ALLOW;
        }
    }

    record ForwardRevokePacket(String target) implements Packet {
        public PacketType type() {
            return PacketType.FORWARD_REVOKE;
        }
    }

    record ForwardListPacket() implements Packet {
        public PacketType type() {
            return PacketType.FORWARD_LIST;
        }
    }

    // bool is null on request, present on response
    record ForwardIsAllowedPacket(String target, Boolean bool) implements Packet {
        public PacketType type() {
            return PacketType.FORWARD_IS_ALLOWED;
        }
    }

    record ForwardRequestPacket(String target) implements Packet {
        public PacketType type() {
            return PacketType.FORWARD_REQUEST;
        }
    }

    // username null on client->server, present on server->client
    record ChangeColorPacket(String username, Integer color) implements Packet {
        public PacketType type() {
            return PacketType.CHANGE_COLOR;
        }
    }

    record ChangePrivacyPacket(String roomId, RoomAccessMode roomAccessMode, String roomPassword) implements Packet {
        public PacketType type() {
            return PacketType.CHANGE_PRIVACY;
        }
    }

    record ChangePasswordPacket(String roomId, String roomPassword) implements Packet {
        public PacketType type() {
            return PacketType.CHANGE_PASSWORD;
        }
    }

    record RoomInvitePacket(String roomId, String target) implements Packet {
        public PacketType type() {
            return PacketType.ROOM_INVITE;
        }
    }

    record KickPacket(String target, String roomId) implements Packet {
        public PacketType type() {
            return PacketType.KICK;
        }
    }

    // client->server: target present, username/color/isOutgoing null
    // server->client: username/color/isOutgoing present, target null
    record PrivateMsgPacket(String target, String sender, String msg, Integer color, Boolean isOutgoing) implements Packet {
        public PacketType type() {
            return PacketType.PRIVATE_MSG;
        }
    }

    PacketCodec CODEC = new PacketCodec();

    final class PacketCodec {
        private static final Gson GSON = new GsonBuilder()
                .registerTypeAdapter(Packet.class, new PacketAdapter())
                .registerTypeAdapter(PacketType.class, new PacketTypeAdapter())
                .create();

        String toJson(Packet packet) {
            return GSON.toJson(packet, Packet.class);
        }

        Packet fromJson(String json) {
            try {
                return GSON.fromJson(json, Packet.class);
            } catch (JsonParseException | IllegalArgumentException e) {
                return null;
            }
        }
    }

    final class PacketAdapter implements JsonSerializer<Packet>, JsonDeserializer<Packet> {
        @Override
        public JsonElement serialize(Packet src, Type typeOfSrc, JsonSerializationContext ctx) {
            JsonObject obj = ctx.serialize(src, src.getClass()).getAsJsonObject();
            obj.addProperty("type", src.type().getKey()); // inject type since it's not a record component
            return obj;
        }

        @Override
        public Packet deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            if (json == null || json.isJsonNull()) {
                return null;
            }
            JsonObject obj = json.getAsJsonObject();
            PacketType type = ctx.deserialize(obj.get("type"), PacketType.class);
            if (type == null) {
                return null;
            }
            return switch (type) {
                case MSG              -> ctx.deserialize(obj, MsgPacket.class);
                case INFO             -> ctx.deserialize(obj, InfoPacket.class);
                case PERMISSION       -> ctx.deserialize(obj, PermissionPacket.class);
                case SESSION_CHALLENGE -> ctx.deserialize(obj, SessionChallengePacket.class);
                case AUTH_RESPONSE    -> ctx.deserialize(obj, AuthResponsePacket.class);
                case ROOM_CREATE      -> ctx.deserialize(obj, RoomCreatePacket.class);
                case ROOM_JOIN        -> ctx.deserialize(obj, RoomJoinPacket.class);
                case ROOM_LEAVE       -> ctx.deserialize(obj, RoomLeavePacket.class);
                case ROOM_LIST        -> ctx.deserialize(obj, RoomListPacket.class);
                case HOST_CHANGE      -> ctx.deserialize(obj, HostChangePacket.class);
                case USER_LIST        -> ctx.deserialize(obj, UserListPacket.class);
                case FORWARD_MSG      -> ctx.deserialize(obj, ForwardMsgPacket.class);
                case FORWARD_ALLOW    -> ctx.deserialize(obj, ForwardAllowPacket.class);
                case FORWARD_REVOKE   -> ctx.deserialize(obj, ForwardRevokePacket.class);
                case FORWARD_LIST     -> ctx.deserialize(obj, ForwardListPacket.class);
                case FORWARD_IS_ALLOWED -> ctx.deserialize(obj, ForwardIsAllowedPacket.class);
                case FORWARD_REQUEST  -> ctx.deserialize(obj, ForwardRequestPacket.class);
                case CHANGE_COLOR     -> ctx.deserialize(obj, ChangeColorPacket.class);
                case CHANGE_PRIVACY   -> ctx.deserialize(obj, ChangePrivacyPacket.class);
                case CHANGE_PASSWORD  -> ctx.deserialize(obj, ChangePasswordPacket.class);
                case ROOM_INVITE      -> ctx.deserialize(obj, RoomInvitePacket.class);
                case KICK             -> ctx.deserialize(obj, KickPacket.class);
                case PRIVATE_MSG      -> ctx.deserialize(obj, PrivateMsgPacket.class);
            };
        }
    }

    final class PacketTypeAdapter implements JsonSerializer<PacketType>, JsonDeserializer<PacketType> {
        @Override
        public JsonElement serialize(PacketType src, Type typeOfSrc, JsonSerializationContext ctx) {
            return new JsonPrimitive(src == null ? null : src.getKey());
        }

        @Override
        public PacketType deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            return (json == null || json.isJsonNull()) ? null : PacketType.fromString(json.getAsString());
        }
    }
}