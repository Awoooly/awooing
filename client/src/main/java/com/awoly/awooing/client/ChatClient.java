package com.awoly.awooing.client;

import static com.awoly.awooing.client.Awooing.LOGGER;
import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.setAwooing;
import static org.java_websocket.framing.CloseFrame.GOING_AWAY;
import static org.java_websocket.framing.CloseFrame.NEVER_CONNECTED;
import static org.java_websocket.framing.CloseFrame.NORMAL;
import static org.java_websocket.framing.CloseFrame.REFUSE;

import com.awoly.awooing.client.handlers.AuthenticationHandlers;
import com.awoly.awooing.client.handlers.DirectMessageHandlers;
import com.awoly.awooing.client.handlers.ForwardingHandlers;
import com.awoly.awooing.client.handlers.InfoHandlers;
import com.awoly.awooing.client.handlers.PermissionHandlers;
import com.awoly.awooing.client.handlers.RoomHandlers;
import com.awoly.awooing.client.handlers.OnlineCountHandler;
import com.awoly.awooing.client.handlers.UserHandlers;
import com.awoly.awooing.common.Packet;
import com.awoly.awooing.common.PermissionType;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

public class ChatClient extends WebSocketClient {

    private static final int PROTOCOL_VERSION = 3;
    private static final int CONNECTION_LOST_TIMEOUT_DURATION = 10;
    private final Map<String, ChatRoom> joinedRooms = new ConcurrentHashMap<>();
    private final Map<Class<? extends Packet>, Consumer<Packet>> handlers = new HashMap<>();
    private final RoomHandlers roomHandlers = new RoomHandlers();
    private final UserHandlers userHandlers = new UserHandlers();
    private final ForwardingHandlers forwardingHandlers = new ForwardingHandlers();
    private final InfoHandlers infoHandlers = new InfoHandlers();
    private final OnlineCountHandler statusHandler = new OnlineCountHandler();
    private final PermissionHandlers permissionHandlers = new PermissionHandlers();
    private final AuthenticationHandlers authenticationHandlers = new AuthenticationHandlers(this, PROTOCOL_VERSION);
    private final DirectMessageHandlers directMessageHandlers = new DirectMessageHandlers();

    public ChatClient(URI serverUri) {
        super(serverUri);
        setConnectionLostTimeout(CONNECTION_LOST_TIMEOUT_DURATION);
        registerHandler(Packet.RoomJoinPacket.class, roomHandlers::handleUserJoinedRoom);
        registerHandler(Packet.RoomLeavePacket.class, roomHandlers::handleUserLeftRoom);
        registerHandler(Packet.RoomListPacket.class, roomHandlers::handleRoomList);
        registerHandler(Packet.HostChangePacket.class, roomHandlers::handleHostChanged);
        registerHandler(Packet.MsgPacket.class, roomHandlers::handleMsg);
        registerHandler(Packet.UserListPacket.class, roomHandlers::handleUserList);
        registerHandler(Packet.ChangeColorPacket.class, userHandlers::handleChangeColor);
        registerHandler(Packet.ForwardMsgPacket.class, forwardingHandlers::handleForwardMsg);
        registerHandler(Packet.ForwardIsAllowedPacket.class, forwardingHandlers::handleForwardIsAllowed);
        registerHandler(Packet.InfoPacket.class, infoHandlers::handleInfo);
        registerHandler(Packet.OnlineCountPacket.class, statusHandler::handleStatus);
        registerHandler(Packet.PermissionPacket.class, permissionHandlers::handlePermission);
        registerHandler(Packet.SessionChallengePacket.class, authenticationHandlers::handleSessionChallenge);
        registerHandler(Packet.PrivateMsgPacket.class, directMessageHandlers::handlePrivateMsg);
    }

    private <T extends Packet> void registerHandler(Class<T> packetClass, Consumer<T> handler) {
        handlers.put(packetClass, packet -> handler.accept(packetClass.cast(packet)));
    }

    // --- ChatClient Overrides ---

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        LOGGER.debug("Connected to server: {}", getURI());
    }

    @Override
    public void onMessage(String message) {
        LOGGER.debug("Received packet: {}", message);

        Packet packet = Packet.fromJson(message);
        if (packet == null) {
            LOGGER.warn("Invalid packet received: {}", message);
            return;
        }

        Consumer<Packet> handler = handlers.get(packet.getClass());
        if (handler == null) {
            LOGGER.warn("Client received unknown Packet class: {}", packet.getClass().getSimpleName());
            return;
        }

        handler.accept(packet);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.debug("Connection closed: {} ({})", reason, code);
        joinedRooms.clear();
        Awooing.getInstance().currentRoomId = null;
        Awooing.getInstance().permissionType = PermissionType.USER;
        setAwooing(false);

        switch (code) {
            case NEVER_CONNECTED -> renderMsg(INFO_COLOR, "Connecting to server failed");
            case NORMAL          -> { }
            case GOING_AWAY      -> renderMsg(INFO_COLOR, "Server is shutting down, disconnecting");
            case REFUSE          -> renderMsg(INFO_COLOR, reason);
            default              -> renderMsg(INFO_COLOR, "Connection lost");
        }
    }

    @Override
    public void onError(Exception e) {
        LOGGER.error("An error occurred", e);
        if (e instanceof ConnectException || e instanceof UnknownHostException) {
            return; // Handled by onClose
        }
        renderMsg(INFO_COLOR, "An error occurred: check your logs for more info");
    }

    // --- ChatClient Utils ---

    public void sendPacket(Packet packet) {
        String json = packet.toJson();
        send(json);
        LOGGER.debug("Sent Packet: {}", json);
    }

    public Set<String> getKnownUsers() {
        return joinedRooms.values().stream()
            .flatMap(room -> room.getUsers().stream())
            .collect(Collectors.toSet());
    }

    public Set<String> getRoomUsers(String roomId) {
        ChatRoom room = roomId == null ? null : joinedRooms.get(roomId);
        if (room == null) {
            return Set.of();
        }

        return room.getUsers().stream().collect(Collectors.toSet());
    }

    public Map<String, ChatRoom> getJoinedRooms() {
        return Collections.unmodifiableMap(joinedRooms);
    }

    public ChatRoom getOrCreateRoom(String roomId) {
        if (roomId == null) {
            return null;
        }
        return joinedRooms.computeIfAbsent(roomId, ChatRoom::new);
    }

    public ChatRoom getRoom(String roomId) {
        return roomId == null ? null : joinedRooms.get(roomId);
    }

    public ChatRoom removeRoom(String roomId) {
        return roomId == null ? null : joinedRooms.remove(roomId);
    }
}