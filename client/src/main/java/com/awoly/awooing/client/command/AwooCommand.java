package com.awoly.awooing.client.command;

import static com.awoly.awooing.client.Awooing.LOGGER;
import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.configureSsl;
import static com.awoly.awooing.client.Utils.getActiveRoomId;
import static com.awoly.awooing.client.Utils.getLedRoomIds;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.getVersion;
import static com.awoly.awooing.client.Utils.isClientConnected;
import static com.awoly.awooing.client.Utils.isAdmin;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.showUsage;
import static com.awoly.awooing.client.Utils.suggestLedRoomUsers;
import static com.awoly.awooing.client.Utils.suggestLedRooms;
import static com.awoly.awooing.client.Utils.suggestRoomPrivacy;
import static com.awoly.awooing.client.Utils.suggestRooms;
import static com.awoly.awooing.client.Utils.suggestUsers;
import static com.awoly.awooing.client.Utils.text;
import static com.awoly.awooing.client.config.ConfigManager.config;
import static com.awoly.awooing.client.config.ConfigManager.save;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import com.awoly.awooing.client.Awooing;
import com.awoly.awooing.client.ChatClient;
import com.awoly.awooing.client.ChatRoom;
import com.awoly.awooing.common.CommonUtils;
import com.awoly.awooing.common.Packet;
import com.awoly.awooing.common.RoomAccessMode;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;

