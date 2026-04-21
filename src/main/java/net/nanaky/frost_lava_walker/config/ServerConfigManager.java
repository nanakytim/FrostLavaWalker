package net.nanaky.frost_lava_walker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ServerConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("frostlavawalker_server.json");

    public static ServerConfig INSTANCE = new ServerConfig();

    public static void load() {

        if (!CONFIG_PATH.toFile().exists()) {
            save();
            return;
        }
        try (Reader r = new FileReader(CONFIG_PATH.toFile())) {
            ServerConfig loaded = GSON.fromJson(r, ServerConfig.class);
            if (loaded != null) {
                INSTANCE.lavaWalkerEnabled   = loaded.lavaWalkerEnabled;
                INSTANCE.baseRadius          = loaded.baseRadius;
                INSTANCE.gildedInitialTicks  = loaded.gildedInitialTicks;
                INSTANCE.blackstoneMainTicks = loaded.blackstoneMainTicks;
                INSTANCE.gildedWarningTicks  = loaded.gildedWarningTicks;
                INSTANCE.magmaShortTicks     = loaded.magmaShortTicks;
                INSTANCE.cooldownExtraTicks  = loaded.cooldownExtraTicks;
            }
        } catch (Exception e) {
            System.err.println("[LavaWalker] Failed to load config: " + e);
        }
    }

    public static void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(INSTANCE, w);
        } catch (Exception e) {
            System.err.println("[LavaWalker] Failed to save config: " + e);
        }
    }
}