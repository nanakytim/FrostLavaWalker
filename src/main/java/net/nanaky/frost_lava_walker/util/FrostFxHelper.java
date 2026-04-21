package net.nanaky.frost_lava_walker.util;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket;
import net.nanaky.frost_lava_walker.network.SpawnParticlePacket.EventType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class FrostFXHelper {

    private static final int FREEZE_DETECT_WINDOW = 2;
    private static final Map<Integer, Map<BlockPos, Long>> waterTimestamps = new HashMap<>();
    private static final Map<ResourceKey<Level>, Set<BlockPos>> trackedIce  = new HashMap<>();

    public static void onEntityStep(ServerLevel level, LivingEntity entity) {
        int enchantLevel;
        try {
            enchantLevel = EnchantmentHelper.getEnchantmentLevel(
                level.registryAccess()
                    .lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FROST_WALKER),
                entity
            );
        } catch (Exception e) {
            System.err.println("[FrostLavaWalker] enchant lookup failed: " + e);
            return;
        }

        if (enchantLevel <= 0) {
            waterTimestamps.remove(entity.getId());
            return;
        }

        long now      = level.getGameTime();
        BlockPos center = entity.blockPosition();
        BlockPos floor  = entity.getOnPos();
        int radius    = 2 + enchantLevel;

        Map<BlockPos, Long> timestamps = waterTimestamps.computeIfAbsent(
            entity.getId(), k -> new HashMap<>()
        );

        for (BlockPos candidate : BlockPos.betweenClosed(
                new BlockPos(center.getX() - radius, floor.getY(), center.getZ() - radius),
                new BlockPos(center.getX() + radius, floor.getY(), center.getZ() + radius))) {

            if (center.distSqr(candidate) > (radius + 0.5) * (radius + 0.5)) continue;

            BlockPos immutable = candidate.immutable();

            if (level.getFluidState(immutable).is(Fluids.WATER)
                    && level.getFluidState(immutable).isSource()) {
                timestamps.put(immutable, now);

            } else if (level.getBlockState(immutable).is(Blocks.FROSTED_ICE)) {
                Long seenAt = timestamps.get(immutable);
                if (seenAt != null && (now - seenAt) <= FREEZE_DETECT_WINDOW) {
                    trackedIce.computeIfAbsent(level.dimension(), k -> new HashSet<>())
                              .add(immutable);
                    sendVFX(level, immutable, EventType.ICE_FREEZE);
                    timestamps.remove(immutable);
                }
            }
        }

        Iterator<Map.Entry<BlockPos, Long>> it = timestamps.entrySet().iterator();
        while (it.hasNext()) {
            if (now - it.next().getValue() > FREEZE_DETECT_WINDOW) it.remove();
        }
    }

    public static void tickTracked(ServerLevel level) {
        Set<BlockPos> tracked = trackedIce.get(level.dimension());
        if (tracked == null || tracked.isEmpty()) return;

        Iterator<BlockPos> it = tracked.iterator();
        while (it.hasNext()) {
            BlockPos pos = it.next();
            if (!level.getBlockState(pos).is(Blocks.FROSTED_ICE)) {
                sendVFX(level, pos, EventType.ICE_BREAK);
                it.remove();
            }
        }
    }

    public static void onEntityRemoved(LivingEntity entity) {
        waterTimestamps.remove(entity.getId());
    }

    private static void sendVFX(ServerLevel level, BlockPos pos, EventType event) {
        Vec3 center = Vec3.atCenterOf(pos);
        SpawnParticlePacket packet = new SpawnParticlePacket(center.x, center.y, center.z, event);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) < 64 * 64) {
                ServerPlayNetworking.send(player, packet);
            }
        }
    }
}