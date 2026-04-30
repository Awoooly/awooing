package com.awoly.awooing.client.handlers;

import static com.awoly.awooing.client.Utils.INFO_COLOR;
import static com.awoly.awooing.client.Utils.WHITE;
import static com.awoly.awooing.client.Utils.getUsername;
import static com.awoly.awooing.client.Utils.getVersion;
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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
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
                    renderMsg(INFO_COLOR, "Auth token rejected; continuing without Mojang auth");
                }
            } else {
                renderMsg(INFO_COLOR, "Missing auth token/profile; continuing without Mojang auth");
            }
        } else {
            renderMsg(INFO_COLOR, "Missing session service; continuing without Mojang auth");
        }

        chatClient.sendPacket(Packet.authResponse(username, config.userColor, protocolVersion, versionToInt(getVersion())));
    }

    public void handleConnected(Packet.ConnectedPacket packet) {
        renderMsg(INFO_COLOR, "Connected successfully");
        
        if (config.showedConnectedHint) {
            return;
        }

        MinecraftClient.getInstance().execute(() -> {
            String welcome = config.autoConnect ? " Welcome to Awooing, " + getUsername() + "! " : "";

            MutableText welcomeText = text(welcome)
                .append(text("Start with "))
                .append(text("/awoo join", WHITE).styled(style -> style
                    .withClickEvent(new ClickEvent.SuggestCommand("/awoo join "))
                    .withHoverEvent(new HoverEvent.ShowText(text("Click to prepare command")))))
                .append(text(" <room> to join a room or "))
                .append(text("/awoo create ", WHITE).styled(style -> style
                    .withClickEvent(new ClickEvent.SuggestCommand("/awoo create "))
                    .withHoverEvent(new HoverEvent.ShowText(text("Click to prepare command")))))
                .append(text(" <name> to host one yourself. Use "))
                .append(text("/amsg", WHITE).styled(style -> style
                    .withClickEvent(new ClickEvent.SuggestCommand("/amsg "))
                    .withHoverEvent(new HoverEvent.ShowText(text("Click to prepare command")))))
                .append(text(" <user> <message> to send direct messages, and "))
                .append(text("/awoo publicrooms", WHITE).styled(style -> style
                    .withClickEvent(new ClickEvent.SuggestCommand("/awoo publicrooms"))
                    .withHoverEvent(new HoverEvent.ShowText(text("Click to prepare command")))))
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
