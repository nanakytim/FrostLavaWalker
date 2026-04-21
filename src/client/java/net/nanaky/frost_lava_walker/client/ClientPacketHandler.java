package net.nanaky.frost_lava_walker.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.nanaky.frost_lava_walker.config.ClientConfigManager;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket;
import net.nanaky.frost_lava_walker.particle.LavaWalkerParticles;

public class ClientPacketHandler {

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SpawnParticlePacket.TYPE, (payload, ctx) -> {
            ctx.client().execute(() -> handle(payload, ctx.client()));
        });
    }

    private static void handle(SpawnParticlePacket payload, Minecraft client) {
        if (client.level == null) return;

        double x = payload.x();
        double y = payload.y();
        double z = payload.z();

        boolean particles = ClientConfigManager.INSTANCE.showParticles;
        boolean sounds    = ClientConfigManager.INSTANCE.playSoundEffects;

        switch (payload.event()) {

            case LAVA_CONVERSION -> {
                if (particles) {
                    spawnParticles(client, ParticleTypes.WHITE_SMOKE,
                            x, y + 0.5, z,  3,  0.2, 0.1, 0.2, 0.01);
                    spawnParticles(client, ParticleTypes.SMOKE,
                            x, y + 0.25, z, 1,  0.1, 0.05, 0.1, 0.05);
                }
                if (sounds) {
                    client.level.playLocalSound(x, y, z,
                            SoundEvents.MAGMA_CUBE_DEATH, SoundSource.BLOCKS, 0.5f, 0.5f, false);
                }
            }

            case LAVA_SETTLE -> {
                if (particles) {
                    spawnParticles(client, ParticleTypes.SMOKE,
                            x, y + 1.0, z,  4,  0.2, 0.05, 0.2, 0.01);
                }
            }

            case LAVA_WARN -> {
                if (particles) {
                    spawnParticles(client, LavaWalkerParticles.LAVA_POP,
                            x, y + 0.5, z,  4,  0.2, 0.1, 0.2, 0.02);
                    spawnParticles(client, ParticleTypes.SMALL_FLAME,
                            x, y + 0.25, z, 1,  0.1, 0.05, 0.1, 0.01);
                }
            }

            case LAVA_REVERT -> {
                if (particles) {
                    spawnParticles(client, ParticleTypes.SMALL_FLAME,
                            x, y + 0.5, z,  2,  0.1, 0.05, 0.1, 0.01);
                    spawnParticles(client, ParticleTypes.SMOKE,
                            x, y + 0.5, z, 15,  0.2, 0.1,  0.2, 0.05);
                }
                if (sounds) {
                    client.level.playLocalSound(x, y, z,
                            SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.2f, 0.9f, false);
                }
            }

            case ICE_FREEZE -> {
                if (particles) {
                    spawnParticles(client, ParticleTypes.SNOWFLAKE,
                            x, y + 0.4, z,  3,  0.3, 0.05, 0.3, 0.05);
                }
                if (sounds) {
                    client.level.playLocalSound(x, y, z,
                            SoundEvents.SNOW_GOLEM_HURT, SoundSource.BLOCKS, 0.15f, 1.0f, false);
                }
            }

            case ICE_BREAK -> {
                if (particles) {
                    spawnParticles(client,
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.ICE.defaultBlockState()),
                            x, y + 0.1, z, 12,  0.4, 0.1, 0.4, 0.05);
                    spawnParticles(client, ParticleTypes.SNOWFLAKE,
                            x, y - 0.1, z,  3,  0.3, 0.15, 0.3, 0.04);
                }
                if (sounds) {
                    client.level.playLocalSound(x, y, z,
                            SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 0.2f,
                            0.85f + client.level.getRandom().nextFloat() * 0.3f, false);
                }
            }
        }
    }

    private static <T extends ParticleOptions> void spawnParticles(
            Minecraft client, T type,
            double x, double y, double z,
            int count,
            double offX, double offY, double offZ,
            double speed) {

        for (int i = 0; i < count; i++) {
            double px = x + (client.level.getRandom().nextDouble() * 2 - 1) * offX;
            double py = y + (client.level.getRandom().nextDouble() * 2 - 1) * offY;
            double pz = z + (client.level.getRandom().nextDouble() * 2 - 1) * offZ;

            double vx = (client.level.getRandom().nextDouble() * 2 - 1) * speed;
            double vy = (client.level.getRandom().nextDouble() * 2 - 1) * speed;
            double vz = (client.level.getRandom().nextDouble() * 2 - 1) * speed;

            client.level.addParticle(type, px, py, pz, vx, vy, vz);
        }
    }
}