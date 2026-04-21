package net.nanaky.frost_lava_walker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.Path;

public class ClientConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
        FabricLoader.getInstance().getConfigDir().resolve("frostlavawalker_client.json");
    public static ClientConfig INSTANCE = new ClientConfig();

    public static void load() {
        if (FabricLoader.getInstance().getEnvironmentType() 
                != net.fabricmc.api.EnvType.CLIENT) return;

        if (!PATH.toFile().exists()) { save(); return; }
        try (Reader r = new FileReader(PATH.toFile())) {
            ClientConfig loaded = GSON.fromJson(r, ClientConfig.class);
            if (loaded != null) {
                INSTANCE.showParticles    = loaded.showParticles;
                INSTANCE.playSoundEffects = loaded.playSoundEffects;
            }
        } catch (Exception e) {
            System.err.println("[LavaWalker] Failed to load client config: " + e);
        }
    }

    public static void save() {
        if (FabricLoader.getInstance().getEnvironmentType() 
                != net.fabricmc.api.EnvType.CLIENT) return;
        try (Writer w = new FileWriter(PATH.toFile())) {
            GSON.toJson(INSTANCE, w);
        } catch (Exception e) {
            System.err.println("[LavaWalker] Failed to save client config: " + e);
        }
    }
}