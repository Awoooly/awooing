package com.awoly.awooing.client.config;

import static com.awoly.awooing.client.Awooing.LOGGER;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.fabricmc.loader.api.FabricLoader;

public class ConfigManager {
    public static final ModConfig config = new ModConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "awooing.json");

    public static void load() {
        if (!FILE.exists()) {
            save();
            return;
        }

        try (Reader r = Files.newBufferedReader(FILE.toPath(), StandardCharsets.UTF_8)) {
            ModConfig loaded = GSON.fromJson(r, ModConfig.class);
            if (loaded != null) {
                config.lastIp = loaded.lastIp;
                config.lastPort = loaded.lastPort;
                config.autoConnect = loaded.autoConnect;
                config.userColor = loaded.userColor;
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load config, using defaults", e);
        }

        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(FILE.toPath(), StandardCharsets.UTF_8)) {
            GSON.toJson(config, w);
        } catch (Exception e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
