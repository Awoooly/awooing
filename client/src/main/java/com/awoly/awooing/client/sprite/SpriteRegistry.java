package com.awoly.awooing.client.sprite;

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

public class SpriteRegistry {

    private static final ConcurrentMap<String, Sprite> SPRITES_BY_NAME = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, Sprite> SPRITES_BY_ID = new ConcurrentHashMap<>();
    private static final int PUA_START = 0xE800;
    private static final AtomicInteger NEXT_ID = new AtomicInteger(PUA_START);
    private static final String RESOURCE_PATH = "/assets/awooing/textures/assets/";
    private static final String LIST_FILE = "assets.txt";

    public static void initialize() {
        MinecraftClient.getInstance().execute(() -> {
            String listPath = RESOURCE_PATH + LIST_FILE;

            try (InputStream listStream = SpriteRegistry.class.getResourceAsStream(listPath)) {
                if (listStream == null) {
                    Awooing.LOGGER.info("No emoji list found at {}", listPath);
                    return;
                }

                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(listStream, StandardCharsets.UTF_8))) {
                    String line;
                    int spritesLoaded = 0;

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) {
                            continue;
                        }

                        // Ignore legacy section headers from the old split list format.
                        if (line.startsWith("[") && line.endsWith("]")) {
                            continue;
                        }

                        if (registerFromResourceName(line, SpriteSource.EMOJI)) {
                            spritesLoaded++;
                        }
                    }
                    Awooing.LOGGER.info(
                            "Registered {} sprite entries ({} total named sprites)",
                            spritesLoaded,
                            SPRITES_BY_NAME.size());
                }
            } catch (Exception e) {
                Awooing.LOGGER.warn("Failed to load emoji list {}", listPath, e);
            }
        });
    }

    public static void registerFromStream(String name, InputStream stream) {
        registerFromStream(name, stream, SpriteSource.EMOJI);
    }

    private static void registerFromStream(String name, InputStream stream, SpriteSource source) {
        try {
            if (source == SpriteSource.EMOJI && SPRITES_BY_NAME.containsKey(name)) {
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
                if (id > 0xF8FF) {
                    Awooing.LOGGER.warn("Emoji ID overflow (id={}), cannot register {}", id, name);
                    return;
                }
                Sprite emoji = Sprite.emoji(name, id, texture);

                Sprite prev = SPRITES_BY_NAME.putIfAbsent(name, emoji);
                if (prev == null) {
                    Sprite existingById = SPRITES_BY_ID.putIfAbsent(id, emoji);
                    if (existingById != null) {
                        SPRITES_BY_NAME.remove(name, emoji);
                        Awooing.LOGGER.warn("Sprite codepoint collision (id={}) while registering {}", id, name);
                    }
                } else {
                    Awooing.LOGGER.info("Emoji {} already registered by another thread, skipping", name);
                }
            }
        } catch (Exception e) {
            Awooing.LOGGER.warn("Error registering emoji {}: {}", name, e.getMessage());
            e.printStackTrace();
        }
    }

    public static Sprite registerClientAsset(float height, String texturePath) {
        while (true) {
            int id = NEXT_ID.getAndIncrement();
            if (id > 0xF8FF) {
                throw new IllegalStateException("Sprite ID overflow while registering client asset " + texturePath);
            }

            Sprite sprite = Sprite.asset(id, height, texturePath);
            Sprite previous = SPRITES_BY_ID.putIfAbsent(id, sprite);
            if (previous == null) {
                return sprite;
            }
        }
    }

    private static boolean registerFromResourceName(String name, SpriteSource source) {
        String path = RESOURCE_PATH + name + ".png";
        try (InputStream is = SpriteRegistry.class.getResourceAsStream(path)) {
            if (is == null) {
                Awooing.LOGGER.warn("Sprite resource not found: {}", path);
                return false;
            }
            registerFromStream(name, is, source);
            return true;
        } catch (Exception e) {
            Awooing.LOGGER.warn("Failed to load sprite {} from {}", name, path, e);
            return false;
        }
    }

    public static Sprite getByName(String token) {
        return SPRITES_BY_NAME.get(token);
    }

    public static Sprite getById(int id) {
        return SPRITES_BY_ID.get(id);
    }

    public static Map<String, Sprite> getAll() {
        return Collections.unmodifiableMap(SPRITES_BY_NAME);
    }
}