public class AwooCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, WrapperLookup registryAccess) {
        dispatcher.register(literal("awoo").executes(ctx -> {
            renderMsg(INFO_COLOR, "v" + getVersion());
            return Command.SINGLE_SUCCESS;
        })
            .then(literal("connect").executes(ctx -> connectToHost(config.lastIp, config.lastPort)))
            .then(literal("changeip")
                .executes(ctx -> showUsage("/awoo changeip <IP> [Port]"))
                .then(argument("ip", word())
                    .executes(ctx -> updateConnectionConfig(getString(ctx, "ip"), null))
                    .then(argument("port", integer())
                        .executes(ctx -> updateConnectionConfig(getString(ctx, "ip"), getInteger(ctx, "port"))))))
            .then(literal("roomprivacy")
                .executes(ctx -> showUsage("/awoo roomprivacy <room> <public|private> [password]"))
                .then(argument("room", word()).suggests(suggestLedRooms())
                    .then(argument("privacy", word()).suggests(suggestRoomPrivacy())
                        .executes(ctx -> editRoomPrivacy(getString(ctx, "room"), getString(ctx, "privacy"), null))
                        .then(argument("password", greedyString())
                            .executes(ctx -> editRoomPrivacy(
                                getString(ctx, "room"),
                                getString(ctx, "privacy"),
                                getString(ctx, "password")))))))
            .then(literal("create")
                .executes(ctx -> showUsage("/awoo create <name> [public|private] [password]"))
                .then(argument("name", word())
                    .executes(ctx -> createRoom(getString(ctx, "name"), "public", null))
                    .then(argument("privacy", word()).suggests(suggestRoomPrivacy())
                        .executes(ctx -> createRoom(getString(ctx, "name"), getString(ctx, "privacy"), null))
                        .then(argument("password", word())
                            .executes(ctx -> createRoom(getString(ctx, "name"), getString(ctx, "privacy"), getString(ctx, "password")))))))
            .then(literal("publicrooms").executes(ctx -> publicRooms()))
            .then(literal("join")
                .executes(ctx -> showUsage("/awoo join <room> [password]"))
                .then(argument("room", word())
                    .executes(ctx -> joinRoom(getString(ctx, "room"), null))
                    .then(argument("password", greedyString())
                        .executes(ctx -> joinRoom(getString(ctx, "room"), getString(ctx, "password"))))))
            .then(literal("invite")
                .executes(ctx -> showUsage("/awoo invite <user> [room]"))
                .then(argument("user", word())
                    .executes(ctx -> invite(getString(ctx, "user"), null))
                    .then(argument("room", word()).suggests(suggestLedRooms())
                        .executes(ctx -> invite(getString(ctx, "user"), getString(ctx, "room"))))))
            .then(literal("list")
                .executes(ctx -> list(null))
                .then(argument("room", word()).suggests(suggestRooms())
                    .executes(ctx -> list(getString(ctx, "room")))))
            .then(literal("online")
                .requires(ctx -> isAdmin())
                .executes(ctx -> requestStatus()))
            .then(literal("autoconnect").executes(ctx -> toggleAutoConnect()))
            .then(literal("disconnect").executes(ctx -> disconnect()))
            .then(literal("leave")
                .executes(ctx -> leaveRoom(null))
                .then(argument("room", word()).suggests(suggestRooms())
                    .executes(ctx -> leaveRoom(getString(ctx, "room")))))
            .then(literal("color").executes(ctx -> showColor())
                .then(argument("color", word()).executes(ctx -> changeColor(getString(ctx, "color")))))
            .then(literal("forward")
                .executes(ctx -> showUsage("/awoo forward <allow|revoke|request|list|sender> ..."))
                .then(literal("allow").executes(ctx -> showUsage("/awoo forward allow <user>"))
                    .then(argument("user", word()).suggests(suggestUsers())
                        .executes(ctx -> allow(getString(ctx, "user")))))
                .then(literal("revoke").executes(ctx -> showUsage("/awoo forward revoke <user>"))
                    .then(argument("user", word()).suggests(suggestUsers())
                        .executes(ctx -> revoke(getString(ctx, "user")))))
                .then(literal("request").executes(ctx -> showUsage("/awoo forward request <user>"))
                    .then(argument("user", word()).suggests(suggestUsers())
                        .executes(ctx -> request(getString(ctx, "user")))))
                .then(literal("list").executes(ctx -> forwardList()))
                .then(literal("sender").executes(ctx -> showUsage("/awoo forward sender <user>"))
                    .then(argument("user", word()).suggests(suggestUsers())
                        .executes(ctx -> sender(getString(ctx, "user"))))))
            .then(literal("kick")
                .executes(ctx -> showUsage("/awoo kick <user> [room]"))
                .then(argument("user", word()).suggests(suggestLedRoomUsers())
                    .executes(ctx -> kick(getString(ctx, "user"), null))
                    .then(argument("room", word()).suggests(suggestLedRooms())
                        .executes(ctx -> kick(getString(ctx, "user"), getString(ctx, "room"))))))
            .then(literal("host")
                .executes(ctx -> showUsage("/awoo host <user> [room]"))
                .then(argument("user", word()).suggests(suggestLedRoomUsers())
                    .executes(ctx -> host(getString(ctx, "user"), null))
                    .then(argument("room", word()).suggests(suggestLedRooms())
                        .executes(ctx -> host(getString(ctx, "user"), getString(ctx, "room")))))));
    }

    private static int updateConnectionConfig(String ip, Integer port) {
        config.lastIp = ip;
        if (port != null) {
            config.lastPort = port;
        }
        save();
        renderMsg(INFO_COLOR, "Server set to " + config.lastIp + ":" + config.lastPort);
        return Command.SINGLE_SUCCESS;
    }

    private static int toggleAutoConnect() {
        config.autoConnect = !config.autoConnect;
        save();
        renderMsg(INFO_COLOR, "Autoconnect " + (config.autoConnect ? "enabled" : "disabled"));
        return Command.SINGLE_SUCCESS;
    }

    private static int requestStatus() {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.reqOnlineCount());
        return Command.SINGLE_SUCCESS;
    }

    private static int list(String roomId) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        List<String> joinedRoomIds = Awooing.getInstance().chatClient.getJoinedRooms().keySet().stream().toList();

        if (roomId == null) {
            if (joinedRoomIds.isEmpty()) {
                renderMsg(INFO_COLOR, "You haven't joined any room");
                return Command.SINGLE_SUCCESS;
            }

            if (joinedRoomIds.size() == 1) {
                roomId = getActiveRoomId();
            } else {
                renderMsg(INFO_COLOR, "Rooms you are in: " + String.join(", ", joinedRoomIds));
                return Command.SINGLE_SUCCESS;
            }
        }

        String resolvedRoomId = resolveJoinedRoomId(roomId);
        if (resolvedRoomId == null) {
            renderMsg(INFO_COLOR, "You are not part of room " + roomId);
            return Command.SINGLE_SUCCESS;
        }

        Set<String> users = Awooing.getInstance().chatClient.getRoomUsers(resolvedRoomId);
        renderMsg(resolvedRoomId, INFO_COLOR, "Connected users: " + String.join(", ", users.stream().sorted().toList()));

        return Command.SINGLE_SUCCESS;
    }

    private static int disconnect() {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        try {
            Awooing.getInstance().chatClient.close();
            renderMsg(INFO_COLOR, "Disconnected successfully");
        } catch (Exception e) {
            LOGGER.error("Disconnecting error", e);
            renderMsg(INFO_COLOR, "Disconnecting error. Check logs for details");
        }

        return Command.SINGLE_SUCCESS;
    }

    private static int leaveRoom(String roomId) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (roomId == null) {
            roomId = getActiveRoomId();
            if (roomId == null) {
                renderMsg(INFO_COLOR, "Usage: /awoo leave <room>");
                return Command.SINGLE_SUCCESS;
            }
        }

        String resolvedRoomId = resolveJoinedRoomId(roomId);
        if (resolvedRoomId == null) {
            renderMsg(INFO_COLOR, "You are not joined to room " + roomId);
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.leaveRoom(resolvedRoomId));
        return Command.SINGLE_SUCCESS;
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

    private static int showColor() {
        renderMsg(INFO_COLOR, text("Your current color: ").append(text(getUsername(), config.userColor)));
        return Command.SINGLE_SUCCESS;
    }

    private static int changeColor(String hex) {
        int color;

        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }

        try {
            color = Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            renderMsg(INFO_COLOR, "Invalid color. Expected HEX format like (#)RRGGBB");
            return Command.SINGLE_SUCCESS;
        }

        if (!isClientConnected()) {
            renderMsg(INFO_COLOR, "Color " + String.format("#%06X", color) + " changed successfully");
            config.userColor = color;
            save();

            return Command.SINGLE_SUCCESS;
        } else {
            Awooing.getInstance().chatClient.sendPacket(Packet.changeColor(color));
            return Command.SINGLE_SUCCESS;
        }
    }

    private static int allow(String user) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.forwardAllow(user));
        return Command.SINGLE_SUCCESS;
    }

    private static int revoke(String user) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.forwardRevoke(user));
        return Command.SINGLE_SUCCESS;
    }

    private static int request(String user) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.forwardRequest(user));
        return Command.SINGLE_SUCCESS;
    }

    private static int forwardList() {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.reqForwardList());
        return Command.SINGLE_SUCCESS;
    }

    private static int sender(String target) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (Awooing.getInstance().forwardAs != null && Awooing.getInstance().forwardAs.equals(target)) {
            renderMsg(INFO_COLOR, "You already set " + target + " as your sender for forwarded messages");
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.reqForwardAllowance(target));
        return Command.SINGLE_SUCCESS;
    }

    private static int kick(String user, String roomId) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (roomId == null) {
            List<String> ledRoomIds = getLedRoomIds();
            if (ledRoomIds.size() == 1) {
                roomId = ledRoomIds.getFirst();
            } else {
                if (ledRoomIds.isEmpty()) {
                    renderMsg(INFO_COLOR, "You are not the leader of any room");
                } else {
                    renderMsg(INFO_COLOR, "You are leading multiple rooms. Specify a room to kick from.");
                }
                return Command.SINGLE_SUCCESS;
            }
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.kick(user, roomId));
        return Command.SINGLE_SUCCESS;
    }

    private static int host(String user, String roomId) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (roomId == null) {
            List<String> ledRoomIds = getLedRoomIds();
            if (ledRoomIds.size() == 1) {
                roomId = ledRoomIds.getFirst();
            } else {
                if (ledRoomIds.isEmpty()) {
                    renderMsg(INFO_COLOR, "You are not the leader of any room");
                } else {
                    renderMsg(INFO_COLOR, "You are leading multiple rooms. Specify a room to change the host for.");
                }
                return Command.SINGLE_SUCCESS;
            }
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.setRoomHost(roomId, user));
        return Command.SINGLE_SUCCESS;
    }

    private static int joinRoom(String roomId, String password) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (password == null) {
            Awooing.getInstance().chatClient.sendPacket(Packet.requestRoomJoin(roomId, null));
        } else {
            Awooing.getInstance().chatClient.sendPacket(Packet.requestRoomJoin(roomId, password));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int invite(String user, String roomId) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (roomId == null) {
            List<String> ledRoomIds = getLedRoomIds();
            if (ledRoomIds.size() == 1) {
                roomId = ledRoomIds.getFirst();
            } else {
                if (ledRoomIds.isEmpty()) {
                    renderMsg(INFO_COLOR, "You are not the leader of any room");
                } else {
                    renderMsg(INFO_COLOR, "You are leading multiple rooms. Specify a room to invite from.");
                }
                return Command.SINGLE_SUCCESS;
            }
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.invite(roomId, user));
        return Command.SINGLE_SUCCESS;
    }

    private static int createRoom(String roomName, String privacy, String password) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        if (!CommonUtils.isValidRoomName(roomName)) {
            renderMsg(INFO_COLOR, CommonUtils.getRoomNameErrorMessage());
            return Command.SINGLE_SUCCESS;
        }

        RoomAccessMode roomAccessMode;

        try {
            roomAccessMode = RoomAccessMode.fromString(privacy);
        } catch (IllegalArgumentException e) {
            renderMsg(INFO_COLOR, e.getMessage() + ". Use public or private.");
            return Command.SINGLE_SUCCESS;
        }

        if (roomAccessMode != RoomAccessMode.PRIVATE && password != null) {
            renderMsg(INFO_COLOR, "Password is only valid with the private privacy option");
            return Command.SINGLE_SUCCESS;
        }

        if (password == null) {
            Awooing.getInstance().chatClient.sendPacket(Packet.requestRoomCreate(roomName, roomName, roomAccessMode, null));
        } else {
            Awooing.getInstance().chatClient.sendPacket(Packet.requestRoomCreate(roomName, roomName, roomAccessMode, password));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int publicRooms() {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.sendPacket(Packet.requestRoomList());
        return Command.SINGLE_SUCCESS;
    }

    private static int editRoomPrivacy(String roomId, String privacy, String password) {
        if (!requireConnected()) {
            return Command.SINGLE_SUCCESS;
        }

        String resolvedRoomId = resolveJoinedRoomId(roomId);
        if (resolvedRoomId == null) {
            renderMsg(INFO_COLOR, "You are not part of room " + roomId);
            return Command.SINGLE_SUCCESS;
        }

        RoomAccessMode roomAccessMode;
        try {
            roomAccessMode = RoomAccessMode.fromString(privacy);
        } catch (IllegalArgumentException e) {
            renderMsg(INFO_COLOR, e.getMessage() + ". Use public or private.");
            return Command.SINGLE_SUCCESS;
        }

        if (roomAccessMode == RoomAccessMode.PUBLIC && password != null) {
            renderMsg(INFO_COLOR, "Password is only valid with the private privacy option");
            return Command.SINGLE_SUCCESS;
        }

        if (password == null) {
            Awooing.getInstance().chatClient.sendPacket(Packet.requestChangePrivacy(resolvedRoomId, roomAccessMode, null));
        } else {
            Awooing.getInstance().chatClient.sendPacket(Packet.requestChangePrivacy(resolvedRoomId, roomAccessMode, password));
        }
        return Command.SINGLE_SUCCESS;
    }

    public static int connectToHost(String ip, int port) {
        if (isClientConnected()) {
            renderMsg(INFO_COLOR, "You are already connected to the server");
            return Command.SINGLE_SUCCESS;
        }

        URI uri;

        try {
            uri = new URI("wss://" + ip + ":" + port);
        } catch (URISyntaxException e) {
            renderMsg(INFO_COLOR, "Invalid address");
            return Command.SINGLE_SUCCESS;
        }

        renderMsg(INFO_COLOR, "Connecting...");

        if (Awooing.getInstance().chatClient != null && !Awooing.getInstance().chatClient.isClosed()) {
            Awooing.getInstance().chatClient.close();
        }

        Awooing.getInstance().chatClient = new ChatClient(uri);

        try {
            configureSsl(Awooing.getInstance().chatClient, "client-truststore.p12", "password");
        } catch (Exception e) {
            LOGGER.error("Failed to configure SSL", e);
            renderMsg(INFO_COLOR, "Failed to configure SSL: " + e.getMessage());
            return Command.SINGLE_SUCCESS;
        }

        Awooing.getInstance().chatClient.connect();

        return Command.SINGLE_SUCCESS;
    }

    private static boolean requireConnected() {
        if (!isClientConnected()) {
            renderMsg(INFO_COLOR, "You are not connected to the server");
            return false;
        }
        return true;
    }
}
