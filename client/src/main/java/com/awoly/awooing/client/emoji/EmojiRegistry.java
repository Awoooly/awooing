package com.awoly.awooing.client.emoji;

import com.awoly.awooing.client.Awooing;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;

public class EmojiRegistry {

    private static final ConcurrentMap<String, Emoji> EMOJI_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, Emoji> EMOJI_BY_ID = new ConcurrentHashMap<>();
    private static final int PUA_START = 0xE800;
    private static final AtomicInteger NEXT_ID = new AtomicInteger(PUA_START);
    private static final String EMOJI_RESOURCE_PATH = "/assets/awooing/textures/emojis/";
    private static final String EMOJI_LIST = "emojis.txt";

    public static void initialize() {
        MinecraftClient.getInstance().execute(() -> {
            String listPath = EMOJI_RESOURCE_PATH + EMOJI_LIST;

            try (InputStream listStream = EmojiRegistry.class.getResourceAsStream(listPath)) {
                if (listStream == null) {
                    Awooing.LOGGER.info("No emoji list found at {}", listPath);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(listStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            continue;
                        }

                        registerFromResourceName(line);
                    }
                    Awooing.LOGGER.info("Registered {} emojis", EMOJI_MAP.size());
                }
            } catch (Exception e) {
                Awooing.LOGGER.warn("Failed to load emoji list {}", listPath, e);
            }
        });
    }

    public static void registerFromStream(String name, InputStream stream) {
        try {
            if (EMOJI_MAP.containsKey(name)) {
                Awooing.LOGGER.info("Emoji {} already registered, skipping", name);
                return;
            }

            byte[] data = stream.readAllBytes();
            try (NativeImage image = NativeImage.read(new ByteArrayInputStream(data))) {
                if (image == null) {
                    Awooing.LOGGER.warn("Emoji resource not found");
                    return;
                }
                NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "emoji:" + name, image);

                int id = NEXT_ID.getAndIncrement();
                // Ensure we don't exceed BMP PUA (U+E000..U+F8FF)
                if (id > 0xF8FF) {
                    Awooing.LOGGER.warn("Emoji ID overflow (id={}), cannot register {}", id, name);
                    return;
                }
                Emoji emoji = new Emoji(name, id, texture);

                Emoji prev = EMOJI_MAP.putIfAbsent(name, emoji);
                if (prev == null) {
                    EMOJI_BY_ID.put(id, emoji);
                } else {
                    Awooing.LOGGER.info("Emoji {} already registered by another thread, skipping", name);
                }
            }
        } catch (Exception e) {
            Awooing.LOGGER.warn("Error registering emoji {}: {}", name, e.getMessage());
            e.printStackTrace();
        }
    }

    private static void registerFromResourceName(String name) {
        String path = EMOJI_RESOURCE_PATH + name + ".png";
        try (InputStream is = EmojiRegistry.class.getResourceAsStream(path)) {
            if (is == null) {
                Awooing.LOGGER.warn("Emoji resource not found: {}", path);
                return;
            }
            registerFromStream(name, is);
        } catch (Exception e) {
            Awooing.LOGGER.warn("Failed to load emoji {} from {}", name, path, e);
        }
    }

    public static Emoji get(String token) {
        return EMOJI_MAP.get(token);
    }

    public static Emoji getById(int id) {
        return EMOJI_BY_ID.get(id);
    }

    public static Map<String, Emoji> getAll() {
        return Collections.unmodifiableMap(EMOJI_MAP);
    }
}
