package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.getVersion;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.versionToInt;
import static com.awoly.awooing.client.config.ConfigManager.config;

import com.awoly.awooing.client.ChatClient;
import com.awoly.awooing.common.Packet;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import org.java_websocket.framing.CloseFrame;

public final class AuthenticationHandlers {

    private static final Pattern SERVER_ID_PATTERN = Pattern.compile("^awoo[0-9a-f]{8}$");
    private final ChatClient chatClient;
    private final int protocolVersion;

    public AuthenticationHandlers(ChatClient chatClient, int protocolVersion) {
        this.chatClient = chatClient;
        this.protocolVersion = protocolVersion;
    }

    public void handleSessionChallenge(Packet.SessionChallengePacket packet) {
        if (packet.serverId() == null || !SERVER_ID_PATTERN.matcher(packet.serverId()).matches()) {
            failAuthentication("Server sent invalid auth challenge");
            return;
        }

        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Session session = minecraftClient.getSession();

        String username = getUsername();
        if (username == null || username.isBlank()) {
            failAuthentication("Missing Minecraft username");
            return;
        }

        boolean canJoinServer = session != null
            && minecraftClient.getApiServices() != null
            && minecraftClient.getApiServices().sessionService() != null;

        if (canJoinServer) {
            String accessToken = session.getAccessToken();
            UUID uuid = session.getUuidOrNull();

            if (accessToken != null && !accessToken.isBlank() && uuid != null) {
                try {
                    minecraftClient.getApiServices().sessionService().joinServer(uuid, accessToken, packet.serverId());
                } catch (Exception e) {
                    renderMsg(INFO_COLOR, "Auth token rejected; continuing without Mojang auth");
                }
            } else {
                renderMsg(INFO_COLOR, "Missing auth token/profile; continuing without Mojang auth");
            }
        } else {
            renderMsg(INFO_COLOR, "Missing session service; continuing without Mojang auth");
        }

        int clientVersion;
        try {
            clientVersion = versionToInt(getVersion());
        } catch (RuntimeException e) {
            clientVersion = 0;
        }

        chatClient.sendPacket(Packet.authResponse(username, config.userColor, protocolVersion, clientVersion));
    }

    private void failAuthentication(String reason) {
        if (chatClient.isOpen()) {
            chatClient.close(CloseFrame.REFUSE, reason);
            return;
        }

        renderMsg(INFO_COLOR, reason);
    }
}
