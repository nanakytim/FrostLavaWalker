package net.nanaky.frost_lava_walker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.nanaky.frost_lava_walker.config.ServerConfigManager;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket;
import net.nanaky.frost_lava_walker.util.BlockConversionScheduler;
import net.nanaky.frost_lava_walker.util.LavaWalkerLogic;

public class LavaWalkerMod implements ModInitializer {
    public static final String MOD_ID = "frost_lava_walker";
    @Override
    public void onInitialize() {
        ServerConfigManager.load();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                LavaWalkerLogic.recoverFromCrash(level);
            }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) ServerConfigManager.load();
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerLevel level : server.getAllLevels()) {
                BlockConversionScheduler.tick(level);
            }
        });
        SpawnParticlePacket.register();
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (!(world instanceof ServerLevel level)) return;
            if (!BlockConversionScheduler.isTracked(pos, level)) return;
            BlockConversionScheduler.cancel(pos, level);
            level.setBlock(pos, Blocks.LAVA.defaultBlockState(), 3);
            LavaWalkerLogic.killDroppedItems(level, pos);
        });
    }
}