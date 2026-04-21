package net.nanaky.frost_lava_walker.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;
import net.nanaky.frost_lava_walker.config.ClientConfig;
import net.nanaky.frost_lava_walker.config.ClientConfigManager;
import net.nanaky.frost_lava_walker.config.ServerConfigManager;
import net.nanaky.frost_lava_walker.config.ServerConfig;

public class ClothConfigScreenFactory {

    public static ConfigScreenFactory<?> create() {
    return parent -> {
        ServerConfig cfg = ServerConfigManager.INSTANCE;
        ServerConfig def = new ServerConfig();
        ClientConfig clientCfg = ClientConfigManager.INSTANCE;
        ClientConfig clientDef = new ClientConfig();

        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("config.frost_lava_walker.title"))
            .setSavingRunnable(() -> {
                ServerConfigManager.save();
                ClientConfigManager.save();
            });

        ConfigEntryBuilder eb = builder.entryBuilder();

        ConfigCategory client = builder.getOrCreateCategory(
            Component.translatable("config.frost_lava_walker.category.client"));

        client.addEntry(eb.startBooleanToggle(
                Component.translatable("config.frost_lava_walker.show_particles"), clientCfg.showParticles)
            .setDefaultValue(clientDef.showParticles)
            .setTooltip(Component.translatable("config.frost_lava_walker.show_particles.tooltip"))
            .setSaveConsumer(v -> clientCfg.showParticles = v)
            .build());

        client.addEntry(eb.startBooleanToggle(
                Component.translatable("config.frost_lava_walker.play_sound_effects"), clientCfg.playSoundEffects)
            .setDefaultValue(clientDef.playSoundEffects)
            .setTooltip(Component.translatable("config.frost_lava_walker.play_sound.tooltip"))
            .setSaveConsumer(v -> clientCfg.playSoundEffects = v)
            .build());

            ConfigCategory server = builder.getOrCreateCategory(
                Component.translatable("config.frost_lava_walker.category.server"));

            server.addEntry(eb.startTextDescription(
                Component.translatable("config.frost_lava_walker.server_warning"))
            .build());

            server.addEntry(eb.startBooleanToggle(
                    Component.translatable("config.frost_lava_walker.lava_walker_enabled"), cfg.lavaWalkerEnabled)
                .setDefaultValue(def.lavaWalkerEnabled)
                .setTooltip(Component.translatable("config.frost_lava_walker.lava_walker_enabled.tooltip"))
                .setSaveConsumer(v -> cfg.lavaWalkerEnabled = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.base_radius"), cfg.baseRadius, 0, 9)
                .setDefaultValue(def.baseRadius)
                .setTooltip(Component.translatable("config.frost_lava_walker.base_radius.tooltip"))
                .setSaveConsumer(v -> cfg.baseRadius = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.gilded_initial_ticks"), cfg.gildedInitialTicks, 1, 40)
                .setDefaultValue(def.gildedInitialTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.gilded_initial_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.gildedInitialTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.blackstone_main_ticks"), cfg.blackstoneMainTicks, 1, 200)
                .setDefaultValue(def.blackstoneMainTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.blackstone_main_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.blackstoneMainTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.gilded_warning_ticks"), cfg.gildedWarningTicks, 1, 40)
                .setDefaultValue(def.gildedWarningTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.gilded_warning_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.gildedWarningTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.magma_short_ticks"), cfg.magmaShortTicks, 1, 40)
                .setDefaultValue(def.magmaShortTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.magma_short_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.magmaShortTicks = v)
                .build());

            server.addEntry(eb.startIntSlider(
                    Component.translatable("config.frost_lava_walker.cooldown_extra_ticks"), cfg.cooldownExtraTicks, 0, 200)
                .setDefaultValue(def.cooldownExtraTicks)
                .setTooltip(Component.translatable("config.frost_lava_walker.cooldown_extra_ticks.tooltip"))
                .setSaveConsumer(v -> cfg.cooldownExtraTicks = v)
                .build());

            return builder.build();
        };
    }
}