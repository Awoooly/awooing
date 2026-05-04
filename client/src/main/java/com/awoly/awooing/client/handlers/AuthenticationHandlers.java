package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.canDisplayMessage;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.getVersion;
import static com.awoly.awooing.client.Utils.prepareCmd;
import static com.awoly.awooing.client.Utils.renderMsg;
import static com.awoly.awooing.client.Utils.text;
import static com.awoly.awooing.client.Utils.versionToInt;
import static com.awoly.awooing.client.config.ConfigManager.config;

import com.awoly.awooing.client.ChatClient;
import com.awoly.awooing.client.config.ConfigManager;
import com.awoly.awooing.common.Packet;
import java.util.UUID;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;
import net.minecraft.text.MutableText;
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
                    if (canDisplayMessage()) {
                        renderMsg(INFO_COLOR, "Auth token rejected; continuing without Mojang auth");
                    }
                }
            } else {
                if (canDisplayMessage()) {
                    renderMsg(INFO_COLOR, "Missing auth token/profile; continuing without Mojang auth");
                }
            }
        } else {
            if (canDisplayMessage()) {
                renderMsg(INFO_COLOR, "Missing session service; continuing without Mojang auth");
            }
        }

        chatClient.sendPacket(Packet.authResponse(username, config.userColor, protocolVersion, versionToInt(getVersion())));
    }

    public void handleConnected(Packet.ConnectedPacket packet) {
        if (canDisplayMessage()) {
            renderMsg(INFO_COLOR, "Connected successfully");
        }
        
        if (config.showedConnectedHint) {
            return;
        }

        MinecraftClient.getInstance().execute(() -> {
            String welcome = config.autoConnect ? " Welcome to Awooing, " + getUsername() + "! " : "";

            MutableText welcomeText = text(welcome)
                .append(text("Start with "))
                .append(prepareCmd("/awoo join", "/awoo join "))
                .append(text(" <room> to join a room or "))
                .append(prepareCmd("/awoo create", "/awoo create "))
                .append(text(" <name> to host one yourself. Use "))
                .append(prepareCmd("/amsg", "/amsg "))
                .append(text(" <user> <message> to send direct messages, and "))
                .append(prepareCmd("/awoo publicrooms", "/awoo publicrooms"))
                .append(text(" to browse public rooms."));

            renderMsg(INFO_COLOR, welcomeText);
            config.showedConnectedHint = true;
            ConfigManager.save();
        });
    }

    private void failAuthentication(String reason) {
        if (chatClient.isOpen()) {
            chatClient.close(CloseFrame.REFUSE, reason);
            return;
        }

        renderMsg(INFO_COLOR, reason);
    }
}
