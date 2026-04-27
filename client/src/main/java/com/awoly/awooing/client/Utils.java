package com.awoly.awooing.client;

import static com.awoly.awooing.client.Awooing.LOGGER;
import static com.awoly.awooing.client.Awooing.getInstance;
// joinedRooms accessed via Awooing.getInstance().chatClient.getJoinedRooms()

import com.awoly.awooing.common.RoomAccessMode;
import com.awoly.awooing.common.PermissionType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class Utils {
    public static final int INFO_COLOR = 0xFCFC00;
    public static final int CHAT_COLOR = 0xA8A9FB;
    public static final int PRIVATE_COLOR = 0xFC7DFC;
    public static final int WARN_COLOR = 0xFC465C;
    public static final int WHITE = 0xFFFFFF;
    public static final Font SPRITE_FONT = new StyleSpriteSource.Font(Identifier.of("awooing", "emoji_font"));
    public static final Font SPRITE_GLYPH_FONT = new StyleSpriteSource.Font(
            Identifier.of("awooing", "emoji_glyph_font"));
    public static final Pattern EMOJI_PATTERN = Pattern.compile(":([a-z0-9_]+):");

    private static final String[] AWOO_COMMAND_PREFIXES = new String[]{"/a ", "/amsg ", "/ar ", "/f a "};

    public static void renderMsg(String prefix, TextColor color, Text message) {
        MinecraftClient.getInstance().execute(() -> {
            String normalizedPrefix = (prefix == null || prefix.isBlank()) ? null : prefix;
            if (MinecraftClient.getInstance().player == null) {
                if (normalizedPrefix == null) {
                    LOGGER.info("{}", message.getString());
                } else {
                    LOGGER.info("[{}] {}", normalizedPrefix, message.getString());
                }
                return;
            }

            Text transformed = normalizedPrefix == null
                ? message.copy().setStyle(message.getStyle().withColor(color))
                : Text.literal("[" + normalizedPrefix + "] ")
                    .setStyle(Style.EMPTY.withFont(SPRITE_FONT).withColor(color))
                    .append(message);

            MinecraftClient.getInstance().player.sendMessage(transformed, false);
        });
    }

    public static void renderMsg(String prefix, int color, String message) {
        renderMsg(prefix, TextColor.fromRgb(color), text(message));
    }

    public static void renderMsg(String prefix, int color, Text message) {
        renderMsg(prefix, TextColor.fromRgb(color), message);
    }

    public static void renderMsg(int color, String message) {
        renderMsg("Awoo", TextColor.fromRgb(color), text(message));
    }

    public static void renderMsg(int color, Text text) {
        renderMsg("Awoo", TextColor.fromRgb(color), text);
    }

    public static void configureSsl(ChatClient client, String truststorePath, String truststorePassword) throws Exception {
        try (InputStream stream = Optional
            .ofNullable(ChatClient.class.getResourceAsStream("/" + truststorePath))
            .orElseGet(() -> ChatClient.class.getClassLoader().getResourceAsStream(truststorePath))) {

            if (stream == null) {
                throw new IllegalArgumentException("Truststore file not found: " + truststorePath);
            }

            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(stream, truststorePassword.toCharArray());

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            client.setSocketFactory(sslContext.getSocketFactory());
            LOGGER.info("SSL/TLS configured successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SSL", e);
        }
    }

    public static int showUsage(String usage) {
        renderMsg(INFO_COLOR, "Usage: " + usage);
        return Command.SINGLE_SUCCESS;
    }

    public static boolean isClientConnected() {
        return getInstance().chatClient != null && getInstance().chatClient.isOpen();
    }

    public static boolean isAdmin() {
        return getInstance().permissionType == PermissionType.ADMIN;
    }

    public static boolean isLeaderInAnyJoinedRoom() {
        ChatClient client = getInstance().chatClient;
        if (client == null) {
            return false;
        }
        return client.getJoinedRooms().values().stream().anyMatch(ChatRoom::isLeader);
    }

    public static List<String> getLedRoomIds() {
        ChatClient client = getInstance().chatClient;
        if (client == null) {
            return List.of();
        }
        return client.getJoinedRooms().values().stream()
                .filter(ChatRoom::isLeader)
                .map(ChatRoom::getName)
                .toList();
    }

    public static void setAwooing(boolean awooing) {
        setAwooing(awooing, null);
    }

    public static void setAwooing(boolean awooing, String roomId) {
        String resolvedRoomId = roomId != null ? roomId : getActiveRoomId();

        if (awooing && resolvedRoomId == null) {
            return;
        }

        if (getInstance().isAwooing == awooing && (!awooing || Objects.equals(getInstance().currentRoomId, resolvedRoomId))) {
            return;
        }

        if (awooing) {
            getInstance().currentRoomId = resolvedRoomId;
        }

        getInstance().isAwooing = awooing;
        if (awooing) {
            renderMsg(INFO_COLOR, "You are now awooing in room " + resolvedRoomId);
        } else {
            renderMsg(INFO_COLOR, "You are no longer awooing");
        }
    }

    public static String getActiveRoomId() {
        ChatClient client = getInstance().chatClient;
        if (getInstance().currentRoomId != null && client != null && client.getJoinedRooms().containsKey(getInstance().currentRoomId)) {
            return getInstance().currentRoomId;
        }

        List<String> joinedRoomIds = client == null ? List.of() : client.getJoinedRooms().keySet().stream().toList();
        if (joinedRoomIds.size() == 1) {
            return joinedRoomIds.getFirst();
        }

        return null;
    }

    public static String getUsername() {
        return MinecraftClient.getInstance().getSession().getUsername();
    }

    public static MutableText text(String text) {
        return Text.literal(text == null ? "" : text);
    }

    public static MutableText text(String text, int color) {
        return Text.literal(text == null ? "" : text).styled(s -> s.withColor(TextColor.fromRgb(color)));
    }

    public static MutableText text(String text, Formatting color) {
        return Text.literal(text == null ? "" : text).styled(s -> s.withFormatting(color));
    }

    public static SuggestionProvider<FabricClientCommandSource> suggestUsers() {
        return suggestUsers(null);
    }

    public static SuggestionProvider<FabricClientCommandSource> suggestUsers(String roomId) {
        return (ctx, builder) -> {
            ChatClient chatClient = getInstance().chatClient;
            if (chatClient == null) {
                return builder.buildFuture();
            }

            if (roomId != null) {
                return suggestUserNames(builder, getRoomUsers(roomId));
            } else {
                return suggestUserNames(builder, chatClient.getKnownUsers());
            }
        };
    }

    public static SuggestionProvider<FabricClientCommandSource> suggestLedRoomUsers() {
        return (ctx, builder) -> {
            ChatClient chatClient = getInstance().chatClient;
            if (chatClient == null) {
                return builder.buildFuture();
            }

            List<String> ledRoomUsers = chatClient.getJoinedRooms().values().stream()
                    .filter(ChatRoom::isLeader)
                    .flatMap(room -> room.getUsers().stream())
                    .distinct()
                    .toList();

            return suggestUserNames(builder, ledRoomUsers);
        };
    }

    private static Collection<String> getRoomUsers(String roomId) {
        ChatClient client = getInstance().chatClient;
        ChatRoom room = client == null ? null : client.getJoinedRooms().get(roomId);
        return room == null ? List.of() : room.getUsers();
    }

    private static CompletableFuture<Suggestions> suggestUserNames(SuggestionsBuilder builder, Collection<String> names) {
        String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);
        String self = getUsername();

        for (String name : names) {
            if (name.equalsIgnoreCase(self)) {
                continue;
            }

            if (name.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    public static SuggestionProvider<FabricClientCommandSource> suggestRooms() {
        return (ctx, builder) -> {
            ChatClient client = getInstance().chatClient;
            List<String> joinedRoomIds = client == null ? List.of() : client.getJoinedRooms().keySet().stream().toList();
            if (joinedRoomIds.size() <= 1) {
                return builder.buildFuture();
            }

            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

            for (String roomId : joinedRoomIds) {
                if (roomId.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(roomId);
                }
            }

            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<FabricClientCommandSource> suggestLedRooms() {
        return (ctx, builder) -> {
            ChatClient client = getInstance().chatClient;
            List<String> ledRoomIds = client == null ? List.of() : client.getJoinedRooms().values().stream()
                .filter(ChatRoom::isLeader)
                .map(ChatRoom::getName)
                .toList();
            if (ledRoomIds.isEmpty()) {
                return builder.buildFuture();
            }

            String remaining = builder.getRemaining().toLowerCase(Locale.ROOT);

            for (String roomId : ledRoomIds) {
                if (roomId.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                    builder.suggest(roomId);
                }
            }

            return builder.buildFuture();
        };
    }

    public static SuggestionProvider<FabricClientCommandSource> suggestRoomPrivacy() {
        return (ctx, builder) -> {
            for (RoomAccessMode mode : RoomAccessMode.values()) {
                String suggestion = mode.getValue();
                if (builder.getRemaining().isBlank() || suggestion.startsWith(builder.getRemaining().toLowerCase(Locale.ROOT))) {
                    builder.suggest(suggestion);
                }
            }

            return builder.buildFuture();
        };
    }

    public static boolean isEmojiStyled(Style style) {
        return SPRITE_FONT.equals(style.getFont());
    }

    public static boolean isEmojiGlyphStyled(Style style) {
        return SPRITE_GLYPH_FONT.equals(style.getFont());
    }

    public static boolean isTypingAwooMessage(String string) {
        if (string == null) {
            return false;
        }

        String s = string.stripLeading();

        if (getInstance().isAwooing) {
            if (s.startsWith("/")) {
                return Arrays.stream(AWOO_COMMAND_PREFIXES).anyMatch(s::startsWith);
            }
            return true;
        }

        return Arrays.stream(AWOO_COMMAND_PREFIXES).anyMatch(s::startsWith);
    }

    public static boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    public static String getVersion() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return "0";
        } 
        
        return FabricLoader.getInstance()
            .getModContainer("awooing")
            .map(c -> c.getMetadata().getVersion().getFriendlyString())
            .orElse("unknown");
    }

    public static int versionToInt(String version) {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            return 0; 
        }

        String[] parts = version.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
        int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
        return major * 1_000_000 + minor * 1_000 + patch;
    }
}
